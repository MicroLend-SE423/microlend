package pk.edu.giki.microlend.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.delinquency.Classification;
import pk.edu.giki.microlend.delinquency.DailyResultStore;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * SQLite implementation of {@link DailyResultStore} (M1 section 3.6). Each
 * {@link #record} opens its own connection and runs one {@code BEGIN IMMEDIATE}
 * transaction: the snapshot row and the penalty ledger lines commit together or not at
 * all. The write lock is taken before the existence check, so two racing records for the
 * same loan and date cannot both pass it; the {@code UNIQUE (loan_id, business_date)} key
 * is the second line of defence.
 */
public final class SqliteDailyResultStore implements DailyResultStore {

    private static final String ACTIVE = "SELECT id FROM loan WHERE status = 'ACTIVE' ORDER BY id";
    private static final String BEFORE = "SELECT classification FROM delinquency_snapshot"
        + " WHERE loan_id = ? AND business_date < ? ORDER BY business_date DESC LIMIT 1";
    private static final String EXISTS = "SELECT 1 FROM delinquency_snapshot"
        + " WHERE loan_id = ? AND business_date = ?";
    private static final String INSERT = "INSERT INTO delinquency_snapshot"
        + " (loan_id, business_date, days_past_due, classification) VALUES (?, ?, ?, ?)";

    private final SqliteDatabase database;

    /**
     * Creates the store.
     *
     * @param database the database to write to
     */
    public SqliteDailyResultStore(final SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database is required");
    }

    @Override
    public List<LoanId> activeLoans() {
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(ACTIVE)) {
            final List<LoanId> loans = new ArrayList<>();
            while (rows.next()) {
                loans.add(LoanId.of(rows.getString(1)));
            }
            return loans;
        } catch (SQLException e) {
            throw new PersistenceException("could not list active loans", e);
        }
    }

    @Override
    public Optional<Classification> classificationBefore(final LoanId loanId,
                                                         final LocalDate businessDate) {
        try (Connection connection = database.connect();
             PreparedStatement select = connection.prepareStatement(BEFORE)) {
            select.setString(1, loanId.value());
            select.setString(2, businessDate.toString());
            try (ResultSet row = select.executeQuery()) {
                return row.next()
                    ? Optional.of(Classification.valueOf(row.getString(1)))
                    : Optional.empty();
            }
        } catch (SQLException e) {
            throw new PersistenceException("could not read the classification of " + loanId, e);
        }
    }

    @Override
    public boolean record(final LoanId loanId, final LocalDate businessDate,
                          final int daysPastDue, final Classification classification,
                          final List<LedgerEntry> accruals) {
        Objects.requireNonNull(accruals, "accrual lines are required");
        try (Connection connection = database.connect();
             Statement transaction = connection.createStatement()) {
            transaction.execute("BEGIN IMMEDIATE");
            boolean committed = false;
            try {
                if (exists(connection, loanId, businessDate)) {
                    return false;
                }
                insertSnapshot(connection, loanId, businessDate, daysPastDue, classification);
                for (final LedgerEntry line : accruals) {
                    SqliteLedgerRepository.append(connection, line);
                }
                transaction.execute("COMMIT");
                committed = true;
                return true;
            } finally {
                if (!committed) {
                    transaction.execute("ROLLBACK");
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("could not record end of day " + businessDate
                + " for " + loanId, e);
        }
    }

    private static boolean exists(final Connection connection, final LoanId loanId,
                                  final LocalDate businessDate) throws SQLException {
        try (PreparedStatement select = connection.prepareStatement(EXISTS)) {
            select.setString(1, loanId.value());
            select.setString(2, businessDate.toString());
            try (ResultSet row = select.executeQuery()) {
                return row.next();
            }
        }
    }

    private static void insertSnapshot(final Connection connection, final LoanId loanId,
                                       final LocalDate businessDate, final int daysPastDue,
                                       final Classification classification) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insert.setString(1, loanId.value());
            insert.setString(2, businessDate.toString());
            insert.setInt(3, daysPastDue);
            insert.setString(4, classification.name());
            insert.executeUpdate();
        }
    }
}
