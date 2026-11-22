package pk.edu.giki.microlend.delinquency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LockRegistry;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.ledger.InstalmentsDueSource;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * Tests for {@link EndOfDayEngine} (M1 section 3.6; BR-27 to BR-34; FR-21, FR-26, FR-27;
 * QR-06). Every concurrency test is driven by latches, never by sleeping.
 */
class EndOfDayEngineTest {

    private static final LocalDate TODAY = LocalDate.of(2027, 3, 11);
    private static final Clock CLOCK = Clock.fixed(Instant.parse("2027-03-11T18:00:00Z"),
        ZoneOffset.UTC);

    private final LockRegistry locks = new LockRegistry();

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static List<LoanId> loans(final int count) {
        final List<LoanId> ids = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            ids.add(LoanId.of("LN-" + i));
        }
        return ids;
    }

    /** Each loan owes 10,000.00 principal that fell due {@code daysOverdue} days ago. */
    private static InstalmentsDueSource overdueBy(final int daysOverdue) {
        return (loan, date) -> List.of(new InstalmentDue(1, date.minusDays(daysOverdue),
            Money.ZERO, Money.ZERO, Money.ZERO, money("10000.00")));
    }

    private EndOfDayEngine engine(final InMemoryDailyResultStore store,
                                  final InstalmentsDueSource source) {
        return new EndOfDayEngine(store, source, locks, CLOCK);
    }

    private static Money total(final List<LedgerEntry> lines, final boolean debits) {
        Money sum = Money.ZERO;
        for (final LedgerEntry line : lines) {
            sum = sum.plus(debits ? line.debit() : line.credit());
        }
        return sum;
    }

    /** Each loan owes 10,000.00 principal that fell due on 2027-03-10, whatever the run date. */
    private static InstalmentsDueSource dueOnTenthOfMarch() {
        return (loan, date) -> List.of(new InstalmentDue(1, LocalDate.of(2027, 3, 10),
            Money.ZERO, Money.ZERO, Money.ZERO, money("10000.00")));
    }

    @Test
    @DisplayName("TC-DEL-06.1: every active loan gets a snapshot and a balanced penalty pair")
    void processesEveryLoan() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(20), locks);
        final EodReport report = engine(store, overdueBy(10)).run(TODAY);

        assertEquals(20, report.recorded());
        assertEquals(0, report.alreadyRecorded());
        assertEquals(Map.of(), report.failures());
        assertEquals(20, store.snapshotCount());
        assertEquals(money("1000.00"), report.penaltyAccrued(),
            "a loan never processed before accrues all ten overdue days: 20 x 50.00");
        final List<LedgerEntry> lines = store.ledger();
        assertEquals(40, lines.size());
        assertEquals(total(lines, true), total(lines, false));
        assertTrue(lines.stream().allMatch(line -> line.isDebit()
            ? line.type() == EntryType.PENALTY_ACCRUAL
            : line.type() == EntryType.PENALTY_INCOME));
    }

    @Test
    @DisplayName("TC-DEL-06.2/06.3: a second run for the same date adds nothing; the next date accrues")
    void idempotentPerBusinessDate() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(5), locks);
        final EndOfDayEngine engine = engine(store, overdueBy(10));
        engine.run(TODAY);

        final EodReport again = engine.run(TODAY);
        assertEquals(0, again.recorded());
        assertEquals(5, again.alreadyRecorded());
        assertEquals(Money.ZERO, again.penaltyAccrued());
        assertEquals(10, store.ledger().size());

        final EodReport tomorrow = engine.run(TODAY.plusDays(1));
        assertEquals(5, tomorrow.recorded());
        assertEquals(20, store.ledger().size());
    }

    @Test
    @DisplayName("BR-27: dates skipped between runs are caught up, so no overdue day goes unpenalised")
    void catchesUpSkippedDates() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(3), locks);
        final EndOfDayEngine engine = engine(store, dueOnTenthOfMarch());
        assertEquals(money("15.00"), engine.run(TODAY).penaltyAccrued());

        final EodReport afterGap = engine.run(TODAY.plusDays(4));
        assertEquals(money("60.00"), afterGap.penaltyAccrued(), "four days for each of three loans");
        assertEquals(money("75.00"), total(store.ledger(), true),
            "five overdue days in all for each loan: 3 x 25.00");
    }

    @Test
    @DisplayName("a run for a date earlier than the loan's last run records nothing (no double accrual)")
    void earlierDateAfterALaterRunIsSkipped() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(3), locks);
        final EndOfDayEngine engine = engine(store, dueOnTenthOfMarch());
        engine.run(TODAY.plusDays(4));

        final EodReport backdated = engine.run(TODAY);
        assertEquals(0, backdated.recorded());
        assertEquals(3, backdated.alreadyRecorded());
        assertEquals(Money.ZERO, backdated.penaltyAccrued());
        assertEquals(3, store.snapshotCount());
    }

    @Test
    @DisplayName("a loan with nothing overdue gets a Standard snapshot and no ledger lines")
    void nothingOverdue() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(3), locks);
        final EodReport report = engine(store, (loan, date) -> List.of()).run(TODAY);
        assertEquals(3, report.recorded());
        assertEquals(List.of(), store.ledger());
    }

    @Test
    @DisplayName("a task that throws is reported and does not block the other loans")
    void failingTaskDoesNotBlockThePool() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(10), locks);
        final InstalmentsDueSource healthy = overdueBy(5);
        final EodReport report = engine(store, (loan, date) -> {
            if ("LN-3".equals(loan.value())) {
                throw new IllegalStateException("schedule unreadable");
            }
            return healthy.owing(loan, date);
        }).run(TODAY);

        assertEquals(9, report.recorded());
        assertEquals(1, report.failures().size());
        assertTrue(report.failures().get(LoanId.of("LN-3")).contains("schedule unreadable"));
    }

    @Test
    @DisplayName("four loans are processed at the same time on the four-thread pool")
    void runsFourTasksInParallel() {
        final CountDownLatch allFourInside = new CountDownLatch(EndOfDayEngine.POOL_SIZE);
        final InMemoryDailyResultStore store =
            new InMemoryDailyResultStore(loans(EndOfDayEngine.POOL_SIZE), locks);
        final EodReport report = engine(store, (loan, date) -> {
            allFourInside.countDown();
            try {
                if (!allFourInside.await(5, TimeUnit.SECONDS)) {
                    throw new IllegalStateException("tasks did not run in parallel");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("interrupted", e);
            }
            return List.of();
        }).run(TODAY);

        assertEquals(Map.of(), report.failures());
        assertEquals(EndOfDayEngine.POOL_SIZE, report.recorded());
    }

    @Test
    @DisplayName("each loan is read and written only while its account lock is held")
    void workRunsUnderTheAccountLock() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(12), locks);
        final InstalmentsDueSource source = overdueBy(3);
        engine(store, (loan, date) -> {
            store.checkLocked(loan);
            return source.owing(loan, date);
        }).run(TODAY);
        assertEquals(0, store.unlockedCalls());
    }

    @Test
    @DisplayName("TC-DEL-07: a downgrade and an upgrade raise one event each; no change raises none")
    void classificationChangeEvents() {
        final LoanId down = LoanId.of("LN-1");
        final LoanId up = LoanId.of("LN-2");
        final LoanId same = LoanId.of("LN-3");
        final InMemoryDailyResultStore store =
            new InMemoryDailyResultStore(List.of(down, up, same), locks);
        final LocalDate yesterday = TODAY.minusDays(1);
        store.seed(down, yesterday, Classification.WATCH);
        store.seed(up, yesterday, Classification.SUBSTANDARD);
        store.seed(same, yesterday, Classification.WATCH);
        final Map<LoanId, Integer> overdueDays = Map.of(down, 31, up, 30, same, 30);

        final EodReport report = engine(store, (loan, date) -> List.of(new InstalmentDue(1,
            date.minusDays(overdueDays.get(loan)), Money.ZERO, Money.ZERO, Money.ZERO,
            money("1000.00")))).run(TODAY);

        assertEquals(List.of(
            new ClassificationChangeEvent(down, Classification.WATCH, Classification.SUBSTANDARD),
            new ClassificationChangeEvent(up, Classification.SUBSTANDARD, Classification.WATCH)),
            report.classificationChanges());
    }

    @Test
    @DisplayName("TC-DEL-05.3 (partial): a loan more than 180 days past due is listed for write-off;"
        + " posting the write-off is M3")
    void writeOffDueIsReported() {
        final InMemoryDailyResultStore store = new InMemoryDailyResultStore(loans(1), locks);
        final EodReport report = engine(store, overdueBy(181)).run(TODAY);
        assertEquals(List.of(LoanId.of("LN-1")), report.writeOffsDue());
    }
}
