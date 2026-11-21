package pk.edu.giki.microlend.origination;

import java.util.Objects;
import java.util.Optional;

/**
 * The result of evaluating a loan application: approved, or rejected with one
 * typed reason (BR-08).
 */
public final class Decision {

    private final RuleOutcome rejection;

    private Decision(final RuleOutcome rejection) {
        this.rejection = rejection;
    }

    /**
     * Returns an approval.
     *
     * @return an approved decision
     */
    public static Decision approved() {
        return new Decision(null);
    }

    /**
     * Returns a rejection.
     *
     * @param failure the failed outcome of the first rule that failed
     * @return a rejected decision
     * @throws IllegalArgumentException if the outcome passed
     */
    public static Decision rejected(final RuleOutcome failure) {
        Objects.requireNonNull(failure, "failure is required");
        if (failure.passed()) {
            throw new IllegalArgumentException("a passed outcome cannot reject an application");
        }
        return new Decision(failure);
    }

    /**
     * Reports whether the application was approved.
     *
     * @return true when approved
     */
    public boolean isApproved() {
        return rejection == null;
    }

    /**
     * Returns why the application was rejected.
     *
     * @return the reason, or empty when approved
     */
    public Optional<RejectionReason> reason() {
        return isApproved() ? Optional.empty() : Optional.of(rejection.reason());
    }

    /**
     * Returns this decision if it is an approval, for callers that treat a
     * rejection as an error.
     *
     * @return this decision
     * @throws IneligibleApplicantException if the application was rejected
     */
    public Decision requireApproved() {
        if (!isApproved()) {
            throw new IneligibleApplicantException(rejection);
        }
        return this;
    }
}
