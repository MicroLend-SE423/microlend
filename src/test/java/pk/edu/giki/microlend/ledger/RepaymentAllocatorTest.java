package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link RepaymentAllocator} (queue item L-05, TC-LED-01, TC-LED-02).
 *
 * <p>BR-18/BR-19 are read tier by tier (owner decision of 2026-11-14, pending
 * team confirmation): all penalties oldest first, then all fees oldest first,
 * then all interest oldest first, then all principal oldest first.</p>
 */
class RepaymentAllocatorTest {

    private static final LocalDate JAN = LocalDate.of(2026, 1, 15);
    private static final LocalDate FEB = LocalDate.of(2026, 2, 15);
    private static final LocalDate MAR = LocalDate.of(2026, 3, 15);

    private final RepaymentAllocator allocator = new RepaymentAllocator();

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static InstalmentDue due(final int sequence, final LocalDate dueDate,
                                     final String penalty, final String fees,
                                     final String interest, final String principal) {
        return new InstalmentDue(sequence, dueDate, money(penalty), money(fees),
            money(interest), money(principal));
    }

    private static Allocation tiers(final String penalty, final String fees,
                                    final String interest, final String principal) {
        return Allocation.of(money(penalty), money(fees), money(interest), money(principal));
    }

    @Nested
    @DisplayName("BR-18: penalty -> fees -> interest -> principal")
    class TierOrder {

        @Test
        @DisplayName("a payment covering part of each tier stops inside interest")
        void partOfEachTier() {
            final AllocationResult result = allocator.allocate(money("60.00"),
                List.of(due(1, JAN, "10.00", "0.00", "100.00", "1000.00")));
            assertEquals(tiers("10.00", "0.00", "50.00", "0.00"), result.total());
            assertEquals(Money.ZERO, result.unapplied());
        }

        @Test
        @DisplayName("fees, when present, are paid after penalty and before interest")
        void feesBeforeInterest() {
            final AllocationResult result = allocator.allocate(money("25.00"),
                List.of(due(1, JAN, "10.00", "20.00", "100.00", "1000.00")));
            assertEquals(tiers("10.00", "15.00", "0.00", "0.00"), result.total());
        }

        @Test
        @DisplayName("principal is reached only after penalty and interest are cleared")
        void principalLast() {
            final AllocationResult result = allocator.allocate(money("110.01"),
                List.of(due(1, JAN, "10.00", "0.00", "100.00", "1000.00")));
            assertEquals(tiers("10.00", "0.00", "100.00", "0.01"), result.total());
        }
    }

    @Nested
    @DisplayName("BR-19: oldest instalment first within each tier")
    class OldestFirst {

        @Test
        @DisplayName("two overdue: both penalties before either instalment's interest")
        void twoOverdueTierByTier() {
            final AllocationResult result = allocator.allocate(money("120.00"), List.of(
                due(1, JAN, "10.00", "0.00", "100.00", "1000.00"),
                due(2, FEB, "5.00", "0.00", "100.00", "1000.00")));
            assertEquals(tiers("15.00", "0.00", "105.00", "0.00"), result.total());
            assertEquals(Map.of(
                1, tiers("10.00", "0.00", "100.00", "0.00"),
                2, tiers("5.00", "0.00", "5.00", "0.00")), result.bySequence());
        }

        @Test
        @DisplayName("three overdue, given out of order, are settled by due date")
        void threeOverdueOutOfOrder() {
            final AllocationResult result = allocator.allocate(money("2350.00"), List.of(
                due(3, MAR, "0.00", "0.00", "100.00", "1000.00"),
                due(1, JAN, "20.00", "0.00", "100.00", "1000.00"),
                due(2, FEB, "10.00", "0.00", "100.00", "1000.00")));
            assertEquals(tiers("30.00", "0.00", "300.00", "2020.00"), result.total());
            assertEquals(tiers("20.00", "0.00", "100.00", "1000.00"), result.bySequence().get(1));
            assertEquals(tiers("10.00", "0.00", "100.00", "1000.00"), result.bySequence().get(2));
            assertEquals(tiers("0.00", "0.00", "100.00", "20.00"), result.bySequence().get(3));
        }

