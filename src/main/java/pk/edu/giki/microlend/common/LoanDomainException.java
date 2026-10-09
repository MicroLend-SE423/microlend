package pk.edu.giki.microlend.common;

/**
 * Base type for every exception raised by MicroLend's domain modules.
 *
 * <p>Nothing outside a module ever catches a storage-layer or third-party
 * exception: a repository translates a storage failure into a subtype of this
 * class before it crosses a module boundary. Each subtype carries the data
 * needed to explain the failure, because an exception whose message is
 * "invalid" tells an operator nothing.</p>
 */
public abstract class LoanDomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a domain exception with an explanatory message.
     *
     * @param message what went wrong, in terms an operator can act on
     */
    protected LoanDomainException(final String message) {
        super(message);
    }

    /**
     * Creates a domain exception that wraps an underlying failure.
     *
     * @param message what went wrong, in terms an operator can act on
     * @param cause the underlying failure being translated into domain terms
     */
    protected LoanDomainException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
