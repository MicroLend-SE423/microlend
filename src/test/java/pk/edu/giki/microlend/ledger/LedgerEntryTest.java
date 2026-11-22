package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link LedgerEntry}.
 *
 * <p>Traces to BR-23 (balanced lines), BR-24 (a posted entry is immutable),
 * BR-43 (direction by column, never by sign), FR-17 and QR-05.</p>
 */
class LedgerEntryTest {

    private static final LoanId LOAN = LoanId.of("LN-0001");
    private static final Instant POSTED = Instant.parse("2026-11-04T09:00:00Z");
    private static final Money AMOUNT = Money.of(new BigDecimal("2500.00"));

    @Nested
    @DisplayName("construction enforces the invariants")
    class Construction {

        @Test
        @DisplayName("a debit entry carries the amount in the debit column only")
        void debitEntry() {
            final LedgerEntry entry = LedgerEntry.debit(LOAN, EntryType.PRINCIPAL_DISBURSEMENT, AMOUNT, POSTED);
            assertEquals(AMOUNT, entry.debit());
            assertTrue(entry.credit().isZero());
            assertTrue(entry.isDebit());
            assertFalse(entry.isCredit());
        }

        @Test
        @DisplayName("a credit entry carries the amount in the credit column only")
        void creditEntry() {
            final LedgerEntry entry = LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, AMOUNT, POSTED);
            assertEquals(AMOUNT, entry.credit());
            assertTrue(entry.debit().isZero());
            assertTrue(entry.isCredit());
            assertFalse(entry.isDebit());
        }

        @Test
        @DisplayName("a zero amount is rejected: an entry must move money (BR-23)")
        void rejectsZeroAmount() {
            assertThrows(InvalidLedgerEntryException.class,
                () -> LedgerEntry.debit(LOAN, EntryType.PENALTY_ACCRUAL, Money.ZERO, POSTED));
            assertThrows(InvalidLedgerEntryException.class,
                () -> LedgerEntry.credit(LOAN, EntryType.PENALTY_REPAYMENT, Money.ZERO, POSTED));
        }

        @Test
        @DisplayName("every field is required")
        void rejectsNullFields() {
            assertThrows(InvalidLedgerEntryException.class,
                () -> LedgerEntry.debit(null, EntryType.PRINCIPAL_DISBURSEMENT, AMOUNT, POSTED));
            assertThrows(InvalidLedgerEntryException.class,
                () -> LedgerEntry.debit(LOAN, null, AMOUNT, POSTED));
            assertThrows(InvalidLedgerEntryException.class,
                () -> LedgerEntry.debit(LOAN, EntryType.PRINCIPAL_DISBURSEMENT, null, POSTED));
            assertThrows(InvalidLedgerEntryException.class,
                () -> LedgerEntry.debit(LOAN, EntryType.PRINCIPAL_DISBURSEMENT, AMOUNT, null));
        }

        @Test
        @DisplayName("the amount moved is the non-zero column, whichever side it is on")
        void amountIsTheNonZeroColumn() {
            assertEquals(AMOUNT, LedgerEntry.debit(LOAN, EntryType.WRITE_OFF, AMOUNT, POSTED).amount());
            assertEquals(AMOUNT, LedgerEntry.credit(LOAN, EntryType.INTEREST_REPAYMENT, AMOUNT, POSTED).amount());
        }
    }

    @Nested
    @DisplayName("a posted entry is immutable (BR-24, QR-05)")
    class Immutability {

        @Test
        @DisplayName("the class is final and every field is private and final")
        void classAndFieldsAreFinal() {
            assertTrue(Modifier.isFinal(LedgerEntry.class.getModifiers()), "class must be final");
            for (final Field field : LedgerEntry.class.getDeclaredFields()) {
                if (field.isSynthetic()) {
                    continue;
                }
                assertTrue(Modifier.isPrivate(field.getModifiers()), field.getName() + " must be private");
                assertTrue(Modifier.isFinal(field.getModifiers()), field.getName() + " must be final");
            }
        }

        @Test
        @DisplayName("no public method can change state")
        void noMutators() {
            for (final Method method : LedgerEntry.class.getDeclaredMethods()) {
                if (!Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())) {
                    continue;
                }
                final String name = method.getName();
                assertFalse(name.startsWith("set") || name.startsWith("update")
                        || name.startsWith("add") || name.startsWith("remove"),
                    name + " looks like a mutator");
            }
        }

        @Test
        @DisplayName("entries with the same content are equal")
        void valueEquality() {
            final LedgerEntry a = LedgerEntry.debit(LOAN, EntryType.PRINCIPAL_DISBURSEMENT, AMOUNT, POSTED);
            final LedgerEntry b = LedgerEntry.debit(LOAN, EntryType.PRINCIPAL_DISBURSEMENT, AMOUNT, POSTED);
            assertEquals(a, b);
            assertEquals(a.hashCode(), b.hashCode());
        }
    }
}
