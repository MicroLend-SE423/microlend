package pk.edu.giki.microlend.persistence;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;

/**
 * Creates the MicroLend schema (M1 design section 3.7) in a SQLite database.
 *
 * <p>The schema re-states the code's invariants, so a corrupt write fails in
 * the domain object and again in the database. Monetary columns are
 * {@code INTEGER} minor units (paisa), never {@code REAL} (constraint C-04).
 * The applied version is kept in SQLite's {@code user_version}; a database
 * already at {@link #CURRENT_VERSION} is left untouched, so migrating is
 * idempotent. All statements of a version run in one transaction.</p>
 *
 * <p>The {@code customer}, {@code loan_product}, {@code instalment} and
 * {@code delinquency_snapshot} tables hold only the columns the M1 design
 * names; their module owners extend them through later versions.</p>
 */
public final class SchemaMigrator {

    /** The schema version this class produces. */
    public static final int CURRENT_VERSION = 2;

    /** Version 1 as first released; never edited, so old databases upgrade cleanly. */
    static final List<String> VERSION_1 = List.of(
        "CREATE TABLE customer ("
            + " id TEXT PRIMARY KEY,"
            + " full_name TEXT NOT NULL,"
            + " date_of_birth TEXT NOT NULL,"
            + " monthly_income INTEGER NOT NULL CHECK (monthly_income >= 0))",
        "CREATE TABLE loan_product ("
            + " code TEXT PRIMARY KEY,"
            + " product_type TEXT NOT NULL"
            + "   CHECK (product_type IN ('FLAT_RATE', 'REDUCING_BALANCE', 'BULLET')),"
            + " annual_rate_bp INTEGER NOT NULL CHECK (annual_rate_bp >= 0),"
            + " min_amount INTEGER NOT NULL CHECK (min_amount > 0),"
            + " max_amount INTEGER NOT NULL,"
            + " min_tenure_months INTEGER NOT NULL CHECK (min_tenure_months >= 1),"
            + " max_tenure_months INTEGER NOT NULL,"
            + " CHECK (min_amount <= max_amount),"
            + " CHECK (min_tenure_months <= max_tenure_months))",
        "CREATE TABLE loan ("
            + " id TEXT PRIMARY KEY,"
            + " customer_id TEXT NOT NULL REFERENCES customer (id),"
            + " product_code TEXT NOT NULL REFERENCES loan_product (code),"
            + " principal INTEGER NOT NULL CHECK (principal > 0),"
            + " outstanding INTEGER NOT NULL CHECK (outstanding >= 0),"
            + " status TEXT NOT NULL CHECK (status IN ('ACTIVE', 'CLOSED', 'WRITTEN_OFF')),"
            + " risk_grade TEXT NOT NULL CHECK (risk_grade IN ('A', 'B', 'C')),"
            + " disbursed_on TEXT NOT NULL,"
            + " closed_on TEXT,"
            + " CHECK ((status = 'ACTIVE') = (closed_on IS NULL)))",
        "CREATE TABLE instalment ("
            + " id INTEGER PRIMARY KEY,"
            + " loan_id TEXT NOT NULL REFERENCES loan (id),"
            + " sequence INTEGER NOT NULL CHECK (sequence >= 1),"
            + " due_date TEXT NOT NULL,"
            + " principal INTEGER NOT NULL CHECK (principal >= 0),"
            + " interest INTEGER NOT NULL CHECK (interest >= 0),"
            + " UNIQUE (loan_id, sequence))",
        "CREATE TABLE ledger_entry ("
            + " id INTEGER PRIMARY KEY,"
            + " loan_id TEXT NOT NULL REFERENCES loan (id),"
            + " entry_type TEXT NOT NULL CHECK (entry_type IN ('PRINCIPAL_DISBURSEMENT',"
            + "   'PENALTY_ACCRUAL', 'PAYMENT_RECEIVED', 'PENALTY_REPAYMENT', 'FEE_REPAYMENT', 'INTEREST_REPAYMENT',"
            + "   'PRINCIPAL_REPAYMENT', 'ADVANCE_CREDIT', 'INTEREST_REBATE', 'WRITE_OFF',"
            + "   'REVERSAL')),"
            + " debit INTEGER NOT NULL,"
            + " credit INTEGER NOT NULL,"
            + " posted_at TEXT NOT NULL,"
            + " reverses_entry_id INTEGER REFERENCES ledger_entry (id),"
            + " reason TEXT,"
            + " CHECK (debit >= 0 AND credit >= 0),"
            + " CHECK ((debit = 0) <> (credit = 0)),"
            + " CHECK ((reverses_entry_id IS NULL) = (reason IS NULL)),"
            + " CHECK (reverses_entry_id IS NULL OR entry_type = 'REVERSAL'))",
        "CREATE TABLE payment ("
            + " id INTEGER PRIMARY KEY,"
            + " loan_id TEXT NOT NULL REFERENCES loan (id),"
            + " amount INTEGER NOT NULL CHECK (amount > 0),"
            + " business_date TEXT NOT NULL,"
            + " source_bank TEXT,"
            + " bank_reference TEXT,"
            + " CHECK ((source_bank IS NULL) = (bank_reference IS NULL)),"
            + " UNIQUE (source_bank, bank_reference))",
        "CREATE TABLE delinquency_snapshot ("
            + " id INTEGER PRIMARY KEY,"
            + " loan_id TEXT NOT NULL REFERENCES loan (id),"
            + " business_date TEXT NOT NULL,"
            + " days_past_due INTEGER NOT NULL CHECK (days_past_due >= 0),"
            + " classification TEXT NOT NULL CHECK (classification IN"
            + "   ('STANDARD', 'WATCH', 'SUBSTANDARD', 'DOUBTFUL', 'LOSS')),"
            + " UNIQUE (loan_id, business_date))"
    );

