package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for the Factory Method structure of {@code products} (M1 design section 5.1):
 * {@link LoanProduct} is the Creator, its subclasses override
 * {@code createInterestCalculator()}, and {@code generateSchedule()} never names a concrete
 * calculator. Traces to BR-11, BR-14, BR-16; FR-08, FR-11, FR-12; TC-PRD-01, 04 and 05.
 */
class LoanProductTest {

    private static final LocalDate DISBURSED = LocalDate.of(2027, 1, 15);

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    private static LoanProduct flat() {
        return new FlatRateProduct("FLAT-12", 1200, money("1000.00"), money("500000.00"), 1, 36);
    }

    private static LoanProduct reducing() {
        return new ReducingBalanceProduct("RB-12", 1200, money("1000.00"), money("500000.00"), 1, 36);
    }

    private static LoanProduct bullet() {
        return new BulletProduct("BUL-24", 2400, money("1000.00"), money("500000.00"), 1, 36);
    }

    @Test
    @DisplayName("flat-rate: 12,000.00 at 12% over 3 months is 4,120.00 a month (TC-PRD-01.1)")
    void flatRateSchedule() {
        final Schedule schedule = flat().generateSchedule(money("12000.00"), 3, DISBURSED);
        assertEquals(3, schedule.instalments().size());
        for (Instalment instalment : schedule.instalments()) {
            assertEquals(money("4000.00"), instalment.principal());
            assertEquals(money("120.00"), instalment.interest());
        }
        assertEquals(LocalDate.of(2027, 2, 15), schedule.instalments().get(0).dueDate());
        assertEquals(LocalDate.of(2027, 4, 15), schedule.instalments().get(2).dueDate());
    }

    @Test
    @DisplayName("reducing-balance: 1,200.00 at 12% over 2 months (TC-PRD-01.2)")
    void reducingBalanceSchedule() {
        final Schedule schedule = reducing().generateSchedule(money("1200.00"), 2, DISBURSED);
        assertEquals(money("597.01"), schedule.instalments().get(0).principal());
        assertEquals(money("12.00"), schedule.instalments().get(0).interest());
        assertEquals(money("602.99"), schedule.instalments().get(1).principal());
        assertEquals(money("6.03"), schedule.instalments().get(1).interest());
    }

    @Test
    @DisplayName("bullet: 10,000.00 at 24% over 3 months pays interest monthly (TC-PRD-01.3)")
    void bulletSchedule() {
        final Schedule schedule = bullet().generateSchedule(money("10000.00"), 3, DISBURSED);
        assertEquals(money("0.00"), schedule.instalments().get(0).principal());
        assertEquals(money("0.00"), schedule.instalments().get(1).principal());
        assertEquals(money("10000.00"), schedule.instalments().get(2).principal());
        assertEquals(money("200.00"), schedule.instalments().get(1).interest());
    }

    @Test
    @DisplayName("instalments sum to principal plus total interest for awkward amounts (TC-PRD-04)")
    void sumInvariant() {
        final String[] amounts = {"10000.00", "1000.01", "99999.99", "12345.67", "500000.00"};
        final int[] tenures = {1, 3, 6, 7, 12, 36};
        for (LoanProduct product : List.of(flat(), reducing(), bullet())) {
            for (String amount : amounts) {
                for (int tenure : tenures) {
                    final Schedule schedule = product.generateSchedule(money(amount), tenure, DISBURSED);
                    Money paid = Money.ZERO;
                    Money interest = Money.ZERO;
                    for (Instalment instalment : schedule.instalments()) {
                        paid = paid.plus(instalment.principal()).plus(instalment.interest());
                        interest = interest.plus(instalment.interest());
                    }
                    assertEquals(money(amount).plus(interest), paid);
                    assertEquals(money(amount), schedule.totalPrincipal());
                    assertEquals(paid, schedule.totalPayable());
                }
            }
        }
    }

    @Test
    @DisplayName("flat-rate total interest is principal x rate x tenure / 12 exactly (TC-PRD-04.1)")
    void flatTotalInterest() {
        // 10,000.00 at 12% for 3 months = 300.00, in 100.00 parts; principal 3,333.33 x 2 + 3,333.34.
        final Schedule schedule = flat().generateSchedule(money("10000.00"), 3, DISBURSED);
        assertEquals(money("300.00"), schedule.totalInterest());
        assertEquals(money("3333.34"), schedule.instalments().get(2).principal());
    }

    @Test
    @DisplayName("exactly tenureMonths instalments for tenure 1, 6 and 36 (TC-PRD-05)")
    void instalmentCount() {
        for (LoanProduct product : List.of(flat(), reducing(), bullet())) {
            for (int tenure : new int[] {1, 6, 36}) {
                assertEquals(tenure,
                    product.generateSchedule(money("50000.00"), tenure, DISBURSED).instalments().size());
            }
        }
    }

