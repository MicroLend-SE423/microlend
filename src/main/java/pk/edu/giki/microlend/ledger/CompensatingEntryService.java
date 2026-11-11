package pk.edu.giki.microlend.ledger;

import java.time.Clock;
import java.util.Objects;

/**
 * The only way a posted ledger entry is corrected (BR-24, FR-17, QR-05).
 *
 * <p>Nothing is updated or deleted: the service appends a compensating
 * {@link EntryType#REVERSAL} entry that moves the same amount in the opposite
 * column and references the original, so the original stays in the ledger and
 * its effect nets to zero. It depends on the {@link LedgerWriter} role only
 * (ISP, DIP), and on a {@link Clock} so posting times are testable.</p>
 */
public final class CompensatingEntryService {

    private final LedgerWriter ledger;
    private final Clock clock;

    /**
     * Creates the service.
     *
     * @param ledger where the reversal is appended
     * @param clock the source of the reversal's posting time
     * @throws NullPointerException if either argument is null
     */
    public CompensatingEntryService(final LedgerWriter ledger, final Clock clock) {
        this.ledger = Objects.requireNonNull(ledger, "ledger writer is required");
        this.clock = Objects.requireNonNull(clock, "clock is required");
    }

    /**
     * Appends the reversal of a posted entry.
     *
     * @param entry the entry being corrected
     * @param reason why the correction is made; must not be blank
     * @return the reversal that was appended
     * @throws InvalidLedgerEntryException if the entry is null or the reason is blank;
     *     nothing is appended in that case
     */
    public LedgerEntry reverse(final LedgerEntry entry, final String reason) {
        final LedgerEntry reversal = LedgerEntry.reversalOf(entry, reason, clock.instant());
        ledger.append(reversal);
        return reversal;
    }
}
