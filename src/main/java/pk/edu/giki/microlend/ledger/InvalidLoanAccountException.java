package pk.edu.giki.microlend.ledger;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised when a {@link LoanAccount} cannot be constructed because its state
 * would be inconsistent, for example a closed loan with no closure date.
 */
public final class InvalidLoanAccountException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message which invariant was violated
     */
    public InvalidLoanAccountException(final String message) {
        super(message);
    }
}
