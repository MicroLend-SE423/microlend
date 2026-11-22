package pk.edu.giki.microlend.ledger;

import java.util.List;
import pk.edu.giki.microlend.common.LoanId;

/** Read-only role for reports (M1 design section 4.4, ISP). */
public interface LedgerReader {
    /**
     * Returns an immutable snapshot in append order, empty for an unknown loan.
     * @param loanId non-null loan identifier
     * @return entries belonging only to that loan
     * @throws NullPointerException if loanId is null
     */
    List<LedgerEntry> entriesFor(LoanId loanId);
}
