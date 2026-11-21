package pk.edu.giki.microlend.delinquency;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * The storage the end-of-day engine needs, declared here and implemented in
 * {@code persistence} (dependency inversion, M1 section 4.5).
 */
public interface DailyResultStore {

    /**
     * The loans the end-of-day run must process.
     *
     * @return the ids of every active loan
     */
    List<LoanId> activeLoans();

    /**
     * The loan's latest recorded result. Its date tells the engine from which day penalty
     * is still owed (BR-27) and whether a date has already been processed; its
     * classification is compared with today's to detect a change (BR-34).
     *
     * @param loanId the loan
     * @return the latest snapshot, or empty for a loan never processed
     */
    Optional<DailySnapshot> lastSnapshot(LoanId loanId);

    /**
     * Records one loan's result for one business date: the snapshot and the day's
     * penalty lines, all or nothing. A date already recorded for the loan is left
     * untouched, which makes the end-of-day run idempotent (BR-33).
     *
     * @param loanId the loan
     * @param businessDate the date processed
     * @param daysPastDue days past due on that date
     * @param classification the classification on that date
     * @param accruals the balanced penalty lines, empty when no penalty accrued
     * @return true if recorded now, false if the date was already recorded
     */
    boolean record(LoanId loanId, LocalDate businessDate, int daysPastDue,
                   Classification classification, List<LedgerEntry> accruals);
}
