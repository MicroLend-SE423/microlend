package pk.edu.giki.microlend.ledger;

import java.util.List;

/** Append-only role for posting services; exposes no reporting or mutation API. */
public interface LedgerWriter {
    /**
     * Appends one movement; balanced multi-line transactions use {@link #appendAll}.
     * @param entry non-null immutable movement
     * @throws NullPointerException if entry is null
     */
    void append(LedgerEntry entry);

    /**
     * Appends the lines of one posting all or nothing: if any line cannot be
     * written, none is, so a failure never leaves half a posting (QR-03).
     * @param entries the posting's lines, in order; non-null, no null element
     * @throws NullPointerException if the list or an element is null
     */
    void appendAll(List<LedgerEntry> entries);
}
