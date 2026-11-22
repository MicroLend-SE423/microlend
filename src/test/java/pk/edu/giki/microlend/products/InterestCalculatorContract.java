package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * The contract every {@link InterestCalculator} honours, run against each implementation by
 * its own test class (M1 design section 4.3, Liskov substitution; BR-15, BR-16).
 *
 * <p>A calculator that passes these checks can stand in for any other without a caller
 * noticing, which is what lets a fourth product arrive without touching the schedule
 * workflow.</p>
 */
abstract class InterestCalculatorContract {

    private static final BigDecimal RATE = new BigDecimal("24.00");

    /**
     * Supplies the implementation under test.
     *
     * @return a fresh calculator
     */
    protected abstract InterestCalculator calculator();

    @Test
    @DisplayName("exactly one interest and one principal amount per month, never null (BR-16)")
    void oneAmountPerMonth() {
        for (int tenure : new int[] {1, 6, 36}) {
            final List<Money> interest =
                calculator().interestSchedule(Money.of(new BigDecimal("100000.00")), RATE, tenure);
            final List<Money> principal =
                calculator().principalSchedule(Money.of(new BigDecimal("100000.00")), RATE, tenure);
            assertNotNull(interest);
            assertNotNull(principal);
            assertEquals(tenure, interest.size());
            assertEquals(tenure, principal.size());
            assertTrue(noNulls(interest, principal));
        }
    }

    @Test
    @DisplayName("principal portions add up to the principal within one paisa per instalment (BR-14)")
    void principalAddsUpToPrincipal() {
        final Money principal = Money.of(new BigDecimal("10000.00"));
        for (int tenure : new int[] {1, 3, 7, 36}) {
            Money sum = Money.ZERO;
            for (Money part : calculator().principalSchedule(principal, RATE, tenure)) {
                sum = sum.plus(part);
            }
            final Money gap = sum.isGreaterThan(principal) ? sum.minus(principal) : principal.minus(sum);
            assertTrue(!gap.isGreaterThan(Money.of(new BigDecimal("0.01").multiply(
                BigDecimal.valueOf(tenure)))), "principal portions drifted by " + gap);
        }
    }

    @Test
    @DisplayName("a zero rate charges no interest")
    void zeroRateChargesNothing() {
        for (Money part : calculator().interestSchedule(
            Money.of(new BigDecimal("5000.00")), BigDecimal.ZERO, 6)) {
            assertTrue(part.isZero());
        }
    }

    @Test
    @DisplayName("a zero principal gives all-zero schedules rather than an error")
    void zeroPrincipal() {
        for (Money part : calculator().interestSchedule(Money.ZERO, RATE, 4)) {
            assertTrue(part.isZero());
        }
        for (Money part : calculator().principalSchedule(Money.ZERO, RATE, 4)) {
            assertTrue(part.isZero());
        }
    }

    @Test
    @DisplayName("a tenure below 1 is refused with a typed exception")
    void rejectsTenureBelowOne() {
        assertThrows(InvalidScheduleException.class,
            () -> calculator().interestSchedule(Money.of(new BigDecimal("100.00")), RATE, 0));
        assertThrows(InvalidScheduleException.class,
            () -> calculator().principalSchedule(Money.of(new BigDecimal("100.00")), RATE, 0));
    }

    @Test
    @DisplayName("a negative rate is refused with a typed exception")
    void rejectsNegativeRate() {
        assertThrows(InvalidScheduleException.class, () -> calculator().interestSchedule(
            Money.of(new BigDecimal("100.00")), new BigDecimal("-1.00"), 3));
    }

    private static boolean noNulls(final List<Money> first, final List<Money> second) {
        return first.stream().allMatch(m -> m != null) && second.stream().allMatch(m -> m != null);
    }
}
