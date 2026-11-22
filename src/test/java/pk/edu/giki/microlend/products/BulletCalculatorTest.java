package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link BulletCalculator}: interest paid monthly on the full principal, the whole
 * principal due at maturity (BR-11). Also runs the shared contract.
 */
class BulletCalculatorTest extends InterestCalculatorContract {

    private final BulletCalculator calculator = new BulletCalculator();

    @Override
    protected InterestCalculator calculator() {
        return calculator;
    }

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    @DisplayName("every month carries one month of interest on the full principal")
    void monthlyInterest() {
        assertEquals(List.of(money("200.00"), money("200.00"), money("200.00")),
            calculator.interestSchedule(money("10000.00"), new BigDecimal("24.00"), 3));
    }

    @Test
    @DisplayName("no principal falls due until the final month")
    void principalAtMaturity() {
        assertEquals(List.of(money("0.00"), money("0.00"), money("10000.00")),
            calculator.principalSchedule(money("10000.00"), new BigDecimal("24.00"), 3));
        assertEquals(List.of(money("10000.00")),
            calculator.principalSchedule(money("10000.00"), new BigDecimal("24.00"), 1));
    }

    @Test
    @DisplayName("a monthly interest ending in .005 rounds to the even paisa (BR-15)")
    void halfEvenAtTheBoundary() {
        assertEquals(List.of(money("0.00")),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.06"), 1));
        assertEquals(List.of(money("0.02")),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.18"), 1));
        assertEquals(List.of(money("0.02")),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.30"), 1));
    }
}
