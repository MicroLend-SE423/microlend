package pk.edu.giki.microlend.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;
import pk.edu.giki.microlend.ledger.PaymentReferences;

/**
 * SQLite implementation of {@link PaymentReferences} (BR-25, FR-18). Claimed pairs are
 * rows of {@code payment_reference}, so a statement imported again after a restart is
 * still refused.
 *
 * <p>A claim is one {@code INSERT OR IGNORE}. SQLite runs it under its write lock and the
 * primary key admits a pair once, so of several concurrent claims exactly one inserts a
 * row and returns true. A release deletes the row.</p>
 *
 * <p>The claim commits on its own, before the posting. If the process stops between the
 * two, the pair stays claimed although nothing was posted, and the row must be deleted
 * before the payment is imported again. Claiming inside the posting's transaction belongs
 * with writing full {@code payment} rows.</p>
 */
public final class SqlitePaymentReferences implements PaymentReferences {

    private static final String CLAIM = "INSERT OR IGNORE INTO payment_reference"
        + " (source_bank, bank_reference) VALUES (?, ?)";
    private static final String RELEASE = "DELETE FROM payment_reference"
        + " WHERE source_bank = ? AND bank_reference = ?";

    private final SqliteDatabase database;

    /**
     * Creates the store.
     *
     * @param database the database holding the claimed pairs
     * @throws NullPointerException if the database is null
     */
    public SqlitePaymentReferences(final SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database is required");
    }

    @Override
    public boolean claim(final String bank, final String reference) {
        return execute(CLAIM, bank, reference) == 1;
    }

    @Override
    public void release(final String bank, final String reference) {
        execute(RELEASE, bank, reference);
    }

    /** Runs one statement in its own connection; returns the number of rows changed. */
    private int execute(final String sql, final String bank, final String reference) {
        // Checked here because INSERT OR IGNORE would silently skip a NULL and report a duplicate.
        Objects.requireNonNull(bank, "bank is required");
        Objects.requireNonNull(reference, "reference is required");
        try (Connection connection = database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, bank);
            statement.setString(2, reference);
            return statement.executeUpdate();
        } catch (SQLException e) {
            throw new PersistenceException("could not update payment reference " + bank + "/"
                + reference, e);
        }
    }
}
