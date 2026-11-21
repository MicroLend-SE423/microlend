package pk.edu.giki.microlend.products;

import pk.edu.giki.microlend.common.Money;

/**
 * ConcreteCreator: the reducing-balance product (BR-11). Its only decision is which
 * calculator it creates; the schedule workflow is inherited unchanged from {@link LoanProduct}.
 */
public final class ReducingBalanceProduct extends LoanProduct {

    /**
     * Creates the product.
     *
     * @param code the catalogue code
     * @param annualRateBp the yearly rate in basis points
     * @param minAmount the smallest amount that can be lent
     * @param maxAmount the largest amount that can be lent
     * @param minTenureMonths the shortest tenure in months
     * @param maxTenureMonths the longest tenure in months
     * @throws InvalidScheduleException if a value is missing or a band is empty
     */
    public ReducingBalanceProduct(final String code, final int annualRateBp, final Money minAmount,
                                   final Money maxAmount, final int minTenureMonths,
                                   final int maxTenureMonths) {
        super(code, annualRateBp, minAmount, maxAmount, minTenureMonths, maxTenureMonths);
    }

    @Override
    protected InterestCalculator createInterestCalculator() {
        return new ReducingBalanceCalculator();
    }
}
