package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.time.LocalDate;
import pk.edu.giki.microlend.common.Money;

/**
 * The grace-period rule of BR-13: a grace of 0 to 3 months defers the first instalment, but
 * interest still accrues during it and is added to the first instalment.
 *
 * <p>Grace does not change the number of instalments (BR-16): the whole series simply starts
 * later. Interest accrues on the full principal for each month of grace, because nothing has
 * been repaid yet.</p>
 */
public final class GracePeriodPolicy {

    /** The longest grace period allowed (BR-13). */
    public static final int MAX_GRACE_MONTHS = 3;

    private final DueDateCalculator dueDates = new DueDateCalculator();

    /**
     * Returns the date the first instalment falls due.
     *
     * @param disbursement the disbursement date
     * @param graceMonths the grace period in months, 0 to {@value #MAX_GRACE_MONTHS}
     * @return one month after disbursement plus the grace period, on the disbursement
     *     day-of-month clamped to the month end (BR-12)
     * @throws InvalidGracePeriodException if the grace period is outside 0 to 3 months
     * @throws InvalidScheduleException if the date is null
     */
    public LocalDate firstDueDate(final LocalDate disbursement, final int graceMonths) {
        requireValid(graceMonths);
        return dueDates.dueDates(disbursement, 1 + graceMonths, 1).get(0);
    }

    /**
     * Returns the interest that accrues during the grace period.
     *
     * @param principal the amount lent
     * @param annualRatePercent the yearly rate in percent, for example {@code 24.00}
     * @param graceMonths the grace period in months, 0 to {@value #MAX_GRACE_MONTHS}
     * @return principal x rate / 12 for each month of grace, rounded once HALF_EVEN (BR-15)
     * @throws InvalidGracePeriodException if the grace period is outside 0 to 3 months
     */
    public Money accruedInterest(final Money principal, final BigDecimal annualRatePercent,
                                 final int graceMonths) {
        requireValid(graceMonths);
        return Rounding.round(Rounding.monthlyInterest(principal, annualRatePercent)
            .multiply(BigDecimal.valueOf(graceMonths)));
    }

    private static void requireValid(final int graceMonths) {
        if (graceMonths < 0 || graceMonths > MAX_GRACE_MONTHS) {
            throw new InvalidGracePeriodException(graceMonths);
        }
    }
}
