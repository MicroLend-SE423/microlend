package pk.edu.giki.microlend.ledger;

import java.time.Duration;
import java.util.Objects;
import java.util.function.LongSupplier;

/**
 * Decorator that measures how long each posting takes (queue item L-09,
 * M1 design section 5.3). Both a successful and a refused posting are
 * recorded; the result or exception passes through unchanged.
 */
public final class TimingPoster extends RepaymentPosterDecorator {

    private final LongSupplier nanoTicker;
    private final PostingTimeRecorder recorder;

    /**
     * Creates the decorator.
     *
     * @param wrapped the poster being timed
     * @param nanoTicker a monotonic nanosecond source, normally {@code System::nanoTime}
     * @param recorder where each measurement goes
     * @throws NullPointerException if any argument is null
     */
    public TimingPoster(final RepaymentPoster wrapped, final LongSupplier nanoTicker,
                        final PostingTimeRecorder recorder) {
        super(wrapped);
        this.nanoTicker = Objects.requireNonNull(nanoTicker, "a ticker is required");
        this.recorder = Objects.requireNonNull(recorder, "a recorder is required");
    }

    @Override
    public PostingResult post(final RepaymentCommand command) {
        final long start = nanoTicker.getAsLong();
        boolean succeeded = false;
        try {
            final PostingResult result = super.post(command);
            succeeded = true;
            return result;
        } finally {
            recorder.record(command, Duration.ofNanos(nanoTicker.getAsLong() - start), succeeded);
        }
    }
}
