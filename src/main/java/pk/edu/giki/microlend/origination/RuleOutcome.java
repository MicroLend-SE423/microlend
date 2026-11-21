package pk.edu.giki.microlend.origination;

import java.io.Serializable;
import java.util.Objects;

/**
 * The result of checking one {@link EligibilityRule}: passed, or failed with a
 * typed reason and a detail that names the business rule, for example
 * {@code "BR-02: age 66 at maturity is above 65"}.
 *
 * @param reason why the rule failed, or {@code null} when it passed
 * @param detail what was checked; names the business rule when the rule failed
 */
public record RuleOutcome(RejectionReason reason, String detail) implements Serializable {

    private static final RuleOutcome PASSED = new RuleOutcome(null, "passed");

    /**
     * Validates the outcome.
     *
     * @throws NullPointerException if the detail is missing
     */
    public RuleOutcome {
        Objects.requireNonNull(detail, "detail is required");
    }

    /**
     * Returns the outcome of a rule that passed.
     *
     * @return a passed outcome
     */
    public static RuleOutcome pass() {
        return PASSED;
    }

    /**
     * Returns the outcome of a rule that failed.
     *
     * @param reason the typed reason
     * @param detail names the business rule and the value that broke it
     * @return a failed outcome
     */
    public static RuleOutcome fail(final RejectionReason reason, final String detail) {
        Objects.requireNonNull(reason, "reason is required");
        return new RuleOutcome(reason, detail);
    }

    /**
     * Reports whether the rule passed.
     *
     * @return true when there is no rejection reason
     */
    public boolean passed() {
        return reason == null;
    }
}
