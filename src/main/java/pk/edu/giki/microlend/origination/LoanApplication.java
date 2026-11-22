package pk.edu.giki.microlend.origination;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import pk.edu.giki.microlend.common.Money;

/**
 * A request for a loan, validated once in its constructor so the eligibility
 * rules never re-check their input (M1 design section 3.5).
 *
 * @param applicant who is applying, with their loan history
 * @param requestedAmount the amount asked for; must be greater than zero
 * @param tenureMonths the number of monthly instalments asked for; at least one
 * @param disbursementDate the date the loan would be paid out (BR-01)
 * @param product the limits of the product applied for (BR-06, BR-07)
 */
public record LoanApplication(Applicant applicant, Money requestedAmount, int tenureMonths,
                              LocalDate disbursementDate, ProductBand product) {

    private static final BigDecimal PER_CENT = new BigDecimal("100");

    /**
     * Validates the application.
     *
     * @throws InvalidLoanApplicationException if a field is missing, the amount is
     *     zero, the tenure is under one month, or disbursement is not after birth
     */
    public LoanApplication {
        if (applicant == null || requestedAmount == null || disbursementDate == null
            || product == null) {
            throw new InvalidLoanApplicationException(
                "applicant, requested amount, disbursement date and product are required");
        }
        if (requestedAmount.isZero()) {
            throw new InvalidLoanApplicationException("requested amount must be greater than zero");
        }
        if (tenureMonths < 1) {
            throw new InvalidLoanApplicationException("tenure must be at least one month: " + tenureMonths);
        }
        if (!applicant.dateOfBirth().isBefore(disbursementDate)) {
            throw new InvalidLoanApplicationException("disbursement date " + disbursementDate
                + " must be after the date of birth " + applicant.dateOfBirth());
        }
    }

    /**
     * Returns the date the loan matures, used for the age limit of BR-02.
     *
     * <p>Taken as the disbursement date plus the tenure in months. This assumes no
     * grace period; once {@code products} supplies the schedule, the last due
     * date (which a grace period, BR-13, would push later) should be used.</p>
     *
     * @return the maturity date
     */
    public LocalDate maturityDate() {
        return disbursementDate.plusMonths(tenureMonths);
    }

    /**
     * Returns the monthly instalment burden as a percentage of declared income,
     * counting the new loan (BR-04).
     *
     * <p>The result is rounded <em>up</em> to two decimal places, so a burden a
     * fraction over a limit can never round down onto it. Every limit the rules
     * use (25%, 35%, 40%) is a whole number, so comparing this value with them
     * gives the same answer as the exact ratio.</p>
     *
     * @param newInstalment the new loan's monthly instalment at the product's base rate
     * @return the burden, for example {@code 40.00}
     */
    public BigDecimal burdenPercent(final Money newInstalment) {
        final Money total = applicant.existingMonthlyInstalments().plus(newInstalment);
        return amountOf(total).multiply(PER_CENT)
            .divide(amountOf(applicant.monthlyIncome()), Money.SCALE, RoundingMode.UP);
    }

    private static BigDecimal amountOf(final Money money) {
        return new BigDecimal(money.toPlainString());
    }
}
