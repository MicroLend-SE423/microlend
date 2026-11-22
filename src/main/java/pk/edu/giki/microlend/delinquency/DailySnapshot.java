package pk.edu.giki.microlend.delinquency;

import java.time.LocalDate;
import java.util.Objects;

/**
 * The latest end-of-day result recorded for a loan: the date it was recorded for and the
 * classification on that date.
 *
 * @param businessDate the business date of the run
 * @param classification the loan's classification on that date
 */
public record DailySnapshot(LocalDate businessDate, Classification classification) {

    /**
     * Validates the snapshot.
     *
     * @throws NullPointerException if a component is null
     */
    public DailySnapshot {
        Objects.requireNonNull(businessDate, "business date is required");
        Objects.requireNonNull(classification, "classification is required");
    }
}
