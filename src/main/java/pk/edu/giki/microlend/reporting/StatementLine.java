package pk.edu.giki.microlend.reporting;

import java.util.Objects;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * One line of a statement: a ledger entry and the balance after it.
 *
 * @param entry the ledger entry, which is itself immutable
 * @param balance the running balance after this entry
 */
public record StatementLine(LedgerEntry entry, Balance balance) {

    /**
     * Validates the line.
     *
     * @throws NullPointerException if an argument is null
     */
    public StatementLine {
        Objects.requireNonNull(entry, "entry is required");
        Objects.requireNonNull(balance, "balance is required");
    }
}
