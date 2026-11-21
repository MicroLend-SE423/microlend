package pk.edu.giki.microlend.products;

import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;

/**
 * Raised when a loan's product or schedule is needed and is not on file. It is an error, not
 * "nothing owed": answering with zero would hide a loan that has no schedule.
 */
public class ScheduleNotFoundException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param loanId the loan whose product or schedule is missing
     * @param what what was being looked up, for example {@code "product"} or {@code "schedule"}
     */
    public ScheduleNotFoundException(final LoanId loanId, final String what) {
        super("no " + what + " on file for loan " + loanId);
    }
}
