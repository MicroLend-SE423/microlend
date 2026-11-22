package pk.edu.giki.microlend.delinquency;

/**
 * Delinquency classification of a loan by days past due (BR-30). The names match the
 * {@code delinquency_snapshot.classification} CHECK constraint.
 */
public enum Classification {
    /** 0 days past due. */
    STANDARD,
    /** 1 to 30 days past due. */
    WATCH,
    /** 31 to 60 days past due. */
    SUBSTANDARD,
    /** 61 to 90 days past due. */
    DOUBTFUL,
    /** 91 or more days past due. */
    LOSS
}
