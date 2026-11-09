package pk.edu.giki.microlend.ledger;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised when an {@link Allocation} cannot be built because a tier is missing,
 * or cannot be applied to a {@link LoanAccount} because it would take the
 * outstanding principal below zero.
 */
public final class InvalidAllocationException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message which invariant was violated
     */
    public InvalidAllocationException(final String message) {
        super(message);
    }
}
