package pk.edu.giki.microlend.reporting;

import java.util.Objects;
import pk.edu.giki.microlend.common.Money;

/**
 * A running balance on a statement: debits less credits, shown as an amount on the side that is
 * larger (BR-35, BR-43).
 *
 * @param amount the difference between the two columns, never negative
 * @param side the column that is larger, or {@link BalanceSide#NIL} when they are equal
 */
public record Balance(Money amount, BalanceSide side) {

    /** The balance of an account with no movement, or whose debits and credits cancel. */
    public static final Balance NIL = new Balance(Money.ZERO, BalanceSide.NIL);

    /**
     * Validates the balance.
     *
     * @throws NullPointerException if an argument is null
     */
    public Balance {
        Objects.requireNonNull(amount, "amount is required");
        Objects.requireNonNull(side, "side is required");
    }

    /**
     * Works out the balance from two column totals.
     *
     * @param debits the total of the debit column
     * @param credits the total of the credit column
     * @return the difference, on the larger side; {@link #NIL} when they are equal
     * @throws NullPointerException if an argument is null
     */
    public static Balance of(final Money debits, final Money credits) {
        Objects.requireNonNull(debits, "debits are required");
        Objects.requireNonNull(credits, "credits are required");
        if (debits.isGreaterThan(credits)) {
            return new Balance(debits.minus(credits), BalanceSide.DEBIT);
        }
        if (credits.isGreaterThan(debits)) {
            return new Balance(credits.minus(debits), BalanceSide.CREDIT);
        }
        return NIL;
    }
}
