package pk.edu.giki.microlend.persistence;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * A storage failure translated into domain terms, so that no caller outside
 * the persistence layer ever catches a JDBC exception (M1 design section 3.5).
 */
public final class PersistenceException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception for a refusal detected by the persistence layer
     * itself rather than by the database.
     *
     * @param message what was refused and why
     */
    public PersistenceException(final String message) {
        super(message);
    }

    /**
     * Creates the exception.
     *
     * @param message what the persistence layer was trying to do
     * @param cause the underlying storage failure
     */
    public PersistenceException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
