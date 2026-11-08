package pk.edu.giki.microlend.ledger;

import java.time.LocalDate;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * The ledger's view of one loan: its identity, lifecycle status and outstanding
 * principal.
 *
 * <p>The account defends its own consistency: an active loan has no closure
 * date, and a closed or written-off loan always has one. It also answers the
 * one question every posting path must ask first, whether the loan still
 * accepts money (BR-26). Applying an allocation arrives with queue item L-04;
 * closing and write-off transitions arrive with settlement (L-06) and the
 * end-of-day engine.</p>
 */
public final class LoanAccount {

    private final LoanId id;
    private final LoanStatus status;
    private final Money outstandingPrincipal;
    private final LocalDate closedOn;

    private LoanAccount(final LoanId id, final LoanStatus status,
                        final Money outstandingPrincipal, final LocalDate closedOn) {
        this.id = id;
        this.status = status;
        this.outstandingPrincipal = outstandingPrincipal;
        this.closedOn = closedOn;
    }

    /**
     * Creates an active account, as at disbursement.
     *
     * @param id the loan identifier
     * @param outstandingPrincipal the principal still owed
     * @return the account
     * @throws InvalidLoanAccountException if either argument is null
     */
    public static LoanAccount active(final LoanId id, final Money outstandingPrincipal) {
        return restore(id, LoanStatus.ACTIVE, outstandingPrincipal, null);
    }

    /**
     * Rebuilds an account from stored state, for the repository.
     *
     * @param id the loan identifier
     * @param status the stored status
     * @param outstandingPrincipal the principal still owed
     * @param closedOn the closure or write-off date; null for an active loan
     * @return the account
     * @throws InvalidLoanAccountException if a required value is null, or the
     *     closure date does not match the status
     */
    public static LoanAccount restore(final LoanId id, final LoanStatus status,
                                      final Money outstandingPrincipal, final LocalDate closedOn) {
        if (id == null) {
            throw new InvalidLoanAccountException("loan id is required");
        }
        if (status == null) {
            throw new InvalidLoanAccountException("status is required for loan " + id);
        }
        if (outstandingPrincipal == null) {
            throw new InvalidLoanAccountException("outstanding principal is required for loan " + id);
        }
        if (status == LoanStatus.ACTIVE && closedOn != null) {
            throw new InvalidLoanAccountException("active loan " + id + " cannot have a closure date");
        }
        if (status != LoanStatus.ACTIVE && closedOn == null) {
            throw new InvalidLoanAccountException(status + " loan " + id + " must have a closure date");
        }
        return new LoanAccount(id, status, outstandingPrincipal, closedOn);
    }

    /**
     * Refuses a payment if the loan no longer accepts money (BR-26).
     *
     * @throws LoanClosedException if the loan is closed or written off
     */
    public void ensureAcceptsPayment() {
        if (isClosed()) {
            throw new LoanClosedException(id, status, closedOn);
        }
    }

    /**
     * Reports whether the loan has stopped accepting payments, either because it
     * was repaid or because it was written off.
     *
     * @return true unless the status is {@link LoanStatus#ACTIVE}
     */
    public boolean isClosed() {
        return status != LoanStatus.ACTIVE;
    }

    /**
     * Returns the loan identifier.
     *
     * @return the identifier
     */
    public LoanId id() {
        return id;
    }

    /**
     * Returns the lifecycle status.
     *
     * @return the status
     */
    public LoanStatus status() {
        return status;
    }

    /**
     * Returns the principal still owed.
     *
     * @return the outstanding principal
     */
    public Money outstandingPrincipal() {
        return outstandingPrincipal;
    }

    /**
     * Returns the date the loan stopped accepting payments.
     *
     * @return the closure date, or empty for an active loan
     */
    public Optional<LocalDate> closedOn() {
        return Optional.ofNullable(closedOn);
    }
}
