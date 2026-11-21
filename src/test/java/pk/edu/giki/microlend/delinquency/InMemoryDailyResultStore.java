package pk.edu.giki.microlend.delinquency;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import pk.edu.giki.microlend.common.LockRegistry;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * Test double for {@link DailyResultStore}. It keeps one snapshot per loan and date,
 * like the SQLite unique key, and counts every call made without the loan's lock held.
 */
final class InMemoryDailyResultStore implements DailyResultStore {

    private final List<LoanId> active;
    private final LockRegistry locks;
    private final Map<LoanId, ConcurrentSkipListMap<LocalDate, Classification>> snapshots =
        new ConcurrentHashMap<>();
    private final List<LedgerEntry> ledger = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger unlockedCalls = new AtomicInteger();

    InMemoryDailyResultStore(final List<LoanId> active, final LockRegistry locks) {
        this.active = List.copyOf(active);
        this.locks = locks;
    }

    void seed(final LoanId loanId, final LocalDate date, final Classification classification) {
        snapshots.computeIfAbsent(loanId, id -> new ConcurrentSkipListMap<>())
            .put(date, classification);
    }

    @Override
    public List<LoanId> activeLoans() {
        return active;
    }

    @Override
    public Optional<Classification> classificationBefore(final LoanId loanId,
                                                         final LocalDate businessDate) {
        checkLocked(loanId);
        final ConcurrentSkipListMap<LocalDate, Classification> history = snapshots.get(loanId);
        if (history == null) {
            return Optional.empty();
        }
        final Map.Entry<LocalDate, Classification> earlier = history.lowerEntry(businessDate);
        return earlier == null ? Optional.empty() : Optional.of(earlier.getValue());
    }

    @Override
    public boolean record(final LoanId loanId, final LocalDate businessDate,
                          final int daysPastDue, final Classification classification,
                          final List<LedgerEntry> accruals) {
        checkLocked(loanId);
        final boolean first = snapshots.computeIfAbsent(loanId, id -> new ConcurrentSkipListMap<>())
            .putIfAbsent(businessDate, classification) == null;
        if (first) {
            ledger.addAll(accruals);
        }
        return first;
    }

    void checkLocked(final LoanId loanId) {
        if (!locks.lockFor(loanId).isHeldByCurrentThread()) {
            unlockedCalls.incrementAndGet();
        }
    }

    int snapshotCount() {
        return snapshots.values().stream().mapToInt(Map::size).sum();
    }

    List<LedgerEntry> ledger() {
        synchronized (ledger) {
            return List.copyOf(ledger);
        }
    }

    int unlockedCalls() {
        return unlockedCalls.get();
    }
}
