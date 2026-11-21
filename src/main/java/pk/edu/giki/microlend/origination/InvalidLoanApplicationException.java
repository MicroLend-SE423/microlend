package pk.edu.giki.microlend.origination;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised when a {@link LoanApplication}, {@link Applicant} or {@link ProductBand}
 * cannot be constructed because an input is missing or impossible.
 *
 * <p>This is invalid input, not an ineligible applicant: an application that is
 * well formed but fails a business rule is rejected with a
 * {@link RejectionReason} instead.</p>
 */
public final class InvalidLoanApplicationException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message which input was refused, and its value
     */
    public InvalidLoanApplicationException(final String message) {
        super(message);
    }
}