    /**
     * Version 2. SQLite cannot alter a CHECK constraint, so {@code ledger_entry} is
     * rebuilt with {@code PENALTY_INCOME} added (D-04, T-02) and its rows copied in id
     * order, which keeps every reversal link valid. The unique index allows each entry
     * to be reversed at most once (BR-24); SQLite unique indexes admit many NULLs.
     * {@code payment_reference} holds the claimed (bank, reference) pairs (BR-25), so a
     * duplicate is refused across restarts; its primary key makes a claim atomic.
     */
    private static final List<String> VERSION_2 = List.of(
        "CREATE TABLE ledger_entry_v2 ("
            + " id INTEGER PRIMARY KEY,"
            + " loan_id TEXT NOT NULL REFERENCES loan (id),"
            + " entry_type TEXT NOT NULL CHECK (entry_type IN ('PRINCIPAL_DISBURSEMENT',"
            + "   'PENALTY_ACCRUAL', 'PENALTY_INCOME', 'PAYMENT_RECEIVED', 'PENALTY_REPAYMENT',"
            + "   'FEE_REPAYMENT', 'INTEREST_REPAYMENT', 'PRINCIPAL_REPAYMENT', 'ADVANCE_CREDIT',"
            + "   'INTEREST_REBATE', 'WRITE_OFF', 'REVERSAL')),"
            + " debit INTEGER NOT NULL,"
            + " credit INTEGER NOT NULL,"
            + " posted_at TEXT NOT NULL,"
            + " reverses_entry_id INTEGER REFERENCES ledger_entry_v2 (id),"
            + " reason TEXT,"
            + " CHECK (debit >= 0 AND credit >= 0),"
            + " CHECK ((debit = 0) <> (credit = 0)),"
            + " CHECK ((reverses_entry_id IS NULL) = (reason IS NULL)),"
            + " CHECK (reverses_entry_id IS NULL OR entry_type = 'REVERSAL'))",
        "INSERT INTO ledger_entry_v2 (id, loan_id, entry_type, debit, credit, posted_at,"
            + " reverses_entry_id, reason) SELECT id, loan_id, entry_type, debit, credit,"
            + " posted_at, reverses_entry_id, reason FROM ledger_entry ORDER BY id",
        "DROP TABLE ledger_entry",
        "ALTER TABLE ledger_entry_v2 RENAME TO ledger_entry",
        "CREATE UNIQUE INDEX ledger_entry_reversed_once ON ledger_entry (reverses_entry_id)",
        "CREATE TABLE payment_reference ("
            + " source_bank TEXT NOT NULL,"
            + " bank_reference TEXT NOT NULL,"
            + " PRIMARY KEY (source_bank, bank_reference))"
    );

    /**
     * Brings the database on this connection up to {@link #CURRENT_VERSION}.
     *
     * @param connection an open connection; its auto-commit setting is restored afterwards
     * @throws PersistenceException if the schema cannot be read or created; a failed
     *     version is rolled back as a whole
     */
    public void migrate(final Connection connection) {
        try {
            final int version = currentVersion(connection);
            if (version >= CURRENT_VERSION) {
                return;
            }
            apply(connection, version);
        } catch (SQLException e) {
            throw new PersistenceException("could not migrate the schema to version "
                + CURRENT_VERSION, e);
        }
    }

    private static int currentVersion(final Connection connection) throws SQLException {
        try (Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery("PRAGMA user_version")) {
            if (!rows.next()) {
                throw new SQLException("PRAGMA user_version returned no row");
            }
            return rows.getInt(1);
        }
    }

    private static void apply(final Connection connection, final int fromVersion)
        throws SQLException {
        final boolean autoCommit = connection.getAutoCommit();
        connection.setAutoCommit(false);
        try (Statement statement = connection.createStatement()) {
            if (fromVersion < 1) {
                for (final String ddl : VERSION_1) {
                    statement.executeUpdate(ddl);
                }
            }
            for (final String ddl : VERSION_2) {
                statement.executeUpdate(ddl);
            }
            statement.executeUpdate("PRAGMA user_version = " + CURRENT_VERSION);
            connection.commit();
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(autoCommit);
        }
    }
}
