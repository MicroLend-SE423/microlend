package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for the Decorator base {@link RepaymentPosterDecorator} and
 * {@link TimingPoster} (queue item L-09, M1 design section 5.3).
 */
class TimingPosterTest {

    private static final RepaymentCommand COMMAND = RepaymentCommand.cash(
        LoanId.of("LN-1"), Money.of(new BigDecimal("10.00")), LocalDate.of(2026, 3, 20));

    private static final PostingResult RESULT = new PostingResult(COMMAND,
        new AllocationResult(Allocation.of(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO),
            Map.of(), Money.ZERO),
        new SettlementOutcome(SettlementOutcome.Kind.NO_SURPLUS,
            LoanAccount.active(LoanId.of("LN-1"), Money.ZERO),
            Money.ZERO, Money.ZERO, Money.ZERO),
        List.of());

    private record Timing(RepaymentCommand command, Duration elapsed, boolean succeeded) { }

    private final List<Timing> timings = new ArrayList<>();
    private final Deque<Long> ticks = new ArrayDeque<>(List.of(1_000L, 251_000L));

    private TimingPoster timing(final RepaymentPoster inner) {
        return new TimingPoster(inner, ticks::removeFirst,
            (command, elapsed, succeeded) -> timings.add(new Timing(command, elapsed, succeeded)));
    }

    @Test
    @DisplayName("the decorator base IS-A RepaymentPoster and HAS-A RepaymentPoster")
    void decoratorStructure() {
        assertTrue(RepaymentPoster.class.isAssignableFrom(RepaymentPosterDecorator.class));
        assertTrue(Modifier.isAbstract(RepaymentPosterDecorator.class.getModifiers()));
        final List<Class<?>> fieldTypes = Arrays.stream(
                RepaymentPosterDecorator.class.getDeclaredFields())
            .map(Field::getType).<Class<?>>map(type -> type).toList();
        assertEquals(List.of(RepaymentPoster.class), fieldTypes);
        assertTrue(RepaymentPosterDecorator.class.isAssignableFrom(TimingPoster.class));
    }

    @Test
    @DisplayName("delegates to the wrapped poster and returns its result unchanged")
    void delegates() {
        final List<RepaymentCommand> seen = new ArrayList<>();
        final PostingResult result = timing(command -> {
            seen.add(command);
            return RESULT;
        }).post(COMMAND);
        assertSame(RESULT, result);
        assertEquals(List.of(COMMAND), seen);
    }

    @Test
    @DisplayName("records the elapsed time of a successful posting")
    void recordsSuccess() {
        timing(command -> RESULT).post(COMMAND);
        assertEquals(List.of(new Timing(COMMAND, Duration.ofNanos(250_000), true)), timings);
    }

    @Test
    @DisplayName("records a failed posting too, then rethrows the same exception")
    void recordsFailure() {
        final LoanNotFoundException failure = new LoanNotFoundException(LoanId.of("LN-1"));
        final TimingPoster poster = timing(command -> {
            throw failure;
        });
        assertSame(failure, assertThrows(LoanNotFoundException.class, () -> poster.post(COMMAND)));
        assertEquals(List.of(new Timing(COMMAND, Duration.ofNanos(250_000), false)), timings);
    }

    @Test
    @DisplayName("missing collaborators are rejected")
    void rejectsNulls() {
        assertThrows(NullPointerException.class,
            () -> new TimingPoster(null, System::nanoTime, (c, e, s) -> { }));
        assertThrows(NullPointerException.class,
            () -> new TimingPoster(command -> RESULT, null, (c, e, s) -> { }));
        assertThrows(NullPointerException.class,
            () -> new TimingPoster(command -> RESULT, System::nanoTime, null));
    }
}
