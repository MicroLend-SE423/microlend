package pk.edu.giki.microlend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.ledger.importing.ImportReport;
import pk.edu.giki.microlend.ledger.importing.InvalidStatementException;
import pk.edu.giki.microlend.persistence.SqliteDatabase;
import pk.edu.giki.microlend.persistence.SqliteLoanRepository;

/**
 * The Adapter pattern in running code (I-04, FR-20, BR-25): a bank statement imported
 * through {@link CompositionRoot} reaches SQLite through the production import chain, with
 * the idempotency guard outermost.
 */
class StatementImportWiringTest {

    private static final Clock CLOCK =
        Clock.fixed(Instant.parse("2026-10-06T10:00:00Z"), ZoneOffset.UTC);
    private static final Path HBL = Path.of("src/test/resources/statements/hbl-statement.csv");

    @TempDir
    private Path dir;

    private SqliteDatabase database;
    private CompositionRoot root;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("import.db"));
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            for (final String loan : List.of("LN-0001", "LN-0002")) {
                statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code,"
                    + " principal, outstanding, status, risk_grade, disbursed_on) VALUES ('"
                    + loan + "', 'C-1', 'FLAT', 5000000, 5000000, 'ACTIVE', 'A', '2026-09-01')");
            }
        }
        final List<InstalmentDue> owing = List.of(new InstalmentDue(1, LocalDate.of(2026, 10, 1),
            Money.ZERO, Money.ZERO, Money.ZERO, money("40000.00")));
        root = new CompositionRoot(database, CLOCK, (loan, date) -> owing,
            (loan, date) -> Money.ZERO, () -> "teller.amina");
    }

    private ImportReport importHbl() throws IOException {
        try (Reader statement = Files.newBufferedReader(HBL, StandardCharsets.UTF_8)) {
            return root.importStatement("HBL", statement, "hbl-statement.csv");
        }
    }

    @Test
    @DisplayName("an HBL statement posts its credit rows to the loans it names")
    void importsThroughTheProductionChain() throws IOException {
        assertEquals(new ImportReport(2, List.of()), importHbl());
        assertEquals(money("47451.00"), new SqliteLoanRepository(database)
            .findById(LoanId.of("LN-0001")).orElseThrow().outstandingPrincipal());
    }

    @Test
    @DisplayName("BR-25: importing the same statement again posts nothing and lists the duplicates")
    void secondImportIsRefused() throws IOException {
        importHbl();
        assertEquals(new ImportReport(0, List.of("HBL-7781", "HBL-7782")), importHbl());
    }

    @Test
    @DisplayName("a bank with no adapter is refused before anything is read")
    void unknownBank() {
        assertThrows(InvalidStatementException.class,
            () -> root.importStatement("UBL", new StringReader(""), "ubl.csv"));
    }
}
