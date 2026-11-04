package pk.edu.giki.microlend.ledger;

/**
 * The kind of money movement a {@link LedgerEntry} records.
 *
 * <p>The repayment types mirror the allocation waterfall of BR-18
 * (penalty, fees, interest, principal); the remaining types are the
 * movements the end-of-day engine and the correction service write.</p>
 */
public enum EntryType {

    /** Principal paid out to the borrower at disbursement. */
    PRINCIPAL_DISBURSEMENT,

    /** Penalty charged by the end-of-day engine (BR-27). */
    PENALTY_ACCRUAL,

    /** Repayment applied to outstanding penalty. */
    PENALTY_REPAYMENT,

    /** Repayment applied to fees; always zero at M1 (BR-18). */
    FEE_REPAYMENT,

    /** Repayment applied to interest. */
    INTEREST_REPAYMENT,

    /** Repayment applied to principal. */
    PRINCIPAL_REPAYMENT,

    /** Overpayment held against future instalments (BR-21). */
    ADVANCE_CREDIT,

    /** Interest rebated on early settlement (BR-22). */
    INTEREST_REBATE,

    /** Outstanding principal moved to the write-off account (BR-32). */
    WRITE_OFF,

    /** Compensating entry that reverses an earlier entry (BR-24). */
    REVERSAL
}
