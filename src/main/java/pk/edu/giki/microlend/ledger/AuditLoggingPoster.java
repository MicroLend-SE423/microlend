package pk.edu.giki.microlend.ledger;

import java.time.Clock;
import java.util.Objects;
import java.util.function.Supplier;
import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Decorator that writes an audit line naming the operator for every teller
 * posting (queue item L-10; FR-31; M1 design section 5.3).
 *
 * <p>A posting with no operator named is refused before it is forwarded,
 * because FR-31 requires every teller posting to name one. Both posted and
 * refused attempts are journalled; a refusal is rethrown unchanged.</p>
 */
public final class AuditLoggingPoster extends RepaymentPosterDecorator {

    private final Supplier<String> operator;
    private final AuditJournal journal;
    private final Clock clock;

    /**
     * Creates the decorator.
     *
     * @param wrapped the poster being audited
     * @param operator the operator logged in at the teller screen
     * @param journal where audit lines go
     * @param clock source of audit timestamps
     * @throws NullPointerException if any argument is null
     */
    public AuditLoggingPoster(final RepaymentPoster wrapped, final Supplier<String> operator,
                              final AuditJournal journal, final Clock clock) {
        super(wrapped);
        this.operator = Objects.requireNonNull(operator, "an operator source is required");
        this.journal = Objects.requireNonNull(journal, "an audit journal is required");
        this.clock = Objects.requireNonNull(clock, "a clock is required");
    }

    @Override
    public PostingResult post(final RepaymentCommand command) {
        final String name = operator.get();
        if (name == null || name.trim().isEmpty()) {
            throw new InvalidRepaymentException(
                "a teller posting must name its operator (FR-31)", String.valueOf(name),
                command == null ? "unknown" : command.bank().orElse("cash"));
        }
        final String who = name.trim();
        try {
            final PostingResult result = super.post(command);
            journal.record(new AuditRecord(who, command, clock.instant(), true, "posted"));
            return result;
        } catch (LoanDomainException refused) {
            journal.record(new AuditRecord(who, command, clock.instant(), false,
                refused.getMessage()));
            throw refused;
        }
    }
}
