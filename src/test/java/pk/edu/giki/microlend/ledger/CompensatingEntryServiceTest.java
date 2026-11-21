package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link CompensatingEntryService}: a posted entry is corrected only
 * by appending a reversal that references it (BR-24, FR-17, TC-LED-05).
 */
class CompensatingEntryServiceTest {

    private static final LoanId LOAN = LoanId.of("LN-0001");
    private static final Money AMOUNT = Money.of(new BigDecimal("2549.00"));
    private static final Instant POSTED = Instant.parse("2026-10-04T09:00:00Z");
    private static final Instant NOW = Instant.parse("2026-10-05T10:30:00Z");

    private final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
    private final CompensatingEntryService service =
        new CompensatingEntryService(ledger, Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    @DisplayName("reversing a credit appends a debit of the same amount on the same loan")
    void reversesACredit() {
        final LedgerEntry original =
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, AMOUNT, POSTED);
        final LedgerEntry reversal = service.reverse(original, "teller keyed wrong loan");
        assertEquals(LOAN, reversal.loanId());
        assertEquals(EntryType.REVERSAL, reversal.type());
        assertEquals(AMOUNT, reversal.debit());
        assertTrue(reversal.credit().isZero());
        assertEquals(NOW, reversal.postedAt());
    }

    @Test
    @DisplayName("reversing a debit appends a credit of the same amount")
    void reversesADebit() {
        final LedgerEntry original =
            LedgerEntry.debit(LOAN, EntryType.PENALTY_ACCRUAL, AMOUNT, POSTED);
        final LedgerEntry reversal = service.reverse(original, "penalty charged in error");
        assertEquals(AMOUNT, reversal.credit());
        assertTrue(reversal.debit().isZero());
    }

    @Test
    @DisplayName("the reversal references the original and records the reason (BR-24)")
    void referencesOriginalAndReason() {
        final LedgerEntry original =
            LedgerEntry.credit(LOAN, EntryType.INTEREST_REPAYMENT, AMOUNT, POSTED);
        final LedgerEntry reversal = service.reverse(original, "  duplicate slip  ");
        assertTrue(reversal.isReversal());
        assertEquals(Optional.of(original), reversal.reverses());
        assertEquals(Optional.of("duplicate slip"), reversal.reason());
        assertFalse(original.isReversal());
        assertEquals(Optional.empty(), original.reverses());
        assertEquals(Optional.empty(), original.reason());
    }

    @Test
    @DisplayName("the reversal is appended to the ledger and returned")
    void appendsTheReversal() {
        final LedgerEntry original =
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, AMOUNT, POSTED);
        ledger.append(original);
        final LedgerEntry reversal = service.reverse(original, "bounced cheque");
        assertEquals(List.of(original, reversal), ledger.entriesFor(LOAN));
    }

    @Test
    @DisplayName("a reason is required and may not be blank")
    void requiresReason() {
        final LedgerEntry original =
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, AMOUNT, POSTED);
        assertThrows(InvalidLedgerEntryException.class, () -> service.reverse(original, null));
        assertThrows(InvalidLedgerEntryException.class, () -> service.reverse(original, "   "));
        assertTrue(ledger.entriesFor(LOAN).isEmpty(), "nothing is appended on rejection");
    }

    @Test
    @DisplayName("the entry to reverse is required")
    void requiresEntry() {
        assertThrows(InvalidLedgerEntryException.class, () -> service.reverse(null, "reason"));
    }

    @Test
    @DisplayName("collaborators are required")
    void requiresCollaborators() {
        final Clock clock = Clock.systemUTC();
        assertThrows(NullPointerException.class, () -> new CompensatingEntryService(null, clock));
        assertThrows(NullPointerException.class, () -> new CompensatingEntryService(ledger, null));
    }
}
