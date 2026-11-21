package pk.edu.giki.microlend;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import pk.edu.giki.microlend.ledger.importing.ImportReport;
import pk.edu.giki.microlend.persistence.SqliteDatabase;
import pk.edu.giki.microlend.web.OperatorContext;
import pk.edu.giki.microlend.web.TellerWebServer;

/**
 * Entry point for MicroLend.
 *
 * <p>Opens (or creates) the local SQLite file {@code microlend.db} in the
 * working directory and wires the application through {@link CompositionRoot}.
 * With no arguments it serves the teller web interface, including the end-of-day
 * page, on {@code http://127.0.0.1:8080}. With {@code import <HBL|MCB> <file>} it
 * imports one bank statement and prints what was posted. The server listens on
 * the loopback interface only; nothing in this project is deployed. Until the
 * {@code products} module supplies what each loan owes, postings and end-of-day
 * tasks report which module they are waiting for.</p>
 */
public final class MicroLendApplication {

    private static final int PORT = 8080;
    private static final int IMPORT_ARGUMENTS = 3;

    private MicroLendApplication() {
        // Entry point only.
    }

    /**
     * Runs the application.
     *
     * @param args none to serve the web interface, or {@code import <HBL|MCB> <file>}
     * @throws IOException if the local port cannot be bound or the statement cannot be read
     */
    public static void main(final String[] args) throws IOException {
        final OperatorContext operator = new OperatorContext();
        final CompositionRoot root = CompositionRoot.withPlaceholders(
            new SqliteDatabase("jdbc:sqlite:microlend.db"), operator);
        if (args.length == IMPORT_ARGUMENTS && "import".equals(args[0])) {
            final Path file = Path.of(args[2]);
            try (Reader statement = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                final ImportReport report = root.importStatement(args[1], statement,
                    file.getFileName().toString());
                System.out.println("Posted " + report.posted() + "; refused as duplicates: "
                    + report.duplicateReferences());
            }
            return;
        }
        final TellerWebServer server = new TellerWebServer(root.tellerPoster(),
            root.endOfDayEngine()::run, operator, PORT);
        server.start();
        System.out.println("MicroLend teller interface: " + server.baseUrl()
            + "  (local only; stop with Ctrl+C)");
    }
}
