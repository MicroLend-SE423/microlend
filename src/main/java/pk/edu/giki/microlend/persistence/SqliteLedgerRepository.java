package pk.edu.giki.microlend.persistence;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.LedgerEntry;
import pk.edu.giki.microlend.ledger.LedgerReader;
import pk.edu.giki.microlend.ledger.LedgerWriter;

/**
 * The SQLite implementation of the {@link LedgerWriter} and {@link LedgerReader}
 * roles (queue item L-14, QR-05).
 *
 * <p>The ledger is append-only: this class issues {@code INSERT} and
 * {@code SELECT} against {@code ledger_entry} and nothing else, so a posted
 * entry can only be corrected by appending a reversal (BR-24). Money is stored
 * as exact {@code INTEGER} paisa (C-04) and timestamps as ISO-8601 text.</p>
 *
 * <p>A reversal is stored against the row id of the entry it reverses. When
 * identical entries exist, the earliest one not yet reversed is chosen, so two
 * reversals of two identical lines reference two different rows.</p>
 */
public final class SqliteLedgerRepository implements LedgerWriter, LedgerReader {

    private static final String INSERT = "INSERT INTO ledger_entry (loan_id, entry_type,"
        + " debit, credit, posted_at, reverses_entry_id, reason) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SELECT_FOR_LOAN = "SELECT id, entry_type, debit, credit,"
        + " posted_at, reverses_entry_id, reason FROM ledger_entry WHERE loan_id = ? ORDER BY id";
    private static final String SELECT_REVERSED = "SELECT reverses_entry_id FROM ledger_entry"
        + " WHERE loan_id = ? AND reverses_entry_id IS NOT NULL";

    private final SqliteDatabase database;

    /**
     * Creates the repository over a database handle.
     *
     * @param database the database to read and append to
     * @throws NullPointerException if the database is null
     */
    public SqliteLedgerRepository(final SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database is required");
    }

    @Override
    public void append(final LedgerEntry entry) {
        Objects.requireNonNull(entry, "entry is required");
        try (Connection connection = database.connect()) {
            append(connection, entry);
        } catch (SQLException e) {
            throw new PersistenceException("could not append " + entry, e);
        }
    }

    @Override
    public void appendAll(final List<LedgerEntry> entries) {
        final List<LedgerEntry> lines = List.copyOf(Objects.requireNonNull(entries,
            "entries are required"));
        try (Connection connection = database.connect()) {
            connection.setAutoCommit(false);
            try {
                for (final LedgerEntry line : lines) {
                    append(connection, line);
                }
                connection.commit();
            } catch (SQLException | PersistenceException e) {
                connection.rollback();
                throw e;
            }
        } catch (SQLException e) {
            throw new PersistenceException("could not append a posting of "
                + lines.size() + " lines; none was written", e);
        }
    }

    /**
     * Appends one entry on a connection the caller controls, so a posting
     * service can write several lines inside its own transaction.
     *
     * @param connection an open connection from {@link SqliteDatabase#connect()}
     * @param entry the entry to append
     * @throws SQLException if the database refuses the row
     * @throws PersistenceException if the entry reverses one that is not in the ledger
     */
    static void append(final Connection connection, final LedgerEntry entry)
        throws SQLException {
        Long reversedRow = null;
        if (entry.isReversal()) {
            reversedRow = rowToReverse(connection, entry.reverses().orElseThrow());
        }
        try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
            insert.setString(1, entry.loanId().value());
            insert.setString(2, entry.type().name());
            insert.setLong(3, toPaisa(entry.debit()));
            insert.setLong(4, toPaisa(entry.credit()));
            insert.setString(5, entry.postedAt().toString());
            if (reversedRow == null) {
                insert.setNull(6, Types.INTEGER);
                insert.setNull(7, Types.VARCHAR);
            } else {
                insert.setLong(6, reversedRow);
                insert.setString(7, entry.reason().orElseThrow());
            }
            insert.executeUpdate();
        }
    }

    @Override
    public List<LedgerEntry> entriesFor(final LoanId loanId) {
        Objects.requireNonNull(loanId, "loan id is required");
        try (Connection connection = database.connect()) {
            return List.copyOf(readAll(connection, loanId).values());
        } catch (SQLException e) {
            throw new PersistenceException("could not read the ledger of " + loanId, e);
        }
    }

    private static Map<Long, LedgerEntry> readAll(final Connection connection,
                                                  final LoanId loanId) throws SQLException {
        final Map<Long, LedgerEntry> byRow = new LinkedHashMap<>();
        try (PreparedStatement select = connection.prepareStatement(SELECT_FOR_LOAN)) {
            select.setString(1, loanId.value());
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    byRow.put(rows.getLong("id"), toEntry(loanId, rows, byRow));
                }
            }
        }
        return byRow;
    }

    private static LedgerEntry toEntry(final LoanId loanId, final ResultSet row,
                                       final Map<Long, LedgerEntry> earlier)
        throws SQLException {
        final Instant postedAt = Instant.parse(row.getString("posted_at"));
        final long reversedRow = row.getLong("reverses_entry_id");
        if (!row.wasNull()) {
            final LedgerEntry original = earlier.get(reversedRow);
            if (original == null) {
                throw new PersistenceException("ledger row " + row.getLong("id")
                    + " reverses row " + reversedRow + " of another loan");
            }
            return LedgerEntry.reversalOf(original, row.getString("reason"), postedAt);
        }
        final EntryType type = EntryType.valueOf(row.getString("entry_type"));
        final Money debit = fromPaisa(row.getLong("debit"));
        return debit.isZero()
            ? LedgerEntry.credit(loanId, type, fromPaisa(row.getLong("credit")), postedAt)
            : LedgerEntry.debit(loanId, type, debit, postedAt);
    }

    private static long rowToReverse(final Connection connection, final LedgerEntry original)
        throws SQLException {
        final Map<Long, LedgerEntry> ledger = readAll(connection, original.loanId());
        final Set<Long> alreadyReversed = new HashSet<>();
        try (PreparedStatement select = connection.prepareStatement(SELECT_REVERSED)) {
            select.setString(1, original.loanId().value());
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    alreadyReversed.add(rows.getLong(1));
                }
            }
        }
        final List<Long> matches = new ArrayList<>();
        for (final Map.Entry<Long, LedgerEntry> row : ledger.entrySet()) {
            if (row.getValue().equals(original)) {
                matches.add(row.getKey());
            }
        }
        if (matches.isEmpty()) {
            throw new PersistenceException("cannot reverse " + original
                + ": it is not in the ledger");
        }
        for (final Long id : matches) {
            if (!alreadyReversed.contains(id)) {
                return id;
            }
        }
        return matches.get(0);
    }

    private static long toPaisa(final Money amount) {
        return new BigDecimal(amount.toPlainString()).movePointRight(Money.SCALE).longValueExact();
    }

    private static Money fromPaisa(final long paisa) {
        return Money.of(BigDecimal.valueOf(paisa, Money.SCALE));
    }
}
