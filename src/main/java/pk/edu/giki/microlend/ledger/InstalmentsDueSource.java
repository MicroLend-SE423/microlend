package pk.edu.giki.microlend.ledger;

import java.time.LocalDate;
import java.util.List;
import pk.edu.giki.microlend.common.LoanId;

/**
 * Role that tells the posting path what is still owed on a loan, instalment
 * by instalment, as {@link InstalmentDue} values.
 *
 * <p>The amounts come from other modules: principal and interest from the
 * schedule ({@code products}, Hassan Khalid) and accrued penalty from the
 * end-of-day engine ({@code delinquency}, Muhammad Ibrahim). The composition
 * root supplies an implementation that maps those modules' types to
 * {@code InstalmentDue}, so the ledger does not depend on either module.
 * Which instalments are included (only those due by the business date, or
 * every remaining one so that a payoff can be detected) is the
 * implementation's decision and an open team question.</p>
 */
@FunctionalInterface
public interface InstalmentsDueSource {

    /**
     * Returns the unpaid instalments of a loan as at a business date.
     *
     * @param loanId the loan
     * @param businessDate the business date of the posting
     * @return the unpaid instalments, in any order; empty when nothing is owed
     */
    List<InstalmentDue> owing(LoanId loanId, LocalDate businessDate);
}
