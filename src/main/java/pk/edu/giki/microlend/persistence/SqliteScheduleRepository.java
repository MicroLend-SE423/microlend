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
import pk.edu.giki.microlend.products.Instalment;
import pk.edu.giki.microlend.products.Schedule;
import pk.edu.giki.microlend.products.ScheduleRepository;

/**
 * The SQLite implementation of {@link ScheduleRepository} over the {@code instalment} table
 * (queue item H-06; M1 design section 3.7).
 *
 * <p>One connection is opened per call. A save runs in a {@code BEGIN IMMEDIATE} transaction
 * and writes every instalment or none, so a schedule is never half stored. Amounts are
 * integer paisa and dates are ISO-8601 text.</p>
 */
public final class SqliteScheduleRepository implements ScheduleRepository {

    private static final String COUNT = "SELECT COUNT(*) FROM instalment WHERE loan_id = ?";
    private static final String INSERT = "INSERT INTO instalment (loan_id, sequence, due_date,"
        + " principal, interest) VALUES (?, ?, ?, ?, ?)";
    private static final String SELECT = "SELECT sequence, due_date, principal, interest"
        + " FROM instalment WHERE loan_id = ? ORDER BY sequence";

    private final SqliteDatabase database;

    /**
     * Creates the repository over a database handle.
     *
     * @param database the database holding the {@code instalment} table
     * @throws NullPointerException if the database is null
     */
    public SqliteScheduleRepository(final SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database is required");
    }

    @Override
    public void save(final LoanId loanId, final Schedule schedule) {
        Objects.requireNonNull(loanId, "loan id is required");
        Objects.requireNonNull(schedule, "schedule is required");
        try (Connection connection = database.connect();
             Statement transaction = connection.createStatement()) {
            transaction.execute("BEGIN IMMEDIATE");
            boolean committed = false;
            try {
                if (countFor(connection, loanId) > 0) {
                    throw new PersistenceException("loan " + loanId
                        + " already has a schedule; a schedule is fixed for the life of the loan");
                }
                insertAll(connection, loanId, schedule);
                transaction.execute("COMMIT");
                committed = true;
            } finally {
                if (!committed) {
                    transaction.execute("ROLLBACK");
                }
            }
        } catch (SQLException e) {
            throw new PersistenceException("could not save the schedule of loan " + loanId, e);
        }
    }

    @Override
    public Optional<Schedule> scheduleFor(final LoanId loanId) {
        Objects.requireNonNull(loanId, "loan id is required");
        try (Connection connection = database.connect();
             PreparedStatement select = connection.prepareStatement(SELECT)) {
            select.setString(1, loanId.value());
            final List<Instalment> instalments = new ArrayList<>();
            try (ResultSet rows = select.executeQuery()) {
                while (rows.next()) {
                    instalments.add(new Instalment(rows.getInt("sequence"),
                        LocalDate.parse(rows.getString("due_date")),
                        Paisa.toMoney(rows.getLong("principal")),
                        Paisa.toMoney(rows.getLong("interest"))));
                }
            }
            return instalments.isEmpty() ? Optional.empty() : Optional.of(new Schedule(instalments));
        } catch (SQLException e) {
            throw new PersistenceException("could not read the schedule of loan " + loanId, e);
        }
    }

    private static long countFor(final Connection connection, final LoanId loanId)
        throws SQLException {
        try (PreparedStatement count = connection.prepareStatement(COUNT)) {
            count.setString(1, loanId.value());
            try (ResultSet row = count.executeQuery()) {
                row.next();
                return row.getLong(1);
            }
        }
    }

    private static void insertAll(final Connection connection, final LoanId loanId,
                                  final Schedule schedule) throws SQLException {
        try (PreparedStatement insert = connection.prepareStatement(INSERT)) {
            for (Instalment instalment : schedule.instalments()) {
                insert.setString(1, loanId.value());
                insert.setInt(2, instalment.sequence());
                insert.setString(3, instalment.dueDate().toString());
                insert.setLong(4, Paisa.of(instalment.principal()));
                insert.setLong(5, Paisa.of(instalment.interest()));
                insert.executeUpdate();
            }
        }
    }
}
