package pk.edu.giki.microlend.persistence;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Objects;

/**
 * Opens connections to the local, file-based SQLite database.
 *
 * <p>SQLite leaves foreign keys off by default and the setting is per
 * connection, so every connection handed out has {@code PRAGMA foreign_keys = ON}
 * (M1 design section 3.7). The first connection also applies the schema
 * through {@link SchemaMigrator}. Busy timeouts and {@code BEGIN IMMEDIATE}
 * transactions belong to the repositories (queue item L-15).</p>
 */
public final class SqliteDatabase {

    private final String jdbcUrl;
    private final SchemaMigrator migrator = new SchemaMigrator();
    private volatile boolean migrated;

    /**
     * Creates a database handle; nothing is opened until {@link #connect()}.
     *
     * @param jdbcUrl a SQLite JDBC URL such as {@code jdbc:sqlite:microlend.db}
     * @throws NullPointerException if the URL is null
     */
    public SqliteDatabase(final String jdbcUrl) {
        this.jdbcUrl = Objects.requireNonNull(jdbcUrl, "JDBC URL is required");
    }

    /**
     * Opens a new connection with foreign keys enforced, applying the schema
     * on the first call.
     *
     * @return an open connection that the caller must close
     * @throws PersistenceException if the database cannot be opened or migrated
     */
    public Connection connect() {
        final Connection connection = open();
        try {
            ensureMigrated(connection);
            return connection;
        } catch (PersistenceException e) {
            closeQuietly(connection, e);
            throw e;
        }
    }

    private Connection open() {
        Connection connection = null;
        try {
            connection = DriverManager.getConnection(jdbcUrl);
            try (Statement statement = connection.createStatement()) {
                statement.execute("PRAGMA foreign_keys = ON");
            }
            return connection;
        } catch (SQLException e) {
            final PersistenceException failure =
                new PersistenceException("could not open the database at " + jdbcUrl, e);
            closeQuietly(connection, failure);
            throw failure;
        }
    }

    private void ensureMigrated(final Connection connection) {
        if (migrated) {
            return;
        }
        synchronized (this) {
            if (!migrated) {
                migrator.migrate(connection);
                migrated = true;
            }
        }
    }

    private static void closeQuietly(final Connection connection,
                                     final PersistenceException failure) {
        if (connection == null) {
            return;
        }
        try {
            connection.close();
        } catch (SQLException suppressed) {
            failure.addSuppressed(suppressed);
        }
    }
}
