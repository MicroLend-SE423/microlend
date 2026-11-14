package pk.edu.giki.microlend.ledger;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised when a {@link LedgerEntry} cannot be constructed because it would
 * violate one of the ledger's invariants.
 *
 * <p>Thrown from the factories, so an entry that moves no money or that has
 * no loan, type or timestamp can never reach the ledger.</p>
 */
public final class InvalidLedgerEntryException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message which invariant was violated
     */
    public InvalidLedgerEntryException(final String message) {
        super(message);
    }
}
