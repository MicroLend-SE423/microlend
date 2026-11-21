package pk.edu.giki.microlend.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.delinquency.Classification;
import pk.edu.giki.microlend.delinquency.ClassificationChangeEvent;
import pk.edu.giki.microlend.delinquency.EodReport;
import pk.edu.giki.microlend.ledger.Allocation;
import pk.edu.giki.microlend.ledger.AllocationResult;
import pk.edu.giki.microlend.ledger.DuplicatePaymentException;
import pk.edu.giki.microlend.ledger.InvalidRepaymentException;
import pk.edu.giki.microlend.ledger.LoanAccount;
import pk.edu.giki.microlend.ledger.LoanClosedException;
import pk.edu.giki.microlend.ledger.LoanNotFoundException;
import pk.edu.giki.microlend.ledger.LoanStatus;
import pk.edu.giki.microlend.ledger.PostingResult;
import pk.edu.giki.microlend.ledger.RepaymentCommand;
import pk.edu.giki.microlend.ledger.RepaymentPoster;
import pk.edu.giki.microlend.ledger.SettlementOutcome;
import pk.edu.giki.microlend.persistence.PersistenceException;

/**
 * Tests for {@link TellerWebServer} (queue item L-18): the local teller
 * posting form and the end-of-day page, served on the loopback interface only.
 */
class TellerWebServerTest {

    private final List<String> operatorsSeen = new CopyOnWriteArrayList<>();
    private final List<RepaymentCommand> posted = new CopyOnWriteArrayList<>();
    private final List<LocalDate> endOfDayDates = new ArrayList<>();
    private Function<LocalDate, EodReport> endOfDayRunner = this::runEndOfDay;
    private final OperatorContext operator = new OperatorContext();
    private RepaymentPoster poster = this::accept;
    private TellerWebServer server;
    private final HttpClient client = HttpClient.newHttpClient();

    private PostingResult accept(final RepaymentCommand command) {
        operatorsSeen.add(operator.get());
        posted.add(command);
        return new PostingResult(command,
            new AllocationResult(Allocation.of(Money.ZERO, Money.ZERO,
                Money.of(new BigDecimal("100.00")), Money.of(new BigDecimal("50.00"))),
                Map.of(), Money.ZERO),
            new SettlementOutcome(SettlementOutcome.Kind.NO_SURPLUS,
                LoanAccount.active(command.loanId(), Money.of(new BigDecimal("2950.00"))),
                Money.ZERO, Money.ZERO, Money.ZERO),
            List.of());
    }

    private EodReport runEndOfDay(final LocalDate businessDate) {
        endOfDayDates.add(businessDate);
        return new EodReport(businessDate, 3, 0, Money.of(new BigDecimal("15.00")),
            List.of(new ClassificationChangeEvent(LoanId.of("LN-7"), Classification.WATCH,
                Classification.SUBSTANDARD)),
            List.of(LoanId.of("LN-9")),
            Map.of(LoanId.of("LN-4"), "schedule <unreadable>"));
    }

