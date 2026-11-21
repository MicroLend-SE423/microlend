package pk.edu.giki.microlend.delinquency;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.InstalmentDue;

/**
 * Penalty on overdue principal (BR-27, BR-28, FR-21): 0.05% per day, simple and
 * non-compounding, capped at 10% of the overdue principal. Overdue interest accrues
 * nothing. Stateless and thread-safe.
 *
 * <p>A run's penalty is the difference between two cumulative figures: the penalty owed
 * up to this business date minus the penalty owed up to the previous run. So the amounts
 * of successive runs always add up exactly to the cumulative penalty, rounding included,
 * and a date skipped between runs (a holiday, or the server down) is caught up rather
 * than lost.</p>
 */
public final class PenaltyAccruer {

    /** Pass as the previous run for a loan never processed: every overdue day is owed. */
    public static final LocalDate NO_PREVIOUS_RUN = LocalDate.MIN;

    private static final BigDecimal DAILY_PERCENT = new BigDecimal("0.05");
    private static final BigDecimal CAP_PERCENT = new BigDecimal("10");

    private final DaysPastDueCalculator days = new DaysPastDueCalculator();

    /**
     * Cumulative penalty after a number of overdue days, capped at 10% of the principal.
     *
     * @param overduePrincipal the principal that is overdue
     * @param daysOverdue days overdue, zero or more
     * @return the penalty accrued over those days
     * @throws InvalidDaysPastDueException if {@code daysOverdue} is negative
     */
    public Money accrued(final Money overduePrincipal, final int daysOverdue) {
        Objects.requireNonNull(overduePrincipal, "overdue principal is required");
        if (daysOverdue < 0) {
            throw new InvalidDaysPastDueException(daysOverdue);
        }
        final Money uncapped = overduePrincipal.percentage(
            DAILY_PERCENT.multiply(BigDecimal.valueOf(daysOverdue)));
        final Money cap = overduePrincipal.percentage(CAP_PERCENT);
        return uncapped.isGreaterThan(cap) ? cap : uncapped;
    }

    /**
     * The penalty one business date adds when the previous run was the day before.
     *
     * @param owing the instalments due by the business date, with outstanding amounts
     * @param businessDate the date being processed
     * @return the penalty to accrue for that date, zero when nothing is overdue
     */
    public Money accrue(final List<InstalmentDue> owing, final LocalDate businessDate) {
        return accrue(owing, businessDate.minusDays(1), businessDate);
    }

    /**
     * The penalty owed for the days after the previous run up to and including this
     * business date, for every instalment whose principal is overdue.
     *
     * @param owing the instalments due by the business date, with outstanding amounts
     * @param previousRun the business date of the loan's previous run, or
     *     {@link #NO_PREVIOUS_RUN}
     * @param businessDate the date being processed
     * @return the penalty to accrue, zero when nothing is overdue
     * @throws IllegalArgumentException if {@code previousRun} is not before
     *     {@code businessDate}
     */
    public Money accrue(final List<InstalmentDue> owing, final LocalDate previousRun,
                        final LocalDate businessDate) {
        Objects.requireNonNull(owing, "instalments are required");
        Objects.requireNonNull(previousRun, "previous run is required");
        Objects.requireNonNull(businessDate, "business date is required");
        if (!previousRun.isBefore(businessDate)) {
            throw new IllegalArgumentException("previous run " + previousRun
                + " must come before business date " + businessDate);
        }
        Money owed = Money.ZERO;
        for (final InstalmentDue instalment : owing) {
            final Money principal = instalment.principal();
            owed = owed.plus(accrued(principal, days.daysPastDue(instalment.dueDate(), businessDate))
                .minus(accrued(principal, days.daysPastDue(instalment.dueDate(), previousRun))));
        }
        return owed;
    }
}
