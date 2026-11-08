package pk.edu.giki.microlend.ledger;

import java.time.LocalDate;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;

/**
 * Raised when a payment is posted against a loan that is closed or written off
 * (BR-26, FR-19).
 *
 * <p>Carries the loan, its status and the date it closed, so a teller screen
 * or an import report can say exactly why the payment was refused.</p>
 */
public final class LoanClosedException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    private final transient LoanId loanId;
    private final LoanStatus status;
    private final LocalDate closedOn;

    /**
     * Creates the exception.
     *
     * @param loanId the loan the payment was posted against
     * @param status the loan's status, {@link LoanStatus#CLOSED} or {@link LoanStatus#WRITTEN_OFF}
     * @param closedOn the date the loan stopped accepting payments
     */
    public LoanClosedException(final LoanId loanId, final LoanStatus status, final LocalDate closedOn) {
        super("loan " + loanId + " is " + status + " since " + closedOn + " and accepts no payments");
        this.loanId = loanId;
        this.status = status;
        this.closedOn = closedOn;
    }

    /**
     * Returns the loan the payment was posted against.
     *
     * @return the loan identifier
     */
    public LoanId loanId() {
        return loanId;
    }

    /**
     * Returns why the loan accepts no payments.
     *
     * @return the closed or written-off status
     */
    public LoanStatus status() {
        return status;
    }

    /**
     * Returns the date the loan stopped accepting payments.
     *
     * @return the closure or write-off date
     */
    public LocalDate closedOn() {
        return closedOn;
    }
}
