package pk.edu.giki.microlend.ledger;

import java.time.LocalDate;
import java.util.Objects;
import pk.edu.giki.microlend.common.Money;

/**
 * Decides what happens to a payment once it has been allocated (queue item
 * L-06; BR-21, BR-22; FR-15).
 *
 * <ul>
 *   <li>If principal is still owed, any unapplied surplus becomes advance
 *       credit held against future instalments (BR-21).</li>
 *   <li>If no principal is left, the payment cleared the loan: the account is
 *       closed and the product's rebate is requested through
 *       {@link EarlySettlementRebate} (BR-22). Any money left over is reported
 *       as excess, because no rule yet says how it is returned.</li>
 * </ul>
 *
 * <p>This class detects payoff and closes the account; it does not compute
 * the per-product rebate, which is {@code products} knowledge, so FR-15 is
 * only partly implemented here.</p>
 */
public final class SettlementPolicy {

    private final EarlySettlementRebate rebates;

    /**
     * Creates the policy.
     *
     * @param rebates where the per-product rebate comes from
     * @throws NullPointerException if rebates is null
     */
    public SettlementPolicy(final EarlySettlementRebate rebates) {
        this.rebates = Objects.requireNonNull(rebates, "a rebate source is required");
    }

    /**
     * Settles one allocated payment.
     *
     * @param after the account with the allocation already applied
     * @param allocated the allocator's result for the same payment
     * @param businessDate the business date of the payment
     * @return the outcome
     * @throws InvalidAllocationException if an argument is null or the rebate source returns null
     * @throws LoanClosedException if the loan is already closed or written off (BR-26)
     */
    public SettlementOutcome settle(final LoanAccount after, final AllocationResult allocated,
                                    final LocalDate businessDate) {
        if (after == null || allocated == null || businessDate == null) {
            throw new InvalidAllocationException(
                "an account, an allocation result and a business date are required");
        }
        after.ensureAcceptsPayment();
        final Money surplus = allocated.unapplied();
        if (!after.outstandingPrincipal().isZero()) {
            return surplus.isZero()
                ? new SettlementOutcome(SettlementOutcome.Kind.NO_SURPLUS, after,
                    Money.ZERO, Money.ZERO, Money.ZERO)
                : new SettlementOutcome(SettlementOutcome.Kind.ADVANCE_CREDIT, after,
                    surplus, Money.ZERO, Money.ZERO);
        }
        final Money rebate = rebates.rebateFor(after.id(), businessDate);
        if (rebate == null) {
            throw new InvalidAllocationException(
                "the rebate source returned no amount for loan " + after.id());
        }
        return new SettlementOutcome(SettlementOutcome.Kind.PAID_OFF,
            after.closeAsRepaid(businessDate), Money.ZERO, rebate, surplus);
    }
}
