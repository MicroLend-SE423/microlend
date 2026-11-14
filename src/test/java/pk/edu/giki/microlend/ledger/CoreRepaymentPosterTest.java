package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link CoreRepaymentPoster} (queue item L-08, TC-LED-04; BR-23, QR-03).
 */
class CoreRepaymentPosterTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);
    private static final Instant NOW = Instant.parse("2026-03-20T10:00:00Z");

    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final InMemoryLoanRepository loans = new InMemoryLoanRepository()
        .with(LoanAccount.active(LOAN, money("2000.00")));
    private List<InstalmentDue> owing = List.of(
        new InstalmentDue(1, LocalDate.of(2026, 1, 15), money("10.00"), Money.ZERO,
            money("100.00"), money("1000.00")),
        new InstalmentDue(2, LocalDate.of(2026, 2, 15), Money.ZERO, Money.ZERO,
            money("100.00"), money("1000.00")));

    private final CoreRepaymentPoster poster = new CoreRepaymentPoster(
        new RepaymentAllocator(),
        new SettlementPolicy((loan, date) -> money("15.00")),
        loans,
        (loan, date) -> owing,
        ledger,
        Clock.fixed(NOW, ZoneOffset.UTC));

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static RepaymentCommand cash(final String amount) {
        return RepaymentCommand.cash(LOAN, money(amount), TODAY);
    }

    private static Money sum(final List<LedgerEntry> lines, final boolean debits) {
        Money total = Money.ZERO;
        for (final LedgerEntry line : lines) {
            total = total.plus(debits ? line.debit() : line.credit());
        }
        return total;
    }

    @Test
    @DisplayName("a partial payment writes one receipt debit balanced by the tier credits")
    void partialPaymentLines() {
        final PostingResult result = poster.post(cash("60.00"));
        assertEquals(List.of(
            LedgerEntry.debit(LOAN, EntryType.PAYMENT_RECEIVED, money("60.00"), NOW),
            LedgerEntry.credit(LOAN, EntryType.PENALTY_REPAYMENT, money("10.00"), NOW),
            LedgerEntry.credit(LOAN, EntryType.INTEREST_REPAYMENT, money("50.00"), NOW)),
            ledger.entriesFor(LOAN));
        assertEquals(ledger.entriesFor(LOAN), result.lines());
        assertEquals(money("2000.00"), loans.saves().get(0).outstandingPrincipal());
    }

    @Test
    @DisplayName("principal paid reduces the saved account's outstanding principal")
    void principalReducesAccount() {
        poster.post(cash("1300.00"));
        assertEquals(money("910.00"), loans.saves().get(0).outstandingPrincipal());
        assertTrue(ledger.entriesFor(LOAN).contains(
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, money("1090.00"), NOW)));
    }

    @Test
    @DisplayName("BR-23 invariant: debits equal credits after every posting")
    void ledgerBalancesAfterEveryPosting() {
        for (final String amount : List.of("0.01", "9.99", "60.00", "500.00", "1000.00")) {
            poster.post(cash(amount));
            final List<LedgerEntry> lines = ledger.entriesFor(LOAN);
            assertEquals(sum(lines, true), sum(lines, false), "after paying " + amount);
        }
    }

    @Test
    @DisplayName("BR-21: a surplus with principal still owed is credited as advance credit")
    void surplusBecomesAdvanceCredit() {
        owing = List.of(owing.get(0));
        final PostingResult result = poster.post(cash("1110.01"));
        assertEquals(SettlementOutcome.Kind.ADVANCE_CREDIT, result.settlement().kind());
        assertTrue(ledger.entriesFor(LOAN).contains(
            LedgerEntry.credit(LOAN, EntryType.ADVANCE_CREDIT, money("0.01"), NOW)));
        assertEquals(sum(ledger.entriesFor(LOAN), true), sum(ledger.entriesFor(LOAN), false));
    }

    @Test
    @DisplayName("BR-22: an exact payoff closes and saves the loan and reports the rebate")
    void exactPayoffClosesLoan() {
        final PostingResult result = poster.post(cash("2210.00"));
        assertEquals(LoanStatus.CLOSED, loans.saves().get(0).status());
        assertEquals(money("15.00"), result.settlement().rebate());
    }

    @Test
    @DisplayName("payoff + 0.01 is refused and nothing is written, since no rule says how excess is returned")
    void excessOverPayoffIsRefused() {
        assertThrows(InvalidRepaymentException.class, () -> poster.post(cash("2210.01")));
        assertTrue(ledger.entriesFor(LOAN).isEmpty());
        assertTrue(loans.saves().isEmpty());
    }

    @Test
    @DisplayName("BR-26: a closed loan is refused before anything is written")
    void closedLoanRefused() {
        loans.with(LoanAccount.restore(LOAN, LoanStatus.CLOSED, Money.ZERO, TODAY));
        assertThrows(LoanClosedException.class, () -> poster.post(cash("1.00")));
        assertTrue(ledger.entriesFor(LOAN).isEmpty());
    }

    @Test
    @DisplayName("an unknown loan is reported with a typed exception")
    void unknownLoan() {
        final RepaymentCommand stranger =
            RepaymentCommand.cash(LoanId.of("LN-404"), money("1.00"), TODAY);
        final LoanNotFoundException thrown =
            assertThrows(LoanNotFoundException.class, () -> poster.post(stranger));
        assertEquals(LoanId.of("LN-404"), thrown.loanId());
    }

    @Test
    @DisplayName("if the ledger write fails, the loan is not saved")
    void failedWriteDoesNotSaveLoan() {
        final LedgerWriter failing = new LedgerWriter() {
            @Override
            public void append(final LedgerEntry entry) {
                throw new IllegalStateException("disk full");
            }

            @Override
            public void appendAll(final List<LedgerEntry> batch) {
                throw new IllegalStateException("disk full");
            }
        };
        final CoreRepaymentPoster broken = new CoreRepaymentPoster(new RepaymentAllocator(),
            new SettlementPolicy((loan, date) -> Money.ZERO), loans, (loan, date) -> owing,
            failing, Clock.fixed(NOW, ZoneOffset.UTC));
        assertThrows(IllegalStateException.class, () -> broken.post(cash("60.00")));
        assertTrue(loans.saves().isEmpty());
    }

    @Test
    @DisplayName("an unbalanced set of lines is refused with LedgerImbalanceException")
    void imbalanceDetected() {
        final List<LedgerEntry> lines = List.of(
            LedgerEntry.debit(LOAN, EntryType.PAYMENT_RECEIVED, money("10.00"), NOW),
            LedgerEntry.credit(LOAN, EntryType.INTEREST_REPAYMENT, money("9.99"), NOW));
        assertThrows(LedgerImbalanceException.class,
            () -> CoreRepaymentPoster.requireBalanced(LOAN, lines));
    }

    @Test
    @DisplayName("missing collaborators or command are rejected")
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> poster.post(null));
        assertThrows(NullPointerException.class, () -> new CoreRepaymentPoster(null,
            new SettlementPolicy((loan, date) -> Money.ZERO), loans, (loan, date) -> owing,
            ledger, Clock.systemUTC()));
    }
}
