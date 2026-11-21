package pk.edu.giki.microlend.ledger.importing;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * A bank statement that cannot be turned into repayments: a file in another
 * format, or a malformed row (FR-20, TC-LED-08.3 and .4).
 *
 * <p>It names the file and the line, counting the header as line 1, so an
 * operator can find and fix the row.</p>
 */
public final class InvalidStatementException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    private final String sourceName;
    private final long line;

    /**
     * Creates the exception.
     *
     * @param sourceName the statement file
     * @param line the line at fault; the header is line 1
     * @param problem what is wrong with it
     */
    public InvalidStatementException(final String sourceName, final long line,
                                     final String problem) {
        this(sourceName, line, problem, null);
    }

    /**
     * Creates the exception, keeping the failure it translates.
     *
     * @param sourceName the statement file
     * @param line the line at fault; the header is line 1
     * @param problem what is wrong with it
     * @param cause the parsing or validation failure, or null
     */
    public InvalidStatementException(final String sourceName, final long line,
                                     final String problem, final Throwable cause) {
        super(sourceName + " line " + line + ": " + problem, cause);
        this.sourceName = sourceName;
        this.line = line;
    }

    /**
     * Returns the statement file.
     *
     * @return the file name given to the adapter
     */
    public String sourceName() {
        return sourceName;
    }

    /**
     * Returns the line at fault.
     *
     * @return the line number; the header is line 1
     */
    public long line() {
        return line;
    }
}
