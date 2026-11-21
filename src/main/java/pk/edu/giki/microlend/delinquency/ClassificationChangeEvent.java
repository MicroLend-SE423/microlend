package pk.edu.giki.microlend.delinquency;

import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;

/**
 * Raised when a loan's classification changes, whether it worsens or improves
 * (BR-34, FR-27).
 *
 * @param loanId the loan
 * @param from the classification before the business date
 * @param to the classification on the business date
 */
public record ClassificationChangeEvent(LoanId loanId, Classification from, Classification to) {

    /**
     * Validates the event.
     *
     * @throws NullPointerException if any component is null
     */
    public ClassificationChangeEvent {
        Objects.requireNonNull(loanId, "loan id is required");
        Objects.requireNonNull(from, "previous classification is required");
        Objects.requireNonNull(to, "new classification is required");
    }
}
