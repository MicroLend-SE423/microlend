package pk.edu.giki.microlend.ledger;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * One movement of money on a loan account, as written to the append-only ledger.
 *
 * <p>An entry is immutable: once posted it is never changed, and a mistake is
 * corrected only by a later compensating entry that reverses it (BR-24, FR-17,
 * QR-05). Direction is expressed by the column the amount sits in, never by a
 * sign (BR-43): exactly one of {@link #debit()} and {@link #credit()} is
 * non-zero. An entry that moves nothing cannot be constructed (BR-23).</p>
 */
public final class LedgerEntry {

    private final LoanId loanId;
    private final EntryType type;
    private final Money debit;
    private final Money credit;
    private final Instant postedAt;
    private final LedgerEntry reverses;
    private final String reason;

    private LedgerEntry(final LoanId loanId, final EntryType type,
                        final Money debit, final Money credit, final Instant postedAt) {
        this.loanId = loanId;
        this.type = type;
        this.debit = debit;
        this.credit = credit;
        this.postedAt = postedAt;
        this.reverses = null;
        this.reason = null;
    }

    private LedgerEntry(final LedgerEntry original, final String reason,
                        final Instant postedAt) {
        this.loanId = original.loanId;
        this.type = EntryType.REVERSAL;
        this.debit = original.credit;
        this.credit = original.debit;
        this.postedAt = postedAt;
        this.reverses = original;
        this.reason = reason;
    }

    /**
     * Creates the compensating entry that reverses an earlier one (BR-24).
     *
     * <p>The reversal sits on the same loan, moves the same amount in the
     * opposite column, has type {@link EntryType#REVERSAL}, and references the
     * original together with the reason for the correction. The original is
     * left unchanged.</p>
     *
     * @param original the entry being corrected
     * @param reason why the correction is made; surrounding whitespace is removed
     * @param postedAt when the reversal is posted
     * @return the reversal
     * @throws InvalidLedgerEntryException if any argument is null or the reason is blank
     */
    public static LedgerEntry reversalOf(final LedgerEntry original, final String reason,
                                         final Instant postedAt) {
        if (original == null) {
            throw new InvalidLedgerEntryException("the entry to reverse is required");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new InvalidLedgerEntryException("a reversal must state its reason");
        }
        if (postedAt == null) {
            throw new InvalidLedgerEntryException("posting time is required");
        }
        return new LedgerEntry(original, reason.trim(), postedAt);
    }

    /**
     * Creates an entry whose amount sits in the debit column.
     *
     * @param loanId the account the movement belongs to
     * @param type what kind of movement this is
     * @param amount the amount moved; must be greater than zero
     * @param postedAt when the entry was posted
     * @return the entry
     * @throws InvalidLedgerEntryException if any argument is null or the amount is zero
     */
    public static LedgerEntry debit(final LoanId loanId, final EntryType type,
                                    final Money amount, final Instant postedAt) {
        requireValid(loanId, type, amount, postedAt);
        return new LedgerEntry(loanId, type, amount, Money.ZERO, postedAt);
    }

    /**
     * Creates an entry whose amount sits in the credit column.
     *
     * @param loanId the account the movement belongs to
     * @param type what kind of movement this is
     * @param amount the amount moved; must be greater than zero
     * @param postedAt when the entry was posted
     * @return the entry
     * @throws InvalidLedgerEntryException if any argument is null or the amount is zero
     */
    public static LedgerEntry credit(final LoanId loanId, final EntryType type,
                                     final Money amount, final Instant postedAt) {
        requireValid(loanId, type, amount, postedAt);
        return new LedgerEntry(loanId, type, Money.ZERO, amount, postedAt);
    }

    private static void requireValid(final LoanId loanId, final EntryType type,
                                     final Money amount, final Instant postedAt) {
        if (loanId == null) {
            throw new InvalidLedgerEntryException("loan id is required");
        }
        if (type == null) {
            throw new InvalidLedgerEntryException("entry type is required");
        }
        if (amount == null) {
            throw new InvalidLedgerEntryException("amount is required");
        }
        if (postedAt == null) {
            throw new InvalidLedgerEntryException("posting time is required");
        }
        if (amount.isZero()) {
            throw new InvalidLedgerEntryException(
                "a ledger entry must move money; a zero " + type + " entry is not allowed");
        }
    }

    /**
     * Returns the account this movement belongs to.
     *
     * @return the loan identifier
     */
    public LoanId loanId() {
        return loanId;
    }

    /**
     * Returns the kind of movement.
     *
     * @return the entry type
     */
    public EntryType type() {
        return type;
    }

    /**
     * Returns the debit column, which is zero for a credit entry.
     *
     * @return the debit amount
     */
    public Money debit() {
        return debit;
    }

    /**
     * Returns the credit column, which is zero for a debit entry.
     *
     * @return the credit amount
     */
    public Money credit() {
        return credit;
    }

    /**
     * Returns when the entry was posted.
     *
     * @return the posting timestamp
     */
    public Instant postedAt() {
        return postedAt;
    }

    /**
     * Returns the amount moved, whichever column it sits in.
     *
     * @return the non-zero column
     */
    public Money amount() {
        return isDebit() ? debit : credit;
    }

    /**
     * Reports whether the amount sits in the debit column.
     *
     * @return true for a debit entry
     */
    public boolean isDebit() {
        return !debit.isZero();
    }

    /**
     * Reports whether the amount sits in the credit column.
     *
     * @return true for a credit entry
     */
    public boolean isCredit() {
        return !credit.isZero();
    }

    /**
     * Reports whether this entry is a compensating reversal of another.
     *
     * @return true for a reversal
     */
    public boolean isReversal() {
        return reverses != null;
    }

    /**
     * Returns the entry this one reverses.
     *
     * @return the original entry, or empty when this is not a reversal
     */
    public Optional<LedgerEntry> reverses() {
        return Optional.ofNullable(reverses);
    }

    /**
     * Returns the reason recorded for a reversal.
     *
     * @return the reason, or empty when this is not a reversal
     */
    public Optional<String> reason() {
        return Optional.ofNullable(reason);
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof LedgerEntry)) {
            return false;
        }
        final LedgerEntry that = (LedgerEntry) other;
        return loanId.equals(that.loanId)
            && type == that.type
            && debit.equals(that.debit)
            && credit.equals(that.credit)
            && postedAt.equals(that.postedAt)
            && Objects.equals(reverses, that.reverses)
            && Objects.equals(reason, that.reason);
    }

    @Override
    public int hashCode() {
        return Objects.hash(loanId, type, debit, credit, postedAt, reverses, reason);
    }

    @Override
    public String toString() {
        return "LedgerEntry[" + loanId + " " + type
            + " debit=" + debit.toPlainString() + " credit=" + credit.toPlainString()
            + " at " + postedAt + "]";
    }
}
