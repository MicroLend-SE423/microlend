package pk.edu.giki.microlend.reporting;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.LedgerEntry;
import pk.edu.giki.microlend.ledger.LedgerReader;

/**
 * Renders the customer statement of a loan for a date range (BR-35, FR-28, TC-REP-01).
 *
 * <p>It reads only through {@link LedgerReader}, the read-only role (M1 section 4.4, interface
 * segregation), so a report can never write to the ledger.</p>
 *
 * <p><b>Reading of "running balance".</b> BR-35 does not define it. The balance here is the
 * total of the debit column less the total of the credit column over every entry so far, shown
 * as an amount on the larger side ({@link Balance}). Because each posting is balanced (BR-23),
 * the balance returns to nil after a whole posting. Entries before the range make up the opening
 * balance. The range is inclusive at both ends. An entry's day is its posting instant in the
 * renderer's time zone. These readings are proposals for the team to confirm.</p>
 */
public final class StatementRenderer {

    private final LedgerReader ledger;
    private final ZoneId zone;

    /**
     * Creates a renderer.
     *
     * @param ledger the read-only view of the ledger
     * @param zone the time zone that turns a posting instant into a business day
     * @throws NullPointerException if an argument is null
     */
    public StatementRenderer(final LedgerReader ledger, final ZoneId zone) {
        this.ledger = Objects.requireNonNull(ledger, "ledger reader is required");
        this.zone = Objects.requireNonNull(zone, "time zone is required");
    }

    /**
     * Renders the statement of one loan.
     *
     * @param loanId the loan
     * @param from the first day, inclusive
     * @param to the last day, inclusive
     * @return the statement; empty of lines, with the opening balance, when nothing was posted in
     *     the range
     * @throws NullPointerException if an argument is null
     * @throws InvalidStatementRangeException if {@code from} is after {@code to}
     */
    public Statement render(final LoanId loanId, final LocalDate from, final LocalDate to) {
        Objects.requireNonNull(loanId, "loan id is required");
        Objects.requireNonNull(from, "start date is required");
        Objects.requireNonNull(to, "end date is required");
        if (from.isAfter(to)) {
            throw new InvalidStatementRangeException(from, to);
        }
        final List<LedgerEntry> entries = ledger.entriesFor(loanId);
        Money debits = Money.ZERO;
        Money credits = Money.ZERO;
        for (LedgerEntry entry : entries) {
            if (dayOf(entry).isBefore(from)) {
                debits = debits.plus(entry.debit());
                credits = credits.plus(entry.credit());
            }
        }
        final Balance opening = Balance.of(debits, credits);
        final List<StatementLine> lines = new ArrayList<>();
        for (LedgerEntry entry : entries) {
            final LocalDate day = dayOf(entry);
            if (!day.isBefore(from) && !day.isAfter(to)) {
                debits = debits.plus(entry.debit());
                credits = credits.plus(entry.credit());
                lines.add(new StatementLine(entry, Balance.of(debits, credits)));
            }
        }
        final Balance closing = Balance.of(debits, credits);
        return new Statement(loanId, from, to, opening, lines, closing);
    }

    private LocalDate dayOf(final LedgerEntry entry) {
        return entry.postedAt().atZone(zone).toLocalDate();
    }
}
