package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import pk.edu.giki.microlend.common.Money;

/**
 * The shared arithmetic behind the per-product early-settlement rules (BR-22): which part of a
 * schedule's interest has not yet been earned on a given date. Each {@link LoanProduct}
 * subclass decides which of these parts it refunds.
 *
 * <p>The interest of instalment <em>n</em> is earned over the month that ends on its due date.
 * That month starts on the previous due date; the first month starts one month before the
 * first due date. (When a grace period defers the first instalment, its interest also covers
 * the grace months, which this treats as part of that first month: a documented
 * approximation, since a schedule does not record its disbursement date.)</p>
 */
final class SettlementInterest {

    private SettlementInterest() {
    }

    /**
     * Sums the interest of every month that has not begun by the settlement date.
     *
     * @param schedule the loan's schedule
     * @param settlementDate the date the loan is paid off
     * @return the interest of instalments whose month starts on or after the settlement date
     */
    static Money ofMonthsNotYetBegun(final Schedule schedule, final LocalDate settlementDate) {
        final List<Instalment> instalments = schedule.instalments();
        Money sum = Money.ZERO;
        for (int i = 0; i < instalments.size(); i++) {
            if (!monthStart(instalments, i).isBefore(settlementDate)) {
                sum = sum.plus(instalments.get(i).interest());
            }
        }
        return sum;
    }

    /**
     * Returns the unearned part of the month the settlement date falls inside: the interest of
     * that month times the days still to run over the days in the month, rounded HALF_EVEN.
     *
     * @param schedule the loan's schedule
     * @param settlementDate the date the loan is paid off
     * @return the unearned part of the current month; zero when the date is on a due date or
     *     outside the schedule
     */
    static Money unearnedPartOfCurrentMonth(final Schedule schedule, final LocalDate settlementDate) {
        final List<Instalment> instalments = schedule.instalments();
        for (int i = 0; i < instalments.size(); i++) {
            final LocalDate start = monthStart(instalments, i);
            final LocalDate end = instalments.get(i).dueDate();
            if (start.isBefore(settlementDate) && settlementDate.isBefore(end)) {
                final BigDecimal remaining =
                    BigDecimal.valueOf(ChronoUnit.DAYS.between(settlementDate, end));
                final BigDecimal length = BigDecimal.valueOf(ChronoUnit.DAYS.between(start, end));
                return Rounding.round(Rounding.exact(instalments.get(i).interest())
                    .multiply(remaining).divide(length, Rounding.PRECISION));
            }
        }
        return Money.ZERO;
    }

    private static LocalDate monthStart(final List<Instalment> instalments, final int index) {
        return index == 0 ? instalments.get(0).dueDate().minusMonths(1)
            : instalments.get(index - 1).dueDate();
    }
}
