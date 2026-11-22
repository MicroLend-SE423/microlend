package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link ReducingBalanceCalculator}: interest on the outstanding balance with equal
 * instalments (BR-11), rounded HALF_EVEN at every step (BR-15). Also runs the shared contract.
 */
class ReducingBalanceCalculatorTest extends InterestCalculatorContract {

    private final ReducingBalanceCalculator calculator = new ReducingBalanceCalculator();

    @Override
    protected InterestCalculator calculator() {
        return calculator;
    }

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    @DisplayName("hand-computed two-month loan: 1200.00 at 12% a year")
    void handComputedTwoMonths() {
        // i = 1% a month; instalment = 1200 x 0.01 x 1.0201 / 0.0201 = 609.0149 -> 609.01.
        // Month 1: interest 12.00, principal 597.01, balance 602.99.
        // Month 2: interest 6.03 (6.0299), the whole balance 602.99 falls due.
        assertEquals(List.of(money("12.00"), money("6.03")),
            calculator.interestSchedule(money("1200.00"), new BigDecimal("12.00"), 2));
        assertEquals(List.of(money("597.01"), money("602.99")),
            calculator.principalSchedule(money("1200.00"), new BigDecimal("12.00"), 2));
    }

    @Test
    @DisplayName("interest falls month by month as the balance is repaid")
    void interestFalls() {
        final List<Money> interest =
            calculator.interestSchedule(money("100000.00"), new BigDecimal("24.00"), 12);
        for (int i = 1; i < interest.size(); i++) {
            assertEquals(true, interest.get(i - 1).isGreaterThan(interest.get(i)));
        }
        assertEquals(money("2000.00"), interest.get(0));
    }

    @Test
    @DisplayName("a first-month interest ending in .005 rounds to the even paisa (BR-15)")
    void halfEvenAtTheBoundary() {
        assertEquals(money("0.00"),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.06"), 2).get(0));
        assertEquals(money("0.02"),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.18"), 2).get(0));
        assertEquals(money("0.02"),
            calculator.interestSchedule(money("100.00"), new BigDecimal("0.30"), 2).get(0));
    }

    @Test
    @DisplayName("with no interest the principal is repaid in equal parts")
    void zeroRateEqualParts() {
        assertEquals(List.of(money("250.00"), money("250.00"), money("250.00"), money("250.00")),
            calculator.principalSchedule(money("1000.00"), BigDecimal.ZERO, 4));
    }
}
