package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link GracePeriodPolicy} (BR-13, FR-10, TC-PRD-03): a grace of 0 to 3 months defers
 * the first instalment; 4 is rejected.
 */
class GracePeriodPolicyTest {

    private static final LocalDate DISBURSED = LocalDate.of(2027, 1, 31);

    private final GracePeriodPolicy policy = new GracePeriodPolicy();

    @Test
    @DisplayName("grace 0: the first instalment is one month after disbursement (TC-PRD-03.2)")
    void graceZero() {
        assertEquals(LocalDate.of(2027, 2, 28), policy.firstDueDate(DISBURSED, 0));
    }

    @Test
    @DisplayName("grace 1: the first instalment is deferred one month (TC-PRD-03.3)")
    void graceOne() {
        assertEquals(LocalDate.of(2027, 3, 31), policy.firstDueDate(DISBURSED, 1));
    }

    @Test
    @DisplayName("grace 3: the first instalment is deferred three months, clamped to month end (TC-PRD-03.4)")
    void graceThree() {
        assertEquals(LocalDate.of(2027, 5, 31), policy.firstDueDate(DISBURSED, 3));
        assertEquals(LocalDate.of(2027, 5, 30), policy.firstDueDate(LocalDate.of(2027, 1, 30), 3));
    }

    @Test
    @DisplayName("grace 4 and grace -1 are rejected with a domain exception (TC-PRD-03.1, .5)")
    void outOfRangeRejected() {
        final LoanDomainException tooLong = assertThrows(InvalidGracePeriodException.class,
            () -> policy.firstDueDate(DISBURSED, 4));
        assertTrue(tooLong.getMessage().contains("4"));
        assertThrows(InvalidGracePeriodException.class, () -> policy.firstDueDate(DISBURSED, -1));
    }

    @Test
    @DisplayName("interest accrued during grace is principal x rate / 12 per month, rounded HALF_EVEN")
    void accruedInterest() {
        final Money principal = Money.of(new BigDecimal("12000.00"));
        assertEquals(Money.ZERO, policy.accruedInterest(principal, new BigDecimal("12.00"), 0));
        assertEquals(Money.of(new BigDecimal("120.00")),
            policy.accruedInterest(principal, new BigDecimal("12.00"), 1));
        assertEquals(Money.of(new BigDecimal("360.00")),
            policy.accruedInterest(principal, new BigDecimal("12.00"), 3));
        // 100.00 at 0.18% for 1 month = 0.015 -> 0.02 (HALF_EVEN, BR-15)
        assertEquals(Money.of(new BigDecimal("0.02")),
            policy.accruedInterest(Money.of(new BigDecimal("100.00")), new BigDecimal("0.18"), 1));
    }
}
