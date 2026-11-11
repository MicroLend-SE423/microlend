package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Tests for the SQLite schema of M1 design section 3.7, applied by
 * {@link SchemaMigrator} on the first connection from {@link SqliteDatabase}.
 *
 * <p>Each constraint test inserts a row the code would never produce and
 * checks that the database refuses it too, so a corrupt write fails at two
 * independent levels.</p>
 */
class SchemaMigratorTest {

    private static final Set<String> SEVEN_TABLES = Set.of("customer", "loan_product", "loan",
        "instalment", "ledger_entry", "payment", "delinquency_snapshot");

    @TempDir
    private Path dir;

    private SqliteDatabase database;

    @BeforeEach
    void openDatabase() {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("microlend.db"));
    }

    private static void exec(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(sql);
        }
    }

    private static long queryLong(final Connection connection, final String sql)
        throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            rows.next();
            return rows.getLong(1);
        }
    }

    private static void seedLoan(final Connection connection) throws SQLException {
        exec(connection, "INSERT INTO customer (id, full_name, date_of_birth, monthly_income)"
            + " VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
        exec(connection, "INSERT INTO loan_product (code, product_type, annual_rate_bp,"
            + " min_amount, max_amount, min_tenure_months, max_tenure_months)"
            + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
        exec(connection, "INSERT INTO loan (id, customer_id, product_code, principal,"
            + " outstanding, status, risk_grade, disbursed_on, closed_on)"
            + " VALUES ('LN-1', 'C-1', 'FLAT', 5000000, 5000000, 'ACTIVE', 'A', '2026-10-01', NULL)");
    }

    @Test
    @DisplayName("the first connection creates all seven tables")
    void createsSevenTables() throws SQLException {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(
                 "SELECT name FROM sqlite_master WHERE type = 'table'"
                     + " AND name NOT LIKE 'sqlite_%'")) {
            final Set<String> tables = new TreeSet<>();
            while (rows.next()) {
                tables.add(rows.getString(1));
            }
            assertEquals(new TreeSet<>(SEVEN_TABLES), tables);
        }
    }

    @Test
    @DisplayName("foreign keys are enforced on every connection, not only the first")
    void foreignKeysOnEveryConnection() throws SQLException {
        try (Connection first = database.connect(); Connection second = database.connect()) {
            assertEquals(1L, queryLong(first, "PRAGMA foreign_keys"));
            assertEquals(1L, queryLong(second, "PRAGMA foreign_keys"));
        }
    }

    @Test
    @DisplayName("migration is recorded and running it again changes nothing")
    void migrationIsIdempotent() throws SQLException {
        try (Connection connection = database.connect()) {
            seedLoan(connection);
            assertEquals(SchemaMigrator.CURRENT_VERSION,
                queryLong(connection, "PRAGMA user_version"));
            assertDoesNotThrow(() -> new SchemaMigrator().migrate(connection));
            assertEquals(1L, queryLong(connection, "SELECT COUNT(*) FROM loan"));
        }
        final SqliteDatabase reopened =
            new SqliteDatabase("jdbc:sqlite:" + dir.resolve("microlend.db"));
        try (Connection connection = reopened.connect()) {
            assertEquals(1L, queryLong(connection, "SELECT COUNT(*) FROM loan"));
        }
    }

    @Test
    @DisplayName("every monetary column is INTEGER minor units (C-04)")
    void moneyColumnsAreInteger() throws SQLException {
        final Map<String, List<String>> moneyColumns = new HashMap<>();
        moneyColumns.put("customer", List.of("monthly_income"));
        moneyColumns.put("loan_product", List.of("min_amount", "max_amount"));
        moneyColumns.put("loan", List.of("principal", "outstanding"));
        moneyColumns.put("instalment", List.of("principal", "interest"));
        moneyColumns.put("ledger_entry", List.of("debit", "credit"));
        moneyColumns.put("payment", List.of("amount"));
        try (Connection connection = database.connect()) {
            for (final Map.Entry<String, List<String>> table : moneyColumns.entrySet()) {
                final Map<String, String> types = new HashMap<>();
                try (Statement statement = connection.createStatement();
                     ResultSet rows = statement.executeQuery(
                         "PRAGMA table_info(" + table.getKey() + ")")) {
                    while (rows.next()) {
                        types.put(rows.getString("name"), rows.getString("type"));
                    }
                }
                for (final String column : table.getValue()) {
                    assertEquals("INTEGER", types.get(column), table.getKey() + "." + column);
                }
            }
        }
    }

    @Test
    @DisplayName("an unusable database location is reported as a domain exception")
    void storageFailureIsTranslated() {
        final SqliteDatabase broken = new SqliteDatabase(
            "jdbc:sqlite:" + dir.resolve("missing-folder").resolve("x").resolve("db.db"));
        final PersistenceException thrown =
            assertThrows(PersistenceException.class, broken::connect);
        assertTrue(thrown instanceof LoanDomainException);
    }

    @Nested
    @DisplayName("the schema re-states the code's invariants")
    class Constraints {

        private Connection connection;

        @BeforeEach
        void seed() throws SQLException {
            connection = database.connect();
            seedLoan(connection);
        }

        @org.junit.jupiter.api.AfterEach
        void close() throws SQLException {
            connection.close();
        }

        private void rejects(final String sql) {
            assertThrows(SQLException.class, () -> exec(connection, sql), sql);
        }

        @Test
        @DisplayName("ledger_entry: a valid credit line is accepted")
        void ledgerAcceptsValidLine() {
            assertDoesNotThrow(() -> exec(connection, "INSERT INTO ledger_entry (loan_id,"
                + " entry_type, debit, credit, posted_at)"
                + " VALUES ('LN-1', 'PRINCIPAL_REPAYMENT', 0, 254900, '2026-10-04T09:00:00Z')"));
        }

        @Test
        @DisplayName("ledger_entry: negative, zero-zero and two-sided lines are refused (BR-43)")
        void ledgerColumns() {
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at)"
                + " VALUES ('LN-1', 'PENALTY_ACCRUAL', -1, 0, '2026-10-04T09:00:00Z')");
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at)"
                + " VALUES ('LN-1', 'PENALTY_ACCRUAL', 0, 0, '2026-10-04T09:00:00Z')");
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at)"
                + " VALUES ('LN-1', 'PENALTY_ACCRUAL', 100, 100, '2026-10-04T09:00:00Z')");
        }

        @Test
        @DisplayName("ledger_entry: an unknown loan or entry type is refused")
        void ledgerForeignKeyAndType() {
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at)"
                + " VALUES ('LN-404', 'PENALTY_ACCRUAL', 100, 0, '2026-10-04T09:00:00Z')");
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at)"
                + " VALUES ('LN-1', 'GIFT', 100, 0, '2026-10-04T09:00:00Z')");
        }

        @Test
        @DisplayName("ledger_entry: a reversal must reference an existing entry and give a reason")
        void ledgerReversalShape() throws SQLException {
            exec(connection, "INSERT INTO ledger_entry (loan_id, entry_type, debit, credit,"
                + " posted_at) VALUES ('LN-1', 'PRINCIPAL_REPAYMENT', 0, 100, '2026-10-04T09:00:00Z')");
            assertDoesNotThrow(() -> exec(connection, "INSERT INTO ledger_entry (loan_id,"
                + " entry_type, debit, credit, posted_at, reverses_entry_id, reason)"
                + " VALUES ('LN-1', 'REVERSAL', 100, 0, '2026-10-05T09:00:00Z', 1, 'wrong loan')"));
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at,"
                + " reverses_entry_id, reason)"
                + " VALUES ('LN-1', 'REVERSAL', 100, 0, '2026-10-05T09:00:00Z', 999, 'x')");
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at,"
                + " reverses_entry_id, reason)"
                + " VALUES ('LN-1', 'REVERSAL', 100, 0, '2026-10-05T09:00:00Z', 1, NULL)");
            rejects("INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at,"
                + " reverses_entry_id, reason)"
                + " VALUES ('LN-1', 'PENALTY_ACCRUAL', 100, 0, '2026-10-05T09:00:00Z', 1, 'x')");
        }

        @Test
        @DisplayName("payment: a (bank, reference) pair is unique; cash rows and other banks are not")
        void paymentIdempotencyKey() throws SQLException {
            final String hbl = "INSERT INTO payment (loan_id, amount, business_date, source_bank,"
                + " bank_reference) VALUES ('LN-1', 254900, '2026-10-04', 'HBL', 'TXN-1')";
            exec(connection, hbl);
            rejects(hbl);
            assertDoesNotThrow(() -> exec(connection, "INSERT INTO payment (loan_id, amount,"
                + " business_date, source_bank, bank_reference)"
                + " VALUES ('LN-1', 254900, '2026-10-04', 'MCB', 'TXN-1')"));
            final String cash = "INSERT INTO payment (loan_id, amount, business_date)"
                + " VALUES ('LN-1', 1000, '2026-10-04')";
            exec(connection, cash);
            assertDoesNotThrow(() -> exec(connection, cash));
        }

        @Test
        @DisplayName("payment: zero amount, or a bank without a reference, is refused")
        void paymentShape() {
            rejects("INSERT INTO payment (loan_id, amount, business_date)"
                + " VALUES ('LN-1', 0, '2026-10-04')");
            rejects("INSERT INTO payment (loan_id, amount, business_date, source_bank)"
                + " VALUES ('LN-1', 100, '2026-10-04', 'HBL')");
        }

        @Test
        @DisplayName("delinquency_snapshot: one row per loan per business date (BR-33)")
        void snapshotPerLoanPerDate() throws SQLException {
            final String snapshot = "INSERT INTO delinquency_snapshot (loan_id, business_date,"
                + " days_past_due, classification) VALUES ('LN-1', '2026-11-01', 0, 'STANDARD')";
            exec(connection, snapshot);
            rejects(snapshot);
        }

        @Test
        @DisplayName("instalment: one row per loan per sequence number")
        void instalmentSequenceUnique() throws SQLException {
            final String line = "INSERT INTO instalment (loan_id, sequence, due_date, principal,"
                + " interest) VALUES ('LN-1', 1, '2026-11-01', 400000, 100000)";
            exec(connection, line);
            rejects(line);
        }

        @Test
        @DisplayName("loan: outstanding below zero, or status and closure date disagreeing, is refused")
        void loanShape() {
            rejects("UPDATE loan SET outstanding = -1 WHERE id = 'LN-1'");
            rejects("UPDATE loan SET status = 'CLOSED' WHERE id = 'LN-1'");
            rejects("UPDATE loan SET closed_on = '2026-12-01' WHERE id = 'LN-1'");
            assertDoesNotThrow(() -> exec(connection,
                "UPDATE loan SET status = 'CLOSED', closed_on = '2026-12-01' WHERE id = 'LN-1'"));
        }

        @Test
        @DisplayName("loan_product: a minimum above the maximum is refused")
        void productBands() {
            rejects("INSERT INTO loan_product (code, product_type, annual_rate_bp, min_amount,"
                + " max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('BAD', 'BULLET', 2400, 500, 499, 3, 24)");
            rejects("INSERT INTO loan_product (code, product_type, annual_rate_bp, min_amount,"
                + " max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('BAD2', 'BULLET', 2400, 100, 500, 25, 24)");
        }
    }
}
