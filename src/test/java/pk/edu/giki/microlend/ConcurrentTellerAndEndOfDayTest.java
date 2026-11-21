package pk.edu.giki.microlend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.delinquency.EodReport;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.ledger.RepaymentCommand;
import pk.edu.giki.microlend.persistence.SqliteDatabase;
import pk.edu.giki.microlend.persistence.SqliteLoanRepository;

/**
 * The concurrent operation end to end (M1 section 3.6, QR-06): four teller threads post
 * to the same loans while the end-of-day engine runs, all through the production wiring
 * in {@link CompositionRoot} against one SQLite file. No posting may be lost, every
 * posting must balance, and every loan must get its snapshot.
 */
class ConcurrentTellerAndEndOfDayTest {

    private static final int LOANS = 5;
    private static final int TELLERS = 4;
    private static final int POSTS_PER_TELLER_PER_LOAN = 5;
    private static final LocalDate TODAY = LocalDate.of(2027, 3, 11);
    private static final Clock CLOCK =
        Clock.fixed(Instant.parse("2027-03-11T18:00:00Z"), ZoneOffset.UTC);

    @TempDir
    private Path dir;

    private SqliteDatabase database;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("race.db"));
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            for (int i = 1; i <= LOANS; i++) {
                statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code,"
                    + " principal, outstanding, status, risk_grade, disbursed_on) VALUES ('LN-"
                    + i + "', 'C-1', 'FLAT', 5000000, 5000000, 'ACTIVE', 'A', '2026-10-01')");
            }
        }
    }

    private long query(final String sql) throws SQLException {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    @DisplayName("QR-06: tellers and end of day on the same loans lose no posting and stay balanced")
    void tellersAndEndOfDayTogether() throws Exception {
        final List<InstalmentDue> owing = List.of(new InstalmentDue(1, TODAY.minusDays(10),
            Money.ZERO, Money.ZERO, Money.ZERO, money("40000.00")));
        final CompositionRoot root = new CompositionRoot(database, CLOCK, (loan, date) -> owing,
            (loan, date) -> Money.ZERO, () -> "teller.amina");
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(TELLERS + 1);
        final List<Future<?>> work = new ArrayList<>();
        try {
            for (int t = 0; t < TELLERS; t++) {
                work.add(pool.submit(() -> {
                    start.await();
                    for (int round = 0; round < POSTS_PER_TELLER_PER_LOAN; round++) {
                        for (int i = 1; i <= LOANS; i++) {
                            root.tellerPoster().post(RepaymentCommand.cash(LoanId.of("LN-" + i),
                                money("10.00"), TODAY));
                        }
                    }
                    return null;
                }));
            }
            final Future<EodReport> endOfDay = pool.submit(() -> {
                start.await();
                return root.endOfDayEngine().run(TODAY);
            });
            start.countDown();
            for (final Future<?> teller : work) {
                teller.get(60, TimeUnit.SECONDS);
            }
            final EodReport report = endOfDay.get(60, TimeUnit.SECONDS);
            assertEquals(Map.of(), report.failures());
            assertEquals(LOANS, report.recorded());
        } finally {
            pool.shutdownNow();
        }

        final Money paidPerLoan = money("10.00").percentage(
            BigDecimal.valueOf(100L * TELLERS * POSTS_PER_TELLER_PER_LOAN));
        final SqliteLoanRepository loans = new SqliteLoanRepository(database);
        for (int i = 1; i <= LOANS; i++) {
            assertEquals(money("50000.00").minus(paidPerLoan),
                loans.findById(LoanId.of("LN-" + i)).orElseThrow().outstandingPrincipal(),
                "LN-" + i + " lost a posting");
        }
        assertEquals(query("SELECT SUM(debit) FROM ledger_entry"),
            query("SELECT SUM(credit) FROM ledger_entry"));
        assertEquals(LOANS, query("SELECT COUNT(*) FROM delinquency_snapshot"));
        assertTrue(query("SELECT COUNT(*) FROM ledger_entry WHERE entry_type = 'PENALTY_INCOME'")
            == LOANS);
    }
}
