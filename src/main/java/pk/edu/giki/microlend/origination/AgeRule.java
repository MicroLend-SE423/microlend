package pk.edu.giki.microlend.origination;

/**
 * BR-01 and BR-02 (FR-01): the applicant must be at least 18 at disbursement and
 * no more than 65 at maturity, both in completed years.
 *
 * <p>The upper limit is tested at maturity, not at application, so a 64-year-old
 * asking for 24 months is rejected.</p>
 */
public final class AgeRule implements EligibilityRule {

    private static final int MINIMUM_AGE_AT_DISBURSEMENT = 18;
    private static final int MAXIMUM_AGE_AT_MATURITY = 65;

    @Override
    public RuleOutcome check(final LoanApplication application) {
        final Applicant applicant = application.applicant();
        final int ageAtDisbursement = applicant.ageOn(application.disbursementDate());
        if (ageAtDisbursement < MINIMUM_AGE_AT_DISBURSEMENT) {
            return RuleOutcome.fail(RejectionReason.AGE, "BR-01: age " + ageAtDisbursement
                + " at disbursement is below " + MINIMUM_AGE_AT_DISBURSEMENT);
        }
        final int ageAtMaturity = applicant.ageOn(application.maturityDate());
        if (ageAtMaturity > MAXIMUM_AGE_AT_MATURITY) {
            return RuleOutcome.fail(RejectionReason.AGE, "BR-02: age " + ageAtMaturity
                + " at maturity is above " + MAXIMUM_AGE_AT_MATURITY);
        }
        return RuleOutcome.pass();
    }
}
