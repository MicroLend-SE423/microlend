package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
import pk.edu.giki.microlend.delinquency.Classification;
import pk.edu.giki.microlend.delinquency.DailySnapshot;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * Tests for {@link SqliteDailyResultStore} (BR-33, FR-26, M1 section 3.6): the snapshot
 * and the day's penalty lines are written in one {@code BEGIN IMMEDIATE} transaction, and
 * the unique (loan, business date) key makes a second run for the date a no-op.
 */
class SqliteDailyResultStoreTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate DAY = LocalDate.of(2027, 3, 11);
    private static final Instant NOW = Instant.parse("2027-03-11T18:00:00Z");

    @TempDir
    private Path dir;

    private SqliteDatabase database;
    private SqliteDailyResultStore store;

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("eod.db"));
        store = new SqliteDailyResultStore(database);
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            for (final String loan : List.of("LN-2", "LN-1")) {
                statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code,"
                    + " principal, outstanding, status, risk_grade, disbursed_on) VALUES ('"
                    + loan + "', 'C-1', 'FLAT', 1000000, 1000000, 'ACTIVE', 'A', '2026-10-01')");
            }
            statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code,"
                + " principal, outstanding, status, risk_grade, disbursed_on, closed_on) VALUES"
                + " ('LN-9', 'C-1', 'FLAT', 1000000, 0, 'CLOSED', 'A', '2026-01-01',"
                + " '2026-09-01')");
        }
    }

    private static List<LedgerEntry> penalty(final LoanId loan, final String amount) {
        final Money money = Money.of(new BigDecimal(amount));
        return List.of(LedgerEntry.debit(loan, EntryType.PENALTY_ACCRUAL, money, NOW),
            LedgerEntry.credit(loan, EntryType.PENALTY_INCOME, money, NOW));
    }

    private long count(final String table) throws SQLException {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM " + table)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    @DisplayName("only active loans are listed, in id order")
    void listsActiveLoans() {
        assertEquals(List.of(LoanId.of("LN-1"), LoanId.of("LN-2")), store.activeLoans());
    }

    @Test
    @DisplayName("TC-DEL-06.1: the snapshot and the balanced penalty pair are written together")
    void recordsSnapshotAndPenalty() throws SQLException {
        assertTrue(store.record(LOAN, DAY, 10, Classification.WATCH, penalty(LOAN, "5.00")));
        assertEquals(1, count("delinquency_snapshot"));
        assertEquals(penalty(LOAN, "5.00"), new SqliteLedgerRepository(database).entriesFor(LOAN));
    }

    @Test
    @DisplayName("TC-DEL-06.2: a second record for the same loan and date changes nothing")
    void secondRecordIsANoOp() throws SQLException {
        store.record(LOAN, DAY, 10, Classification.WATCH, penalty(LOAN, "5.00"));
        assertFalse(store.record(LOAN, DAY, 10, Classification.WATCH, penalty(LOAN, "5.00")));
        assertEquals(1, count("delinquency_snapshot"));
        assertEquals(2, count("ledger_entry"));
    }

    @Test
    @DisplayName("a day with no penalty writes the snapshot only")
    void snapshotWithoutPenalty() throws SQLException {
        assertTrue(store.record(LOAN, DAY, 0, Classification.STANDARD, List.of()));
        assertEquals(1, count("delinquency_snapshot"));
        assertEquals(0, count("ledger_entry"));
    }

    @Test
    @DisplayName("a failing ledger line rolls the snapshot back with it")
    void oneTransaction() throws SQLException {
        assertThrows(PersistenceException.class, () -> store.record(LOAN, DAY, 10,
            Classification.WATCH, penalty(LoanId.of("LN-404"), "5.00")));
        assertEquals(0, count("delinquency_snapshot"));
        assertEquals(0, count("ledger_entry"));
    }

    @Test
    @DisplayName("the last snapshot is the loan's latest recorded date with its classification")
    void lastSnapshot() {
        assertEquals(Optional.empty(), store.lastSnapshot(LOAN));
        store.record(LOAN, DAY.plusDays(2), 40, Classification.SUBSTANDARD, List.of());
        store.record(LOAN, DAY, 10, Classification.WATCH, List.of());
        assertEquals(Optional.of(new DailySnapshot(DAY.plusDays(2), Classification.SUBSTANDARD)),
            store.lastSnapshot(LOAN));
        assertEquals(Optional.empty(), store.lastSnapshot(LoanId.of("LN-2")));
    }

    @Test
    @DisplayName("two connections recording the same loan and date at once: exactly one wins")
    void concurrentRecordsForTheSameDate() throws Exception {
        final CountDownLatch start = new CountDownLatch(1);
        final ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            final Future<Boolean> first = pool.submit(() -> {
                start.await();
                return store.record(LOAN, DAY, 10, Classification.WATCH, penalty(LOAN, "5.00"));
            });
            final Future<Boolean> second = pool.submit(() -> {
                start.await();
                return store.record(LOAN, DAY, 10, Classification.WATCH, penalty(LOAN, "5.00"));
            });
            start.countDown();
            assertTrue(first.get(10, TimeUnit.SECONDS) ^ second.get(10, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertEquals(1, count("delinquency_snapshot"));
        assertEquals(2, count("ledger_entry"));
    }
}
