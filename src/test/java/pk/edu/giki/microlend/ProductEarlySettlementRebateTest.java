package pk.edu.giki.microlend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.products.BulletProduct;
import pk.edu.giki.microlend.products.FlatRateProduct;
import pk.edu.giki.microlend.products.InMemoryProductCatalogue;
import pk.edu.giki.microlend.products.InMemoryScheduleRepository;
import pk.edu.giki.microlend.products.LoanProduct;
import pk.edu.giki.microlend.products.ReducingBalanceProduct;
import pk.edu.giki.microlend.products.ScheduleNotFoundException;

/**
 * Tests for {@link ProductEarlySettlementRebate} (decision D-03, BR-22, FR-15): it finds the
 * loan's product and schedule and delegates the rule to the product.
 */
class ProductEarlySettlementRebateTest {

    private static final LocalDate DISBURSED = LocalDate.of(2027, 1, 15);
    private static final LoanId FLAT_LOAN = LoanId.of("LN-FLAT");
    private static final LoanId REDUCING_LOAN = LoanId.of("LN-RED");
    private static final LoanId BULLET_LOAN = LoanId.of("LN-BUL");

    private final InMemoryProductCatalogue catalogue = new InMemoryProductCatalogue();
    private final InMemoryScheduleRepository schedules = new InMemoryScheduleRepository();
    private final ProductEarlySettlementRebate rebate =
        new ProductEarlySettlementRebate(catalogue, schedules);

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    private void enrol(final LoanId loan, final LoanProduct product, final String principal,
                       final int tenure) {
        catalogue.add(product);
        catalogue.assign(loan, product.code());
        schedules.save(loan, product.generateSchedule(money(principal), tenure, DISBURSED));
    }

    @BeforeEach
    void seed() {
        enrol(FLAT_LOAN, new FlatRateProduct("F", 1200, money("1000.00"), money("500000.00"), 1, 36),
            "12000.00", 3);
        enrol(REDUCING_LOAN,
            new ReducingBalanceProduct("R", 1200, money("1000.00"), money("500000.00"), 1, 36),
            "1200.00", 2);
        enrol(BULLET_LOAN, new BulletProduct("B", 2400, money("1000.00"), money("500000.00"), 1, 36),
            "10000.00", 3);
    }

    @Test
    @DisplayName("each loan is rebated by its own product's rule")
    void delegatesToTheLoansProduct() {
        final LocalDate settled = LocalDate.of(2027, 2, 1);
        assertEquals(Money.ZERO, rebate.rebateFor(FLAT_LOAN, settled));
        assertEquals(money("11.45"), rebate.rebateFor(REDUCING_LOAN, settled));
        assertEquals(money("400.00"), rebate.rebateFor(BULLET_LOAN, settled));
    }

    @Test
    @DisplayName("a loan with no product or no schedule is refused with a typed exception, not a zero")
    void unknownLoanRefused() {
        assertThrows(ScheduleNotFoundException.class,
            () -> rebate.rebateFor(LoanId.of("LN-404"), DISBURSED));
        catalogue.assign(LoanId.of("LN-NO-SCHEDULE"), "R");
        assertThrows(ScheduleNotFoundException.class,
            () -> rebate.rebateFor(LoanId.of("LN-NO-SCHEDULE"), DISBURSED));
    }
}
