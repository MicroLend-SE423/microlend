package pk.edu.giki.microlend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link Money}.
 *
 * <p>Traces to BR-40 and BR-43 in the Business Rules Register, and to
 * constraint C-01: money is never a binary floating-point type.</p>
 */
class MoneyTest {

    @Nested
    @DisplayName("construction enforces the invariants")
    class Construction {

        @Test
        @DisplayName("a negative amount is rejected (BR-43)")
        void rejectsNegativeAmount() {
            assertThrows(InvalidMoneyException.class,
                () -> Money.of(new BigDecimal("-0.01")));
        }

        @Test
        @DisplayName("more than two decimal places is rejected (BR-15)")
        void rejectsExcessiveScale() {
            assertThrows(InvalidMoneyException.class,
                () -> Money.of(new BigDecimal("10.005")));
        }

        @Test
        @DisplayName("null is rejected rather than stored")
        void rejectsNull() {
            assertThrows(InvalidMoneyException.class, () -> Money.of(null));
        }

        @Test
        @DisplayName("a value is normalised to scale 2")
        void normalisesScale() {
            assertEquals("5.00", Money.of(new BigDecimal("5")).toPlainString());
        }

        @Test
        @DisplayName("zero is permitted")
        void permitsZero() {
            assertTrue(Money.ZERO.isZero());
        }
    }

    @Nested
    @DisplayName("arithmetic")
    class Arithmetic {

        @Test
        @DisplayName("addition is exact where a double would drift (C-01)")
        void additionIsExact() {
            final Money sum = Money.of(new BigDecimal("0.10"))
                .plus(Money.of(new BigDecimal("0.20")));
            assertEquals("0.30", sum.toPlainString());
        }

        @Test
        @DisplayName("subtraction that would go below zero is rejected")
        void subtractionCannotGoNegative() {
            final Money ten = Money.of(new BigDecimal("10.00"));
            final Money twenty = Money.of(new BigDecimal("20.00"));
            assertThrows(InvalidMoneyException.class, () -> ten.minus(twenty));
        }

        @Test
        @DisplayName("subtraction to exactly zero is allowed")
        void subtractionToZeroIsAllowed() {
            final Money ten = Money.of(new BigDecimal("10.00"));
            assertTrue(ten.minus(ten).isZero());
        }

        @Test
        @DisplayName("a percentage is rounded HALF_EVEN to scale 2 (BR-15)")
        void percentageRoundsHalfEven() {
            // 0.05% of 1000.00 = 0.50
            final Money penalty = Money.of(new BigDecimal("1000.00"))
                .percentage(new BigDecimal("0.05"));
            assertEquals("0.50", penalty.toPlainString());
        }
    }

    @Nested
    @DisplayName("comparison")
    class Comparison {

        @Test
        @DisplayName("values of equal amount but different scale compare equal (C-02)")
        void comparesByValueNotScale() {
            final Money a = Money.of(new BigDecimal("2.5"));
            final Money b = Money.of(new BigDecimal("2.50"));
            assertEquals(0, a.compareTo(b));
            assertEquals(a, b);
        }

        @Test
        @DisplayName("isGreaterThan orders correctly")
        void ordersCorrectly() {
            final Money small = Money.of(new BigDecimal("1.00"));
            final Money large = Money.of(new BigDecimal("1.01"));
            assertTrue(large.isGreaterThan(small));
            assertFalse(small.isGreaterThan(large));
        }
    }
}
