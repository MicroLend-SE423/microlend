package pk.edu.giki.microlend.ledger;

import pk.edu.giki.microlend.common.LoanDomainException;

/** A bank payment whose (bank, reference) pair was already posted (BR-25). */
public final class DuplicatePaymentException extends LoanDomainException {

    private static final long serialVersionUID = 1L;

    private final String bank;
    private final String reference;

    /**
     * Creates the exception.
     *
     * @param bank the reporting bank
     * @param reference the repeated reference
     */
    public DuplicatePaymentException(final String bank, final String reference) {
        super("payment " + bank + "/" + reference + " has already been posted (BR-25)");
        this.bank = bank;
        this.reference = reference;
    }

    /**
     * Returns the reporting bank.
     *
     * @return the bank
     */
    public String bank() {
        return bank;
    }

    /**
     * Returns the repeated reference.
     *
     * @return the reference
     */
    public String reference() {
        return reference;
    }
}
