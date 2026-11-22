package pk.edu.giki.microlend.ledger;

import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Raised when a repayment request cannot be turned into a valid
 * {@link RepaymentCommand}.
 *
 * <p>It carries the offending value and the source of the payment (a bank
 * name, or {@code "cash"}), so an operator can find the bad row or counter
 * transaction (M1 design section 3.5).</p>
 */
public final class InvalidRepaymentException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    private final String offendingValue;
    private final String source;

    /**
     * Creates the exception.
     *
     * @param message which rule the request broke
     * @param offendingValue the rejected value as text, or {@code "null"}
     * @param source where the payment came from: a bank name or {@code "cash"}
     */
    public InvalidRepaymentException(final String message, final String offendingValue,
                                     final String source) {
        super(message + " [value=" + offendingValue + ", source=" + source + "]");
        this.offendingValue = offendingValue;
        this.source = source;
    }

    /**
     * Returns the rejected value.
     *
     * @return the value as text, or {@code "null"} when it was missing
     */
    public String offendingValue() {
        return offendingValue;
    }

    /**
     * Returns where the payment came from.
     *
     * @return a bank name, or {@code "cash"}
     */
    public String source() {
        return source;
    }
}
