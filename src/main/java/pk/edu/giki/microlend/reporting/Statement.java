package pk.edu.giki.microlend.reporting;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;

/**
 * A customer statement for one loan over a date range (BR-35, FR-28).
 *
 * <p>Immutable. {@link #lines()} returns an unmodifiable list, so a statement handed to a
 * printer or a web page cannot be edited on the way.</p>
 */
public final class Statement {

    private final LoanId loanId;
    private final LocalDate from;
    private final LocalDate to;
    private final Balance openingBalance;
    private final List<StatementLine> lines;
    private final Balance closingBalance;

    /**
     * Creates a statement.
     *
     * @param loanId the loan
     * @param from the first day of the range, inclusive
     * @param to the last day of the range, inclusive
     * @param openingBalance the balance before the first day
     * @param lines the entries in the range, in ledger order; copied
     * @param closingBalance the balance after the last line, or the opening balance when empty
     * @throws NullPointerException if an argument, or an element of {@code lines}, is null
     */
    public Statement(final LoanId loanId, final LocalDate from, final LocalDate to,
                     final Balance openingBalance, final List<StatementLine> lines,
                     final Balance closingBalance) {
        this.loanId = Objects.requireNonNull(loanId, "loan id is required");
        this.from = Objects.requireNonNull(from, "start date is required");
        this.to = Objects.requireNonNull(to, "end date is required");
        this.openingBalance = Objects.requireNonNull(openingBalance, "opening balance is required");
        this.lines = List.copyOf(Objects.requireNonNull(lines, "lines are required"));
        this.closingBalance = Objects.requireNonNull(closingBalance, "closing balance is required");
    }

    /**
     * Returns the loan the statement is for.
     *
     * @return the loan identifier
     */
    public LoanId loanId() {
        return loanId;
    }

    /**
     * Returns the first day of the range.
     *
     * @return the start date, inclusive
     */
    public LocalDate from() {
        return from;
    }

    /**
     * Returns the last day of the range.
     *
     * @return the end date, inclusive
     */
    public LocalDate to() {
        return to;
    }

    /**
     * Returns the balance before the first day of the range.
     *
     * @return the opening balance
     */
    public Balance openingBalance() {
        return openingBalance;
    }

    /**
     * Returns the entries in the range with their running balances.
     *
     * @return an unmodifiable list in ledger order; empty when nothing was posted in the range
     */
    public List<StatementLine> lines() {
        return lines;
    }

    /**
     * Returns the balance after the last day of the range.
     *
     * @return the closing balance
     */
    public Balance closingBalance() {
        return closingBalance;
    }
}
