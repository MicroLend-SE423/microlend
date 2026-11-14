package pk.edu.giki.microlend.common;

/**
 * Raised when a {@link LoanId} cannot be constructed because the value is
 * missing or blank.
 *
 * <p>Thrown from the factory, so no empty identifier can exist anywhere in
 * the system.</p>
 */
public final class InvalidLoanIdException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message which rule the value broke
     */
    public InvalidLoanIdException(final String message) {
        super(message);
    }
}
