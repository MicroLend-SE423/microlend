package pk.edu.giki.microlend.products;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link ScheduleAssembler}: the rounding residue goes into the final instalment
 * (BR-14) and a schedule has exactly one instalment per due date (BR-16).
 */
class ScheduleAssemblerTest {

    private static final List<LocalDate> DATES = List.of(
        LocalDate.of(2027, 2, 15), LocalDate.of(2027, 3, 15), LocalDate.of(2027, 4, 15));

    private final ScheduleAssembler assembler = new ScheduleAssembler();

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    @Test
    @DisplayName("principal still missing after rounding is added to the final instalment (BR-14)")
    void residueAddedToFinal() {
        final Schedule schedule = assembler.assemble(money("100.00"), DATES,
            List.of(money("33.33"), money("33.33"), money("33.33")),
            List.of(money("1.00"), money("1.00"), money("1.00")));
        assertEquals(money("33.34"), schedule.instalments().get(2).principal());
        assertEquals(money("33.33"), schedule.instalments().get(0).principal());
        assertEquals(money("100.00"), schedule.totalPrincipal());
    }

    @Test
    @DisplayName("principal already over the total is taken back from the final instalment (BR-14)")
    void overshootTakenFromFinal() {
        final Schedule schedule = assembler.assemble(money("100.00"), DATES,
            List.of(money("33.34"), money("33.34"), money("33.34")),
            List.of(money("0.00"), money("0.00"), money("0.00")));
        assertEquals(money("33.32"), schedule.instalments().get(2).principal());
        assertEquals(money("100.00"), schedule.totalPrincipal());
    }

    @Test
    @DisplayName("sequence numbers start at 1 and follow the due dates")
    void sequencesAndDates() {
        final Schedule schedule = assembler.assemble(money("90.00"), DATES,
            List.of(money("30.00"), money("30.00"), money("30.00")),
            List.of(money("3.00"), money("2.00"), money("1.00")));
        for (int i = 0; i < 3; i++) {
            assertEquals(i + 1, schedule.instalments().get(i).sequence());
            assertEquals(DATES.get(i), schedule.instalments().get(i).dueDate());
        }
        assertEquals(money("6.00"), schedule.totalInterest());
        assertEquals(money("96.00"), schedule.totalPayable());
    }

    @Test
    @DisplayName("an extra amount of interest is added to the first instalment only (BR-13)")
    void extraFirstInterest() {
        final Schedule schedule = assembler.assemble(money("90.00"), DATES,
            List.of(money("30.00"), money("30.00"), money("30.00")),
            List.of(money("3.00"), money("3.00"), money("3.00")), money("1.50"));
        assertEquals(money("4.50"), schedule.instalments().get(0).interest());
        assertEquals(money("3.00"), schedule.instalments().get(1).interest());
    }

    @Test
    @DisplayName("a count that does not match the due dates is refused (BR-16)")
    void countMustMatch() {
        assertThrows(InvalidScheduleException.class, () -> assembler.assemble(money("90.00"), DATES,
            List.of(money("45.00"), money("45.00")), List.of(money("1.00"), money("1.00"))));
        assertThrows(InvalidScheduleException.class, () -> assembler.assemble(money("90.00"), DATES,
            List.of(money("30.00"), money("30.00"), money("30.00")), List.of(money("1.00"))));
    }

    @Test
    @DisplayName("principal already due before the final instalment cannot exceed the principal lent")
    void impossibleResidue() {
        assertThrows(InvalidScheduleException.class, () -> assembler.assemble(money("0.80"), DATES,
            List.of(money("0.50"), money("0.50"), money("0.50")),
            List.of(money("0.00"), money("0.00"), money("0.00"))));
    }
}
