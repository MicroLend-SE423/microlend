package pk.edu.giki.microlend.ledger;

import java.util.Objects;

/**
 * The Decorator of the posting pipeline (GoF Decorator; M1 design section 5.3).
 *
 * <p>A decorator both IS-A {@link RepaymentPoster} and HAS-A
 * {@code RepaymentPoster}: it adds one focused behaviour and forwards the
 * posting to the poster it wraps. Subclasses override {@link #post} and call
 * {@code super.post(command)} to forward. Chains are assembled at the
 * composition root, so each call path picks its own combination.</p>
 */
public abstract class RepaymentPosterDecorator implements RepaymentPoster {

    private final RepaymentPoster wrapped;

    /**
     * Wraps a poster.
     *
     * @param wrapped the poster to forward to; the core poster or another decorator
     * @throws NullPointerException if wrapped is null
     */
    protected RepaymentPosterDecorator(final RepaymentPoster wrapped) {
        this.wrapped = Objects.requireNonNull(wrapped, "the wrapped poster is required");
    }

    /**
     * Forwards the posting to the wrapped poster.
     *
     * @param command the repayment
     * @return the wrapped poster's result
     */
    @Override
    public PostingResult post(final RepaymentCommand command) {
        return wrapped.post(command);
    }
}
