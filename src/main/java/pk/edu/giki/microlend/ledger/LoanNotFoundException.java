package pk.edu.giki.microlend.ledger;

import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;

/** A posting named a loan that does not exist. */
public final class LoanNotFoundException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    private final transient LoanId loanId;

    /**
     * Creates the exception.
     *
     * @param loanId the loan that was not found
     */
    public LoanNotFoundException(final LoanId loanId) {
        super("no loan " + loanId + " exists");
        this.loanId = loanId;
    }

    /**
     * Returns the loan that was not found.
     *
     * @return the loan identifier
     */
    public LoanId loanId() {
        return loanId;
    }
}
