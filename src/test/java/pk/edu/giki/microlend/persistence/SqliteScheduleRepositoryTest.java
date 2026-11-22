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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.products.Instalment;
import pk.edu.giki.microlend.products.Schedule;

/**
 * Tests for {@link SqliteScheduleRepository} over the {@code instalment} table (FR-08, FR-11;
 * queue item H-06). Money is stored as integer paisa (constraint C-04).
 */
class SqliteScheduleRepositoryTest {

    private static final LoanId LOAN = LoanId.of("LN-1");

    @TempDir
    private Path dir;

    private SqliteDatabase database;
    private SqliteScheduleRepository repository;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static Schedule schedule() {
        return new Schedule(List.of(
            new Instalment(1, LocalDate.of(2027, 2, 28), money("3333.33"), money("100.00")),
            new Instalment(2, LocalDate.of(2027, 3, 31), money("3333.33"), money("100.00")),
            new Instalment(3, LocalDate.of(2027, 4, 30), money("3333.34"), money("100.01"))));
    }

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("schedules.db"));
        repository = new SqliteScheduleRepository(database);
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code, principal,"
                + " outstanding, status, risk_grade, disbursed_on) VALUES ('LN-1', 'C-1', 'FLAT',"
                + " 1000000, 1000000, 'ACTIVE', 'A', '2027-01-31')");
        }
    }

    private long rowCount() throws SQLException {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("SELECT COUNT(*) FROM instalment")) {
            rows.next();
            return rows.getLong(1);
        }
    }

    @Test
    @DisplayName("a saved schedule is read back instalment for instalment")
    void roundTrip() {
        repository.save(LOAN, schedule());
        final Schedule read = repository.scheduleFor(LOAN).orElseThrow();
        assertEquals(schedule().instalments(), read.instalments());
    }

    @Test
    @DisplayName("amounts are stored as integer paisa (C-04)")
    void storesPaisa() throws SQLException {
        repository.save(LOAN, schedule());
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                 "SELECT principal, interest, due_date FROM instalment WHERE sequence = 3")) {
            assertTrue(rows.next());
            assertEquals(333334L, rows.getLong("principal"));
            assertEquals(10001L, rows.getLong("interest"));
            assertEquals("2027-04-30", rows.getString("due_date"));
        }
    }

    @Test
    @DisplayName("a loan with no saved schedule reads as empty")
    void unknownLoanIsEmpty() {
        assertEquals(Optional.empty(), repository.scheduleFor(LoanId.of("LN-404")));
        assertEquals(Optional.empty(), repository.scheduleFor(LOAN));
    }

    @Test
    @DisplayName("a schedule is fixed for the life of the loan: saving a second one is refused")
    void secondSaveRefused() throws SQLException {
        repository.save(LOAN, schedule());
        assertThrows(PersistenceException.class, () -> repository.save(LOAN, schedule()));
        assertEquals(3L, rowCount());
    }

    @Test
    @DisplayName("a schedule for a loan that does not exist is refused and nothing is stored")
    void unknownLoanRefused() throws SQLException {
        assertThrows(PersistenceException.class, () -> repository.save(LoanId.of("LN-404"), schedule()));
        assertEquals(0L, rowCount());
    }
}
