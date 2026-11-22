package pk.edu.giki.microlend;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.products.InMemoryScheduleRepository;
import pk.edu.giki.microlend.products.Instalment;
import pk.edu.giki.microlend.products.Schedule;
import pk.edu.giki.microlend.products.ScheduleNotFoundException;

/**
 * Tests for {@link ScheduleInstalmentsDueSource} (decision D-02, BR-19, BR-20, FR-13, FR-14):
 * only instalments due by the business date are listed, and each is mapped field by field.
 */
class ScheduleInstalmentsDueSourceTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate DUE_1 = LocalDate.of(2027, 2, 15);
    private static final LocalDate DUE_2 = LocalDate.of(2027, 3, 15);
    private static final LocalDate DUE_3 = LocalDate.of(2027, 4, 15);

    private final InMemoryScheduleRepository schedules = new InMemoryScheduleRepository();
    private final ScheduleInstalmentsDueSource source = new ScheduleInstalmentsDueSource(schedules);

    private static Money money(final String value) {
        return Money.of(new BigDecimal(value));
    }

    @BeforeEach
    void seed() {
        schedules.save(LOAN, new Schedule(List.of(
            new Instalment(1, DUE_1, money("3000.00"), money("120.00")),
            new Instalment(2, DUE_2, money("3000.00"), money("80.00")),
            new Instalment(3, DUE_3, money("4000.01"), money("40.05")))));
    }

    private static List<Integer> sequences(final List<InstalmentDue> owing) {
        return owing.stream().map(InstalmentDue::sequence).toList();
    }

    @Test
    @DisplayName("an instalment due yesterday is listed; one due tomorrow is not")
    void dueYesterdayListedDueTomorrowNot() {
        assertEquals(List.of(1), sequences(source.owing(LOAN, DUE_1.plusDays(1))));
        assertEquals(List.of(), sequences(source.owing(LOAN, DUE_1.minusDays(1))));
    }

    @Test
    @DisplayName("an instalment due today is listed (dueDate <= business date)")
    void dueTodayListed() {
        assertEquals(List.of(1), sequences(source.owing(LOAN, DUE_1)));
        assertEquals(List.of(1, 2), sequences(source.owing(LOAN, DUE_2)));
    }

    @Test
    @DisplayName("after the last due date every instalment is listed, oldest first")
    void allListedAfterMaturity() {
        assertEquals(List.of(1, 2, 3), sequences(source.owing(LOAN, DUE_3.plusDays(30))));
    }

    @Test
    @DisplayName("each field is mapped: sequence, due date, interest, principal; fees zero")
    void mapsEveryField() {
        final InstalmentDue third = source.owing(LOAN, DUE_3).get(2);
        assertEquals(3, third.sequence());
        assertEquals(DUE_3, third.dueDate());
        assertEquals(money("40.05"), third.interest());
        assertEquals(money("4000.01"), third.principal());
        assertEquals(Money.ZERO, third.fees());
        assertEquals(Money.ZERO, third.penalty());
        assertEquals(money("4040.06"), third.total());
    }

    @Test
    @DisplayName("a penalty lookup supplied by the caller is mapped onto the instalment it names")
    void penaltyLookupPlugsIn() {
        final ScheduleInstalmentsDueSource withPenalty = new ScheduleInstalmentsDueSource(schedules,
            (loan, sequence, date) -> sequence == 1 ? money("25.00") : Money.ZERO);
        final List<InstalmentDue> owing = withPenalty.owing(LOAN, DUE_2);
        assertEquals(money("25.00"), owing.get(0).penalty());
        assertEquals(Money.ZERO, owing.get(1).penalty());
    }

    @Test
    @DisplayName("the penalty lookup is asked about the loan and business date of the call")
    void penaltyLookupReceivesTheCallContext() {
        final StringBuilder seen = new StringBuilder();
        final ScheduleInstalmentsDueSource spy = new ScheduleInstalmentsDueSource(schedules,
            (loan, sequence, date) -> {
                seen.append(loan).append('|').append(sequence).append('|').append(date).append(';');
                return Money.ZERO;
            });
        spy.owing(LOAN, DUE_1);
        assertEquals(LOAN + "|1|" + DUE_1 + ";", seen.toString());
    }

    @Test
    @DisplayName("nothing is treated as paid yet: calling twice lists the same amounts")
    void nothingPaidYet() {
        assertEquals(source.owing(LOAN, DUE_2), source.owing(LOAN, DUE_2));
    }

    @Test
    @DisplayName("the returned list cannot be edited by the caller")
    void resultIsUnmodifiable() {
        final List<InstalmentDue> owing = source.owing(LOAN, DUE_2);
        assertThrows(UnsupportedOperationException.class, () -> owing.add(owing.get(0)));
    }

    @Test
    @DisplayName("a loan with no saved schedule is refused with a typed exception, not an empty list")
    void unknownLoanRefused() {
        final ScheduleNotFoundException thrown = assertThrows(ScheduleNotFoundException.class,
            () -> source.owing(LoanId.of("LN-404"), DUE_1));
        assertTrue(thrown.getMessage().contains("LN-404"), thrown.getMessage());
    }

    @Test
    @DisplayName("missing arguments are refused")
    void nullsRefused() {
        assertThrows(NullPointerException.class, () -> new ScheduleInstalmentsDueSource(null));
        assertThrows(NullPointerException.class,
            () -> new ScheduleInstalmentsDueSource(schedules, null));
        assertThrows(NullPointerException.class, () -> source.owing(null, DUE_1));
        assertThrows(NullPointerException.class, () -> source.owing(LOAN, null));
    }
}
