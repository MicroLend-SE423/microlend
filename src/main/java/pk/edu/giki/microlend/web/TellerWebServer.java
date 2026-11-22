package pk.edu.giki.microlend.web;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import pk.edu.giki.microlend.common.InvalidLoanIdException;
import pk.edu.giki.microlend.common.InvalidMoneyException;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.delinquency.ClassificationChangeEvent;
import pk.edu.giki.microlend.delinquency.EodReport;
import pk.edu.giki.microlend.ledger.DuplicatePaymentException;
import pk.edu.giki.microlend.ledger.InvalidRepaymentException;
import pk.edu.giki.microlend.ledger.LoanClosedException;
import pk.edu.giki.microlend.ledger.LoanNotFoundException;
import pk.edu.giki.microlend.ledger.PostingResult;
import pk.edu.giki.microlend.ledger.RepaymentCommand;
import pk.edu.giki.microlend.ledger.RepaymentPoster;

/**
 * Minimal local web interface (queue item L-18): a teller posting form and an
 * end-of-day page.
 *
 * <p>Built on the JDK's own {@code com.sun.net.httpserver}, so it needs no
 * dependency. It binds to the loopback interface only and is run from a
 * developer's machine; it is never deployed. Input is validated at this
 * boundary and every value echoed back is HTML-escaped.</p>
 */
public final class TellerWebServer {

    private static final String HTML = "text/html; charset=utf-8";

    /** Requests served at once, so tellers at several counters are not queued. */
    private static final int REQUEST_THREADS = 4;

    private final RepaymentPoster tellerPoster;
    private final Function<LocalDate, EodReport> endOfDay;
    private final OperatorContext operator;
    private final HttpServer server;
    private final ExecutorService requestThreads = Executors.newFixedThreadPool(REQUEST_THREADS);

    /**
     * Creates the server; nothing listens until {@link #start()}.
     *
     * @param tellerPoster the teller chain (Audit -> Timing -> Locking -> Core)
     * @param endOfDay runs end of day for a business date, normally
     *     {@code EndOfDayEngine::run}
     * @param operator the context the audit decorator reads the operator from
     * @param port the port on 127.0.0.1; 0 picks a free one
     * @throws IOException if the port cannot be bound
     * @throws NullPointerException if a collaborator is null
     */
    public TellerWebServer(final RepaymentPoster tellerPoster,
                           final Function<LocalDate, EodReport> endOfDay,
                           final OperatorContext operator, final int port) throws IOException {
        this.tellerPoster = Objects.requireNonNull(tellerPoster, "teller poster is required");
        this.endOfDay = Objects.requireNonNull(endOfDay, "end-of-day runner is required");
        this.operator = Objects.requireNonNull(operator, "operator context is required");
        this.server = HttpServer.create(
            new InetSocketAddress(InetAddress.getLoopbackAddress(), port), 0);
        server.createContext("/", this::route);
        server.setExecutor(requestThreads);
    }

    /** Starts listening. */
    public void start() {
        server.start();
    }

    /** Stops listening at once and releases the request threads. */
    public void stop() {
        server.stop(0);
        requestThreads.shutdownNow();
    }

    /**
     * Returns the address the server listens on.
     *
     * @return for example {@code http://127.0.0.1:8080}
     */
    public String baseUrl() {
        final InetSocketAddress address = server.getAddress();
        return "http://" + address.getAddress().getHostAddress() + ":" + address.getPort();
    }

    private void route(final HttpExchange exchange) throws IOException {
        final String path = exchange.getRequestURI().getPath();
        final String method = exchange.getRequestMethod();
        if ("/".equals(path)) {
            respond(exchange, "GET".equals(method) ? 200 : 405, formPage());
        } else if ("/repayments".equals(path)) {
            if ("POST".equals(method)) {
                postRepayment(exchange);
            } else {
                respond(exchange, 405, page("Method not allowed", "<p>Use the form.</p>"));
            }
        } else if ("/end-of-day".equals(path)) {
            endOfDay(exchange, method);
        } else {
            respond(exchange, 404, page("Not found", "<p>No such page.</p>"));
        }
    }

    private void postRepayment(final HttpExchange exchange) throws IOException {
        final Map<String, String> form = readForm(exchange);
        final RepaymentCommand command;
        try {
            command = RepaymentCommand.cash(LoanId.of(form.getOrDefault("loanId", "")),
                Money.of(new BigDecimal(form.getOrDefault("amount", "").trim())),
                LocalDate.parse(form.getOrDefault("businessDate", "").trim()));
        } catch (NumberFormatException | DateTimeParseException | InvalidMoneyException
                 | InvalidLoanIdException | InvalidRepaymentException e) {
            respond(exchange, 400, page("Payment not posted", "<p>Invalid input: "
                + escape(e.getMessage()) + "</p>" + backLink()));
            return;
        }
        try {
            final PostingResult result = operator.runAs(form.get("operator"),
                () -> tellerPoster.post(command));
            respond(exchange, 200, page("Payment posted", receipt(result)));
        } catch (LoanDomainException refused) {
            respond(exchange, statusFor(refused), page("Payment not posted", "<p>"
                + escape(refused.getMessage()) + "</p>" + backLink()));
        }
    }

