package pk.edu.giki.microlend.ledger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link AuditLoggingPoster} (queue item L-10, TC-LED-09; FR-31).
 */
class AuditLoggingPosterTest {

    private static final Instant NOW = Instant.parse("2026-03-20T10:00:00Z");
    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final RepaymentCommand COMMAND = RepaymentCommand.cash(
        LOAN, Money.of(new BigDecimal("10.00")), LocalDate.of(2026, 3, 20));
    private static final PostingResult RESULT = new PostingResult(COMMAND,
        new AllocationResult(Allocation.of(Money.ZERO, Money.ZERO, Money.ZERO, Money.ZERO),
            Map.of(), Money.ZERO),
        new SettlementOutcome(SettlementOutcome.Kind.NO_SURPLUS,
            LoanAccount.active(LOAN, Money.ZERO), Money.ZERO, Money.ZERO, Money.ZERO),
        List.of());

    private final List<AuditRecord> journal = new ArrayList<>();
    private final List<RepaymentCommand> forwarded = new ArrayList<>();
    private String operator = "teller.amina";

    private AuditLoggingPoster audited(final RepaymentPoster inner) {
        return new AuditLoggingPoster(inner, () -> operator, journal::add,
            Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private final RepaymentPoster succeeding = command -> {
        forwarded.add(command);
        return RESULT;
    };

    @Test
    @DisplayName("a successful posting writes one audit line naming the operator")
    void auditsSuccess() {
        assertSame(RESULT, audited(succeeding).post(COMMAND));
        assertEquals(List.of(new AuditRecord("teller.amina", COMMAND, NOW, true, "posted")),
            journal);
        assertEquals(List.of(COMMAND), forwarded);
    }

    @Test
    @DisplayName("a refused posting is audited with the reason, then rethrown")
    void auditsRefusal() {
        final LoanNotFoundException refusal = new LoanNotFoundException(LOAN);
        final AuditLoggingPoster poster = audited(command -> {
            throw refusal;
        });
        assertSame(refusal, assertThrows(LoanNotFoundException.class, () -> poster.post(COMMAND)));
        assertEquals(List.of(new AuditRecord("teller.amina", COMMAND, NOW, false,
            refusal.getMessage())), journal);
    }

    @Test
    @DisplayName("operator names are trimmed")
    void trimsOperator() {
        operator = "  teller.amina ";
        audited(succeeding).post(COMMAND);
        assertEquals("teller.amina", journal.get(0).operator());
    }

    @Test
    @DisplayName("with no operator named, nothing is forwarded or audited (FR-31)")
    void refusesAnonymousPosting() {
        for (final String missing : new String[] {null, "", "   "}) {
            operator = missing;
            assertThrows(InvalidRepaymentException.class, () -> audited(succeeding).post(COMMAND));
        }
        assertTrue(forwarded.isEmpty());
        assertTrue(journal.isEmpty());
    }

    @Test
    @DisplayName("the decorator is a RepaymentPosterDecorator")
    void isADecorator() {
        assertTrue(RepaymentPosterDecorator.class.isAssignableFrom(AuditLoggingPoster.class));
    }

    @Test
    @DisplayName("missing collaborators are rejected")
    void rejectsNulls() {
        final Clock clock = Clock.systemUTC();
        assertThrows(NullPointerException.class,
            () -> new AuditLoggingPoster(succeeding, null, journal::add, clock));
        assertThrows(NullPointerException.class,
            () -> new AuditLoggingPoster(succeeding, () -> "x", null, clock));
        assertThrows(NullPointerException.class,
            () -> new AuditLoggingPoster(succeeding, () -> "x", journal::add, null));
    }
}
