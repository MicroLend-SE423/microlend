package pk.edu.giki.microlend.common;

/**
 * Raised when a monetary value cannot be constructed because it would violate
 * one of {@link Money}'s invariants.
 *
 * <p>Because this is thrown from the constructor, no invalid {@code Money}
 * instance can exist, and the rest of the system never has to defend against
 * a state it cannot be in.</p>
 */
public final class InvalidMoneyException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param message which invariant was violated, and by what value
     */
    public InvalidMoneyException(final String message) {
        super(message);
    }
}
