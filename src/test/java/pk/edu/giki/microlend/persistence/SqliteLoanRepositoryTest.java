package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.LoanAccount;
import pk.edu.giki.microlend.ledger.LoanStatus;

/**
 * Tests for {@link SqliteLoanRepository} (queue item L-15; M1 design section 3.6).
 */
class SqliteLoanRepositoryTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate PAID_OFF = LocalDate.of(2026, 11, 14);

    @TempDir
    private Path dir;

    private SqliteDatabase database;
    private SqliteLoanRepository repository;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("loans.db"));
        repository = new SqliteLoanRepository(database);
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code, principal,"
                + " outstanding, status, risk_grade, disbursed_on) VALUES ('LN-1', 'C-1', 'FLAT',"
                + " 5000000, 5000000, 'ACTIVE', 'A', '2026-10-01')");
        }
    }

    private long rawOutstanding() throws SQLException {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                 "SELECT outstanding FROM loan WHERE id = 'LN-1'")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    @DisplayName("an active loan is read with its status and outstanding principal")
    void findsActiveLoan() {
        final LoanAccount account = repository.findById(LOAN).orElseThrow();
        assertEquals(LOAN, account.id());
        assertEquals(LoanStatus.ACTIVE, account.status());
        assertEquals(money("50000.00"), account.outstandingPrincipal());
        assertEquals(Optional.empty(), account.closedOn());
    }

    @Test
    @DisplayName("an unknown loan reads as empty")
    void unknownLoanIsEmpty() {
        assertTrue(repository.findById(LoanId.of("LN-404")).isEmpty());
    }

    @Test
    @DisplayName("save stores outstanding principal as paisa, and status with its closure date")
    void saveRoundTrips() throws SQLException {
        repository.save(LoanAccount.active(LOAN, money("12345.67")));
        assertEquals(1234567L, rawOutstanding());

        repository.save(LoanAccount.active(LOAN, Money.ZERO).closeAsRepaid(PAID_OFF));
        final LoanAccount closed = repository.findById(LOAN).orElseThrow();
        assertEquals(LoanStatus.CLOSED, closed.status());
        assertEquals(Optional.of(PAID_OFF), closed.closedOn());
        assertEquals(Money.ZERO, closed.outstandingPrincipal());
    }

    @Test
    @DisplayName("saving a loan that was never originated is refused, not inserted")
    void saveOfUnknownLoanIsRefused() {
        assertThrows(PersistenceException.class,
            () -> repository.save(LoanAccount.active(LoanId.of("LN-404"), Money.ZERO)));
        assertTrue(repository.findById(LoanId.of("LN-404")).isEmpty());
    }

    @Test
    @DisplayName("a save waits for another writer's lock instead of failing (busy timeout)")
    void saveWaitsForConcurrentWriter() throws Exception {
        final CountDownLatch locked = new CountDownLatch(1);
        final CompletableFuture<Void> otherWriter = CompletableFuture.runAsync(() -> {
            try (Connection other = database.connect();
                 Statement statement = other.createStatement()) {
                statement.execute("BEGIN IMMEDIATE");
                locked.countDown();
                Thread.sleep(500);
                statement.execute("COMMIT");
            } catch (SQLException | InterruptedException e) {
                throw new IllegalStateException(e);
            }
        });
        assertTrue(locked.await(5, TimeUnit.SECONDS));
        repository.save(LoanAccount.active(LOAN, money("1.00")));
        otherWriter.get(5, TimeUnit.SECONDS);
        assertEquals(100L, rawOutstanding());
    }

    @Test
    @DisplayName("null arguments are rejected as the role contract states")
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> repository.findById(null));
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> new SqliteLoanRepository(null));
    }
}
