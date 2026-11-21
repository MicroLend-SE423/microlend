package pk.edu.giki.microlend.ledger.importing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import pk.edu.giki.microlend.ledger.DuplicatePaymentException;
import pk.edu.giki.microlend.ledger.RepaymentCommand;
import pk.edu.giki.microlend.ledger.RepaymentPoster;

/**
 * Posts every repayment from one bank statement: the Client of the Adapter
 * pattern (queue item I-04; M1 design section 5.2; FR-20).
 *
 * <p>It sees only a {@link RepaymentSource} and a {@link RepaymentPoster},
 * never a bank format. In production the poster is the composition root's
 * import chain, with {@code IdempotencyGuardPoster} outermost.</p>
 *
 * <p>Duplicates (BR-25, FR-18): a row whose (bank, reference) pair was already
 * posted is refused, listed in the report, and the rest of the file is still
 * posted. Banks send statements whose dates overlap, so a repeated row is
 * expected and must not block the new ones. Any other refusal, for example a
 * closed loan (BR-26), stops the import with that exception; the rows before
 * it stay posted, and re-running the corrected file refuses them as
 * duplicates while the guard still holds their references.</p>
 */
public final class StatementImportJob {

    private final RepaymentSource source;
    private final RepaymentPoster poster;

    /**
     * Creates the job for one statement.
     *
     * @param source the adapted statement
     * @param poster the posting chain; production passes {@code CompositionRoot.importPoster()}
     * @throws NullPointerException if any argument is null
     */
    public StatementImportJob(final RepaymentSource source, final RepaymentPoster poster) {
        this.source = Objects.requireNonNull(source, "source is required");
        this.poster = Objects.requireNonNull(poster, "poster is required");
    }

    /**
     * Posts every command the source yields, batch by batch, until it returns
     * an empty batch.
     *
     * @return how many were posted and which references were refused as duplicates
     * @throws InvalidStatementException if the statement is malformed; the bank
     *     adapters read the whole file first, so nothing from it is posted
     * @throws pk.edu.giki.microlend.common.LoanDomainException if the poster
     *     refuses a row for any reason other than a duplicate
     */
    public ImportReport run() {
        int posted = 0;
        final List<String> duplicates = new ArrayList<>();
        for (List<RepaymentCommand> batch = source.nextBatch(); !batch.isEmpty();
             batch = source.nextBatch()) {
            for (final RepaymentCommand command : batch) {
                try {
                    poster.post(command);
                    posted++;
                } catch (final DuplicatePaymentException duplicate) {
                    duplicates.add(duplicate.reference());
                }
            }
        }
        return new ImportReport(posted, duplicates);
    }
}
