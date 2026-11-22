package pk.edu.giki.microlend.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.ledger.LoanAccount;
import pk.edu.giki.microlend.ledger.LoanRepository;
import pk.edu.giki.microlend.ledger.LoanStatus;

/**
 * The SQLite implementation of {@link LoanRepository} (queue item L-15; M1
 * design section 3.6).
 *
 * <p>Only the ledger's view of a loan is read and written: status,
 * outstanding principal and closure date. A loan row is created by
 * origination, so saving a loan that does not exist is refused rather than
 * inserted. Every save runs in a {@code BEGIN IMMEDIATE} transaction, which
 * takes SQLite's write lock up front; a writer that finds the lock held waits
 * up to the connection's busy timeout ({@link SqliteDatabase}) instead of
 * failing at once.</p>
 */
public final class SqliteLoanRepository implements LoanRepository {

    private static final String SELECT = "SELECT status, outstanding, closed_on FROM loan"
        + " WHERE id = ?";
    private static final String UPDATE = "UPDATE loan SET status = ?, outstanding = ?,"
        + " closed_on = ? WHERE id = ?";

    private final SqliteDatabase database;

    /**
     * Creates the repository over a database handle.
     *
     * @param database the database holding the {@code loan} table
     * @throws NullPointerException if the database is null
     */
    public SqliteLoanRepository(final SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database is required");
    }

    @Override
    public Optional<LoanAccount> findById(final LoanId loanId) {
        Objects.requireNonNull(loanId, "loan id is required");
        try (Connection connection = database.connect();
             PreparedStatement select = connection.prepareStatement(SELECT)) {
            select.setString(1, loanId.value());
            try (ResultSet row = select.executeQuery()) {
                if (!row.next()) {
                    return Optional.empty();
                }
                final String closedOn = row.getString("closed_on");
                return Optional.of(LoanAccount.restore(loanId,
                    LoanStatus.valueOf(row.getString("status")),
                    Paisa.toMoney(row.getLong("outstanding")),
                    closedOn == null ? null : LocalDate.parse(closedOn)));
            }
        } catch (SQLException e) {
            throw new PersistenceException("could not read loan " + loanId, e);
        }
    }

    @Override
    public void save(final LoanAccount account) {
        Objects.requireNonNull(account, "account is required");
        try (Connection connection = database.connect();
             Statement transaction = connection.createStatement()) {
            transaction.execute("BEGIN IMMEDIATE");
            boolean committed = false;
            try (PreparedStatement update = connection.prepareStatement(UPDATE)) {
                update.setString(1, account.status().name());
                update.setLong(2, Paisa.of(account.outstandingPrincipal()));
                update.setString(3, account.closedOn().map(LocalDate::toString).orElse(null));
                update.setString(4, account.id().value());
                if (update.executeUpdate() != 1) {
                    throw new PersistenceException("no loan " + account.id()
                        + " to save; loans are created by origination");
                }
                transaction.execute("COMMIT");
                committed = true;
            } finally {
                if (!committed) {
                    transaction.execute("ROLLBACK");
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("could not save loan " + account.id(), e);
        }
    }
}
