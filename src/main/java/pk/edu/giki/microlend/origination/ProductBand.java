package pk.edu.giki.microlend.origination;

import pk.edu.giki.microlend.common.Money;

/**
 * The amount and tenure limits of the product applied for (BR-06, BR-07), both
 * inclusive.
 *
 * <p>The {@code products} module does not exist yet, so the limits travel on the
 * application as this small value. When {@code products.LoanProduct} is built,
 * the application will carry the product instead and this record will go.</p>
 *
 * @param minAmount the smallest amount the product lends
 * @param maxAmount the largest amount the product lends
 * @param minTenureMonths the shortest tenure, at least one month
 * @param maxTenureMonths the longest tenure
 */
public record ProductBand(Money minAmount, Money maxAmount,
                          int minTenureMonths, int maxTenureMonths) {

    /**
     * Validates the band.
     *
     * @throws InvalidLoanApplicationException if an amount is missing or a
     *     minimum exceeds its maximum
     */
    public ProductBand {
        if (minAmount == null || maxAmount == null) {
            throw new InvalidLoanApplicationException("product minimum and maximum amounts are required");
        }
        if (minAmount.isGreaterThan(maxAmount)) {
            throw new InvalidLoanApplicationException("product minimum " + minAmount.toPlainString()
                + " exceeds its maximum " + maxAmount.toPlainString());
        }
        if (minTenureMonths < 1 || minTenureMonths > maxTenureMonths) {
            throw new InvalidLoanApplicationException("product tenure band " + minTenureMonths
                + " to " + maxTenureMonths + " months is invalid");
        }
    }
}
