package pk.edu.giki.microlend.origination;

/**
 * BR-03 (FR-02): an applicant may hold at most 2 active loans including the one
 * applied for, so 2 or more existing active loans are rejected.
 *
 * <p>A loan in arrears counts as active and a written-off loan does not; the
 * caller applies that when it fills {@link Applicant#activeLoans()}.</p>
 */
public final class ActiveLoanLimitRule implements EligibilityRule {

    private static final int MAXIMUM_ACTIVE_LOANS = 2;

    @Override
    public RuleOutcome check(final LoanApplication application) {
        final int activeWithNewLoan = application.applicant().activeLoans() + 1;
        if (activeWithNewLoan > MAXIMUM_ACTIVE_LOANS) {
            return RuleOutcome.fail(RejectionReason.ACTIVE_LOAN_LIMIT, "BR-03: "
                + activeWithNewLoan + " active loans including this one is above "
                + MAXIMUM_ACTIVE_LOANS);
        }
        return RuleOutcome.pass();
    }
}
