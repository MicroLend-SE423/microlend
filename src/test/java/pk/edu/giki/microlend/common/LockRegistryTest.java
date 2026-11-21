package pk.edu.giki.microlend.common;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LockRegistry} (M1 section 3.6: one lock per account).
 */
class LockRegistryTest {

    private final LockRegistry registry = new LockRegistry();

    @Test
    @DisplayName("the same account always gets the same lock, even through a new LoanId")
    void sameAccountSameLock() {
        assertSame(registry.lockFor(LoanId.of("LN-1")), registry.lockFor(LoanId.of("LN-1")));
    }

    @Test
    @DisplayName("different accounts get different locks")
    void differentAccountsDifferentLocks() {
        assertNotSame(registry.lockFor(LoanId.of("LN-1")), registry.lockFor(LoanId.of("LN-2")));
    }

    @Test
    @DisplayName("a loan id is required")
    void loanIdRequired() {
        assertThrows(NullPointerException.class, () -> registry.lockFor(null));
    }

    @Test
    @DisplayName("threads asking at the same moment all receive one lock")
    void concurrentRequestsShareOneLock() throws InterruptedException {
        final int threads = 8;
        final CountDownLatch start = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final Set<ReentrantLock> seen = ConcurrentHashMap.newKeySet();
        final ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            for (int i = 0; i < threads; i++) {
                pool.execute(() -> {
                    try {
                        start.await();
                        seen.add(registry.lockFor(LoanId.of("LN-RACE")));
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    } finally {
                        done.countDown();
                    }
                });
            }
            start.countDown();
            assertTrue(done.await(5, TimeUnit.SECONDS));
        } finally {
            pool.shutdownNow();
        }
        assertTrue(seen.size() == 1, "expected one lock, saw " + seen.size());
    }
}
