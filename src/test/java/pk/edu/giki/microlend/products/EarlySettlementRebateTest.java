package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for the per-product early-settlement rebate (BR-22, decision D-03): each
 * {@link LoanProduct} subclass knows its own rule, so no caller switches on product type.
 */
class EarlySettlementRebateTest {

    private static final LocalDate DISBURSED = LocalDate.of(2027, 1, 15);

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    private static LoanProduct flat() {
        return new FlatRateProduct("F", 1200, money("1000.00"), money("500000.00"), 1, 36);
    }

    /** Reducing 1,200.00 at 12% over 2 months: instalments due 15 Feb and 15 Mar, interest 12.00 and 6.03. */
    private static LoanProduct reducing() {
        return new ReducingBalanceProduct("R", 1200, money("1000.00"), money("500000.00"), 1, 36);
    }

    /** Bullet 10,000.00 at 24% over 3 months: interest 200.00 due 15 Feb, 15 Mar and 15 Apr. */
    private static LoanProduct bullet() {
        return new BulletProduct("B", 2400, money("1000.00"), money("500000.00"), 1, 36);
    }

    @Test
    @DisplayName("flat-rate refunds nothing, whenever the loan is settled")
    void flatRateGivesNoRebate() {
        final Schedule schedule = flat().generateSchedule(money("12000.00"), 3, DISBURSED);
        for (LocalDate date : new LocalDate[] {DISBURSED, LocalDate.of(2027, 2, 15),
            LocalDate.of(2027, 3, 1), LocalDate.of(2027, 4, 15), LocalDate.of(2027, 6, 1)}) {
            assertEquals(Money.ZERO, flat().earlySettlementRebate(schedule, date));
        }
    }

    @Test
    @DisplayName("reducing-balance settled on the first day refunds all the interest")
    void reducingFullRefund() {
        final Schedule schedule = reducing().generateSchedule(money("1200.00"), 2, DISBURSED);
        assertEquals(money("18.03"), reducing().earlySettlementRebate(schedule, DISBURSED));
    }

    @Test
    @DisplayName("reducing-balance refunds the unearned part of the current month and every later month")
    void reducingMidPeriod() {
        // 1 Feb: month 1 (15 Jan to 15 Feb, 31 days) has 14 days to run: 12.00 x 14/31 = 5.42;
        // month 2 has not begun: 6.03. Total 11.45.
        final Schedule schedule = reducing().generateSchedule(money("1200.00"), 2, DISBURSED);
        assertEquals(money("11.45"),
            reducing().earlySettlementRebate(schedule, LocalDate.of(2027, 2, 1)));
    }

    @Test
    @DisplayName("reducing-balance: the day before a due date refunds one day of that month; on it, none (BR-22 boundary)")
    void reducingAroundADueDate() {
        final Schedule schedule = reducing().generateSchedule(money("1200.00"), 2, DISBURSED);
        // 14 Feb: 1 day left of 31 -> 12.00 x 1/31 = 0.387 -> 0.39; plus month 2 in full, 6.03.
        assertEquals(money("6.42"),
            reducing().earlySettlementRebate(schedule, LocalDate.of(2027, 2, 14)));
        // 15 Feb: instalment 1 is due and earned; month 2 has just begun and is refunded in full.
        assertEquals(money("6.03"),
            reducing().earlySettlementRebate(schedule, LocalDate.of(2027, 2, 15)));
    }

    @Test
    @DisplayName("reducing-balance settled on or after the final due date refunds nothing")
    void reducingAtMaturity() {
        final Schedule schedule = reducing().generateSchedule(money("1200.00"), 2, DISBURSED);
        assertEquals(Money.ZERO, reducing().earlySettlementRebate(schedule, LocalDate.of(2027, 3, 15)));
        assertEquals(Money.ZERO, reducing().earlySettlementRebate(schedule, LocalDate.of(2027, 5, 1)));
    }

    @Test
    @DisplayName("bullet refunds only whole months not yet begun, never part of the current month")
    void bulletRefundsUnaccruedMonths() {
        final Schedule schedule = bullet().generateSchedule(money("10000.00"), 3, DISBURSED);
        // 1 Feb is inside month 1: months 2 and 3 are unaccrued, month 1 is not refunded.
        assertEquals(money("400.00"), bullet().earlySettlementRebate(schedule, LocalDate.of(2027, 2, 1)));
        // On 15 Feb month 2 has just begun: it and month 3 are refunded.
        assertEquals(money("400.00"), bullet().earlySettlementRebate(schedule, LocalDate.of(2027, 2, 15)));
        // 16 Feb is inside month 2: only month 3 remains unaccrued.
        assertEquals(money("200.00"), bullet().earlySettlementRebate(schedule, LocalDate.of(2027, 2, 16)));
        // Settled on the final due date: nothing left to refund.
        assertEquals(Money.ZERO, bullet().earlySettlementRebate(schedule, LocalDate.of(2027, 4, 15)));
    }
}
