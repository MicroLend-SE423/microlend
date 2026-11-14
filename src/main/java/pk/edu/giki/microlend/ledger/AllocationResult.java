package pk.edu.giki.microlend.ledger;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import pk.edu.giki.microlend.common.Money;

/**
 * The outcome of allocating one payment (queue item L-05).
 *
 * @param total the payment split across the four BR-18 tiers, summed over all instalments
 * @param bySequence the split for each instalment, keyed by its sequence number;
 *     an instalment that received nothing maps to a zero allocation
 * @param unapplied the part of the payment left after everything owed was paid;
 *     non-zero only for an overpayment, which settlement turns into advance
 *     credit (BR-21, queue item L-06)
 */
public record AllocationResult(Allocation total, Map<Integer, Allocation> bySequence,
                               Money unapplied) {

    /**
     * Validates and defensively copies the result.
     *
     * @throws NullPointerException if any component is null
     */
    public AllocationResult {
        Objects.requireNonNull(total, "total is required");
        Objects.requireNonNull(unapplied, "unapplied is required");
        bySequence = Collections.unmodifiableMap(
            new LinkedHashMap<>(Objects.requireNonNull(bySequence, "bySequence is required")));
    }
}
