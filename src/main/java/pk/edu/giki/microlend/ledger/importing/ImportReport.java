package pk.edu.giki.microlend.ledger.importing;

import java.util.List;

/**
 * What one statement import did (queue item I-04; FR-20).
 *
 * @param posted how many repayments were posted
 * @param duplicateReferences the bank references refused because their
 *     (bank, reference) pair was already posted (BR-25), in file order
 */
public record ImportReport(int posted, List<String> duplicateReferences) {

    /**
     * Copies the references, so the report cannot change after it is made.
     *
     * @throws NullPointerException if the list or any reference is null
     */
    public ImportReport {
        duplicateReferences = List.copyOf(duplicateReferences);
    }
}
