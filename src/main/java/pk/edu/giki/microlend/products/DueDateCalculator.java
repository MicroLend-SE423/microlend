package pk.edu.giki.microlend.products;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates the monthly instalment dates of a loan (BR-12).
 *
 * <p>Every instalment falls on the same day-of-month as the disbursement, clamped to the last
 * day of a shorter month. Each date is computed from the disbursement date, not from the
 * previous instalment, so a 31 January disbursement gives 28 February and then 31 March
 * rather than drifting to the 28th.</p>
 */
public final class DueDateCalculator {

    /**
     * Generates consecutive monthly due dates, the first one month after disbursement.
     *
     * @param disbursement the date the loan was disbursed
     * @param count the number of instalments, at least 1
     * @return the due dates in chronological order; an unmodifiable list
     * @throws InvalidScheduleException if the date is null or the count is below 1
     */
    public List<LocalDate> dueDates(final LocalDate disbursement, final int count) {
        return dueDates(disbursement, 1, count);
    }

    /**
     * Generates consecutive monthly due dates starting a given number of months after
     * disbursement. A grace period (BR-13) uses a first offset above 1.
     *
     * @param disbursement the date the loan was disbursed
     * @param firstOffsetMonths months from disbursement to the first due date, at least 1
     * @param count the number of instalments, at least 1
     * @return the due dates in chronological order; an unmodifiable list
     * @throws InvalidScheduleException if the date is null, or the offset or count is below 1
     */
    public List<LocalDate> dueDates(final LocalDate disbursement, final int firstOffsetMonths,
                                    final int count) {
        if (disbursement == null) {
            throw new InvalidScheduleException("disbursement date is required");
        }
        if (firstOffsetMonths < 1) {
            throw new InvalidScheduleException(
                "the first instalment is at least one month after disbursement, got "
                    + firstOffsetMonths);
        }
        if (count < 1) {
            throw new InvalidScheduleException(
                "a schedule needs at least one instalment, got " + count);
        }
        final List<LocalDate> dates = new ArrayList<>(count);
        final YearMonth start = YearMonth.from(disbursement);
        for (int i = 0; i < count; i++) {
            final YearMonth month = start.plusMonths((long) firstOffsetMonths + i);
            dates.add(month.atDay(Math.min(disbursement.getDayOfMonth(), month.lengthOfMonth())));
        }
        return List.copyOf(dates);
    }
}
