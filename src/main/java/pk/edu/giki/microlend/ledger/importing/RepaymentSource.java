package pk.edu.giki.microlend.ledger.importing;

import java.util.List;
import pk.edu.giki.microlend.ledger.RepaymentCommand;

/**
 * Where imported repayments come from: the Target of the Adapter pattern
 * (queue item I-04; M1 design section 5.2; FR-20).
 *
 * <p>The import workflow knows only this interface. Each supported bank
 * format has its own adapter that turns that bank's statement rows into
 * validated {@link RepaymentCommand}s, so adding a bank is a new adapter class
 * and the workflow does not change.</p>
 */
public interface RepaymentSource {

    /**
     * Returns the next repayments to post.
     *
     * @return the next batch; an empty list means the source is exhausted
     * @throws InvalidStatementException if the statement holds a malformed row
     */
    List<RepaymentCommand> nextBatch();
}
