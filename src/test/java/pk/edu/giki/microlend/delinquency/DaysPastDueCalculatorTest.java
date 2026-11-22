package pk.edu.giki.microlend.delinquency;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
 * Tests for {@link DaysPastDueCalculator} (BR-29, FR-22, TC-DEL-02).
 */
class DaysPastDueCalculatorTest {

    private static final LocalDate DUE = LocalDate.of(2027, 3, 10);

    private final DaysPastDueCalculator calculator = new DaysPastDueCalculator();

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    private static InstalmentDue owing(final int sequence, final LocalDate dueDate,
                                       final String penalty, final String interest,
                                       final String principal) {
        return new InstalmentDue(sequence, dueDate, money(penalty), Money.ZERO,
            money(interest), money(principal));
    }

    @ParameterizedTest(name = "TC-DEL-02: due 2027-03-10, business date {0} -> {1} days")
    @CsvSource({
        "2027-03-09, 0",
        "2027-03-10, 0",
        "2027-03-11, 1",
        "2027-04-09, 30",
    })
    void countedFromTheDayAfterTheDueDate(final LocalDate businessDate, final int expected) {
        assertEquals(expected, calculator.daysPastDue(DUE, businessDate));
    }

    @Test
    @DisplayName("the oldest instalment still owing principal or interest sets the count")
    void oldestUnpaidInstalmentDrives() {
        final List<InstalmentDue> instalments = List.of(
            owing(2, DUE.plusMonths(1), "0.00", "50.00", "500.00"),
            owing(1, DUE, "0.00", "0.00", "200.00"));
        assertEquals(41, calculator.daysPastDue(instalments, DUE.plusDays(41)));
    }

    @Test
    @DisplayName("an instalment with only penalty left does not keep the loan past due")
    void penaltyOnlyIsNotPastDue() {
        final List<InstalmentDue> instalments = List.of(
            owing(1, DUE, "12.00", "0.00", "0.00"),
            owing(2, DUE.plusMonths(1), "0.00", "50.00", "500.00"));
        assertEquals(10, calculator.daysPastDue(instalments, DUE.plusMonths(1).plusDays(10)));
    }

    @Test
    @DisplayName("nothing owing means zero days past due")
    void nothingOwing() {
        assertEquals(0, calculator.daysPastDue(List.of(), DUE));
    }
}
