package pk.edu.giki.microlend.delinquency;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * The outcome of one end-of-day run. Immutable.
 *
 * @param businessDate the date processed
 * @param recorded loans recorded by this run
 * @param alreadyRecorded loans skipped because the date was already recorded (BR-33)
 * @param penaltyAccrued total penalty accrued by this run
 * @param classificationChanges one event per changed classification (BR-34)
 * @param writeOffsDue loans more than 180 days past due (BR-32); listed, not yet posted
 * @param failures loans whose task failed, with the reason; nothing was written for them
 */
public record EodReport(LocalDate businessDate, int recorded, int alreadyRecorded,
                        Money penaltyAccrued, List<ClassificationChangeEvent> classificationChanges,
                        List<LoanId> writeOffsDue, Map<LoanId, String> failures) {

    /**
     * Validates the report and takes immutable copies of its collections.
     *
     * @throws NullPointerException if any component is null
     */
    public EodReport {
        Objects.requireNonNull(businessDate, "business date is required");
        Objects.requireNonNull(penaltyAccrued, "penalty total is required");
        classificationChanges = List.copyOf(classificationChanges);
        writeOffsDue = List.copyOf(writeOffsDue);
        failures = Map.copyOf(failures);
    }
}
