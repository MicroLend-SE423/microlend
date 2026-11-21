package pk.edu.giki.microlend.origination;

import java.math.BigDecimal;
import pk.edu.giki.microlend.common.Money;

/**
 * The risk grade of an approved applicant (BR-09, FR-06). Each grade adds a
 * fixed spread to the product's rate and caps the approved amount at a share of
 * the amount requested (BR-10).
 *
 * <p>Assignment, from the burden ratio of BR-04 and the worst days past due on
 * any earlier loan:</p>
 * <ul>
 *   <li><b>A</b>: burden at most 25%, at least one earlier loan, never more than
 *       30 days past due;</li>
 *   <li><b>B</b>: burden at most 35% and never more than 60 days past due,
 *       first-time borrowers included;</li>
 *   <li><b>C</b>: any other applicant who passes BR-01 to BR-07.</li>
 * </ul>
 */
public enum RiskGrade {

    /** Spread +0.00%, approved up to 100% of the amount requested. */
    A(new BigDecimal("0.00"), new BigDecimal("100")),

    /** Spread +1.50%, approved up to 80% of the amount requested. */
    B(new BigDecimal("1.50"), new BigDecimal("80")),

    /** Spread +3.00%, approved up to 60% of the amount requested. */
    C(new BigDecimal("3.00"), new BigDecimal("60"));

    private static final BigDecimal GRADE_A_MAXIMUM_BURDEN = new BigDecimal("25");
    private static final BigDecimal GRADE_B_MAXIMUM_BURDEN = new BigDecimal("35");
    private static final int GRADE_A_MAXIMUM_DAYS_PAST_DUE = 30;
    private static final int GRADE_B_MAXIMUM_DAYS_PAST_DUE = 60;

    private final BigDecimal spreadPercent;
    private final BigDecimal capPercent;

    RiskGrade(final BigDecimal spreadPercent, final BigDecimal capPercent) {
        this.spreadPercent = spreadPercent;
        this.capPercent = capPercent;
    }

    /**
     * Assigns the grade of an applicant who passed every eligibility rule.
     *
     * @param burdenPercent the burden ratio of BR-04, in percent
     * @param earlierLoans how many loans the applicant held before
     * @param worstDaysPastDue the most days past due on any earlier loan
     * @return the grade
     */
    public static RiskGrade assign(final BigDecimal burdenPercent, final int earlierLoans,
                                   final int worstDaysPastDue) {
        if (burdenPercent.compareTo(GRADE_A_MAXIMUM_BURDEN) <= 0 && earlierLoans >= 1
            && worstDaysPastDue <= GRADE_A_MAXIMUM_DAYS_PAST_DUE) {
            return A;
        }
        if (burdenPercent.compareTo(GRADE_B_MAXIMUM_BURDEN) <= 0
            && worstDaysPastDue <= GRADE_B_MAXIMUM_DAYS_PAST_DUE) {
            return B;
        }
        return C;
    }

    /**
     * Returns the spread this grade adds to the product's base rate.
     *
     * @return the spread in percentage points, for example {@code 1.50}
     */
    public BigDecimal spreadPercent() {
        return spreadPercent;
    }

    /**
     * Returns the amount this grade may be approved for (BR-10).
     *
     * @param requested the amount requested
     * @return the grade's share of it, rounded to the paisa (BR-15)
     */
    public Money cap(final Money requested) {
        return requested.percentage(capPercent);
    }
}
