package pk.edu.giki.microlend.origination;

/**
 * One eligibility rule from {@code docs/business-rules.md} section 2.
 *
 * <p>Each rule is its own class, so a new rule is a new implementation and one
 * line where the evaluator's list is built, not an edit to
 * {@link EligibilityEvaluator} (OCP, M1 design section 4.2).</p>
 */
@FunctionalInterface
public interface EligibilityRule {

    /**
     * Checks the application against this rule.
     *
     * @param application a validated application
     * @return passed, or failed with the reason and the business rule broken
     */
    RuleOutcome check(LoanApplication application);
}
