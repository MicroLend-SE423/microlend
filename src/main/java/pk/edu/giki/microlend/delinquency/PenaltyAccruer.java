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
 * <p>The daily amount is the difference between two cumulative figures, so the daily
 * amounts always add up exactly to the cumulative penalty, rounding included.</p>
 */
public final class PenaltyAccruer {

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
     * The penalty one business date adds: one more day for every instalment whose
     * principal is overdue on that date.
     *
     * @param owing the instalments due by the business date, with outstanding amounts
     * @param businessDate the date being processed
     * @return the penalty to accrue for that date, zero when nothing is overdue
     */
    public Money accrue(final List<InstalmentDue> owing, final LocalDate businessDate) {
        Objects.requireNonNull(owing, "instalments are required");
        Money today = Money.ZERO;
        for (final InstalmentDue instalment : owing) {
            final int overdue = days.daysPastDue(instalment.dueDate(), businessDate);
            if (overdue > 0) {
                today = today.plus(accrued(instalment.principal(), overdue)
                    .minus(accrued(instalment.principal(), overdue - 1)));
            }
        }
        return today;
    }
}
