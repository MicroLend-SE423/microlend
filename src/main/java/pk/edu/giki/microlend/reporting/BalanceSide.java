package pk.edu.giki.microlend.reporting;

/**
 * Which column a statement balance sits in. Money is never negative (BR-43), so a balance is an
 * amount plus the side it falls on.
 */
public enum BalanceSide {

    /** Debits exceed credits. */
    DEBIT,

    /** Credits exceed debits. */
    CREDIT,

    /** Debits and credits are equal; the amount is zero. */
    NIL
}
