package pk.edu.giki.microlend.common;

import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * One {@link ReentrantLock} per loan account, shared by the teller posting chain and the
 * end-of-day engine (M1 section 3.6). It lives in {@code common} because {@code ledger}
 * and {@code delinquency} both need it and {@code ledger} may not depend on
 * {@code delinquency}.
 *
 * <p>Callers hold at most one account lock at a time, so no lock cycle, and therefore
 * no deadlock, can form. Locks are created on first use and kept for the life of the
 * registry; at 10,000 accounts that is a few hundred kilobytes.</p>
 */
public final class LockRegistry {

    private final ConcurrentHashMap<LoanId, ReentrantLock> locks = new ConcurrentHashMap<>();

    /**
     * Returns the lock for an account, creating it atomically on first use.
     *
     * @param loanId the account
     * @return the same lock instance for every call with an equal id
     */
    public ReentrantLock lockFor(final LoanId loanId) {
        Objects.requireNonNull(loanId, "loan id is required");
        return locks.computeIfAbsent(loanId, id -> new ReentrantLock());
    }
}
