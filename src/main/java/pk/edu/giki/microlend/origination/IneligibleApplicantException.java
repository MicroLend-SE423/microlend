package pk.edu.giki.microlend.origination;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised for a rejected application when the caller needs an exception rather
 * than a {@link Decision}; see {@link Decision#requireApproved()}.
 *
 * <p>Carries the failed {@link RuleOutcome}, so the caller knows the typed
 * reason and the business rule that failed (BR-08).</p>
 */
public final class IneligibleApplicantException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    private final RuleOutcome outcome;

    /**
     * Creates the exception.
     *
     * @param outcome the failed outcome that rejected the application
     */
    public IneligibleApplicantException(final RuleOutcome outcome) {
        super("application rejected, " + outcome.reason() + ": " + outcome.detail());
        this.outcome = outcome;
    }

    /**
     * Returns the failed outcome.
     *
     * @return the reason and the business rule that failed
     */
    public RuleOutcome outcome() {
        return outcome;
    }
}
