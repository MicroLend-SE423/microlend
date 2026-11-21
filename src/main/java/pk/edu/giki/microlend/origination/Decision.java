package pk.edu.giki.microlend.origination;

import java.util.Objects;
import java.util.Optional;
import pk.edu.giki.microlend.common.Money;

/**
 * The result of evaluating a loan application: approved with a risk grade and
 * the capped amount (BR-09, BR-10), or rejected with one typed reason (BR-08).
 *
 * <p>The rate spread is the grade's: {@code grade().get().spreadPercent()}.</p>
 */
public final class Decision {

    private final RuleOutcome rejection;
    private final RiskGrade grade;
    private final Money approvedAmount;

    private Decision(final RuleOutcome rejection, final RiskGrade grade, final Money approvedAmount) {
        this.rejection = rejection;
        this.grade = grade;
        this.approvedAmount = approvedAmount;
    }

    /**
     * Returns an approval.
     *
     * @param grade the applicant's risk grade
     * @param approvedAmount the amount approved, already capped by the grade
     * @return an approved decision
     */
    public static Decision approved(final RiskGrade grade, final Money approvedAmount) {
        Objects.requireNonNull(grade, "grade is required");
        Objects.requireNonNull(approvedAmount, "approvedAmount is required");
        return new Decision(null, grade, approvedAmount);
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
        return new Decision(failure, null, null);
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
     * Returns the applicant's risk grade.
     *
     * @return the grade, or empty when rejected
     */
    public Optional<RiskGrade> grade() {
        return Optional.ofNullable(grade);
    }

    /**
     * Returns the amount approved, which is the capped amount, not the requested
     * one (BR-10).
     *
     * @return the approved amount, or empty when rejected
     */
    public Optional<Money> approvedAmount() {
        return Optional.ofNullable(approvedAmount);
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
