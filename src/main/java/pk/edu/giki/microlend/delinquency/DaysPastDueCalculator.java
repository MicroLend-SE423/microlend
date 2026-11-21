package pk.edu.giki.microlend.delinquency;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.ledger.InstalmentDue;

/**
 * Counts days past due (BR-29, FR-22). Stateless and thread-safe.
 */
public final class DaysPastDueCalculator {

    /**
     * Days past due for one instalment: counted from the day after the due date, so the
     * due date itself is day 0.
     *
     * @param dueDate the instalment's due date
     * @param businessDate the date being processed
     * @return days past due, never negative
     */
    public int daysPastDue(final LocalDate dueDate, final LocalDate businessDate) {
        Objects.requireNonNull(dueDate, "due date is required");
        Objects.requireNonNull(businessDate, "business date is required");
        return (int) Math.max(0, ChronoUnit.DAYS.between(dueDate, businessDate));
    }

    /**
     * Days past due for a loan: measured from the oldest instalment that still owes
     * principal, interest or fees. Penalty alone does not keep a loan past due, because
     * penalty is a consequence of lateness, not an instalment obligation.
     *
     * @param owing the instalments due by the business date, with outstanding amounts
     * @param businessDate the date being processed
     * @return days past due of the oldest unpaid instalment, or 0 when nothing is owed
     */
    public int daysPastDue(final List<InstalmentDue> owing, final LocalDate businessDate) {
        Objects.requireNonNull(owing, "instalments are required");
        LocalDate oldest = null;
        for (final InstalmentDue instalment : owing) {
            final boolean unpaid = !instalment.principal().plus(instalment.interest())
                .plus(instalment.fees()).isZero();
            if (unpaid && (oldest == null || instalment.dueDate().isBefore(oldest))) {
                oldest = instalment.dueDate();
            }
        }
        return oldest == null ? 0 : daysPastDue(oldest, businessDate);
    }
}
