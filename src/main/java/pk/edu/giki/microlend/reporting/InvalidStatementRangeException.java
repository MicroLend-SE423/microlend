package pk.edu.giki.microlend.reporting;

import java.time.LocalDate;
import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised when a statement is requested for a range that ends before it starts. BR-35 is silent
 * on this case; refusing it is the team's proposed reading (TC-REP-01.3).
 */
public class InvalidStatementRangeException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param from the requested start date
     * @param to the requested end date
     */
    public InvalidStatementRangeException(final LocalDate from, final LocalDate to) {
        super("a statement cannot start on " + from + ", after it ends on " + to);
    }
}
