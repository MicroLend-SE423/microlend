package pk.edu.giki.microlend.origination;

/**
 * BR-05 (FR-04): an applicant with any prior write-off is rejected. A loan that
 * was repaid late but never written off does not count.
 */
public final class WriteOffHistoryRule implements EligibilityRule {

    @Override
    public RuleOutcome check(final LoanApplication application) {
        if (application.applicant().priorWriteOff()) {
            return RuleOutcome.fail(RejectionReason.PRIOR_WRITE_OFF,
                "BR-05: an earlier loan was written off");
        }
        return RuleOutcome.pass();
    }
}
