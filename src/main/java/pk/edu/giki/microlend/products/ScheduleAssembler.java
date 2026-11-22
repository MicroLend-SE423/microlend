package pk.edu.giki.microlend.products;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import pk.edu.giki.microlend.common.Money;

/**
 * Builds a {@link Schedule} from due dates and per-month amounts, and makes it exact: all
 * rounding residue lands in the final instalment (BR-14) and there is exactly one instalment
 * per due date (BR-16). A fixed step of the {@link LoanProduct} workflow, not a varying one.
 */
public final class ScheduleAssembler {

    /**
     * Assembles a schedule.
     *
     * @param principal the amount lent; the principal portions are made to add up to it
     * @param dueDates one date per instalment, in order
     * @param principals the principal due each month, as the calculator rounded them
     * @param interests the interest due each month
     * @return the schedule
     * @throws InvalidScheduleException if the lists differ in size, or the principal residue is
     *     larger than the final instalment's principal
     */
    public Schedule assemble(final Money principal, final List<LocalDate> dueDates,
                             final List<Money> principals, final List<Money> interests) {
        return assemble(principal, dueDates, principals, interests, Money.ZERO);
    }

    /**
     * Assembles a schedule with extra interest added to the first instalment, for the interest
     * that accrued during a grace period (BR-13).
     *
     * @param principal the amount lent; the principal portions are made to add up to it
     * @param dueDates one date per instalment, in order
     * @param principals the principal due each month, as the calculator rounded them
     * @param interests the interest due each month
     * @param firstInstalmentExtraInterest interest to add to the first instalment
     * @return the schedule
     * @throws InvalidScheduleException if the lists differ in size, or the principal residue is
     *     larger than the final instalment's principal
     */
    public Schedule assemble(final Money principal, final List<LocalDate> dueDates,
                             final List<Money> principals, final List<Money> interests,
                             final Money firstInstalmentExtraInterest) {
        final int count = dueDates.size();
        if (principals.size() != count || interests.size() != count) {
            throw new InvalidScheduleException("a schedule needs one principal and one interest per "
                + "due date: " + count + " dates, " + principals.size() + " principals, "
                + interests.size() + " interests");
        }
        final List<Instalment> instalments = new ArrayList<>(count);
        Money principalBeforeLast = Money.ZERO;
        for (int i = 0; i < count - 1; i++) {
            final Money interest = i == 0 ? interests.get(i).plus(firstInstalmentExtraInterest)
                : interests.get(i);
            instalments.add(new Instalment(i + 1, dueDates.get(i), principals.get(i), interest));
            principalBeforeLast = principalBeforeLast.plus(principals.get(i));
        }
        if (principalBeforeLast.isGreaterThan(principal)) {
            throw new InvalidScheduleException("the principal due before the final instalment ("
                + principalBeforeLast + ") exceeds the principal lent (" + principal + ")");
        }
        final Money lastInterest = count == 1
            ? interests.get(0).plus(firstInstalmentExtraInterest) : interests.get(count - 1);
        instalments.add(new Instalment(count, dueDates.get(count - 1),
            principal.minus(principalBeforeLast), lastInterest));
        return new Schedule(instalments);
    }
}
