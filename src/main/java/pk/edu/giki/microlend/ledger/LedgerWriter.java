package pk.edu.giki.microlend.ledger;

/** Append-only role for posting services; exposes no reporting or mutation API. */
public interface LedgerWriter {
    /**
     * Appends one movement; balanced multi-line transactions belong to the posting service.
     * @param entry non-null immutable movement
     * @throws NullPointerException if entry is null
     */
    void append(LedgerEntry entry);
}
