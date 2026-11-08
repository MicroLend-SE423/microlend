package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link LoanAccount}.
 *
 * <p>Traces to BR-26 and FR-19 (test case TC-LED-07): a payment against a
 * closed or written-off loan is rejected with a typed exception. The three
 * equivalence classes are the three {@link LoanStatus} values.</p>
 */
class LoanAccountTest {

    private static final LoanId LOAN = LoanId.of("LN-0001");
    private static final Money OUTSTANDING = Money.of(new BigDecimal("40000.00"));
    private static final LocalDate CLOSED_ON = LocalDate.of(2026, 10, 31);

    @Nested
    @DisplayName("payment acceptance by status (BR-26, TC-LED-07)")
    class PaymentAcceptance {

        @Test
        @DisplayName("an active loan accepts a payment")
        void activeLoanAcceptsPayment() {
            final LoanAccount account = LoanAccount.active(LOAN, OUTSTANDING);
            assertFalse(account.isClosed());
            assertDoesNotThrow(account::ensureAcceptsPayment);
        }

        @Test
        @DisplayName("a closed loan rejects a payment with LoanClosedException")
        void closedLoanRejectsPayment() {
            final LoanAccount account =
                LoanAccount.restore(LOAN, LoanStatus.CLOSED, Money.ZERO, CLOSED_ON);
            assertTrue(account.isClosed());
            final LoanClosedException thrown =
                assertThrows(LoanClosedException.class, account::ensureAcceptsPayment);
            assertEquals(LOAN, thrown.loanId());
            assertEquals(LoanStatus.CLOSED, thrown.status());
            assertEquals(CLOSED_ON, thrown.closedOn());
        }

        @Test
        @DisplayName("a written-off loan rejects a payment with LoanClosedException")
        void writtenOffLoanRejectsPayment() {
            final LoanAccount account =
                LoanAccount.restore(LOAN, LoanStatus.WRITTEN_OFF, OUTSTANDING, CLOSED_ON);
            assertTrue(account.isClosed());
            final LoanClosedException thrown =
                assertThrows(LoanClosedException.class, account::ensureAcceptsPayment);
            assertEquals(LoanStatus.WRITTEN_OFF, thrown.status());
        }

        @Test
        @DisplayName("the rejection is a domain exception, so it crosses module boundaries typed")
        void rejectionIsADomainException() {
            final LoanAccount account =
                LoanAccount.restore(LOAN, LoanStatus.CLOSED, Money.ZERO, CLOSED_ON);
            final LoanClosedException thrown =
                assertThrows(LoanClosedException.class, account::ensureAcceptsPayment);
            assertTrue(thrown instanceof LoanDomainException);
            assertTrue(thrown.getMessage().contains(LOAN.value()));
        }
    }

    @Nested
    @DisplayName("construction enforces the invariants")
    class Construction {

        @Test
        @DisplayName("an active account exposes its id, status and outstanding principal")
        void activeAccountState() {
            final LoanAccount account = LoanAccount.active(LOAN, OUTSTANDING);
            assertEquals(LOAN, account.id());
            assertEquals(LoanStatus.ACTIVE, account.status());
            assertEquals(OUTSTANDING, account.outstandingPrincipal());
            assertEquals(Optional.empty(), account.closedOn());
        }

        @Test
        @DisplayName("a restored closed account keeps its closure date")
        void restoredClosedAccountState() {
            final LoanAccount account =
                LoanAccount.restore(LOAN, LoanStatus.CLOSED, Money.ZERO, CLOSED_ON);
            assertEquals(Optional.of(CLOSED_ON), account.closedOn());
        }

        @Test
        @DisplayName("id, status and outstanding principal are required")
        void rejectsNullFields() {
            assertThrows(InvalidLoanAccountException.class,
                () -> LoanAccount.active(null, OUTSTANDING));
            assertThrows(InvalidLoanAccountException.class,
                () -> LoanAccount.active(LOAN, null));
            assertThrows(InvalidLoanAccountException.class,
                () -> LoanAccount.restore(LOAN, null, OUTSTANDING, null));
        }

        @Test
        @DisplayName("an active account cannot carry a closure date")
        void activeAccountHasNoClosureDate() {
            assertThrows(InvalidLoanAccountException.class,
                () -> LoanAccount.restore(LOAN, LoanStatus.ACTIVE, OUTSTANDING, CLOSED_ON));
        }

        @Test
        @DisplayName("a closed or written-off account must carry a closure date")
        void closedAccountNeedsClosureDate() {
            assertThrows(InvalidLoanAccountException.class,
                () -> LoanAccount.restore(LOAN, LoanStatus.CLOSED, Money.ZERO, null));
            assertThrows(InvalidLoanAccountException.class,
                () -> LoanAccount.restore(LOAN, LoanStatus.WRITTEN_OFF, OUTSTANDING, null));
        }
    }
}
