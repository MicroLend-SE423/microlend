package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link RepaymentCommand}, the validated request to post one
 * repayment, and {@link InvalidRepaymentException}.
 *
 * <p>Equivalence classes: a cash payment (no reference, BR-25) and a bank
 * payment carrying its (bank, reference) pair for the idempotency guard
 * (queue item L-11). Invalid classes: each missing field, a zero amount
 * (a posting must move money, BR-23), and a blank bank or reference.</p>
 */
class RepaymentCommandTest {

    private static final LoanId LOAN = LoanId.of("LN-0001");
    private static final Money AMOUNT = Money.of(new BigDecimal("2549.00"));
    private static final LocalDate DATE = LocalDate.of(2026, 10, 4);

    @Test
    @DisplayName("a cash payment carries no bank and no reference (BR-25)")
    void cashPaymentHasNoReference() {
        final RepaymentCommand command = RepaymentCommand.cash(LOAN, AMOUNT, DATE);
        assertEquals(LOAN, command.loanId());
        assertEquals(AMOUNT, command.amount());
        assertEquals(DATE, command.businessDate());
        assertTrue(command.isCash());
        assertEquals(Optional.empty(), command.bank());
        assertEquals(Optional.empty(), command.reference());
    }

    @Test
    @DisplayName("a bank payment carries its (bank, reference) pair, trimmed")
    void bankPaymentCarriesPair() {
        final RepaymentCommand command =
            RepaymentCommand.bank(LOAN, AMOUNT, DATE, " HBL ", " TXN-778812 ");
        assertFalse(command.isCash());
        assertEquals(Optional.of("HBL"), command.bank());
        assertEquals(Optional.of("TXN-778812"), command.reference());
    }

    @Test
    @DisplayName("the smallest representable amount, 0.01, is accepted")
    void acceptsOnePaisa() {
        final Money onePaisa = Money.of(new BigDecimal("0.01"));
        assertEquals(onePaisa, RepaymentCommand.cash(LOAN, onePaisa, DATE).amount());
    }

    @Test
    @DisplayName("a zero amount is rejected because a posting must move money (BR-23)")
    void rejectsZeroAmount() {
        final InvalidRepaymentException thrown = assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.bank(LOAN, Money.ZERO, DATE, "HBL", "TXN-1"));
        assertEquals("0.00", thrown.offendingValue());
        assertEquals("HBL", thrown.source());
    }

    @Test
    @DisplayName("loan, amount and business date are required")
    void rejectsMissingFields() {
        assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.cash(null, AMOUNT, DATE));
        assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.cash(LOAN, null, DATE));
        assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.cash(LOAN, AMOUNT, null));
    }

    @Test
    @DisplayName("a bank payment needs a non-blank bank and reference (BR-25)")
    void rejectsBlankBankOrReference() {
        assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.bank(LOAN, AMOUNT, DATE, null, "TXN-1"));
        assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.bank(LOAN, AMOUNT, DATE, "  ", "TXN-1"));
        assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.bank(LOAN, AMOUNT, DATE, "HBL", null));
        assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.bank(LOAN, AMOUNT, DATE, "HBL", ""));
    }

    @Test
    @DisplayName("the exception is a domain exception carrying value and source")
    void exceptionCarriesValueAndSource() {
        final InvalidRepaymentException thrown = assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.bank(LOAN, AMOUNT, DATE, "MCB", " "));
        assertTrue(thrown instanceof LoanDomainException);
        assertEquals("MCB", thrown.source());
        assertEquals(" ", thrown.offendingValue());
        assertTrue(thrown.getMessage().contains("reference"));
    }

    @Test
    @DisplayName("a cash rejection names cash as its source")
    void cashRejectionNamesCash() {
        final InvalidRepaymentException thrown = assertThrows(InvalidRepaymentException.class,
            () -> RepaymentCommand.cash(LOAN, Money.ZERO, DATE));
        assertEquals("cash", thrown.source());
    }

    @Test
    @DisplayName("the same reference from two banks is two different pairs (BR-25)")
    void sameReferenceFromTwoBanksDiffers() {
        final RepaymentCommand hbl = RepaymentCommand.bank(LOAN, AMOUNT, DATE, "HBL", "TXN-1");
        final RepaymentCommand mcb = RepaymentCommand.bank(LOAN, AMOUNT, DATE, "MCB", "TXN-1");
        final RepaymentCommand hblAgain =
            RepaymentCommand.bank(LOAN, AMOUNT, DATE, "HBL", "TXN-1");
        assertNotEquals(hbl, mcb);
        assertEquals(hbl, hblAgain);
        assertEquals(hbl.hashCode(), hblAgain.hashCode());
    }
}
