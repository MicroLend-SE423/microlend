package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.IdempotencyGuardPoster;
import pk.edu.giki.microlend.ledger.LoanNotFoundException;
import pk.edu.giki.microlend.ledger.RepaymentCommand;

/**
 * Tests for {@link SqlitePaymentReferences}: the (bank, reference) pairs of BR-25 kept in
 * SQLite, so a statement imported again after a restart is still refused (FR-18).
 */
class SqlitePaymentReferencesTest {

    private static final int THREADS = 8;

    @TempDir
    private Path dir;

    private String url;
    private SqlitePaymentReferences references;

    @BeforeEach
    void openDatabase() {
        url = "jdbc:sqlite:" + dir.resolve("references.db");
        references = new SqlitePaymentReferences(new SqliteDatabase(url));
    }

    /** A new handle on the same database file, as the application has after a restart. */
    private SqlitePaymentReferences restarted() {
        return new SqlitePaymentReferences(new SqliteDatabase(url));
    }

    @Test
    @DisplayName("a new pair is claimed; claiming it again is refused")
    void claimOnce() {
        assertTrue(references.claim("HBL", "TX-1"));
        assertFalse(references.claim("HBL", "TX-1"));
    }

    @Test
    @DisplayName("a claim survives a restart: a new handle on the same file refuses the pair")
    void claimSurvivesRestart() {
        references.claim("HBL", "TX-1");
        assertFalse(restarted().claim("HBL", "TX-1"));
    }

    @Test
    @DisplayName("a released pair can be claimed again")
    void releaseFreesThePair() {
        references.claim("HBL", "TX-1");
        references.release("HBL", "TX-1");
        assertTrue(references.claim("HBL", "TX-1"));
    }

    @Test
    @DisplayName("the same reference from two banks is two different pairs")
    void sameReferenceTwoBanks() {
        assertTrue(references.claim("HBL", "TX-1"));
        assertTrue(references.claim("MCB", "TX-1"));
    }

    @Test
    @DisplayName("eight threads claiming one pair at once: exactly one wins")
    void concurrentClaimsHaveOneWinner() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> outcomes = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            outcomes.add(pool.submit(() -> {
                start.await();
                return references.claim("HBL", "TX-RACE");
            }));
        }
        start.countDown();
        int winners = 0;
        for (final Future<Boolean> outcome : outcomes) {
            winners += outcome.get(10, TimeUnit.SECONDS) ? 1 : 0;
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1, winners);
    }

    @Test
    @DisplayName("a posting that fails after the claim leaves the pair free in the database")
    void failedPostingReleasesTheStoredPair() {
        final LoanId loan = LoanId.of("LN-1");
        final IdempotencyGuardPoster guard = new IdempotencyGuardPoster(command -> {
            throw new LoanNotFoundException(loan);
        }, references);
        assertThrows(LoanNotFoundException.class, () -> guard.post(RepaymentCommand.bank(loan,
            Money.of(new BigDecimal("10.00")), LocalDate.of(2026, 3, 20), "HBL", "TX-9")));
        assertTrue(restarted().claim("HBL", "TX-9"));
    }

    @Test
    @DisplayName("a storage failure is reported as a PersistenceException; null is refused")
    void failuresAreReported() throws SQLException {
        final SqliteDatabase database = new SqliteDatabase(url);
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("DROP TABLE payment_reference");
        }
        final SqlitePaymentReferences broken = new SqlitePaymentReferences(database);
        assertThrows(PersistenceException.class, () -> broken.claim("HBL", "TX-1"));
        assertThrows(PersistenceException.class, () -> broken.release("HBL", "TX-1"));
        assertThrows(NullPointerException.class, () -> references.claim(null, "TX-1"));
        assertThrows(NullPointerException.class, () -> references.claim("HBL", null));
    }
}
