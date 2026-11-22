package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.math.MathContext;
import pk.edu.giki.microlend.common.Money;

/**
 * The single place where schedule arithmetic leaves exact {@link BigDecimal} and becomes
 * {@link Money} again, so that every product rounds the same way: two decimal places,
 * HALF_EVEN (BR-15).
 */
final class Rounding {

    /** Working precision for intermediate results; far beyond what any loan needs. */
    static final MathContext PRECISION = MathContext.DECIMAL128;

    private static final BigDecimal MONTHS_TIMES_PERCENT = new BigDecimal("1200");

    private Rounding() {
    }

    /**
     * Rounds an exact amount to money.
     *
     * @param exact the unrounded amount, not negative
     * @return the amount at two decimal places, HALF_EVEN
     */
    static Money round(final BigDecimal exact) {
        return Money.of(exact.setScale(Money.SCALE, Money.ROUNDING));
    }

    /**
     * Returns a money amount as an exact decimal for further arithmetic.
     *
     * @param amount the amount
     * @return the same value as a {@link BigDecimal}
     */
    static BigDecimal exact(final Money amount) {
        return new BigDecimal(amount.toPlainString());
    }

    /**
     * Returns the interest a balance earns in one month, unrounded.
     *
     * @param balance the balance the interest accrues on
     * @param annualRatePercent the yearly rate in percent, for example 24.00
     * @return balance x rate / 1200
     */
    static BigDecimal monthlyInterest(final Money balance, final BigDecimal annualRatePercent) {
        return exact(balance).multiply(annualRatePercent).divide(MONTHS_TIMES_PERCENT, PRECISION);
    }

    /**
     * Checks the inputs every calculator shares.
     *
     * @param annualRatePercent the yearly rate in percent
     * @param tenureMonths the number of instalments
     * @throws InvalidScheduleException if the rate is missing or negative, or the tenure is below 1
     */
    static void requireValid(final BigDecimal annualRatePercent, final int tenureMonths) {
        if (annualRatePercent == null || annualRatePercent.signum() < 0) {
            throw new InvalidScheduleException(
                "the annual rate must be zero or more, got " + annualRatePercent);
        }
        if (tenureMonths < 1) {
            throw new InvalidScheduleException(
                "a schedule needs at least one instalment, got " + tenureMonths);
        }
    }
}