    private static int statusFor(final LoanDomainException refused) {
        if (refused instanceof InvalidRepaymentException) {
            return 400;
        }
        if (refused instanceof LoanNotFoundException) {
            return 404;
        }
        if (refused instanceof LoanClosedException || refused instanceof DuplicatePaymentException) {
            return 409;
        }
        return 422;
    }

    private void endOfDay(final HttpExchange exchange, final String method) throws IOException {
        if (!"POST".equals(method)) {
            respond(exchange, 200, page("End of day",
                "<form method=\"post\" action=\"/end-of-day\">"
                    + field("Business date (YYYY-MM-DD)", "businessDate", "date")
                    + "<button type=\"submit\">Run end of day</button></form>" + backLink()));
            return;
        }
        final LocalDate businessDate;
        try {
            businessDate = LocalDate.parse(readForm(exchange).getOrDefault("businessDate", "")
                .trim());
        } catch (DateTimeParseException e) {
            respond(exchange, 400, page("End of day not run", "<p>Invalid business date: "
                + escape(e.getParsedString()) + "</p>" + backLink()));
            return;
        }
        final EodReport report;
        try {
            report = endOfDay.apply(businessDate);
        } catch (LoanDomainException | IllegalStateException failed) {
            respond(exchange, 503, page("End of day not run", "<p>"
                + escape(failed.getMessage()) + "</p>" + backLink()));
            return;
        }
        respond(exchange, 200, page("End of day " + businessDate, eodSummary(report) + backLink()));
    }

    private static String eodSummary(final EodReport report) {
        final StringBuilder html = new StringBuilder()
            .append("<p>Business date: ").append(report.businessDate()).append("</p>")
            .append("<p>Loans recorded: ").append(report.recorded())
            .append("; already recorded for this or a later date: ")
            .append(report.alreadyRecorded())
            .append("</p><p>Penalty accrued: ")
            .append(escape(report.penaltyAccrued().toPlainString())).append("</p>")
            .append("<h2>Classification changes</h2><ul>");
        for (final ClassificationChangeEvent change : report.classificationChanges()) {
            html.append("<li>").append(escape(change.loanId().value())).append(": ")
                .append(change.from()).append(" to ").append(change.to()).append("</li>");
        }
        html.append("</ul><h2>Due for write-off (over 180 days)</h2><ul>");
        for (final LoanId loan : report.writeOffsDue()) {
            html.append("<li>").append(escape(loan.value())).append("</li>");
        }
        html.append("</ul><h2>Failed (nothing written)</h2><ul>");
        for (final Map.Entry<LoanId, String> failure : report.failures().entrySet()) {
            html.append("<li>").append(escape(failure.getKey().value())).append(": ")
                .append(escape(failure.getValue())).append("</li>");
        }
        return html.append("</ul>").toString();
    }

    private static String formPage() {
        return page("Teller posting",
            "<form method=\"post\" action=\"/repayments\">"
                + field("Loan id", "loanId", "text")
                + field("Amount (PKR)", "amount", "text")
                + field("Business date (YYYY-MM-DD)", "businessDate", "date")
                + field("Operator", "operator", "text")
                + "<button type=\"submit\">Post cash payment</button></form>"
                + "<p><a href=\"/end-of-day\">End of day</a></p>");
    }

    private static String field(final String label, final String name, final String type) {
        return "<p><label>" + label + " <input type=\"" + type + "\" name=\"" + name
            + "\" required></label></p>";
    }

    private static String receipt(final PostingResult result) {
        return "<p>Loan " + escape(result.command().loanId().value()) + ": "
            + escape(result.command().amount().toPlainString()) + " posted.</p>"
            + "<p>Split: " + escape(result.allocation().total().toString()) + "</p>"
            + "<p>Outstanding principal now "
            + escape(result.settlement().account().outstandingPrincipal().toPlainString())
            + " (" + escape(result.settlement().kind().name()) + ").</p>" + backLink();
    }

    private static String backLink() {
        return "<p><a href=\"/\">Back to the form</a></p>";
    }

    private static String page(final String title, final String body) {
        return "<!doctype html><html><head><meta charset=\"utf-8\"><title>MicroLend - "
            + title + "</title></head><body><h1>" + title + "</h1>" + body + "</body></html>";
    }

    private static Map<String, String> readForm(final HttpExchange exchange) throws IOException {
        final String body = new String(exchange.getRequestBody().readAllBytes(),
            StandardCharsets.UTF_8);
        final Map<String, String> form = new HashMap<>();
        for (final String pair : body.split("&")) {
            final int equals = pair.indexOf('=');
            if (equals > 0) {
                form.put(URLDecoder.decode(pair.substring(0, equals), StandardCharsets.UTF_8),
                    URLDecoder.decode(pair.substring(equals + 1), StandardCharsets.UTF_8));
            }
        }
        return form;
    }

    private static void respond(final HttpExchange exchange, final int status, final String html)
        throws IOException {
        final byte[] bytes = html.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", HTML);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }

    static String escape(final String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
            .replace("\"", "&quot;").replace("'", "&#39;");
    }
}
