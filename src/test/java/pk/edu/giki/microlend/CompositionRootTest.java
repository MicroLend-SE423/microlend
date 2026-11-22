package pk.edu.giki.microlend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
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
import pk.edu.giki.microlend.ledger.DuplicatePaymentException;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.ledger.RepaymentCommand;
import pk.edu.giki.microlend.persistence.SqliteDatabase;
import pk.edu.giki.microlend.persistence.SqliteLedgerRepository;
import pk.edu.giki.microlend.persistence.SqliteLoanRepository;
import pk.edu.giki.microlend.persistence.SqliteScheduleRepository;
import pk.edu.giki.microlend.products.Instalment;
import pk.edu.giki.microlend.products.Schedule;
import pk.edu.giki.microlend.products.ScheduleNotFoundException;

/**
 * Tests for {@link CompositionRoot} (queue item L-17): the two production
 * decorator chains of M1 design section 5.3, wired to SQLite.
 */
class CompositionRootTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);
    private static final Clock CLOCK =
        Clock.fixed(Instant.parse("2026-03-20T10:00:00Z"), ZoneOffset.UTC);
    private static final List<InstalmentDue> OWING = List.of(new InstalmentDue(1,
        LocalDate.of(2026, 3, 1), Money.ZERO, Money.ZERO, money("100.00"), money("1000.00")));

    @TempDir
    private Path dir;

    private SqliteDatabase database;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("app.db"));
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code, principal,"
                + " outstanding, status, risk_grade, disbursed_on) VALUES ('LN-1', 'C-1', 'FLAT',"
                + " 300000, 300000, 'ACTIVE', 'A', '2026-01-01')");
        }
    }

    private CompositionRoot root() {
        return new CompositionRoot(database, CLOCK, (loan, date) -> OWING,
            (loan, date) -> Money.ZERO, () -> "teller.amina");
    }

    @Test
    @DisplayName("teller chain: posts to SQLite, audits the operator and times the posting")
    void tellerChainPostsAuditsAndTimes() {
        final CompositionRoot root = root();
        root.tellerPoster().post(RepaymentCommand.cash(LOAN, money("150.00"), TODAY));

        assertEquals(List.of(EntryType.PAYMENT_RECEIVED, EntryType.INTEREST_REPAYMENT,
                EntryType.PRINCIPAL_REPAYMENT),
            new SqliteLedgerRepository(database).entriesFor(LOAN).stream()
                .map(entry -> entry.type()).toList());
        assertEquals(money("2950.00"),
            new SqliteLoanRepository(database).findById(LOAN).orElseThrow().outstandingPrincipal());
        assertEquals("teller.amina", root.auditTrail().get(0).operator());
        assertEquals(1, root.timings().size());
    }

    @Test
    @DisplayName("import chain: a duplicate is refused outermost, before it is timed; no audit")
    void importChainGuardsOutermost() {
        final CompositionRoot root = root();
        final RepaymentCommand payment =
            RepaymentCommand.bank(LOAN, money("50.00"), TODAY, "HBL", "TX-1");
        root.importPoster().post(payment);
        assertThrows(DuplicatePaymentException.class, () -> root.importPoster().post(payment));
        assertEquals(1, root.timings().size());
        assertTrue(root.auditTrail().isEmpty());
    }

    @Test
    @DisplayName("production wiring: a repayment is allocated from the schedule saved on SQLite")
    void productionPostsFromTheSavedSchedule() {
        new SqliteScheduleRepository(database).save(LOAN, new Schedule(List.of(
            new Instalment(1, LocalDate.of(2026, 3, 1), money("1000.00"), money("100.00")))));
        final CompositionRoot root = CompositionRoot.production(database, () -> "teller.amina");

        root.tellerPoster().post(RepaymentCommand.cash(LOAN, money("150.00"), TODAY));

        assertEquals(List.of(EntryType.PAYMENT_RECEIVED, EntryType.INTEREST_REPAYMENT,
                EntryType.PRINCIPAL_REPAYMENT),
            new SqliteLedgerRepository(database).entriesFor(LOAN).stream()
                .map(entry -> entry.type()).toList());
        assertEquals(money("2950.00"),
            new SqliteLoanRepository(database).findById(LOAN).orElseThrow().outstandingPrincipal());
    }

    @Test
    @DisplayName("production wiring: a loan with no saved schedule is refused and nothing is posted")
    void productionRefusesALoanWithoutASchedule() {
        final CompositionRoot root = CompositionRoot.production(database, () -> "teller.amina");
        assertThrows(ScheduleNotFoundException.class,
            () -> root.tellerPoster().post(RepaymentCommand.cash(LOAN, money("1.00"), TODAY)));
        assertTrue(new SqliteLedgerRepository(database).entriesFor(LOAN).isEmpty());
    }
}
