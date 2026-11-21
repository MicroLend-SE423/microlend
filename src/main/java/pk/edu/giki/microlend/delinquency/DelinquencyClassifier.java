package pk.edu.giki.microlend.delinquency;

/**
 * Maps days past due to a {@link Classification} (BR-30, FR-23). Stateless and thread-safe.
 */
public final class DelinquencyClassifier {

    private static final int WATCH_MAX = 30;
    private static final int SUBSTANDARD_MAX = 60;
    private static final int DOUBTFUL_MAX = 90;

    /**
     * Classifies a loan: 0 Standard, 1-30 Watch, 31-60 Substandard, 61-90 Doubtful,
     * 91 or more Loss.
     *
     * @param daysPastDue days past due, zero or more
     * @return the classification for that count
     * @throws InvalidDaysPastDueException if {@code daysPastDue} is negative
     */
    public Classification classify(final int daysPastDue) {
        if (daysPastDue < 0) {
            throw new InvalidDaysPastDueException(daysPastDue);
        }
        if (daysPastDue == 0) {
            return Classification.STANDARD;
        }
        if (daysPastDue <= WATCH_MAX) {
            return Classification.WATCH;
        }
        if (daysPastDue <= SUBSTANDARD_MAX) {
            return Classification.SUBSTANDARD;
        }
        if (daysPastDue <= DOUBTFUL_MAX) {
            return Classification.DOUBTFUL;
        }
        return Classification.LOSS;
    }
}
