package pk.edu.giki.microlend.ledger;

import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * A posting's lines did not balance, so nothing was written (BR-23).
 * Reaching this is a defect in the posting code, never a user error.
 */
public final class LedgerImbalanceException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param loanId the loan being posted to
     * @param debits the sum of the debit column
     * @param credits the sum of the credit column
     */
    public LedgerImbalanceException(final LoanId loanId, final Money debits, final Money credits) {
        super("posting to loan " + loanId + " does not balance: debits "
            + debits.toPlainString() + " vs credits " + credits.toPlainString());
    }
}
