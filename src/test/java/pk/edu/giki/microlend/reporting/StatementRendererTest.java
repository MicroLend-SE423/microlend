package pk.edu.giki.microlend.reporting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.LedgerEntry;
import pk.edu.giki.microlend.ledger.LedgerReader;

/**
 * Tests for {@link StatementRenderer} (BR-35, FR-28, TC-REP-01): every ledger entry in the date
 * range, inclusive at both ends, with a running balance of debits less credits.
 *
 * <p>The ledger fake is a lambda over {@link LedgerReader}: the renderer is given only the
 * read role, so a test cannot write through it either.</p>
 */
class StatementRendererTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate FROM = LocalDate.of(2027, 3, 10);
    private static final LocalDate TO = LocalDate.of(2027, 3, 15);

    private final List<LedgerEntry> ledger = new ArrayList<>();
    private final LedgerReader reader = loanId -> List.copyOf(ledger);
    private final StatementRenderer renderer = new StatementRenderer(reader, ZoneOffset.UTC);

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    private static Instant noonOn(final int month, final int day) {
        return Instant.parse(String.format("2027-%02d-%02dT12:00:00Z", month, day));
    }

    private void debit(final String amount, final int month, final int day) {
        ledger.add(LedgerEntry.debit(LOAN, EntryType.PENALTY_ACCRUAL, money(amount),
            noonOn(month, day)));
    }

    private void credit(final String amount, final int month, final int day) {
        ledger.add(LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, money(amount),
            noonOn(month, day)));
    }

    /** Before, on the start, inside, on the end and after the range (TC-REP-01.1). */
    private void fiveEntriesAroundTheRange() {
        debit("100.00", 3, 9);
        debit("50.00", 3, 10);
        credit("200.00", 3, 12);
        debit("80.00", 3, 15);
        credit("10.00", 3, 16);
    }

    private static List<LedgerEntry> entriesOf(final Statement statement) {
        return statement.lines().stream().map(StatementLine::entry).toList();
    }

    @Test
    @DisplayName("TC-REP-01.1: the start, inside and end entries are listed; before and after are not")
    void rangeIsInclusiveAtBothEnds() {
        fiveEntriesAroundTheRange();
        final Statement statement = renderer.render(LOAN, FROM, TO);
        assertEquals(List.of(ledger.get(1), ledger.get(2), ledger.get(3)), entriesOf(statement));
    }

    @Test
    @DisplayName("TC-REP-01.1: each line carries the running balance, starting from the opening balance")
    void runningBalanceLineByLine() {
        fiveEntriesAroundTheRange();
        final Statement statement = renderer.render(LOAN, FROM, TO);
        assertEquals(new Balance(money("100.00"), BalanceSide.DEBIT), statement.openingBalance());
        assertEquals(List.of(
                new Balance(money("150.00"), BalanceSide.DEBIT),
                new Balance(money("50.00"), BalanceSide.CREDIT),
                new Balance(money("30.00"), BalanceSide.DEBIT)),
            statement.lines().stream().map(StatementLine::balance).toList());
        assertEquals(new Balance(money("30.00"), BalanceSide.DEBIT), statement.closingBalance());
    }

    @Test
    @DisplayName("TC-REP-01.2: a range with no entries is empty and shows the opening balance")
    void emptyRangeShowsOpeningBalance() {
        fiveEntriesAroundTheRange();
        final Statement statement =
            renderer.render(LOAN, LocalDate.of(2027, 3, 20), LocalDate.of(2027, 3, 25));
        assertTrue(statement.lines().isEmpty());
        final Balance everythingBefore = new Balance(money("20.00"), BalanceSide.DEBIT);
        assertEquals(everythingBefore, statement.openingBalance());
        assertEquals(everythingBefore, statement.closingBalance());
    }

    @Test
    @DisplayName("a loan with no entries gives an empty statement with a nil balance")
    void unknownLoanIsEmpty() {
        final Statement statement = renderer.render(LOAN, FROM, TO);
        assertTrue(statement.lines().isEmpty());
        assertEquals(Balance.NIL, statement.openingBalance());
        assertEquals(Balance.NIL, statement.closingBalance());
    }

    @Test
    @DisplayName("debits and credits that cancel give a nil balance, not a side with 0.00")
    void cancellingEntriesAreNil() {
        debit("75.00", 3, 11);
        credit("75.00", 3, 11);
        final Statement statement = renderer.render(LOAN, FROM, TO);
        assertEquals(BalanceSide.NIL, statement.closingBalance().side());
        assertEquals(Money.ZERO, statement.closingBalance().amount());
    }

    @Test
    @DisplayName("TC-REP-01.3: a start date after the end date is refused")
    void startAfterEndRefused() {
        final InvalidStatementRangeException thrown = assertThrows(
            InvalidStatementRangeException.class, () -> renderer.render(LOAN, TO, FROM));
        assertTrue(thrown.getMessage().contains("2027-03-15"), thrown.getMessage());
    }

    @Test
    @DisplayName("a one-day range (start = end) is allowed")
    void oneDayRange() {
        fiveEntriesAroundTheRange();
        final Statement statement = renderer.render(LOAN, TO, TO);
        assertEquals(List.of(ledger.get(3)), entriesOf(statement));
    }

    @Test
    @DisplayName("the day an entry belongs to is its posting instant in the renderer's time zone")
    void dayIsDecidedByTheTimeZone() {
        ledger.add(LedgerEntry.debit(LOAN, EntryType.PENALTY_ACCRUAL, money("5.00"),
            Instant.parse("2027-03-10T20:00:00Z")));
        final LocalDate eleventh = LocalDate.of(2027, 3, 11);
        final StatementRenderer karachi =
            new StatementRenderer(reader, ZoneId.of("Asia/Karachi"));
        assertEquals(1, karachi.render(LOAN, eleventh, eleventh).lines().size());
        assertEquals(0, renderer.render(LOAN, eleventh, eleventh).lines().size());
    }

    @Test
    @DisplayName("a reversal is listed like any other entry and cancels the original in the balance")
    void reversalIsListed() {
        final LedgerEntry original = LedgerEntry.debit(LOAN, EntryType.PENALTY_ACCRUAL,
            money("40.00"), noonOn(3, 11));
        ledger.add(original);
        ledger.add(LedgerEntry.reversalOf(original, "posted twice", noonOn(3, 12)));
        final Statement statement = renderer.render(LOAN, FROM, TO);
        assertEquals(2, statement.lines().size());
        assertEquals(BalanceSide.NIL, statement.closingBalance().side());
    }

    @Test
    @DisplayName("the statement cannot be edited, and later ledger changes do not alter it")
    void statementIsImmutable() {
        fiveEntriesAroundTheRange();
        final Statement statement = renderer.render(LOAN, FROM, TO);
        assertThrows(UnsupportedOperationException.class,
            () -> statement.lines().add(statement.lines().get(0)));
        debit("1.00", 3, 11);
        assertEquals(3, statement.lines().size());
    }

    @Test
    @DisplayName("the statement records the loan and the range it was rendered for")
    void statementNamesItsLoanAndRange() {
        final Statement statement = renderer.render(LOAN, FROM, TO);
        assertEquals(LOAN, statement.loanId());
        assertEquals(FROM, statement.from());
        assertEquals(TO, statement.to());
    }

    @Test
    @DisplayName("missing arguments are refused")
    void nullsRefused() {
        assertThrows(NullPointerException.class, () -> new StatementRenderer(null, ZoneOffset.UTC));
        assertThrows(NullPointerException.class, () -> new StatementRenderer(reader, null));
        assertThrows(NullPointerException.class, () -> renderer.render(null, FROM, TO));
        assertThrows(NullPointerException.class, () -> renderer.render(LOAN, null, TO));
        assertThrows(NullPointerException.class, () -> renderer.render(LOAN, FROM, null));
    }
}
