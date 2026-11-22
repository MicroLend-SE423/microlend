package pk.edu.giki.microlend;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.ledger.InstalmentsDueSource;
import pk.edu.giki.microlend.products.Instalment;
import pk.edu.giki.microlend.products.Schedule;
import pk.edu.giki.microlend.products.ScheduleNotFoundException;
import pk.edu.giki.microlend.products.ScheduleRepository;

/**
 * Supplies the ledger's {@link InstalmentsDueSource} role from the saved schedule (decision
 * D-02, BR-19, BR-20, FR-13, FR-14).
 *
 * <p>It lists only the instalments with {@code dueDate <= businessDate}, oldest first, so a
 * payment larger than what is due becomes advance credit (BR-21) and interest that is not yet
 * earned is never charged. Each {@link Instalment} is mapped to an {@link InstalmentDue} field
 * by field; fees are always zero (BR-18).</p>
 *
 * <p>It sits in the root package, next to {@link CompositionRoot}, because it reads
 * {@code products} types and produces {@code ledger} types; putting it in either module would
 * make one depend on the other (D-02).</p>
 *
 * <p><b>Limitation: paid-to-date.</b> The amounts are the scheduled amounts, not "still
 * outstanding". Nothing records what earlier postings paid on each instalment until the
 * {@code instalment_payment} table (queue item T-01) lands, so this class treats nothing as
 * paid. That is correct for the first posting on a loan and wrong for repeated postings.</p>
 *
 * <p><b>Penalty.</b> It comes from a {@link PenaltyLookup} given at construction, so the
 * delinquency owner can plug the accrued penalty in without editing this class. Without one, the
 * penalty is zero.</p>
 */
public final class ScheduleInstalmentsDueSource implements InstalmentsDueSource {

    /**
     * Role that says how much penalty is accrued and unpaid on one instalment. Supplied by the
     * delinquency side; declared here so this class depends on no other module's types.
     */
    @FunctionalInterface
    public interface PenaltyLookup {

        /** A lookup that always answers zero: no penalty known. */
        PenaltyLookup NONE = (loanId, sequence, businessDate) -> Money.ZERO;

        /**
         * Returns the penalty accrued and unpaid on one instalment.
         *
         * @param loanId the loan
         * @param sequence the instalment's position in the schedule, from 1
         * @param businessDate the business date of the call
         * @return the penalty, never null
         */
        Money penaltyFor(LoanId loanId, int sequence, LocalDate businessDate);
    }

    private final ScheduleRepository schedules;
    private final PenaltyLookup penalties;

    /**
     * Creates a source that reports no penalty.
     *
     * @param schedules where a loan's schedule is read
     * @throws NullPointerException if the argument is null
     */
    public ScheduleInstalmentsDueSource(final ScheduleRepository schedules) {
        this(schedules, PenaltyLookup.NONE);
    }

    /**
     * Creates a source with a penalty lookup.
     *
     * @param schedules where a loan's schedule is read
     * @param penalties how much penalty is accrued on each instalment
     * @throws NullPointerException if an argument is null
     */
    public ScheduleInstalmentsDueSource(final ScheduleRepository schedules,
                                        final PenaltyLookup penalties) {
        this.schedules = Objects.requireNonNull(schedules, "schedule repository is required");
        this.penalties = Objects.requireNonNull(penalties, "penalty lookup is required");
    }

    /**
     * Returns the instalments due on or before the business date, oldest first.
     *
     * @param loanId the loan
     * @param businessDate the business date of the posting
     * @return an unmodifiable list; empty when nothing has fallen due yet
     * @throws NullPointerException if an argument is null
     * @throws ScheduleNotFoundException if the loan has no saved schedule
     */
    @Override
    public List<InstalmentDue> owing(final LoanId loanId, final LocalDate businessDate) {
        Objects.requireNonNull(loanId, "loan id is required");
        Objects.requireNonNull(businessDate, "business date is required");
        final Schedule schedule = schedules.scheduleFor(loanId)
            .orElseThrow(() -> new ScheduleNotFoundException(loanId, "schedule"));
        final List<InstalmentDue> due = new ArrayList<>();
        for (Instalment instalment : schedule.instalments()) {
            if (!instalment.dueDate().isAfter(businessDate)) {
                due.add(new InstalmentDue(instalment.sequence(), instalment.dueDate(),
                    penalties.penaltyFor(loanId, instalment.sequence(), businessDate),
                    Money.ZERO, instalment.interest(), instalment.principal()));
            }
        }
        return List.copyOf(due);
    }
}
