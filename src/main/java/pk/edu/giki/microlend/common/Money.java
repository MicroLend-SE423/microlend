package pk.edu.giki.microlend.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * An amount of money in the system's single currency.
 *
 * <p>This type exists so that no other class has to think about monetary
 * representation. It is immutable, it cannot hold a negative amount, and it is
 * always normalised to two decimal places using {@code HALF_EVEN} rounding
 * (BR-15, BR-40, BR-43).</p>
 *
 * <p>Constraint C-01: money is never represented as {@code double} or
 * {@code float}. In IEEE 754, {@code 0.1 + 0.2} is not {@code 0.3}, and a
 * fraction of a rupee per instalment accumulates across a portfolio into a
 * ledger that does not balance.</p>
 *
 * <p>Direction is never expressed by a sign. A ledger movement states its
 * direction through the debit or credit column it is written to.</p>
 */
public final class Money implements Comparable<Money> {

    /** The scale every monetary value is held at. */
    public static final int SCALE = 2;

    /** The rounding mode applied wherever a value must be rounded (BR-15). */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_EVEN;

    /** Zero, the only value that is both a valid amount and an empty one. */
    public static final Money ZERO = new Money(BigDecimal.ZERO.setScale(SCALE, ROUNDING));

    private static final BigDecimal PER_CENT_DIVISOR = new BigDecimal("100");

    private final BigDecimal amount;

    private Money(final BigDecimal normalisedAmount) {
        this.amount = normalisedAmount;
    }

    /**
     * Creates a monetary value, rejecting anything that would break an invariant.
     *
     * @param value the amount; must be non-null, non-negative, and must not carry
     *     more than {@link #SCALE} decimal places
     * @return the normalised value
     * @throws InvalidMoneyException if the value is null, negative, or too precise
     */
    public static Money of(final BigDecimal value) {
        if (value == null) {
            throw new InvalidMoneyException("amount is required");
        }
        if (value.scale() > SCALE) {
            throw new InvalidMoneyException(
                "amount carries more than " + SCALE + " decimal places: " + value.toPlainString());
        }
        if (value.signum() < 0) {
            throw new InvalidMoneyException(
                "amount may not be negative: " + value.toPlainString());
        }
        return new Money(value.setScale(SCALE, ROUNDING));
    }

    /**
     * Adds another amount to this one.
     *
     * @param other the amount to add
     * @return a new value; neither operand is modified
     */
    public Money plus(final Money other) {
        Objects.requireNonNull(other, "other is required");
        return new Money(this.amount.add(other.amount));
    }

    /**
     * Subtracts another amount from this one.
     *
     * @param other the amount to subtract
     * @return a new value; neither operand is modified
     * @throws InvalidMoneyException if the result would be negative, which means
     *     the caller is applying more money than is owed
     */
    public Money minus(final Money other) {
        Objects.requireNonNull(other, "other is required");
        final BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new InvalidMoneyException(
                "subtracting " + other.toPlainString() + " from " + this.toPlainString()
                    + " would produce a negative amount");
        }
        return new Money(result);
    }

    /**
     * Takes a percentage of this amount, rounded to the project's scale.
     *
     * @param percent the percentage to apply, for example {@code 0.05} for the
     *     daily penalty rate of BR-27
     * @return the resulting amount
     */
    public Money percentage(final BigDecimal percent) {
        Objects.requireNonNull(percent, "percent is required");
        final BigDecimal result = this.amount
            .multiply(percent)
            .divide(PER_CENT_DIVISOR, SCALE, ROUNDING);
        return new Money(result);
    }

    /**
     * Reports whether this amount is zero.
     *
     * @return true when the amount is exactly zero
     */
    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    /**
     * Reports whether this amount is strictly greater than another.
     *
     * @param other the amount to compare against
     * @return true when this amount is the larger of the two
     */
    public boolean isGreaterThan(final Money other) {
        return compareTo(other) > 0;
    }

    /**
     * Returns the amount as a plain string at the project's scale.
     *
     * @return the amount, for example {@code "1250.00"}
     */
    public String toPlainString() {
        return this.amount.toPlainString();
    }

    @Override
    public int compareTo(final Money other) {
        Objects.requireNonNull(other, "other is required");
        return this.amount.compareTo(other.amount);
    }

    /**
     * Compares by value. Because every instance is normalised to the same scale,
     * this agrees with {@link #compareTo}; constraint C-02 nevertheless requires
     * {@code compareTo() == 0} when comparing raw {@code BigDecimal} values
     * elsewhere, since {@code BigDecimal.equals} also compares scale.
     *
     * @param other the object to compare with
     * @return true when the other object is a Money of the same amount
     */
    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Money)) {
            return false;
        }
        return this.amount.compareTo(((Money) other).amount) == 0;
    }

    @Override
    public int hashCode() {
        return this.amount.stripTrailingZeros().hashCode();
    }

    @Override
    public String toString() {
        return "PKR " + toPlainString();
    }
}
