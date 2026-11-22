package pk.edu.giki.microlend;

import java.io.IOException;
import java.io.Reader;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Supplier;
import pk.edu.giki.microlend.common.LockRegistry;
import pk.edu.giki.microlend.delinquency.EndOfDayEngine;
import pk.edu.giki.microlend.ledger.AuditLoggingPoster;
import pk.edu.giki.microlend.ledger.AuditRecord;
import pk.edu.giki.microlend.ledger.CoreRepaymentPoster;
import pk.edu.giki.microlend.ledger.EarlySettlementRebate;
import pk.edu.giki.microlend.ledger.IdempotencyGuardPoster;
import pk.edu.giki.microlend.ledger.InstalmentsDueSource;
import pk.edu.giki.microlend.ledger.LockingPoster;
import pk.edu.giki.microlend.ledger.RepaymentAllocator;
import pk.edu.giki.microlend.ledger.RepaymentPoster;
import pk.edu.giki.microlend.ledger.SettlementPolicy;
import pk.edu.giki.microlend.ledger.TimingPoster;
import pk.edu.giki.microlend.ledger.importing.HblStatementAdapter;
import pk.edu.giki.microlend.ledger.importing.ImportReport;
import pk.edu.giki.microlend.ledger.importing.InvalidStatementException;
import pk.edu.giki.microlend.ledger.importing.McbStatementAdapter;
import pk.edu.giki.microlend.ledger.importing.RepaymentSource;
import pk.edu.giki.microlend.ledger.importing.StatementImportJob;
import pk.edu.giki.microlend.persistence.SqliteDailyResultStore;
import pk.edu.giki.microlend.persistence.SqliteDatabase;
import pk.edu.giki.microlend.persistence.SqliteLedgerRepository;
import pk.edu.giki.microlend.persistence.SqliteLoanRepository;
import pk.edu.giki.microlend.persistence.SqlitePaymentReferences;
import pk.edu.giki.microlend.persistence.SqliteProductCatalogue;
import pk.edu.giki.microlend.persistence.SqliteScheduleRepository;
import pk.edu.giki.microlend.products.ProductCatalogue;
import pk.edu.giki.microlend.products.ScheduleRepository;

/**
 * The one place that calls {@code new} on infrastructure and assembles the
 * object graph (queue item L-17; M1 design sections 4.5 and 5.3).
 *
 * <p>Two production Decorator chains are built over one shared
 * {@link CoreRepaymentPoster}:</p>
 * <ul>
 *   <li>import chain: {@code IdempotencyGuardPoster -> TimingPoster -> LockingPoster
 *       -> Core}; the guard is outermost so a duplicate is refused before anything is
 *       timed or posted, and its claimed pairs are kept in SQLite so a duplicate is
 *       refused across restarts;</li>
 *   <li>teller chain: {@code AuditLoggingPoster -> TimingPoster -> LockingPoster
 *       -> Core}.</li>
 * </ul>
 *
 * <p>The {@link EndOfDayEngine} shares one {@link LockRegistry} with both chains, so a
 * posting and the engine's work on the same loan never interleave (M1 section 3.6).</p>
 *
 * <p>What comes from other modules is supplied through ledger role interfaces;
 * {@link #production} fills them from {@code products} (schedule and rebate).</p>
 */
public final class CompositionRoot {

    private final List<AuditRecord> auditTrail = new CopyOnWriteArrayList<>();
    private final List<Duration> timings = new CopyOnWriteArrayList<>();
    private final RepaymentPoster importPoster;
    private final RepaymentPoster tellerPoster;
    private final EndOfDayEngine endOfDayEngine;

    /**
     * Wires both chains and the end-of-day engine.
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
        final LockRegistry locks = new LockRegistry();
        final RepaymentPoster core = new LockingPoster(new CoreRepaymentPoster(
            new RepaymentAllocator(), new SettlementPolicy(rebates),
            new SqliteLoanRepository(database), instalments,
            new SqliteLedgerRepository(database), clock), locks);
        this.endOfDayEngine = new EndOfDayEngine(new SqliteDailyResultStore(database),
            instalments, locks, clock);
        this.importPoster = new IdempotencyGuardPoster(
            new TimingPoster(core, System::nanoTime, (c, elapsed, ok) -> timings.add(elapsed)),
            new SqlitePaymentReferences(database));
        this.tellerPoster = new AuditLoggingPoster(
            new TimingPoster(core, System::nanoTime, (c, elapsed, ok) -> timings.add(elapsed)),
            operator, auditTrail::add, clock);
    }

    /**
     * Wires both chains and the end-of-day engine to the real schedule and product data on
     * SQLite (queue items H-07 and H-08, decisions D-02 and D-03).
     *
     * <p>Instalments owed come from {@link ScheduleInstalmentsDueSource} and the
     * early-settlement rebate from {@link ProductEarlySettlementRebate}. The penalty is not
     * wired yet: it stays zero until the delinquency side supplies a
     * {@link ScheduleInstalmentsDueSource.PenaltyLookup}.</p>
     *
     * @param database the local SQLite database
     * @param operator the operator logged in at the teller screen
     * @return the composition root
     * @throws NullPointerException if an argument is null
     */
    public static CompositionRoot production(final SqliteDatabase database,
                                             final Supplier<String> operator) {
        Objects.requireNonNull(database, "database is required");
        final ScheduleRepository schedules = new SqliteScheduleRepository(database);
        final ProductCatalogue catalogue = new SqliteProductCatalogue(database);
        return new CompositionRoot(database, Clock.systemUTC(),
            new ScheduleInstalmentsDueSource(schedules),
            new ProductEarlySettlementRebate(catalogue, schedules), operator);
    }

    /**
     * Imports one bank statement through the import chain (I-04, FR-20). The bank name
     * selects the Adapter; the job and the chain never see a bank format.
     *
     * @param bank {@code HBL} or {@code MCB}
     * @param statement the statement text; the caller closes it
     * @param sourceName the file name, quoted in any error
     * @return how many rows were posted and which were refused as duplicates (BR-25)
     * @throws IOException if the statement cannot be read
     * @throws InvalidStatementException if no adapter exists for the bank, or the file is
     *     malformed; nothing from a malformed file is posted
     */
    public ImportReport importStatement(final String bank, final Reader statement,
                                        final String sourceName) throws IOException {
        final RepaymentSource source;
        if ("HBL".equals(bank)) {
            source = new HblStatementAdapter(statement, sourceName);
        } else if ("MCB".equals(bank)) {
            source = new McbStatementAdapter(statement, sourceName);
        } else {
            throw new InvalidStatementException(sourceName, 1,
                "no statement adapter for bank '" + bank + "'; expected HBL or MCB");
        }
        return new StatementImportJob(source, importPoster).run();
    }

    /**
     * Returns the end-of-day engine, which shares the account locks with both chains.
     *
     * @return the engine
     */
    public EndOfDayEngine endOfDayEngine() {
        return endOfDayEngine;
    }

    /**
     * Returns the CSV-import chain: Idempotency -> Timing -> Locking -> Core.
     *
     * @return the import poster
     */
    public RepaymentPoster importPoster() {
        return importPoster;
    }

    /**
     * Returns the teller chain: Audit -> Timing -> Locking -> Core.
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