    @Test
    @DisplayName("the schedule and its instalment list cannot be modified")
    void scheduleIsImmutable() {
        final Schedule schedule = flat().generateSchedule(money("12000.00"), 3, DISBURSED);
        final List<Instalment> list = schedule.instalments();
        assertThrows(UnsupportedOperationException.class, () -> list.remove(0));
    }

    @Test
    @DisplayName("the Creator asks the subclass for its calculator: the factory method is what varies")
    void factoryMethodIsUsed() {
        final InterestCalculator tenPerInstalment = new InterestCalculator() {
            @Override
            public List<Money> interestSchedule(final Money principal, final BigDecimal rate,
                                                final int tenureMonths) {
                return java.util.Collections.nCopies(tenureMonths, money("10.00"));
            }

            @Override
            public List<Money> principalSchedule(final Money principal, final BigDecimal rate,
                                                 final int tenureMonths) {
                return new BulletCalculator().principalSchedule(principal, rate, tenureMonths);
            }
        };
        final LoanProduct custom = new LoanProduct("CUSTOM", 0, money("1000.00"),
            money("9000.00"), 1, 12) {
            @Override
            protected InterestCalculator createInterestCalculator() {
                return tenPerInstalment;
            }
        };
        final Schedule schedule = custom.generateSchedule(money("3000.00"), 2, DISBURSED);
        assertEquals(money("10.00"), schedule.instalments().get(0).interest());
        assertEquals(money("3000.00"), schedule.instalments().get(1).principal());
    }

    @Test
    @DisplayName("amounts and tenures outside the product band are refused (BR-06, BR-07)")
    void bandIsEnforced() {
        final LoanProduct product = flat();
        assertThrows(InvalidScheduleException.class,
            () -> product.generateSchedule(money("999.99"), 6, DISBURSED));
        assertThrows(InvalidScheduleException.class,
            () -> product.generateSchedule(money("500000.01"), 6, DISBURSED));
        assertThrows(InvalidScheduleException.class,
            () -> product.generateSchedule(money("5000.00"), 0, DISBURSED));
        assertThrows(InvalidScheduleException.class,
            () -> product.generateSchedule(money("5000.00"), 37, DISBURSED));
        assertThrows(InvalidScheduleException.class,
            () -> product.generateSchedule(money("5000.00"), 6, null));
    }

    @Test
    @DisplayName("grace 3: flat-rate first instalment moves to month 4 and carries 3 months of interest (TC-PRD-03)")
    void graceDefersAndAccrues() {
        final Schedule schedule = flat().generateSchedule(money("12000.00"), 3, DISBURSED, 3);
        assertEquals(3, schedule.instalments().size());
        assertEquals(LocalDate.of(2027, 5, 15), schedule.instalments().get(0).dueDate());
        assertEquals(LocalDate.of(2027, 7, 15), schedule.instalments().get(2).dueDate());
        assertEquals(money("480.00"), schedule.instalments().get(0).interest());
        assertEquals(money("120.00"), schedule.instalments().get(1).interest());
    }

    @Test
    @DisplayName("grace 1: reducing-balance adds one month of interest on the principal to the first instalment")
    void graceReducing() {
        final Schedule schedule = reducing().generateSchedule(money("1200.00"), 2, DISBURSED, 1);
        assertEquals(LocalDate.of(2027, 3, 15), schedule.instalments().get(0).dueDate());
        assertEquals(money("24.00"), schedule.instalments().get(0).interest());
        assertEquals(money("597.01"), schedule.instalments().get(0).principal());
    }

    @Test
    @DisplayName("tenure 6 with grace 3 is still exactly 6 instalments; sums still agree (BR-16, TC-PRD-05.2)")
    void graceKeepsInstalmentCount() {
        for (LoanProduct product : List.of(flat(), reducing(), bullet())) {
            final Schedule schedule = product.generateSchedule(money("50000.00"), 6, DISBURSED, 3);
            assertEquals(6, schedule.instalments().size());
            assertEquals(money("50000.00"), schedule.totalPrincipal());
        }
    }

    @Test
    @DisplayName("the three-argument form is grace 0")
    void noGraceByDefault() {
        assertEquals(flat().generateSchedule(money("12000.00"), 3, DISBURSED).instalments(),
            flat().generateSchedule(money("12000.00"), 3, DISBURSED, 0).instalments());
    }

    @Test
    @DisplayName("grace 4 is refused when generating a schedule (BR-13)")
    void graceFourRefused() {
        assertThrows(InvalidGracePeriodException.class,
            () -> flat().generateSchedule(money("12000.00"), 3, DISBURSED, 4));
    }

    @Test
    @DisplayName("a product exposes its catalogue data")
    void exposesCatalogueData() {
        final LoanProduct product = reducing();
        assertEquals("RB-12", product.code());
        assertEquals(1200, product.annualRateBp());
        assertEquals(money("1000.00"), product.minAmount());
        assertEquals(36, product.maxTenureMonths());
        assertTrue(product.generateSchedule(money("1000.00"), 1, DISBURSED).instalments().size() == 1);
    }
}
