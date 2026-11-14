package pk.edu.giki.microlend.ledger;

import java.time.LocalDate;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Supplies the interest rebated when a loan is paid off (BR-22).
 *
 * <p>The rebate depends on the loan product (refund of unearned interest for
 * reducing-balance loans, none for flat-rate loans, unaccrued monthly interest
 * for bullet loans). That is product knowledge owned by the {@code products}
 * module (Hassan Khalid), so the ledger asks for the amount through this role
 * and contains no per-product arithmetic. Until {@code products} provides an
 * implementation, the composition root must supply one explicitly.</p>
 */
@FunctionalInterface
public interface EarlySettlementRebate {

    /**
     * Returns the interest to rebate on a loan settled on the given date.
     *
     * @param loanId the loan being paid off
     * @param settlementDate the business date of the payoff
     * @return the rebate; zero when the product gives none; never null
     */
    Money rebateFor(LoanId loanId, LocalDate settlementDate);
}
