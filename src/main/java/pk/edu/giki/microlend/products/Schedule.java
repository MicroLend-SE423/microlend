package pk.edu.giki.microlend.products;

import java.util.List;
import pk.edu.giki.microlend.common.Money;

/**
 * The repayment schedule of one loan: its instalments in order.
 *
 * <p>Immutable. {@link #instalments()} returns an unmodifiable list, so no caller can change
 * what a loan owes by editing the list it was handed.</p>
 */
public final class Schedule {

    private final List<Instalment> instalments;

    /**
     * Creates a schedule.
     *
     * @param instalments the instalments in order; copied, so later changes to the argument do
     *     not affect the schedule
     * @throws InvalidScheduleException if the list is null, empty, or holds a null element
     */
    public Schedule(final List<Instalment> instalments) {
        if (instalments == null || instalments.isEmpty()) {
            throw new InvalidScheduleException("a schedule needs at least one instalment");
        }
        for (Instalment instalment : instalments) {
            if (instalment == null) {
                throw new InvalidScheduleException("a schedule cannot hold a missing instalment");
            }
        }
        this.instalments = List.copyOf(instalments);
    }

    /**
     * Returns the instalments in order.
     *
     * @return an unmodifiable list
     */
    public List<Instalment> instalments() {
        return instalments;
    }

    /**
     * Returns the principal the instalments add up to.
     *
     * @return the sum of principal over all instalments
     */
    public Money totalPrincipal() {
        Money sum = Money.ZERO;
        for (Instalment instalment : instalments) {
            sum = sum.plus(instalment.principal());
        }
        return sum;
    }

    /**
     * Returns the interest the instalments add up to.
     *
     * @return the sum of interest over all instalments
     */
    public Money totalInterest() {
        Money sum = Money.ZERO;
        for (Instalment instalment : instalments) {
            sum = sum.plus(instalment.interest());
        }
        return sum;
    }

    /**
     * Returns everything the borrower pays over the life of the loan.
     *
     * @return total principal plus total interest
     */
    public Money totalPayable() {
        return totalPrincipal().plus(totalInterest());
    }
}
