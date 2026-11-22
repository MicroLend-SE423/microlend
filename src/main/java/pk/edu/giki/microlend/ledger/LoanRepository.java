package pk.edu.giki.microlend.ledger;

import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;

/**
 * Role through which the posting path reads and stores loan accounts
 * (M1 design View 2 and section 4.5, DIP).
 *
 * <p>Declared in the domain module, not in persistence, so business code
 * depends only on this abstraction; {@code SqliteLoanRepository} (queue item
 * L-15) implements it and is injected at the composition root.</p>
 */
public interface LoanRepository {

    /**
     * Finds a loan account.
     *
     * @param loanId non-null loan identifier
     * @return the account, or empty when no such loan exists
     * @throws NullPointerException if loanId is null
     */
    Optional<LoanAccount> findById(LoanId loanId);

    /**
     * Stores the current state of a loan account.
     *
     * @param account non-null account
     * @throws NullPointerException if account is null
     */
    void save(LoanAccount account);
}
