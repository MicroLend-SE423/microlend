package pk.edu.giki.microlend.origination;

import pk.edu.giki.microlend.common.Money;

/**
 * BR-06 and BR-07 (FR-07): the requested amount must lie within the product's
 * minimum and maximum, and the tenure within its tenure band, both inclusive.
 *
 * <p>The limits come from the {@link ProductBand} on the application. When
 * {@code products.LoanProduct} exists, they will be read from the product.</p>
 */
public final class ProductBandRule implements EligibilityRule {

    @Override
    public RuleOutcome check(final LoanApplication application) {
        final ProductBand band = application.product();
        final Money amount = application.requestedAmount();
        if (amount.compareTo(band.minAmount()) < 0 || amount.isGreaterThan(band.maxAmount())) {
            return RuleOutcome.fail(RejectionReason.OUTSIDE_PRODUCT_BAND, "BR-06: amount "
                + amount.toPlainString() + " is outside " + band.minAmount().toPlainString()
                + " to " + band.maxAmount().toPlainString());
        }
        final int tenure = application.tenureMonths();
        if (tenure < band.minTenureMonths() || tenure > band.maxTenureMonths()) {
            return RuleOutcome.fail(RejectionReason.OUTSIDE_PRODUCT_BAND, "BR-07: tenure "
                + tenure + " months is outside " + band.minTenureMonths() + " to "
                + band.maxTenureMonths());
        }
        return RuleOutcome.pass();
    }
}
