package pk.edu.giki.microlend.products;

/**
 * Raised when a grace period is outside the 0 to 3 months that BR-13 allows.
 */
public class InvalidGracePeriodException extends InvalidScheduleException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates the exception.
     *
     * @param graceMonths the grace period that was requested
     */
    public InvalidGracePeriodException(final int graceMonths) {
        super("a grace period is between 0 and " + GracePeriodPolicy.MAX_GRACE_MONTHS
            + " months, got " + graceMonths);
    }
}