    @BeforeEach
    void start() throws IOException {
        server = new TellerWebServer(command -> poster.post(command),
            date -> endOfDayRunner.apply(date), operator, 0);
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    private HttpResponse<String> get(final String path) throws IOException, InterruptedException {
        return client.send(HttpRequest.newBuilder(URI.create(server.baseUrl() + path)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(final String path, final Map<String, String> form)
        throws IOException, InterruptedException {
        final String body = form.entrySet().stream()
            .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
            .collect(Collectors.joining("&"));
        return client.send(HttpRequest.newBuilder(URI.create(server.baseUrl() + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body)).build(),
            HttpResponse.BodyHandlers.ofString());
    }

    private static Map<String, String> payment(final String loan, final String amount,
                                               final String date, final String who) {
        final Map<String, String> form = new LinkedHashMap<>();
        form.put("loanId", loan);
        form.put("amount", amount);
        form.put("businessDate", date);
        form.put("operator", who);
        return form;
    }

    @Test
    @DisplayName("the server listens on the loopback interface only")
    void loopbackOnly() {
        assertTrue(server.baseUrl().startsWith("http://127.0.0.1:"), server.baseUrl());
    }

    @Test
    @DisplayName("GET / shows the teller posting form")
    void showsForm() throws Exception {
        final HttpResponse<String> response = get("/");
        assertEquals(200, response.statusCode());
        for (final String field : List.of("loanId", "amount", "businessDate", "operator")) {
            assertTrue(response.body().contains("name=\"" + field + "\""), field);
        }
        assertTrue(response.body().contains("/end-of-day"));
    }

    @Test
    @DisplayName("a valid cash payment is posted as the operator named on the form")
    void postsCashPayment() throws Exception {
        final HttpResponse<String> response =
            post("/repayments", payment("LN-1", "150.00", "2026-03-20", "teller.amina"));
        assertEquals(200, response.statusCode());
        assertEquals(List.of(RepaymentCommand.cash(LoanId.of("LN-1"),
            Money.of(new BigDecimal("150.00")), LocalDate.of(2026, 3, 20))), posted);
        assertEquals(List.of("teller.amina"), operatorsSeen);
        assertTrue(response.body().contains("2950.00"), response.body());
        assertEquals(null, operator.get(), "the operator must not leak to the next request");
    }

    @Test
    @DisplayName("malformed amount or date is a 400 and nothing is posted")
    void malformedInputIs400() throws Exception {
        assertEquals(400, post("/repayments",
            payment("LN-1", "abc", "2026-03-20", "teller.amina")).statusCode());
        assertEquals(400, post("/repayments",
            payment("LN-1", "10.001", "2026-03-20", "teller.amina")).statusCode());
        assertEquals(400, post("/repayments",
            payment("LN-1", "10.00", "20-03-2026", "teller.amina")).statusCode());
        assertEquals(400, post("/repayments",
            payment("LN-1", "0.00", "2026-03-20", "teller.amina")).statusCode());
        assertTrue(posted.isEmpty());
    }

    @Test
    @DisplayName("domain refusals map to 404, 409 and 422 with the reason shown")
    void refusalsMapToStatusCodes() throws Exception {
        poster = command -> {
            throw new LoanNotFoundException(command.loanId());
        };
        assertEquals(404, post("/repayments",
            payment("LN-9", "1.00", "2026-03-20", "t")).statusCode());
        poster = command -> {
            throw new LoanClosedException(command.loanId(), LoanStatus.CLOSED,
                LocalDate.of(2026, 1, 1));
        };
        assertEquals(409, post("/repayments",
            payment("LN-9", "1.00", "2026-03-20", "t")).statusCode());
        poster = command -> {
            throw new DuplicatePaymentException("HBL", "TX-1");
        };
        final HttpResponse<String> duplicate =
            post("/repayments", payment("LN-9", "1.00", "2026-03-20", "t"));
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("HBL/TX-1"), duplicate.body());
    }

    @Test
    @DisplayName("user input is HTML-escaped in responses")
    void escapesInput() throws Exception {
        poster = command -> {
            throw new LoanNotFoundException(command.loanId());
        };
        final HttpResponse<String> response =
            post("/repayments", payment("<script>x</script>", "1.00", "2026-03-20", "t"));
        assertFalse(response.body().contains("<script>"), response.body());
        assertTrue(response.body().contains("&lt;script&gt;"), response.body());
    }

    @Test
    @DisplayName("the end-of-day page offers a business date and a run button")
    void endOfDayForm() throws Exception {
        final HttpResponse<String> page = get("/end-of-day");
        assertEquals(200, page.statusCode());
        assertTrue(page.body().contains("name=\"businessDate\""), page.body());
        assertFalse(page.body().contains("disabled"), page.body());
    }

    @Test
    @DisplayName("running end of day shows the engine's report for that business date")
    void endOfDayRuns() throws Exception {
        final HttpResponse<String> response = post("/end-of-day",
            Map.of("businessDate", "2027-03-11"));
        assertEquals(200, response.statusCode());
        assertEquals(List.of(LocalDate.of(2027, 3, 11)), endOfDayDates);
        final String body = response.body();
        assertTrue(body.contains("2027-03-11"), body);
        assertTrue(body.contains("Loans recorded: 3"), body);
        assertTrue(body.contains("15.00"), body);
        assertTrue(body.contains("LN-7: WATCH to SUBSTANDARD"), body);
        assertTrue(body.contains("LN-9"), body);
        assertTrue(body.contains("schedule &lt;unreadable&gt;"), body);
    }

    @Test
    @DisplayName("an end-of-day run that fails as a whole is a 503 page naming the cause")
    void endOfDayFailureIsReported() throws Exception {
        endOfDayRunner = date -> {
            throw new PersistenceException("could not list active loans");
        };
        final HttpResponse<String> response = post("/end-of-day",
            Map.of("businessDate", "2027-03-11"));
        assertEquals(503, response.statusCode());
        assertTrue(response.body().contains("could not list active loans"), response.body());
    }

    @Test
    @DisplayName("an invalid business date is a 400 and the engine is not run")
    void endOfDayRejectsBadDate() throws Exception {
        assertEquals(400, post("/end-of-day", Map.of("businessDate", "11/03/2027")).statusCode());
        assertEquals(List.of(), endOfDayDates);
    }

    @Test
    @DisplayName("two tellers are served at the same time, not one after the other")
    void servesRequestsConcurrently() throws Exception {
        final CountDownLatch bothInside = new CountDownLatch(2);
        poster = command -> {
            bothInside.countDown();
            try {
                if (!bothInside.await(5, TimeUnit.SECONDS)) {
                    throw new InvalidRepaymentException("served one at a time", "10.00", "cash");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return accept(command);
        };
        final ExecutorService tellers = Executors.newFixedThreadPool(2);
        try {
            final Future<HttpResponse<String>> first = tellers.submit(() ->
                post("/repayments", payment("LN-1", "10.00", "2027-03-11", "teller.amina")));
            final Future<HttpResponse<String>> second = tellers.submit(() ->
                post("/repayments", payment("LN-2", "10.00", "2027-03-11", "teller.bilal")));
            assertEquals(200, first.get(10, TimeUnit.SECONDS).statusCode());
            assertEquals(200, second.get(10, TimeUnit.SECONDS).statusCode());
        } finally {
            tellers.shutdownNow();
        }
    }

    @Test
    @DisplayName("unknown paths are 404 and wrong methods are 405")
    void routing() throws Exception {
        assertEquals(404, get("/nowhere").statusCode());
        assertEquals(405, get("/repayments").statusCode());
    }
}
