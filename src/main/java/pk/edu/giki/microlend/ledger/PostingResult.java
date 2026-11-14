package pk.edu.giki.microlend.ledger;

import java.util.List;
import java.util.Objects;

/**
 * What one repayment posting did (queue item L-08).
 *
 * @param command the posted command
 * @param allocation how the payment was split (BR-18 to BR-20)
 * @param settlement what settlement decided (BR-21, BR-22); a rebate it reports
 *     is not yet posted, pending the products rebate implementation
 * @param lines the balanced ledger lines written, in order
 */
public record PostingResult(RepaymentCommand command, AllocationResult allocation,
                            SettlementOutcome settlement, List<LedgerEntry> lines) {

    /**
     * Validates and defensively copies the result.
     *
     * @throws NullPointerException if any component is null
     */
    public PostingResult {
        Objects.requireNonNull(command, "command is required");
        Objects.requireNonNull(allocation, "allocation is required");
        Objects.requireNonNull(settlement, "settlement is required");
        lines = List.copyOf(lines);
    }
}
