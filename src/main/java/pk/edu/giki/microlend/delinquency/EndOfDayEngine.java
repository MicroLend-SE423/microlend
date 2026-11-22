package pk.edu.giki.microlend.delinquency;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import pk.edu.giki.microlend.common.LockRegistry;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.ledger.InstalmentsDueSource;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * The concurrent end-of-day operation (M1 section 3.6; BR-27 to BR-34). Each run puts one
 * task per active loan on a fixed pool of four threads. For each loan, under that loan's
 * {@link LockRegistry} lock (the same lock the teller posting chain takes), it reads what
 * is owed, computes days past due, classification and the day's penalty, and records the
 * snapshot and the balanced penalty pair in one storage transaction.
 *
 * <p>Penalty is accrued from the loan's last recorded run up to this business date, so a
 * date skipped between runs is caught up (BR-27). A loan whose last run is this date or a
 * later one is skipped, so repeating a run, or running an earlier date afterwards, accrues
 * nothing twice (BR-33).</p>
 *
 * <p>Thread safety: the calculators are stateless; a task holds exactly one account lock
 * and never a second, so no deadlock can form; the store's unique (loan, business date)
 * key backs up the skip across processes. A task that throws is reported in the
 * {@link EodReport} and has written nothing; it does not stop the other tasks.</p>
 *
 * <p>Deviation from M1 View 3, recorded in the M2 report: what is owed comes through
 * {@code ledger.InstalmentsDueSource} (team decision D-02) rather than a
 * {@code ScheduleProvider}, and the pool is created per run so no thread outlives it.</p>
 */
public final class EndOfDayEngine {

    /** Worker threads per run (M1 section 3.6). */
    public static final int POOL_SIZE = 4;

    private final DailyResultStore store;
    private final InstalmentsDueSource instalments;
    private final LockRegistry locks;
    private final Clock clock;
    private final DaysPastDueCalculator days = new DaysPastDueCalculator();
    private final DelinquencyClassifier classifier = new DelinquencyClassifier();
    private final PenaltyAccruer penalties = new PenaltyAccruer();
    private final WriteOffPolicy writeOff = new WriteOffPolicy();

    /**
     * Creates the engine.
     *
     * @param store where results are recorded
     * @param instalments what each loan owes on a business date
     * @param locks the account locks shared with the teller posting chain
     * @param clock the source of posting timestamps
     */
    public EndOfDayEngine(final DailyResultStore store, final InstalmentsDueSource instalments,
                          final LockRegistry locks, final Clock clock) {
        this.store = Objects.requireNonNull(store, "result store is required");
        this.instalments = Objects.requireNonNull(instalments, "instalment source is required");
        this.locks = Objects.requireNonNull(locks, "lock registry is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /**
     * Runs end of day for a business date across every active loan.
     *
     * @param businessDate the date to process
     * @return what the run did, including per-loan failures
     * @throws IllegalStateException if the calling thread is interrupted while waiting
     */
    public EodReport run(final LocalDate businessDate) {
        Objects.requireNonNull(businessDate, "business date is required");
        final ExecutorService pool = Executors.newFixedThreadPool(POOL_SIZE);
        try {
            final Map<LoanId, Future<LoanResult>> tasks = new LinkedHashMap<>();
            for (final LoanId loan : store.activeLoans()) {
                tasks.put(loan, pool.submit(() -> process(loan, businessDate)));
            }
            return collect(businessDate, tasks);
        } finally {
            pool.shutdownNow();
        }
    }

    private LoanResult process(final LoanId loan, final LocalDate businessDate) {
        final ReentrantLock lock = locks.lockFor(loan);
        lock.lock();
        try {
            final Optional<DailySnapshot> last = store.lastSnapshot(loan);
            if (last.isPresent() && !last.get().businessDate().isBefore(businessDate)) {
                return LoanResult.alreadyRecorded();
            }
            final List<InstalmentDue> owing = instalments.owing(loan, businessDate);
            final int daysPastDue = days.daysPastDue(owing, businessDate);
            final Classification now = classifier.classify(daysPastDue);
            final Money penalty = penalties.accrue(owing, last.map(DailySnapshot::businessDate)
                .orElse(PenaltyAccruer.NO_PREVIOUS_RUN), businessDate);
            if (!store.record(loan, businessDate, daysPastDue, now, accrual(loan, penalty))) {
                return LoanResult.alreadyRecorded();
            }
            final ClassificationChangeEvent change = last.map(DailySnapshot::classification)
                .filter(previous -> previous != now)
                .map(previous -> new ClassificationChangeEvent(loan, previous, now))
                .orElse(null);
            return new LoanResult(true, penalty, change, writeOff.shouldWriteOff(daysPastDue));
        } finally {
            lock.unlock();
        }
    }

    private List<LedgerEntry> accrual(final LoanId loan, final Money penalty) {
        if (penalty.isZero()) {
            return List.of();
        }
        final Instant now = clock.instant();
        return List.of(LedgerEntry.debit(loan, EntryType.PENALTY_ACCRUAL, penalty, now),
            LedgerEntry.credit(loan, EntryType.PENALTY_INCOME, penalty, now));
    }

    private static EodReport collect(final LocalDate businessDate,
                                     final Map<LoanId, Future<LoanResult>> tasks) {
        int recorded = 0;
        int alreadyRecorded = 0;
        Money penalty = Money.ZERO;
        final List<ClassificationChangeEvent> changes = new ArrayList<>();
        final List<LoanId> writeOffs = new ArrayList<>();
        final Map<LoanId, String> failures = new LinkedHashMap<>();
        for (final Map.Entry<LoanId, Future<LoanResult>> task : tasks.entrySet()) {
            final LoanResult result;
            try {
                result = task.getValue().get();
            } catch (ExecutionException failed) {
                failures.put(task.getKey(), String.valueOf(failed.getCause()));
                continue;
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("end of day for " + businessDate
                    + " was interrupted", interrupted);
            }
            if (!result.recorded()) {
                alreadyRecorded++;
                continue;
            }
            recorded++;
            penalty = penalty.plus(result.penalty());
            if (result.change() != null) {
                changes.add(result.change());
            }
            if (result.writeOffDue()) {
                writeOffs.add(task.getKey());
            }
        }
        return new EodReport(businessDate, recorded, alreadyRecorded, penalty, changes,
            writeOffs, failures);
    }

    /** One task's outcome; {@code change} is null when the classification did not move. */
    private record LoanResult(boolean recorded, Money penalty, ClassificationChangeEvent change,
                              boolean writeOffDue) {

        static LoanResult alreadyRecorded() {
            return new LoanResult(false, Money.ZERO, null, false);
        }
    }
}
