package pk.edu.giki.microlend.ledger;

import java.util.Objects;
import pk.edu.giki.microlend.common.Money;

/**
 * What settlement decided after a payment was allocated (queue item L-06).
 *
 * @param kind which case applied
 * @param account the account afterwards: closed when {@link Kind#PAID_OFF}, otherwise unchanged
 * @param advanceCredit the surplus held against future instalments (BR-21); zero unless
 *     {@link Kind#ADVANCE_CREDIT}
 * @param rebate the interest rebate supplied by the product (BR-22); zero unless
 *     {@link Kind#PAID_OFF}
 * @param excess money left over after a payoff; no rule yet says what happens to it, so it
 *     is reported to the caller and not posted (open team decision)
 */
public record SettlementOutcome(Kind kind, LoanAccount account, Money advanceCredit,
                                Money rebate, Money excess) {

    /** The settlement cases. */
    public enum Kind {
        /** Nothing left over and principal still owed. */
        NO_SURPLUS,
        /** A surplus while principal is still owed becomes advance credit (BR-21). */
        ADVANCE_CREDIT,
        /** The payment cleared the loan; it is closed and the rebate applies (BR-22). */
        PAID_OFF
    }

    /**
     * Validates the outcome.
     *
     * @throws NullPointerException if any component is null
     */
    public SettlementOutcome {
        Objects.requireNonNull(kind, "kind is required");
        Objects.requireNonNull(account, "account is required");
        Objects.requireNonNull(advanceCredit, "advance credit is required");
        Objects.requireNonNull(rebate, "rebate is required");
        Objects.requireNonNull(excess, "excess is required");
    }
}
