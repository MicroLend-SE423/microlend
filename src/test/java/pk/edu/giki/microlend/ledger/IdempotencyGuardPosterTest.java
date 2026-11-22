package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link IdempotencyGuardPoster} and {@link InMemoryPaymentReferences}
 * (queue item L-11, TC-LED-06; BR-25, FR-18).
 */
class IdempotencyGuardPosterTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate DAY = LocalDate.of(2026, 3, 20);
    private static final Money TEN = Money.of(new BigDecimal("10.00"));

    private final List<RepaymentCommand> forwarded = Collections.synchronizedList(new ArrayList<>());
    private final RepaymentPoster core = command -> {
        forwarded.add(command);
        return result(command);
    };
    private final IdempotencyGuardPoster guard =
        new IdempotencyGuardPoster(core, new InMemoryPaymentReferences());

    private static PostingResult result(final RepaymentCommand command) {
        return new PostingResult(command,
            new AllocationResult(Allocation.of(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO),
                Map.of(), Money.ZERO),
            new SettlementOutcome(SettlementOutcome.Kind.NO_SURPLUS,
                LoanAccount.active(LOAN, Money.ZERO), Money.ZERO, Money.ZERO, Money.ZERO),
            List.of());
    }

    private static RepaymentCommand bank(final String bank, final String reference) {
        return RepaymentCommand.bank(LOAN, TEN, DAY, bank, reference);
    }

    @Test
    @DisplayName("the same (bank, reference) pair twice: the second is rejected and not forwarded")
    void samePairTwice() {
        guard.post(bank("HBL", "TX-1"));
        final DuplicatePaymentException thrown = assertThrows(DuplicatePaymentException.class,
            () -> guard.post(bank("HBL", "TX-1")));
        assertEquals("HBL", thrown.bank());
        assertEquals("TX-1", thrown.reference());
        assertEquals(1, forwarded.size());
    }

    @Test
    @DisplayName("the same reference from two banks is two different payments")
    void sameReferenceTwoBanks() {
        guard.post(bank("HBL", "TX-1"));
        guard.post(bank("MCB", "TX-1"));
        assertEquals(2, forwarded.size());
    }

    @Test
    @DisplayName("cash payments carry no reference and are never rejected as duplicates")
    void cashIsNeverADuplicate() {
        final RepaymentCommand cash = RepaymentCommand.cash(LOAN, TEN, DAY);
        guard.post(cash);
        guard.post(cash);
        assertEquals(2, forwarded.size());
    }

    @Test
    @DisplayName("a posting that fails releases its reference, so a corrected retry is accepted")
    void failedPostingReleasesReference() {
        final AtomicInteger attempts = new AtomicInteger();
        final IdempotencyGuardPoster flaky = new IdempotencyGuardPoster(command -> {
            if (attempts.incrementAndGet() == 1) {
                throw new LoanNotFoundException(LOAN);
            }
            return result(command);
        }, new InMemoryPaymentReferences());
        assertThrows(LoanNotFoundException.class, () -> flaky.post(bank("HBL", "TX-9")));
        flaky.post(bank("HBL", "TX-9"));
        assertEquals(2, attempts.get());
    }

    @Test
    @DisplayName("eight threads posting the same pair at once forward it exactly once")
    void concurrentDuplicatesForwardOnce() throws Exception {
        final ExecutorService pool = Executors.newFixedThreadPool(8);
        final CountDownLatch start = new CountDownLatch(1);
        final List<Future<Boolean>> outcomes = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            outcomes.add(pool.submit(() -> {
                start.await();
                try {
                    guard.post(bank("HBL", "TX-RACE"));
                    return true;
                } catch (DuplicatePaymentException expected) {
                    return false;
                }
            }));
        }
        start.countDown();
        int accepted = 0;
        for (final Future<Boolean> outcome : outcomes) {
            accepted += outcome.get(10, TimeUnit.SECONDS) ? 1 : 0;
        }
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1, accepted);
        assertEquals(1, forwarded.size());
    }

    @Test
    @DisplayName("the guard is a RepaymentPosterDecorator and rejects missing collaborators")
    void structureAndNulls() {
        assertTrue(RepaymentPosterDecorator.class.isAssignableFrom(IdempotencyGuardPoster.class));
        assertThrows(NullPointerException.class, () -> new IdempotencyGuardPoster(core, null));
        assertThrows(NullPointerException.class, () -> guard.post(null));
    }
}
