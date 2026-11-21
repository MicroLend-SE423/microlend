package pk.edu.giki.microlend.origination;

/**
 * Why an application was rejected (BR-08). Each value names the rule, or pair of
 * rules, that failed; the {@link RuleOutcome} detail says which one.
 */
public enum RejectionReason {

    /** BR-01 or BR-02: under 18 at disbursement, or over 65 at maturity. */
    AGE,

    /** BR-03: already holds 2 or more active loans. */
    ACTIVE_LOAN_LIMIT,

    /** BR-04: instalments, with the new loan, exceed 40% of declared income. */
    BURDEN_RATIO,

    /** BR-05: an earlier loan was written off. */
    PRIOR_WRITE_OFF,

    /** BR-06 or BR-07: the amount or the tenure is outside the product's band. */
    OUTSIDE_PRODUCT_BAND,

    /** BR-10: the amount capped by the risk grade is below the product minimum. */
    BELOW_PRODUCT_MINIMUM
}
