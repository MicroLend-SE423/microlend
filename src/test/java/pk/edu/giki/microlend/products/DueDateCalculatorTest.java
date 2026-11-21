package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DueDateCalculator}.
 *
 * <p>Traces to BR-12 (instalments fall on the disbursement day-of-month, clamped to the last
 * day of a short month), FR-09 and TC-PRD-02.</p>
 */
class DueDateCalculatorTest {

    private final DueDateCalculator calculator = new DueDateCalculator();

    @Test
    @DisplayName("31 January clamps to the end of each shorter month and never drifts (BR-12)")
    void disbursementOnThe31st() {
        assertEquals(List.of(
            LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 31),
            LocalDate.of(2025, 4, 30), LocalDate.of(2025, 5, 31)),
            calculator.dueDates(LocalDate.of(2025, 1, 31), 4));
    }

    @Test
    @DisplayName("the 30th clamps only in February (BR-12)")
    void disbursementOnThe30th() {
        assertEquals(List.of(
            LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 30), LocalDate.of(2025, 4, 30)),
            calculator.dueDates(LocalDate.of(2025, 1, 30), 3));
    }

    @Test
    @DisplayName("the 29th clamps in a non-leap February and is kept in a leap one (BR-12)")
    void disbursementOnThe29th() {
        assertEquals(LocalDate.of(2025, 2, 28),
            calculator.dueDates(LocalDate.of(2025, 1, 29), 1).get(0));
        assertEquals(LocalDate.of(2028, 2, 29),
            calculator.dueDates(LocalDate.of(2028, 1, 29), 1).get(0));
    }

    @Test
    @DisplayName("the 28th never needs clamping, leap year or not (BR-12)")
    void disbursementOnThe28th() {
        assertEquals(List.of(LocalDate.of(2025, 2, 28), LocalDate.of(2025, 3, 28)),
            calculator.dueDates(LocalDate.of(2025, 1, 28), 2));
        assertEquals(LocalDate.of(2028, 2, 28),
            calculator.dueDates(LocalDate.of(2028, 1, 28), 1).get(0));
    }

    @Test
    @DisplayName("a disbursement in February starts the series in March on the same day")
    void disbursementInFebruary() {
        assertEquals(List.of(LocalDate.of(2025, 3, 28), LocalDate.of(2025, 4, 28)),
            calculator.dueDates(LocalDate.of(2025, 2, 28), 2));
        assertEquals(LocalDate.of(2028, 3, 29),
            calculator.dueDates(LocalDate.of(2028, 2, 29), 1).get(0));
    }

    @Test
    @DisplayName("the series rolls over the year end")
    void crossesYearEnd() {
        assertEquals(List.of(LocalDate.of(2026, 1, 15), LocalDate.of(2026, 2, 15)),
            calculator.dueDates(LocalDate.of(2025, 12, 15), 2));
    }

    @Test
    @DisplayName("exactly the requested number of dates is produced (BR-16)")
    void producesRequestedCount() {
        assertEquals(1, calculator.dueDates(LocalDate.of(2025, 6, 15), 1).size());
        assertEquals(36, calculator.dueDates(LocalDate.of(2025, 6, 15), 36).size());
    }

    @Test
    @DisplayName("the returned list cannot be modified")
    void resultIsUnmodifiable() {
        final List<LocalDate> dates = calculator.dueDates(LocalDate.of(2025, 6, 15), 2);
        assertThrows(UnsupportedOperationException.class, () -> dates.add(LocalDate.of(2030, 1, 1)));
    }

    @Test
    @DisplayName("a missing date or a count below 1 is refused with a typed exception")
    void rejectsInvalidInput() {
        assertThrows(InvalidScheduleException.class, () -> calculator.dueDates(null, 3));
        assertThrows(InvalidScheduleException.class,
            () -> calculator.dueDates(LocalDate.of(2025, 6, 15), 0));
    }
}
