package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LockRegistry;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link LockingPoster} (queue item I-07; M1 section 3.6): a posting runs
 * under its account's lock from the {@link LockRegistry} shared with the end-of-day engine.
 */
class LockingPosterTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LoanId OTHER = LoanId.of("LN-2");
    private static final LocalDate TODAY = LocalDate.of(2027, 3, 11);

    private final LockRegistry locks = new LockRegistry();
    private final AtomicInteger postedUnderLock = new AtomicInteger();
    private final AtomicInteger calls = new AtomicInteger();
    private final ExecutorService worker = Executors.newSingleThreadExecutor();

    @AfterEach
    void stopWorker() {
        worker.shutdownNow();
    }

    private static RepaymentCommand cash(final LoanId loan) {
        return RepaymentCommand.cash(loan, Money.of(new BigDecimal("10.00")), TODAY);
    }

    private PostingResult record(final RepaymentCommand command) {
        calls.incrementAndGet();
        if (locks.lockFor(command.loanId()).isHeldByCurrentThread()) {
            postedUnderLock.incrementAndGet();
        }
        return new PostingResult(command, null, null, List.of());
    }

    @Test
    @DisplayName("the wrapped poster runs while the posting thread holds the account lock")
    void postsUnderTheLock() {
        new LockingPoster(this::record, locks).post(cash(LOAN));
        assertEquals(1, postedUnderLock.get());
        assertFalse(locks.lockFor(LOAN).isLocked());
    }

    @Test
    @DisplayName("the lock is released when the wrapped poster throws")
    void releasedOnFailure() {
        final LockingPoster poster = new LockingPoster(command -> {
            throw new InvalidRepaymentException("refused", "10.00", "cash");
        }, locks);
        assertThrows(InvalidRepaymentException.class, () -> poster.post(cash(LOAN)));
        assertFalse(locks.lockFor(LOAN).isLocked());
    }

    @Test
    @DisplayName("a posting to a locked account waits until the holder releases it")
    void waitsForTheHolder() throws Exception {
        final LockingPoster poster = new LockingPoster(this::record, locks);
        final ReentrantLock lock = locks.lockFor(LOAN);
        lock.lock();
        final Future<PostingResult> waiting;
        try {
            waiting = worker.submit(() -> poster.post(cash(LOAN)));
            final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5);
            while (!lock.hasQueuedThreads() && System.nanoTime() < deadline) {
                Thread.onSpinWait();
            }
            assertTrue(lock.hasQueuedThreads(), "the posting never queued on the lock");
            assertEquals(0, calls.get());
        } finally {
            lock.unlock();
        }
        waiting.get(5, TimeUnit.SECONDS);
        assertEquals(1, calls.get());
    }

    @Test
    @DisplayName("a posting to another account is not held up by a locked account")
    void otherAccountsProceed() throws Exception {
        final LockingPoster poster = new LockingPoster(this::record, locks);
        final ReentrantLock lock = locks.lockFor(LOAN);
        lock.lock();
        try {
            worker.submit(() -> poster.post(cash(OTHER))).get(5, TimeUnit.SECONDS);
        } finally {
            lock.unlock();
        }
        assertEquals(1, postedUnderLock.get());
    }
}
