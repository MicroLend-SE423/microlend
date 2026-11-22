package pk.edu.giki.microlend.products;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised when a repayment schedule cannot be produced from the inputs given: a missing
 * date, an amount or tenure outside what the product allows, or an unsupported grace period.
 */
public class InvalidScheduleException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message what is wrong with the schedule request, in terms an operator can act on
     */
    public InvalidScheduleException(final String message) {
        super(message);
    }
}