        @Test
        @DisplayName("an instalment that receives nothing is reported with a zero allocation")
        void untouchedInstalmentIsZero() {
            final AllocationResult result = allocator.allocate(money("10.00"), List.of(
                due(1, JAN, "10.00", "0.00", "100.00", "1000.00"),
                due(2, FEB, "0.00", "0.00", "100.00", "1000.00")));
            assertEquals(tiers("0.00", "0.00", "0.00", "0.00"), result.bySequence().get(2));
        }
    }

    @Nested
    @DisplayName("BR-20: partial payment applied as far as it reaches")
    class PartialPayment {

        // Two instalments, total due 2,230.00.
        private final List<InstalmentDue> overdue = List.of(
            due(1, JAN, "10.00", "0.00", "100.00", "1000.00"),
            due(2, FEB, "20.00", "0.00", "100.00", "1000.00"));

        @ParameterizedTest(name = "pay {0}: penalty {1}, interest {2}, principal {3}, unapplied {4}")
        @CsvSource({
            "0.00,    0.00,  0.00,    0.00, 0.00",
            "0.01,    0.01,  0.00,    0.00, 0.00",
            "2229.99, 30.00, 200.00, 1999.99, 0.00",
            "2230.00, 30.00, 200.00, 2000.00, 0.00",
            "2230.01, 30.00, 200.00, 2000.00, 0.01",
        })
        void boundaries(final String pay, final String penalty, final String interest,
                        final String principal, final String unapplied) {
            final AllocationResult result = allocator.allocate(money(pay), overdue);
            assertEquals(tiers(penalty, "0.00", interest, principal), result.total());
            assertEquals(money(unapplied), result.unapplied());
        }

        @Test
        @DisplayName("due - 0.01 leaves exactly 0.01 of the newest principal outstanding")
        void oneRupeeShortOnNewestPrincipal() {
            final AllocationResult result = allocator.allocate(money("2229.99"), overdue);
            assertEquals(tiers("20.00", "0.00", "100.00", "999.99"), result.bySequence().get(2));
        }

        @Test
        @DisplayName("allocated plus unapplied always equals the payment")
        void conservesThePayment() {
            for (final String pay : Arrays.asList("0.00", "0.01", "17.35", "2230.00", "9999.99")) {
                final AllocationResult result = allocator.allocate(money(pay), overdue);
                assertEquals(money(pay), result.total().total().plus(result.unapplied()), pay);
            }
        }

        @Test
        @DisplayName("nothing owed: the whole payment is unapplied")
        void nothingOwed() {
            final AllocationResult result = allocator.allocate(money("50.00"), List.of());
            assertEquals(tiers("0.00", "0.00", "0.00", "0.00"), result.total());
            assertEquals(money("50.00"), result.unapplied());
        }
    }

    @Nested
    @DisplayName("input validation at the module boundary")
    class Validation {

        @Test
        @DisplayName("a missing payment, list or instalment is rejected")
        void rejectsMissingInput() {
            final List<InstalmentDue> withNull = Arrays.asList(
                due(1, JAN, "0.00", "0.00", "1.00", "1.00"), null);
            assertThrows(InvalidAllocationException.class,
                () -> allocator.allocate(null, List.of()));
            assertThrows(InvalidAllocationException.class,
                () -> allocator.allocate(money("1.00"), null));
            assertThrows(InvalidAllocationException.class,
                () -> allocator.allocate(money("1.00"), withNull));
        }

        @Test
        @DisplayName("two instalments with the same sequence are rejected")
        void rejectsDuplicateSequence() {
            assertThrows(InvalidAllocationException.class, () -> allocator.allocate(
                money("1.00"), List.of(due(1, JAN, "0.00", "0.00", "1.00", "1.00"),
                    due(1, FEB, "0.00", "0.00", "1.00", "1.00"))));
        }

        @Test
        @DisplayName("an instalment due needs a positive sequence and every field")
        void instalmentDueInvariants() {
            assertThrows(InvalidAllocationException.class,
                () -> due(0, JAN, "0.00", "0.00", "1.00", "1.00"));
            assertThrows(InvalidAllocationException.class,
                () -> new InstalmentDue(1, null, Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO));
            assertThrows(InvalidAllocationException.class,
                () -> new InstalmentDue(1, JAN, null, Money.ZERO, Money.ZERO, Money.ZERO));
            assertThrows(InvalidAllocationException.class,
                () -> new InstalmentDue(1, JAN, Money.ZERO, Money.ZERO, Money.ZERO, null));
            assertEquals(money("1111.00"),
                due(1, JAN, "1.00", "10.00", "100.00", "1000.00").total());
        }
    }
}
