package pk.edu.giki.microlend.ledger;

/**
 * Lifecycle state of a loan account, as far as the ledger needs to know it.
 *
 * <p>Only an {@link #ACTIVE} loan accepts payments (BR-26). A loan in arrears
 * is still active; delinquency is a classification, not a status.</p>
 */
public enum LoanStatus {

    /** Open and accepting payments, whether current or in arrears. */
    ACTIVE,

    /** Fully repaid or settled early (BR-22); accepts no further payments. */
    CLOSED,

    /** Written off after 180 days past due (BR-32); accepts no further payments. */
    WRITTEN_OFF
}
