package pk.edu.giki.microlend;

import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import pk.edu.giki.microlend.ledger.AuditLoggingPoster;
import pk.edu.giki.microlend.ledger.AuditRecord;
import pk.edu.giki.microlend.ledger.CoreRepaymentPoster;
import pk.edu.giki.microlend.ledger.EarlySettlementRebate;
import pk.edu.giki.microlend.ledger.IdempotencyGuardPoster;
import pk.edu.giki.microlend.ledger.InMemoryPaymentReferences;
import pk.edu.giki.microlend.ledger.InstalmentsDueSource;
import pk.edu.giki.microlend.ledger.RepaymentAllocator;
import pk.edu.giki.microlend.ledger.RepaymentPoster;
import pk.edu.giki.microlend.ledger.SettlementPolicy;
import pk.edu.giki.microlend.ledger.TimingPoster;
import pk.edu.giki.microlend.persistence.SqliteDatabase;
import pk.edu.giki.microlend.persistence.SqliteLedgerRepository;
import pk.edu.giki.microlend.persistence.SqliteLoanRepository;

/**
 * The one place that calls {@code new} on infrastructure and assembles the
 * object graph (queue item L-17; M1 design sections 4.5 and 5.3).
 *
 * <p>Two production Decorator chains are built over one shared
 * {@link CoreRepaymentPoster}:</p>
 * <ul>
 *   <li>import chain: {@code IdempotencyGuardPoster -> TimingPoster -> Core};
 *       the guard is outermost so a duplicate is refused before anything is
 *       timed or posted;</li>
 *   <li>teller chain: {@code AuditLoggingPoster -> TimingPoster -> Core}.</li>
 * </ul>
 *
 * <p>Only components that exist are wired. What still comes from teammates'
 * modules is supplied through ledger role interfaces; {@link #withPlaceholders}
 * fills those with placeholders that fail with
 * {@link ModuleNotAvailableException}, see the TODOs below.</p>
 */
public final class CompositionRoot {

    private final List<AuditRecord> auditTrail = new CopyOnWriteArrayList<>();
    private final List<Duration> timings = new CopyOnWriteArrayList<>();
    private final RepaymentPoster importPoster;
    private final RepaymentPoster tellerPoster;

    /**
     * Wires both chains.
     *
     * @param database the local SQLite database
     * @param clock source of posting and audit timestamps
     * @param instalments what is still owed per instalment
     * @param rebates the per-product early-settlement rebate
     * @param operator the operator logged in at the teller screen
     * @throws NullPointerException if any argument is null
     */
    public CompositionRoot(final SqliteDatabase database, final Clock clock,
                           final InstalmentsDueSource instalments,
                           final EarlySettlementRebate rebates, final Supplier<String> operator) {
        final RepaymentPoster core = new CoreRepaymentPoster(new RepaymentAllocator(),
            new SettlementPolicy(rebates), new SqliteLoanRepository(database), instalments,
            new SqliteLedgerRepository(database), clock);
        this.importPoster = new IdempotencyGuardPoster(
            new TimingPoster(core, System::nanoTime, (c, elapsed, ok) -> timings.add(elapsed)),
            new InMemoryPaymentReferences());
        this.tellerPoster = new AuditLoggingPoster(
            new TimingPoster(core, System::nanoTime, (c, elapsed, ok) -> timings.add(elapsed)),
            operator, auditTrail::add, clock);
    }

    /**
     * Wires both chains with placeholders for the modules not built yet.
     *
     * @param database the local SQLite database
     * @param operator the operator logged in at the teller screen
     * @return the composition root
     */
    public static CompositionRoot withPlaceholders(final SqliteDatabase database,
                                                   final Supplier<String> operator) {
        Objects.requireNonNull(database, "database is required");
        // TODO(products, Hassan Khalid; delinquency, Muhammad Ibrahim): replace with a source
        //  that maps products.Instalment and the accrued penalty to ledger.InstalmentDue.
        final InstalmentsDueSource instalmentsNotAvailable = (loan, date) -> {
            throw new ModuleNotAvailableException("products", "Hassan Khalid",
                "listing the instalments owed on " + loan);
        };
        // TODO(products, Hassan Khalid): replace with the per-product BR-22 rebate.
        final EarlySettlementRebate rebateNotAvailable = (loan, date) -> {
            throw new ModuleNotAvailableException("products", "Hassan Khalid",
                "the early-settlement rebate for " + loan);
        };
        return new CompositionRoot(database, Clock.systemUTC(), instalmentsNotAvailable,
            rebateNotAvailable, operator);
    }

    /**
     * Returns the CSV-import chain: Idempotency -> Timing -> Core.
     *
     * @return the import poster
     */
    public RepaymentPoster importPoster() {
        return importPoster;
    }

    /**
     * Returns the teller chain: Audit -> Timing -> Core.
     *
     * @return the teller poster
     */
    public RepaymentPoster tellerPoster() {
        return tellerPoster;
    }

    /**
     * Returns the teller audit lines written since start-up (FR-31).
     *
     * @return an unmodifiable snapshot
     */
    public List<AuditRecord> auditTrail() {
        return List.copyOf(auditTrail);
    }

    /**
     * Returns the posting durations measured since start-up.
     *
     * @return an unmodifiable snapshot
     */
    public List<Duration> timings() {
        return List.copyOf(timings);
    }
}
