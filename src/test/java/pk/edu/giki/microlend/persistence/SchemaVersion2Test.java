package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for schema version 2: the {@code PENALTY_INCOME} entry type that balances
 * penalty accrual (team decision D-04, open item T-02), one reversal at most per
 * ledger entry (BR-24), and the {@code payment_reference} table of claimed bank
 * references (BR-25). Version 1 is never edited; existing databases are upgraded.
 */
class SchemaVersion2Test {

    private static final String SEED_LOAN =
        "INSERT INTO customer (id, full_name, date_of_birth, monthly_income)"
            + " VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000);"
            + "INSERT INTO loan_product (code, product_type, annual_rate_bp, min_amount,"
            + " max_amount, min_tenure_months, max_tenure_months)"
            + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24);"
            + "INSERT INTO loan (id, customer_id, product_code, principal, outstanding, status,"
            + " risk_grade, disbursed_on) VALUES ('LN-1', 'C-1', 'FLAT', 1000000, 1000000,"
            + " 'ACTIVE', 'A', '2026-10-01')";

    @TempDir
    private Path dir;

    private static void exec(final Connection connection, final String sql) throws SQLException {
        try (Statement statement = connection.createStatement()) {
            for (final String part : sql.split(";")) {
                statement.executeUpdate(part);
            }
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

    private static String insertEntry(final String type, final int debit, final int credit) {
        return "INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at)"
            + " VALUES ('LN-1', '" + type + "', " + debit + ", " + credit
            + ", '2027-03-11T18:00:00Z')";
    }

    private static String reversalOf(final int row) {
        return "INSERT INTO ledger_entry (loan_id, entry_type, debit, credit, posted_at,"
            + " reverses_entry_id, reason) VALUES ('LN-1', 'REVERSAL', 0, 500,"
            + " '2027-03-11T18:05:00Z', " + row + ", 'posted in error')";
    }

    @Test
    @DisplayName("a new database is at version 2 and accepts PENALTY_INCOME")
    void newDatabaseAcceptsPenaltyIncome() throws SQLException {
        final SqliteDatabase database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("new.db"));
        try (Connection connection = database.connect()) {
            assertEquals(2, queryLong(connection, "PRAGMA user_version"));
            exec(connection, SEED_LOAN);
            exec(connection, insertEntry("PENALTY_ACCRUAL", 500, 0));
            exec(connection, insertEntry("PENALTY_INCOME", 0, 500));
            assertEquals(2, queryLong(connection, "SELECT COUNT(*) FROM ledger_entry"));
        }
    }

    @Test
    @DisplayName("an entry can be reversed only once (BR-24)")
    void reversedOnlyOnce() throws SQLException {
        final SqliteDatabase database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("rev.db"));
        try (Connection connection = database.connect()) {
            exec(connection, SEED_LOAN);
            exec(connection, insertEntry("PAYMENT_RECEIVED", 500, 0));
            exec(connection, reversalOf(1));
            assertThrows(SQLException.class, () -> exec(connection, reversalOf(1)));
        }
    }

    @Test
    @DisplayName("a version 1 database keeps its rows and reversal links when upgraded")
    void upgradesVersion1InPlace() throws SQLException {
        final String url = "jdbc:sqlite:" + dir.resolve("old.db");
        try (Connection raw = DriverManager.getConnection(url)) {
            exec(raw, "PRAGMA foreign_keys = ON");
            exec(raw, String.join(";", SchemaMigrator.VERSION_1));
            exec(raw, "PRAGMA user_version = 1");
            exec(raw, SEED_LOAN);
            exec(raw, insertEntry("PAYMENT_RECEIVED", 500, 0));
            exec(raw, reversalOf(1));
            assertThrows(SQLException.class, () -> exec(raw, insertEntry("PENALTY_INCOME", 0, 5)));
        }

        try (Connection connection = new SqliteDatabase(url).connect()) {
            assertEquals(2, queryLong(connection, "PRAGMA user_version"));
            assertEquals(2, queryLong(connection, "SELECT COUNT(*) FROM ledger_entry"));
            assertEquals(1, queryLong(connection,
                "SELECT reverses_entry_id FROM ledger_entry WHERE id = 2"));
            exec(connection, insertEntry("PENALTY_INCOME", 0, 5));
            assertEquals(0, queryLong(connection, "SELECT COUNT(*) FROM payment_reference"));
            try (Statement statement = connection.createStatement();
                 ResultSet violations = statement.executeQuery("PRAGMA foreign_key_check")) {
                assertFalse(violations.next());
            }
        }
    }
}
