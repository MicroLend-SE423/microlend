package pk.edu.giki.microlend.ledger;

/** Where teller audit lines are written (FR-31). */
@FunctionalInterface
public interface AuditJournal {

    /**
     * Writes one audit line.
     *
     * @param record the line
     */
    void record(AuditRecord record);
}
