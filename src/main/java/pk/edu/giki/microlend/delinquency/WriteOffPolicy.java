package pk.edu.giki.microlend.delinquency;

/**
 * Automatic write-off threshold (BR-32, FR-25): a loan more than 180 days past due is
 * written off. Stateless.
 */
public final class WriteOffPolicy {

    private static final int WRITE_OFF_AFTER_DAYS = 180;

    /**
     * Whether a loan must be written off.
     *
     * @param daysPastDue days past due, zero or more
     * @return true when the loan is more than 180 days past due
     * @throws InvalidDaysPastDueException if {@code daysPastDue} is negative
     */
    public boolean shouldWriteOff(final int daysPastDue) {
        if (daysPastDue < 0) {
            throw new InvalidDaysPastDueException(daysPastDue);
        }
        return daysPastDue > WRITE_OFF_AFTER_DAYS;
    }
}
