package pk.edu.giki.microlend.ledger;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import pk.edu.giki.microlend.common.LockRegistry;

/**
 * Decorator that runs a posting under its account's lock (queue item I-07; M1 section
 * 3.6). The end-of-day engine takes the same lock from the same {@link LockRegistry}, so
 * a teller posting and the engine's work on one loan never interleave, and two tellers
 * posting to one loan cannot lose an update: the read of the account, the allocation and
 * the writes all happen while the lock is held.
 *
 * <p>Wired innermost, directly around {@code CoreRepaymentPoster}, so the lock covers only
 * the read-allocate-write sequence and not auditing, timing or the idempotency claim.
 * A posting holds one account lock and never a second.</p>
 */
public final class LockingPoster extends RepaymentPosterDecorator {

    private final LockRegistry locks;

    /**
     * Wraps a poster.
     *
     * @param wrapped the poster that does the work
     * @param locks the account locks shared with the end-of-day engine
     */
    public LockingPoster(final RepaymentPoster wrapped, final LockRegistry locks) {
        super(wrapped);
        this.locks = Objects.requireNonNull(locks, "lock registry is required");
    }

    @Override
    public PostingResult post(final RepaymentCommand command) {
        Objects.requireNonNull(command, "command is required");
        final ReentrantLock lock = locks.lockFor(command.loanId());
        lock.lock();
        try {
            return super.post(command);
        } finally {
            lock.unlock();
        }
    }
}
