package pk.edu.giki.microlend.ledger;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;

/** Test fake for the persistence boundary; not a transactional production repository. */
final class InMemoryLedgerRepository implements LedgerWriter, LedgerReader {
    private final List<LedgerEntry> entries = new ArrayList<>();

    @Override
    public void append(final LedgerEntry entry) {
        entries.add(Objects.requireNonNull(entry, "entry is required"));
    }

    @Override
    public List<LedgerEntry> entriesFor(final LoanId loanId) {
        Objects.requireNonNull(loanId, "loan id is required");
        return entries.stream().filter(entry -> entry.loanId().equals(loanId)).toList();
    }
}
