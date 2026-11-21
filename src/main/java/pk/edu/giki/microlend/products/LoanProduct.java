package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.time.LocalDate;
import pk.edu.giki.microlend.common.Money;

/**
 * The Creator of the Factory Method pattern (M1 design section 5.1): a loan product, and the
 * stable workflow that turns an approved amount and tenure into a repayment schedule.
 *
 * <p>{@link #generateSchedule} validates the request, builds the due dates, asks for the
 * product's {@link InterestCalculator} through the factory method
 * {@link #createInterestCalculator()}, and assembles the schedule. Exactly one step varies
 * between products, and it is deferred to the subclass: {@link FlatRateProduct},
 * {@link ReducingBalanceProduct} and {@link BulletProduct} each override the factory method.
 * The workflow never names a concrete calculator, so a new product is a new subclass and a
 * new calculator, and {@code generateSchedule} is never reopened (open for extension, closed
 * for modification).</p>
 *
 * <p>The rate is fixed at construction and applies to loans created from it (BR-17).</p>
 */
public abstract class LoanProduct {

    private final String code;
    private final int annualRateBp;
    private final Money minAmount;
    private final Money maxAmount;
    private final int minTenureMonths;
    private final int maxTenureMonths;
    private final DueDateCalculator dueDates = new DueDateCalculator();
    private final GracePeriodPolicy grace = new GracePeriodPolicy();
    private final ScheduleAssembler assembler = new ScheduleAssembler();

    /**
     * Creates a product.
     *
     * @param code the catalogue code, for example {@code "RB-12"}
     * @param annualRateBp the yearly interest rate in basis points, 1200 meaning 12.00%
     * @param minAmount the smallest amount that can be lent
     * @param maxAmount the largest amount that can be lent
     * @param minTenureMonths the shortest tenure in months
     * @param maxTenureMonths the longest tenure in months
     * @throws InvalidScheduleException if a value is missing, the rate is negative, or a band
     *     is empty or inverted
     */
    protected LoanProduct(final String code, final int annualRateBp, final Money minAmount,
                          final Money maxAmount, final int minTenureMonths,
                          final int maxTenureMonths) {
        if (code == null || code.isBlank() || minAmount == null || maxAmount == null) {
            throw new InvalidScheduleException("a product needs a code and an amount band");
        }
        if (annualRateBp < 0) {
            throw new InvalidScheduleException("the annual rate may not be negative: " + annualRateBp);
        }
        if (minAmount.isGreaterThan(maxAmount) || minTenureMonths < 1
            || minTenureMonths > maxTenureMonths) {
            throw new InvalidScheduleException("product " + code + " has an empty amount or tenure band");
        }
        this.code = code.trim();
        this.annualRateBp = annualRateBp;
        this.minAmount = minAmount;
        this.maxAmount = maxAmount;
        this.minTenureMonths = minTenureMonths;
        this.maxTenureMonths = maxTenureMonths;
    }

    /**
     * Generates the repayment schedule for a loan with no grace period.
     *
     * @param principal the amount lent, within this product's band
     * @param tenureMonths the number of monthly instalments, within this product's band
     * @param disbursement the date the loan is disbursed
     * @return the schedule: exactly {@code tenureMonths} instalments that add up to the
     *     principal plus the total interest (BR-14, BR-16)
     * @throws InvalidScheduleException if the amount, tenure or date is not acceptable
     */
    public final Schedule generateSchedule(final Money principal, final int tenureMonths,
                                           final LocalDate disbursement) {
        return generateSchedule(principal, tenureMonths, disbursement, 0);
    }

    /**
     * Generates the repayment schedule for a loan.
     *
     * <p>This is the stable workflow of the pattern and cannot be overridden: validate, build
     * the due dates (deferred by the grace period), obtain the calculator from the factory
     * method, and assemble. Interest accrued during grace is added to the first instalment
     * (BR-13).</p>
     *
     * @param principal the amount lent, within this product's band
     * @param tenureMonths the number of monthly instalments, within this product's band
     * @param disbursement the date the loan is disbursed
     * @param graceMonths the grace period in months, 0 to 3
     * @return the schedule: exactly {@code tenureMonths} instalments that add up to the
     *     principal plus the total interest (BR-14, BR-16)
     * @throws InvalidScheduleException if the amount, tenure or date is not acceptable
     * @throws InvalidGracePeriodException if the grace period is outside 0 to 3 months
     */
    public final Schedule generateSchedule(final Money principal, final int tenureMonths,
                                           final LocalDate disbursement, final int graceMonths) {
        validate(principal, tenureMonths, disbursement);
        final BigDecimal rate = ratePercent();
        final Money graceInterest = grace.accruedInterest(principal, rate, graceMonths);
        final InterestCalculator calculator = createInterestCalculator();
        return assembler.assemble(principal,
            dueDates.dueDates(disbursement, 1 + graceMonths, tenureMonths),
            calculator.principalSchedule(principal, rate, tenureMonths),
            calculator.interestSchedule(principal, rate, tenureMonths),
            graceInterest);
    }

    /**
     * The factory method: returns this product's interest calculation.
     *
     * @return a calculator honouring the {@link InterestCalculator} contract
     */
    protected abstract InterestCalculator createInterestCalculator();

    /**
     * Returns the catalogue code.
     *
     * @return the product code
     */
    public String code() {
        return code;
    }

    /**
     * Returns the yearly interest rate.
     *
     * @return the rate in basis points, 1200 meaning 12.00%
     */
    public int annualRateBp() {
        return annualRateBp;
    }

    /**
     * Returns the smallest amount that can be lent.
     *
     * @return the minimum amount
     */
    public Money minAmount() {
        return minAmount;
    }

    /**
     * Returns the largest amount that can be lent.
     *
     * @return the maximum amount
     */
    public Money maxAmount() {
        return maxAmount;
    }

    /**
     * Returns the shortest tenure.
     *
     * @return the minimum tenure in months
     */
    public int minTenureMonths() {
        return minTenureMonths;
    }

    /**
     * Returns the longest tenure.
     *
     * @return the maximum tenure in months
     */
    public int maxTenureMonths() {
        return maxTenureMonths;
    }

    private BigDecimal ratePercent() {
        return BigDecimal.valueOf(annualRateBp, 2);
    }

    private void validate(final Money principal, final int tenureMonths, final LocalDate disbursement) {
        if (principal == null || disbursement == null) {
            throw new InvalidScheduleException("an amount and a disbursement date are required");
        }
        if (principal.compareTo(minAmount) < 0 || principal.isGreaterThan(maxAmount)) {
            throw new InvalidScheduleException("amount " + principal + " is outside the " + code
                + " band of " + minAmount + " to " + maxAmount);
        }
        if (tenureMonths < minTenureMonths || tenureMonths > maxTenureMonths) {
            throw new InvalidScheduleException("tenure " + tenureMonths + " is outside the " + code
                + " band of " + minTenureMonths + " to " + maxTenureMonths + " months");
        }
    }
}
