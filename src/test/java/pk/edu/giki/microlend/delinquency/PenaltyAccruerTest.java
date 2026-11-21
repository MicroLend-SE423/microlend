package pk.edu.giki.microlend.delinquency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.InstalmentDue;

/**
 * Tests for {@link PenaltyAccruer} (BR-27, BR-28, FR-21, TC-DEL-01).
 *
 * <p>Accrual is simple and non-compounding: 0.05% of overdue principal per day,
 * capped at 10% of that principal (reached on day 200).</p>
 */
class PenaltyAccruerTest {

    private static final LocalDate TODAY = LocalDate.of(2027, 6, 1);

    private final PenaltyAccruer accruer = new PenaltyAccruer();

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static InstalmentDue overdue(final int sequence, final int days,
                                         final String interest, final String principal) {
        return new InstalmentDue(sequence, TODAY.minusDays(days), Money.ZERO, Money.ZERO,
            money(interest), money(principal));
    }

    @ParameterizedTest(name = "TC-DEL-01: 10,000.00 overdue for {0} days -> {1}")
    @CsvSource({
        "0, 0.00",
        "1, 5.00",
        "30, 150.00",
        "90, 450.00",
        "199, 995.00",
        "200, 1000.00",
        "201, 1000.00",
    })
    void accruedUpToTheCap(final int days, final String expected) {
        assertEquals(money(expected), accruer.accrued(money("10000.00"), days));
    }

    @Test
    @DisplayName("TC-DEL-01.7: overdue interest alone accrues no penalty")
    void interestOnlyAccruesNothing() {
        assertEquals(Money.ZERO, accruer.accrue(List.of(overdue(1, 30, "500.00", "0.00")), TODAY));
    }

    @Test
    @DisplayName("negative days are rejected")
    void negativeDaysRejected() {
        assertThrows(InvalidDaysPastDueException.class,
            () -> accruer.accrued(money("100.00"), -1));
    }

    @Test
    @DisplayName("one business date adds one day of penalty per overdue instalment")
    void oneDayPerOverdueInstalment() {
        final List<InstalmentDue> owing = List.of(
            overdue(1, 40, "0.00", "1000.00"),
            overdue(2, 10, "0.00", "1000.00"));
        assertEquals(money("1.00"), accruer.accrue(owing, TODAY));
    }

    @Test
    @DisplayName("an instalment that falls due today is not yet overdue")
    void dueTodayIsNotOverdue() {
        assertEquals(Money.ZERO, accruer.accrue(List.of(overdue(1, 0, "0.00", "1000.00")), TODAY));
    }

    @Test
    @DisplayName("TC-DEL-01.6: once the cap is reached a further day adds nothing")
    void noAccrualPastTheCap() {
        assertEquals(Money.ZERO,
            accruer.accrue(List.of(overdue(1, 201, "0.00", "10000.00")), TODAY));
        assertEquals(money("5.00"),
            accruer.accrue(List.of(overdue(1, 200, "0.00", "10000.00")), TODAY));
    }

    @Test
    @DisplayName("a skipped business date is caught up: penalty runs from the previous run to this one")
    void catchesUpSkippedDates() {
        final LocalDate due = LocalDate.of(2027, 3, 10);
        final List<InstalmentDue> owing = List.of(new InstalmentDue(1, due, Money.ZERO,
            Money.ZERO, Money.ZERO, money("10000.00")));
        final Money first = accruer.accrue(owing, PenaltyAccruer.NO_PREVIOUS_RUN, due.plusDays(1));
        final Money catchUp = accruer.accrue(owing, due.plusDays(1), due.plusDays(5));
        assertEquals(money("5.00"), first);
        assertEquals(money("20.00"), catchUp);
        assertEquals(accruer.accrued(money("10000.00"), 5), first.plus(catchUp));
    }

    @Test
    @DisplayName("a loan never processed before accrues every overdue day up to the business date")
    void firstRunAccruesEveryOverdueDay() {
        assertEquals(money("50.00"), accruer.accrue(List.of(overdue(1, 10, "0.00", "10000.00")),
            PenaltyAccruer.NO_PREVIOUS_RUN, TODAY));
    }

    @Test
    @DisplayName("the previous run must come before the business date")
    void previousRunMustBeEarlier() {
        assertThrows(IllegalArgumentException.class,
            () -> accruer.accrue(List.of(overdue(1, 10, "0.00", "10000.00")), TODAY, TODAY));
    }

    @Test
    @DisplayName("daily amounts add up exactly to the cumulative penalty, rounding included")
    void dailyAmountsSumToCumulative() {
        final Money principal = money("333.33");
        Money total = Money.ZERO;
        for (int day = 1; day <= 210; day++) {
            total = total.plus(accruer.accrue(
                List.of(new InstalmentDue(1, TODAY.minusDays(day), Money.ZERO, Money.ZERO,
                    Money.ZERO, principal)), TODAY));
        }
        assertEquals(accruer.accrued(principal, 210), total);
        assertEquals(money("33.33"), total);
    }
}
