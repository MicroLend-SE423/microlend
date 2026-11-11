package pk.edu.giki.microlend.ledger;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * A validated request to post one repayment to a loan account.
 *
 * <p>Validation happens once, in the two factories, so the posting pipeline
 * never re-checks its input (M1 design section 3.5). A cash payment carries no
 * reference; a bank payment carries the (bank, reference) pair that the
 * idempotency guard compares to reject a duplicate (BR-25). The amount must
 * be greater than zero, because a posting that moves no money cannot write
 * a ledger line (BR-23). The command is immutable and compares by value.</p>
 */
public final class RepaymentCommand {

    private static final String CASH = "cash";

    private final LoanId loanId;
    private final Money amount;
    private final LocalDate businessDate;
    private final String bank;
    private final String reference;

    private RepaymentCommand(final LoanId loanId, final Money amount,
                             final LocalDate businessDate, final String bank,
                             final String reference) {
        this.loanId = loanId;
        this.amount = amount;
        this.businessDate = businessDate;
        this.bank = bank;
        this.reference = reference;
    }

    /**
     * Creates a cash payment received at the branch.
     *
     * @param loanId the account being repaid
     * @param amount the amount received; must be greater than zero
     * @param businessDate the business date of the payment (BR-42)
     * @return the command
     * @throws InvalidRepaymentException if a field is missing or the amount is zero
     */
    public static RepaymentCommand cash(final LoanId loanId, final Money amount,
                                        final LocalDate businessDate) {
        requireCommon(loanId, amount, businessDate, CASH);
        return new RepaymentCommand(loanId, amount, businessDate, null, null);
    }

    /**
     * Creates a payment received through a bank, identified by its
     * (bank, reference) pair.
     *
     * @param loanId the account being repaid
     * @param amount the amount received; must be greater than zero
     * @param businessDate the business date of the payment (BR-42)
     * @param bank the bank that reported the payment; surrounding whitespace is removed
     * @param reference the bank's transaction reference; surrounding whitespace is removed
     * @return the command
     * @throws InvalidRepaymentException if a field is missing, the amount is zero,
     *     or the bank or reference is blank
     */
    public static RepaymentCommand bank(final LoanId loanId, final Money amount,
                                        final LocalDate businessDate, final String bank,
                                        final String reference) {
        final String trimmedBank = requireText(bank, "bank", String.valueOf(bank));
        requireCommon(loanId, amount, businessDate, trimmedBank);
        final String trimmedReference = requireText(reference, "bank reference", trimmedBank);
        return new RepaymentCommand(loanId, amount, businessDate, trimmedBank, trimmedReference);
    }

    private static void requireCommon(final LoanId loanId, final Money amount,
                                      final LocalDate businessDate, final String source) {
        if (loanId == null) {
            throw new InvalidRepaymentException("loan id is required", "null", source);
        }
        if (amount == null) {
            throw new InvalidRepaymentException("amount is required", "null", source);
        }
        if (amount.isZero()) {
            throw new InvalidRepaymentException(
                "a repayment must move money; a zero amount cannot be posted",
                amount.toPlainString(), source);
        }
        if (businessDate == null) {
            throw new InvalidRepaymentException("business date is required", "null", source);
        }
    }

    private static String requireText(final String value, final String field,
                                      final String source) {
        if (value == null || value.trim().isEmpty()) {
            throw new InvalidRepaymentException(
                field + " is required for a bank payment and may not be blank",
                String.valueOf(value), source);
        }
        return value.trim();
    }

    /**
     * Returns the account being repaid.
     *
     * @return the loan identifier
     */
    public LoanId loanId() {
        return loanId;
    }

    /**
     * Returns the amount received.
     *
     * @return an amount greater than zero
     */
    public Money amount() {
        return amount;
    }

    /**
     * Returns the business date of the payment.
     *
     * @return the business date
     */
    public LocalDate businessDate() {
        return businessDate;
    }

    /**
     * Reports whether this is a cash payment, which carries no reference.
     *
     * @return true for a cash payment
     */
    public boolean isCash() {
        return bank == null;
    }

    /**
     * Returns the bank that reported the payment.
     *
     * @return the bank, or empty for a cash payment
     */
    public Optional<String> bank() {
        return Optional.ofNullable(bank);
    }

    /**
     * Returns the bank's transaction reference.
     *
     * @return the reference, or empty for a cash payment
     */
    public Optional<String> reference() {
        return Optional.ofNullable(reference);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof RepaymentCommand)) {
            return false;
        }
        final RepaymentCommand that = (RepaymentCommand) other;
        return loanId.equals(that.loanId)
            && amount.equals(that.amount)
            && businessDate.equals(that.businessDate)
            && Objects.equals(bank, that.bank)
            && Objects.equals(reference, that.reference);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, amount, businessDate, bank, reference);
    }

    @Override
    public String toString() {
        final String source = isCash() ? CASH : bank + "/" + reference;
        return "RepaymentCommand[" + loanId + " " + amount.toPlainString()
            + " on " + businessDate + " via " + source + "]";
    }
}
