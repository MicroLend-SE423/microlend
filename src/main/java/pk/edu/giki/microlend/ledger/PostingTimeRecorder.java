package pk.edu.giki.microlend.ledger;

import java.time.Duration;

/**
 * Receives the elapsed time of each posting, for the GQM measurement
 * programme (M1 design section 5.3).
 */
@FunctionalInterface
public interface PostingTimeRecorder {

    /**
     * Records one posting's duration.
     *
     * @param command the repayment that was posted
     * @param elapsed how long the wrapped posting took
     * @param succeeded false if the posting threw an exception
     */
    void record(RepaymentCommand command, Duration elapsed, boolean succeeded);
}
