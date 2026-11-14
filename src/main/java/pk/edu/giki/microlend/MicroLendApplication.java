package pk.edu.giki.microlend;

import java.io.IOException;
import pk.edu.giki.microlend.persistence.SqliteDatabase;
import pk.edu.giki.microlend.web.OperatorContext;
import pk.edu.giki.microlend.web.TellerWebServer;

/**
 * Entry point for MicroLend.
 *
 * <p>Opens (or creates) the local SQLite file {@code microlend.db} in the
 * working directory, wires the application through {@link CompositionRoot},
 * and serves the teller web interface on {@code http://127.0.0.1:8080}. The
 * server listens on the loopback interface only; nothing in this project is
 * deployed, and everything runs on a developer's own machine. Until the
 * {@code products} and {@code delinquency} modules exist, a posting reports
 * which module it is waiting for.</p>
 */
public final class MicroLendApplication {

    private static final int PORT = 8080;

    private MicroLendApplication() {
        // Entry point only.
    }

    /**
     * Runs the application until the process is stopped.
     *
     * @param args command-line arguments; none are read at this milestone
     * @throws IOException if the local port cannot be bound
     */
    public static void main(final String[] args) throws IOException {
        final OperatorContext operator = new OperatorContext();
        final CompositionRoot root = CompositionRoot.withPlaceholders(
            new SqliteDatabase("jdbc:sqlite:microlend.db"), operator);
        final TellerWebServer server = new TellerWebServer(root.tellerPoster(), operator, PORT);
        server.start();
        System.out.println("MicroLend teller interface: " + server.baseUrl()
            + "  (local only; stop with Ctrl+C)");
    }
}
