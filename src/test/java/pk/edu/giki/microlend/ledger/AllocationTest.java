package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link Allocation}, the result of splitting one payment across the
 * four BR-18 tiers (penalty, fees, interest, principal).
 *
 * <p>The allocator that computes the split is queue item L-05; this class only
 * carries the split, so the tests pin its value-object behaviour.</p>
 */
class AllocationTest {

    private static final Money PENALTY = money("12.50");
    private static final Money INTEREST = money("300.00");
    private static final Money PRINCIPAL = money("1687.50");

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    @Test
    @DisplayName("exposes each tier in BR-18 order")
    void exposesEachTier() {
        final Allocation allocation = Allocation.of(PENALTY, Money.ZERO, INTEREST, PRINCIPAL);
        assertEquals(PENALTY, allocation.toPenalty());
        assertEquals(Money.ZERO, allocation.toFees());
        assertEquals(INTEREST, allocation.toInterest());
        assertEquals(PRINCIPAL, allocation.toPrincipal());
    }

    @Test
    @DisplayName("total is the sum of the four tiers")
    void totalIsSumOfTiers() {
        final Allocation allocation =
            Allocation.of(PENALTY, money("0.01"), INTEREST, PRINCIPAL);
        assertEquals(money("2000.01"), allocation.total());
    }

    @Test
    @DisplayName("an all-zero allocation has a zero total")
    void allZeroAllocation() {
        final Allocation allocation =
            Allocation.of(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO);
        assertTrue(allocation.total().isZero());
    }

    @Test
    @DisplayName("every tier is required")
    void rejectsNullTiers() {
        assertThrows(InvalidAllocationException.class,
            () -> Allocation.of(null, Money.ZERO, INTEREST, PRINCIPAL));
        assertThrows(InvalidAllocationException.class,
            () -> Allocation.of(PENALTY, null, INTEREST, PRINCIPAL));
        assertThrows(InvalidAllocationException.class,
            () -> Allocation.of(PENALTY, Money.ZERO, null, PRINCIPAL));
        assertThrows(InvalidAllocationException.class,
            () -> Allocation.of(PENALTY, Money.ZERO, INTEREST, null));
    }

    @Test
    @DisplayName("the rejection is a domain exception naming the missing tier")
    void rejectionIsADomainException() {
        final InvalidAllocationException thrown = assertThrows(InvalidAllocationException.class,
            () -> Allocation.of(PENALTY, Money.ZERO, null, PRINCIPAL));
        assertTrue(thrown instanceof LoanDomainException);
        assertTrue(thrown.getMessage().contains("interest"));
    }

    @Test
    @DisplayName("compares by value, tier by tier")
    void comparesByValue() {
        final Allocation first = Allocation.of(PENALTY, Money.ZERO, INTEREST, PRINCIPAL);
        final Allocation same = Allocation.of(money("12.50"), Money.ZERO,
            money("300.00"), money("1687.50"));
        final Allocation swapped = Allocation.of(Money.ZERO, PENALTY, INTEREST, PRINCIPAL);
        assertEquals(first, same);
        assertEquals(first.hashCode(), same.hashCode());
        assertNotEquals(first, swapped);
        assertEquals(first.total(), swapped.total());
    }
}
