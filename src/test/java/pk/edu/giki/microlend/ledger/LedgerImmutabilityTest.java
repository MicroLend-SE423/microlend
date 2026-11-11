package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Corrections happen only by reversal (BR-24, FR-17, QR-05).
 *
 * <p>{@code LedgerEntryTest} already shows that an entry has no mutator. This
 * test covers the other half of QR-05: the write role offers no way to change
 * or remove a posted entry, and a correction leaves the original in place while
 * netting its effect to zero.</p>
 */
class LedgerImmutabilityTest {

    private static final LoanId LOAN = LoanId.of("LN-0042");
    private static final Instant POSTED = Instant.parse("2026-10-04T09:00:00Z");

    @Test
    @DisplayName("the write role can only append: no update, delete or replace method")
    void writerCanOnlyAppend() {
        final List<String> methods = Arrays.stream(LedgerWriter.class.getDeclaredMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()))
            .map(Method::getName)
            .toList();
        assertEquals(List.of("append"), methods);
    }

    @Test
    @DisplayName("the read role exposes reads only")
    void readerOnlyReads() {
        final List<String> methods = Arrays.stream(LedgerReader.class.getDeclaredMethods())
            .filter(method -> !Modifier.isStatic(method.getModifiers()))
            .map(Method::getName)
            .toList();
        assertEquals(List.of("entriesFor"), methods);
    }

    @Test
    @DisplayName("a correction keeps the original entry and nets each column to zero")
    void correctionKeepsOriginalAndNetsToZero() {
        final InMemoryLedgerRepository ledger = new InMemoryLedgerRepository();
        final CompensatingEntryService service = new CompensatingEntryService(
            ledger, Clock.fixed(Instant.parse("2026-10-06T08:00:00Z"), ZoneOffset.UTC));
        final LedgerEntry original = LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT,
            Money.of(new BigDecimal("1000.00")), POSTED);
        final LedgerEntry snapshot = LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT,
            Money.of(new BigDecimal("1000.00")), POSTED);
        ledger.append(original);

        service.reverse(original, "posted to the wrong loan");

        final List<LedgerEntry> entries = ledger.entriesFor(LOAN);
        assertEquals(2, entries.size());
        assertEquals(snapshot, entries.get(0), "the original is still present and unchanged");
        Money debits = Money.ZERO;
        Money credits = Money.ZERO;
        for (final LedgerEntry entry : entries) {
            debits = debits.plus(entry.debit());
            credits = credits.plus(entry.credit());
        }
        assertEquals(debits, credits);
        assertTrue(entries.get(1).isReversal());
    }
}
