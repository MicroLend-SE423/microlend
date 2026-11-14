package pk.edu.giki.microlend.ledger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import pk.edu.giki.microlend.common.Money;

/**
 * Splits one payment across what is owed (queue item L-05; BR-18, BR-19,
 * BR-20; FR-13, FR-14).
 *
 * <p>The waterfall is applied <em>tier by tier</em>: all penalties, oldest
 * instalment first, then all fees oldest first, then all interest oldest
 * first, then all principal oldest first. This is the reading of BR-18/BR-19
 * adopted by Tughral Hussain on 2026-11-14, pending team confirmation (see the
 * note under BR-19 in {@code docs/business-rules.md}). A partial payment is
 * applied as far as it reaches (BR-20); whatever is left once everything is
 * paid is returned as unapplied for settlement to handle (BR-21).</p>
 *
 * <p>{@link #allocate} is deliberately kept as one readable method because it
 * is the team's Fagan inspection target. The allocator is stateless and so
 * safe to share between threads.</p>
 */
public final class RepaymentAllocator {

    /** The BR-18 tiers, in the order they are paid. */
    private static final List<Function<InstalmentDue, Money>> TIERS = List.of(
        InstalmentDue::penalty, InstalmentDue::fees,
        InstalmentDue::interest, InstalmentDue::principal);

    /**
     * Allocates a payment to the instalments still owing.
     *
     * @param payment the amount received; zero is accepted and allocates nothing (BR-20)
     * @param owing the unpaid instalments, in any order; each sequence must be unique
     * @return the split per tier, per instalment, and any unapplied remainder
     * @throws InvalidAllocationException if the payment, the list or an element is
     *     null, or two instalments share a sequence number
     */
    public AllocationResult allocate(final Money payment, final List<InstalmentDue> owing) {
        if (payment == null || owing == null) {
            throw new InvalidAllocationException("a payment and a list of unpaid instalments are required");
        }
        final Set<Integer> sequences = new HashSet<>();
        for (final InstalmentDue instalment : owing) {
            if (instalment == null) {
                throw new InvalidAllocationException("the list of unpaid instalments has a gap");
            }
            if (!sequences.add(instalment.sequence())) {
                throw new InvalidAllocationException(
                    "instalment " + instalment.sequence() + " appears twice");
            }
        }

        // BR-19: oldest due date first; the sequence breaks a tie.
        final List<InstalmentDue> oldestFirst = new ArrayList<>(owing);
        oldestFirst.sort(Comparator.comparing(InstalmentDue::dueDate)
            .thenComparingInt(InstalmentDue::sequence));

        // applied[i][t] = amount paid to tier t of the i-th oldest instalment.
        final Money[][] applied = new Money[oldestFirst.size()][TIERS.size()];
        Money remaining = payment;
        for (int tier = 0; tier < TIERS.size(); tier++) {           // BR-18
            for (int i = 0; i < oldestFirst.size(); i++) {           // BR-19
                final Money owed = TIERS.get(tier).apply(oldestFirst.get(i));
                final Money paid = remaining.compareTo(owed) < 0 ? remaining : owed; // BR-20
                applied[i][tier] = paid;
                remaining = remaining.minus(paid);
            }
        }

        final Map<Integer, Allocation> bySequence = new LinkedHashMap<>();
        Allocation total = Allocation.of(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);
        for (int i = 0; i < oldestFirst.size(); i++) {
            final Money[] row = applied[i];
            final Allocation share = Allocation.of(row[0], row[1], row[2], row[3]);
            bySequence.put(oldestFirst.get(i).sequence(), share);
            total = Allocation.of(total.toPenalty().plus(share.toPenalty()),
                total.toFees().plus(share.toFees()),
                total.toInterest().plus(share.toInterest()),
                total.toPrincipal().plus(share.toPrincipal()));
        }
        return new AllocationResult(total, bySequence, remaining);
    }
}
