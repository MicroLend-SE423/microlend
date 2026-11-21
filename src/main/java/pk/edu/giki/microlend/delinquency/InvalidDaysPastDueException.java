package pk.edu.giki.microlend.delinquency;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Thrown when a negative number of days past due reaches a delinquency calculation.
 */
public final class InvalidDaysPastDueException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception for the rejected value.
     *
     * @param daysPastDue the negative value that was supplied
     */
    public InvalidDaysPastDueException(final int daysPastDue) {
        super("days past due may not be negative, got " + daysPastDue);
    }
}
