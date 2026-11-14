package pk.edu.giki.microlend.ledger;

import java.util.Objects;
import pk.edu.giki.microlend.common.Money;

/**
 * How one payment is split across the four tiers of the BR-18 waterfall:
 * penalty, fees, interest and principal, in that order.
 *
 * <p>An allocation is computed by the repayment allocator (queue item L-05)
 * and applied by {@link LoanAccount#applyAllocation(Allocation)}; the posting
 * path writes it to the ledger. It is an immutable value: no tier can change
 * after construction, so it can be handed between those steps without
 * defensive copies. Each tier is a {@link Money}, so none can be negative
 * (BR-43). No fee type exists at M1, so the allocator always leaves the fees
 * tier at zero (BR-18); this class does not hard-code that, so introducing a
 * fee by a recorded deviation does not change it.</p>
 */
public final class Allocation {

    private final Money toPenalty;
    private final Money toFees;
    private final Money toInterest;
    private final Money toPrincipal;

    private Allocation(final Money toPenalty, final Money toFees,
                       final Money toInterest, final Money toPrincipal) {
        this.toPenalty = toPenalty;
        this.toFees = toFees;
        this.toInterest = toInterest;
        this.toPrincipal = toPrincipal;
    }

    /**
     * Creates an allocation, tiers given in BR-18 order.
     *
     * @param toPenalty the amount applied to penalty
     * @param toFees the amount applied to fees
     * @param toInterest the amount applied to interest
     * @param toPrincipal the amount applied to principal
     * @return the allocation
     * @throws InvalidAllocationException if any tier is null
     */
    public static Allocation of(final Money toPenalty, final Money toFees,
                                final Money toInterest, final Money toPrincipal) {
        require(toPenalty, "penalty");
        require(toFees, "fees");
        require(toInterest, "interest");
        require(toPrincipal, "principal");
        return new Allocation(toPenalty, toFees, toInterest, toPrincipal);
    }

    private static void require(final Money tier, final String name) {
        if (tier == null) {
            throw new InvalidAllocationException("the " + name + " tier of an allocation is required");
        }
    }

    /**
     * Returns the amount applied to penalty.
     *
     * @return the penalty tier
     */
    public Money toPenalty() {
        return toPenalty;
    }

    /**
     * Returns the amount applied to fees.
     *
     * @return the fees tier
     */
    public Money toFees() {
        return toFees;
    }

    /**
     * Returns the amount applied to interest.
     *
     * @return the interest tier
     */
    public Money toInterest() {
        return toInterest;
    }

    /**
     * Returns the amount applied to principal.
     *
     * @return the principal tier
     */
    public Money toPrincipal() {
        return toPrincipal;
    }

    /**
     * Returns the whole amount allocated, the sum of the four tiers.
     *
     * @return the total
     */
    public Money total() {
        return toPenalty.plus(toFees).plus(toInterest).plus(toPrincipal);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Allocation)) {
            return false;
        }
        final Allocation that = (Allocation) other;
        return toPenalty.equals(that.toPenalty)
            && toFees.equals(that.toFees)
            && toInterest.equals(that.toInterest)
            && toPrincipal.equals(that.toPrincipal);
    }

    @Override
    public int hashCode() {
        return Objects.hash(toPenalty, toFees, toInterest, toPrincipal);
    }

    @Override
    public String toString() {
        return "Allocation[penalty=" + toPenalty.toPlainString()
            + ", fees=" + toFees.toPlainString()
            + ", interest=" + toInterest.toPlainString()
            + ", principal=" + toPrincipal.toPlainString() + "]";
    }
}
