package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link FlatRateCalculator}: interest on the original principal throughout
 * (BR-11), rounded HALF_EVEN at every step (BR-15). Also runs the shared contract.
 */
class FlatRateCalculatorTest extends InterestCalculatorContract {

    private final FlatRateCalculator calculator = new FlatRateCalculator();

    @Override
    protected InterestCalculator calculator() {
        return calculator;
    }

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    @DisplayName("interest is the same every month: principal x rate x tenure / 12, spread evenly")
    void evenInterest() {
        assertEquals(List.of(money("120.00"), money("120.00"), money("120.00")),
            calculator.interestSchedule(money("12000.00"), new BigDecimal("12.00"), 3));
    }

    @Test
    @DisplayName("total interest is rounded once, then spread; the last month takes the residue")
    void residueGoesToTheLastMonth() {
        // 1000.00 at 10% for 3 months: total 25.00, 8.33 + 8.33 + 8.34.
        assertEquals(List.of(money("8.33"), money("8.33"), money("8.34")),
            calculator.interestSchedule(money("1000.00"), new BigDecimal("10.00"), 3));
    }

    @Test
    @DisplayName("a total ending in .005 rounds to the even paisa (BR-15)")
    void halfEvenAtTheBoundary() {
        // 100.00 at 0.06% for 1 month = 0.005 -> 0.00; at 0.18% = 0.015 -> 0.02; at 0.30% = 0.025 -> 0.02.
        assertEquals(List.of(money("0.00")),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.06"), 1));
        assertEquals(List.of(money("0.02")),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.18"), 1));
        assertEquals(List.of(money("0.02")),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.30"), 1));
    }

    @Test
    @DisplayName("principal is split evenly over the tenure; the last month takes the residue (BR-14)")
    void evenPrincipal() {
        assertEquals(List.of(money("3333.33"), money("3333.33"), money("3333.34")),
            calculator.principalSchedule(money("10000.00"), new BigDecimal("12.00"), 3));
    }
}
