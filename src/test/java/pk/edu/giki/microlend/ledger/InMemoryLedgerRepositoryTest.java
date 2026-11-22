package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/** Tests the append-only boundary (L-02, BR-24, FR-17). */
class InMemoryLedgerRepositoryTest {
    private static final LoanId LOAN = LoanId.of("LN-0001");

    private LedgerEntry entry(final LoanId loan) {
        return LedgerEntry.credit(loan, EntryType.PRINCIPAL_REPAYMENT,
            Money.of(new BigDecimal("100.00")), Instant.parse("2026-11-06T07:00:00Z"));
    }

    @Test
    void readsOnlyTheRequestedLoanInAppendOrder() {
        final InMemoryLedgerRepository repository = new InMemoryLedgerRepository();
        final LedgerWriter writer = repository;
        final LedgerReader reader = repository;
        final LedgerEntry first = entry(LOAN);
        final LedgerEntry second = LedgerEntry.debit(LOAN, EntryType.WRITE_OFF,
            Money.of(new BigDecimal("50.00")), Instant.parse("2026-11-05T07:00:00Z"));
        assertEquals(List.of(), reader.entriesFor(LOAN));
        writer.append(first);
        writer.append(entry(LoanId.of("LN-0002")));
        writer.append(second);
        assertEquals(List.of(first, second), reader.entriesFor(LoanId.of("LN-0001")));
    }

    @Test
    void returnsAnImmutableSnapshotAndPreservesRepeatedMovements() {
        final InMemoryLedgerRepository repository = new InMemoryLedgerRepository();
        final LedgerEntry movement = entry(LOAN);
        repository.append(movement);
        final List<LedgerEntry> snapshot = repository.entriesFor(LOAN);
        assertThrows(UnsupportedOperationException.class, () -> snapshot.clear());
        repository.append(movement);
        assertEquals(List.of(movement), snapshot);
        assertEquals(List.of(movement, movement), repository.entriesFor(LOAN));
    }

    @Test
    void rejectsNullWithoutChangingStoredEntries() {
        final InMemoryLedgerRepository repository = new InMemoryLedgerRepository();
        repository.append(entry(LOAN));
        assertThrows(NullPointerException.class, () -> repository.append(null));
        assertThrows(NullPointerException.class, () -> repository.entriesFor(null));
        assertEquals(List.of(entry(LOAN)), repository.entriesFor(LOAN));
    }
}
