package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link SettlementPolicy} (queue item L-06, TC-LED-03; BR-21, BR-22).
 *
 * <p>The per-product rebate (BR-22) is product knowledge owned by
 * {@code products}; here it is supplied by the test through
 * {@link EarlySettlementRebate}, so no product arithmetic is tested.</p>
 */
class SettlementPolicyTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 14);

    private final List<String> rebateCalls = new ArrayList<>();
    private final SettlementPolicy policy = new SettlementPolicy((loan, date) -> {
        rebateCalls.add(loan.value() + "@" + date);
        return money("42.50");
    });

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static AllocationResult allocated(final String principal, final String unapplied) {
        return new AllocationResult(Allocation.of(Money.ZERO, Money.ZERO, Money.ZERO,
            money(principal)), Map.of(), money(unapplied));
    }

    @Test
    @DisplayName("a payment with no surplus that leaves principal owing changes nothing")
    void noSurplus() {
        final LoanAccount after = LoanAccount.active(LOAN, money("500.00"));
        final SettlementOutcome outcome = policy.settle(after, allocated("100.00", "0.00"), TODAY);
        assertEquals(SettlementOutcome.Kind.NO_SURPLUS, outcome.kind());
        assertEquals(after, outcome.account());
        assertEquals(Money.ZERO, outcome.advanceCredit());
        assertEquals(Money.ZERO, outcome.rebate());
        assertTrue(rebateCalls.isEmpty());
    }

    @Test
    @DisplayName("BR-21: due + 0.01 with principal still owing becomes 0.01 advance credit")
    void overpaymentBecomesAdvanceCredit() {
        final LoanAccount after = LoanAccount.active(LOAN, money("500.00"));
        final SettlementOutcome outcome = policy.settle(after, allocated("100.00", "0.01"), TODAY);
        assertEquals(SettlementOutcome.Kind.ADVANCE_CREDIT, outcome.kind());
        assertEquals(money("0.01"), outcome.advanceCredit());
        assertFalse(outcome.account().isClosed());
        assertTrue(rebateCalls.isEmpty());
    }

    @Test
    @DisplayName("BR-22: an exact payoff closes the loan and asks the product for its rebate")
    void exactPayoffClosesTheLoan() {
        final LoanAccount after = LoanAccount.active(LOAN, Money.ZERO);
        final SettlementOutcome outcome = policy.settle(after, allocated("500.00", "0.00"), TODAY);
        assertEquals(SettlementOutcome.Kind.PAID_OFF, outcome.kind());
        assertEquals(LoanStatus.CLOSED, outcome.account().status());
        assertEquals(Optional.of(TODAY), outcome.account().closedOn());
        assertEquals(money("42.50"), outcome.rebate());
        assertEquals(Money.ZERO, outcome.excess());
        assertEquals(List.of("LN-1@2026-11-14"), rebateCalls);
    }

    @Test
    @DisplayName("BR-22: payoff + 0.01 closes the loan and reports 0.01 excess, not advance credit")
    void payoffPlusOneIsExcess() {
        final LoanAccount after = LoanAccount.active(LOAN, Money.ZERO);
        final SettlementOutcome outcome = policy.settle(after, allocated("500.00", "0.01"), TODAY);
        assertEquals(SettlementOutcome.Kind.PAID_OFF, outcome.kind());
        assertEquals(money("0.01"), outcome.excess());
        assertEquals(Money.ZERO, outcome.advanceCredit());
        assertTrue(outcome.account().isClosed());
    }

    @Test
    @DisplayName("a product with no rebate supplies zero")
    void zeroRebate() {
        final SettlementPolicy noRebate = new SettlementPolicy((loan, date) -> Money.ZERO);
        final SettlementOutcome outcome = noRebate.settle(
            LoanAccount.active(LOAN, Money.ZERO), allocated("1.00", "0.00"), TODAY);
        assertEquals(Money.ZERO, outcome.rebate());
    }

    @Test
    @DisplayName("a rebate source that returns nothing is a programming error, reported as such")
    void missingRebateIsRejected() {
        final SettlementPolicy broken = new SettlementPolicy((loan, date) -> null);
        assertThrows(InvalidAllocationException.class, () -> broken.settle(
            LoanAccount.active(LOAN, Money.ZERO), allocated("1.00", "0.00"), TODAY));
    }

    @Test
    @DisplayName("BR-26: a closed loan cannot be settled again")
    void closedLoanRefused() {
        final LoanAccount closed =
            LoanAccount.restore(LOAN, LoanStatus.CLOSED, Money.ZERO, TODAY.minusDays(1));
        assertThrows(LoanClosedException.class,
            () -> policy.settle(closed, allocated("0.00", "1.00"), TODAY));
    }

    @Test
    @DisplayName("missing arguments are rejected")
    void rejectsNulls() {
        final LoanAccount after = LoanAccount.active(LOAN, Money.ZERO);
        assertThrows(NullPointerException.class, () -> new SettlementPolicy(null));
        assertThrows(InvalidAllocationException.class,
            () -> policy.settle(null, allocated("0.00", "0.00"), TODAY));
        assertThrows(InvalidAllocationException.class, () -> policy.settle(after, null, TODAY));
        assertThrows(InvalidAllocationException.class,
            () -> policy.settle(after, allocated("0.00", "0.00"), null));
    }

    @Test
    @DisplayName("closeAsRepaid needs an active loan with no principal left")
    void closeAsRepaidGuards() {
        assertThrows(InvalidLoanAccountException.class,
            () -> LoanAccount.active(LOAN, money("0.01")).closeAsRepaid(TODAY));
        assertThrows(InvalidLoanAccountException.class,
            () -> LoanAccount.active(LOAN, Money.ZERO).closeAsRepaid(null));
        final LoanAccount closed = LoanAccount.active(LOAN, Money.ZERO).closeAsRepaid(TODAY);
        assertThrows(LoanClosedException.class, () -> closed.closeAsRepaid(TODAY));
    }
}
