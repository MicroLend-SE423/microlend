package pk.edu.giki.microlend.origination;

import java.time.LocalDate;
import java.time.Period;
import pk.edu.giki.microlend.common.Money;

/**
 * The person applying for a loan, with the summary of their loan history that
 * the eligibility rules and the risk grade need.
 *
 * <p>The caller counts the loans: a loan in arrears is active and a written-off
 * loan is not (BR-03). Earlier loans are every loan the applicant held before
 * this application, whether active, closed or written off.</p>
 *
 * @param dateOfBirth the applicant's date of birth
 * @param monthlyIncome declared monthly income; must be greater than zero
 * @param existingMonthlyInstalments the monthly instalments of the applicant's
 *     active loans, without the loan applied for (BR-04)
 * @param activeLoans existing active loans, not counting the one applied for (BR-03)
 * @param earlierLoans all earlier loans, so at least the active ones plus any written-off one
 * @param worstDaysPastDue the most days past due on any earlier loan; 0 if none (BR-09)
 * @param priorWriteOff whether any earlier loan was written off (BR-05)
 */
public record Applicant(LocalDate dateOfBirth, Money monthlyIncome,
                        Money existingMonthlyInstalments, int activeLoans, int earlierLoans,
                        int worstDaysPastDue, boolean priorWriteOff) {

    /**
     * Validates the applicant.
     *
     * @throws InvalidLoanApplicationException if a field is missing, the income is
     *     zero, a count is negative, or the loan history contradicts itself
     */
    public Applicant {
        if (dateOfBirth == null || monthlyIncome == null || existingMonthlyInstalments == null) {
            throw new InvalidLoanApplicationException(
                "date of birth, monthly income and existing monthly instalments are required");
        }
        if (monthlyIncome.isZero()) {
            throw new InvalidLoanApplicationException(
                "monthly income must be greater than zero, because the burden ratio (BR-04) divides by it");
        }
        if (activeLoans < 0 || earlierLoans < 0 || worstDaysPastDue < 0) {
            throw new InvalidLoanApplicationException("loan counts and days past due may not be negative: "
                + activeLoans + " active, " + earlierLoans + " earlier, " + worstDaysPastDue + " days");
        }
        final int loansOnRecord = activeLoans + (priorWriteOff ? 1 : 0);
        if (earlierLoans < loansOnRecord) {
            throw new InvalidLoanApplicationException("earlier loans (" + earlierLoans
                + ") must include the " + activeLoans + " active loan(s) and any written-off loan");
        }
        if (earlierLoans == 0 && worstDaysPastDue > 0) {
            throw new InvalidLoanApplicationException(
                "an applicant with no earlier loan cannot have days past due: " + worstDaysPastDue);
        }
    }

    /**
     * Returns the applicant's age in completed years on a date.
     *
     * @param date the date to measure the age on
     * @return whole years since the date of birth; the day before a birthday
     *     still counts the previous year
     */
    public int ageOn(final LocalDate date) {
        return Period.between(dateOfBirth, date).getYears();
    }
}
