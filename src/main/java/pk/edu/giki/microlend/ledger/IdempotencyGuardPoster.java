package pk.edu.giki.microlend.ledger;

import java.util.Objects;

/**
 * Decorator that rejects a bank payment whose (bank, reference) pair was
 * already posted (queue item L-11; BR-25, FR-18; M1 design section 5.3).
 *
 * <p>Wherever it is used it wraps outermost, so a duplicate is refused before
 * anything is timed or posted. Cash payments carry no reference and pass
 * straight through. The pair is claimed atomically before forwarding and
 * released if the posting fails, so a corrected retry is accepted.</p>
 */
public final class IdempotencyGuardPoster extends RepaymentPosterDecorator {

    private final PaymentReferences references;

    /**
     * Creates the guard.
     *
     * @param wrapped the poster being guarded
     * @param references the record of posted pairs
     * @throws NullPointerException if any argument is null
     */
    public IdempotencyGuardPoster(final RepaymentPoster wrapped,
                                  final PaymentReferences references) {
        super(wrapped);
        this.references = Objects.requireNonNull(references, "payment references are required");
    }

    @Override
    public PostingResult post(final RepaymentCommand command) {
        Objects.requireNonNull(command, "command is required");
        if (command.isCash()) {
            return super.post(command);
        }
        final String bank = command.bank().orElseThrow();
        final String reference = command.reference().orElseThrow();
        if (!references.claim(bank, reference)) {
            throw new DuplicatePaymentException(bank, reference);
        }
        boolean posted = false;
        try {
            final PostingResult result = super.post(command);
            posted = true;
            return result;
        } finally {
            if (!posted) {
                references.release(bank, reference);
            }
        }
    }
}
