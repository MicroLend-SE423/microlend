package pk.edu.giki.microlend.ledger;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;

/**
 * The concrete component of the posting pipeline (queue item L-08; BR-23,
 * FR-16, QR-03): read the loan, allocate the payment, settle, write balanced
 * ledger lines, save the loan.
 *
 * <p>Each posting writes one {@link EntryType#PAYMENT_RECEIVED} debit for the
 * amount received, balanced by one credit per non-zero tier and, for a
 * surplus while principal is owed, an {@link EntryType#ADVANCE_CREDIT} credit.
 * The lines are checked to balance before anything is written, and written
 * with {@link LedgerWriter#appendAll} so that a failure writes none of them.
 * The loan is saved only after the ledger write succeeded. Making the two
 * writes one database transaction is the job of the SQLite repositories
 * ({@code BEGIN IMMEDIATE}, queue item L-15).</p>
 *
 * <p>Not posted yet, because no rule or owning module defines them: money left
 * over after a payoff (the posting is refused instead) and the BR-22 rebate
 * line (reported in the result, pending the {@code products} implementation).</p>
 */
public final class CoreRepaymentPoster implements RepaymentPoster {

    private final RepaymentAllocator allocator;
    private final SettlementPolicy settlement;
    private final LoanRepository loans;
    private final InstalmentsDueSource instalments;
    private final LedgerWriter ledger;
    private final Clock clock;

    /**
     * Creates the poster from its collaborators (M1 design View 2).
     *
     * @param allocator splits the payment across tiers
     * @param settlement decides advance credit and payoff
     * @param loans reads and stores loan accounts
     * @param instalments says what is still owed
     * @param ledger the append-only ledger
     * @param clock source of posting timestamps
     * @throws NullPointerException if any collaborator is null
     */
    public CoreRepaymentPoster(final RepaymentAllocator allocator,
                               final SettlementPolicy settlement, final LoanRepository loans,
                               final InstalmentsDueSource instalments, final LedgerWriter ledger,
                               final Clock clock) {
        this.allocator = Objects.requireNonNull(allocator, "allocator is required");
        this.settlement = Objects.requireNonNull(settlement, "settlement policy is required");
        this.loans = Objects.requireNonNull(loans, "loan repository is required");
        this.instalments = Objects.requireNonNull(instalments, "instalment source is required");
        this.ledger = Objects.requireNonNull(ledger, "ledger writer is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    @Override
    public PostingResult post(final RepaymentCommand command) {
        Objects.requireNonNull(command, "command is required");
        final LoanId loanId = command.loanId();
        final LoanAccount account = loans.findById(loanId)
            .orElseThrow(() -> new LoanNotFoundException(loanId));
        account.ensureAcceptsPayment();

        final AllocationResult allocated = allocator.allocate(command.amount(),
            instalments.owing(loanId, command.businessDate()));
        final SettlementOutcome outcome = settlement.settle(
            account.applyAllocation(allocated.total()), allocated, command.businessDate());
        if (!outcome.excess().isZero()) {
            throw new InvalidRepaymentException("the payment exceeds the payoff amount by "
                + outcome.excess().toPlainString() + "; no rule yet says how an excess is"
                + " returned, so the payment was not posted",
                command.amount().toPlainString(), command.bank().orElse("cash"));
        }

        final List<LedgerEntry> lines = linesFor(command, allocated.total(), outcome);
        requireBalanced(loanId, lines);
        ledger.appendAll(lines);
        loans.save(outcome.account());
        return new PostingResult(command, allocated, outcome, lines);
    }

    private List<LedgerEntry> linesFor(final RepaymentCommand command, final Allocation tiers,
                                       final SettlementOutcome outcome) {
        final LoanId loanId = command.loanId();
        final Instant now = clock.instant();
        final List<LedgerEntry> lines = new ArrayList<>();
        lines.add(LedgerEntry.debit(loanId, EntryType.PAYMENT_RECEIVED, command.amount(), now));
        addCredit(lines, loanId, EntryType.PENALTY_REPAYMENT, tiers.toPenalty(), now);
        addCredit(lines, loanId, EntryType.FEE_REPAYMENT, tiers.toFees(), now);
        addCredit(lines, loanId, EntryType.INTEREST_REPAYMENT, tiers.toInterest(), now);
        addCredit(lines, loanId, EntryType.PRINCIPAL_REPAYMENT, tiers.toPrincipal(), now);
        addCredit(lines, loanId, EntryType.ADVANCE_CREDIT, outcome.advanceCredit(), now);
        return lines;
    }

    private static void addCredit(final List<LedgerEntry> lines, final LoanId loanId,
                                  final EntryType type, final Money amount, final Instant now) {
        if (!amount.isZero()) {
            lines.add(LedgerEntry.credit(loanId, type, amount, now));
        }
    }

    /**
     * Refuses a set of posting lines whose debits and credits differ (BR-23).
     *
     * @param loanId the loan the lines belong to, for the message
     * @param lines the lines of one posting
     * @throws LedgerImbalanceException if the debit and credit totals differ
     */
    static void requireBalanced(final LoanId loanId, final List<LedgerEntry> lines) {
        Money debits = Money.ZERO;
        Money credits = Money.ZERO;
        for (final LedgerEntry line : lines) {
            debits = debits.plus(line.debit());
            credits = credits.plus(line.credit());
        }
        if (debits.compareTo(credits) != 0) {
            throw new LedgerImbalanceException(loanId, debits, credits);
        }
    }
}
