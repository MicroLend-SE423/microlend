package pk.edu.giki.microlend.common;

import java.util.Objects;

/**
 * Identifies one loan account.
 *
 * <p>Every ledger entry, instalment, delinquency snapshot and per-account lock
 * is keyed by this value, so it is a typed object rather than a bare string:
 * a method that takes a {@code LoanId} cannot be handed a customer number by
 * mistake. It is immutable and compares by value.</p>
 */
public final class LoanId {

    private final String value;

    private LoanId(final String value) {
        this.value = value;
    }

    /**
     * Creates an identifier from its textual form.
     *
     * @param value the identifier; surrounding whitespace is removed
     * @return the identifier
     * @throws InvalidLoanIdException if the value is null or blank
     */
    public static LoanId of(final String value) {
        if (value == null) {
            throw new InvalidLoanIdException("loan id is required");
        }
        final String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw new InvalidLoanIdException("loan id may not be blank");
        }
        return new LoanId(trimmed);
    }

    /**
     * Returns the textual form of the identifier.
     *
     * @return the identifier as stored, for example {@code "LN-0001"}
     */
    public String value() {
        return value;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LoanId)) {
            return false;
        }
        return value.equals(((LoanId) other).value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
