package pk.edu.giki.microlend.origination;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import pk.edu.giki.microlend.common.Money;

/**
 * Decides whether a loan application is approved (FR-05).
 *
 * <p>The evaluator holds no rules of its own. It checks its list of
 * {@link EligibilityRule}s in order and <strong>stops at the first rule that
 * fails</strong>, because BR-08 requires "a typed rejection reason naming the
 * first rule that failed, evaluated in the order BR-01 … BR-07". A rejected
 * {@link Decision} therefore carries exactly one reason, and later rules are
 * not consulted.</p>
 */
public final class EligibilityEvaluator {

    private final List<EligibilityRule> rules;

    /**
     * Creates an evaluator over the given rules.
     *
     * @param rules the rules, in the order they are to be checked
     */
    public EligibilityEvaluator(final List<EligibilityRule> rules) {
        this.rules = List.copyOf(rules);
    }

    /**
     * Creates the evaluator with the five rules of {@code docs/business-rules.md}
     * section 2, in the order BR-08 requires: BR-01 and BR-02, BR-03, BR-04,
     * BR-05, then BR-06 and BR-07.
     *
     * @param newInstalment returns the new loan's monthly instalment at the
     *     product's base rate (BR-04)
     * @return the evaluator
     */
    public static EligibilityEvaluator standard(final Function<LoanApplication, Money> newInstalment) {
        return new EligibilityEvaluator(List.of(
            new AgeRule(),
            new ActiveLoanLimitRule(),
            new BurdenRatioRule(newInstalment),
            new WriteOffHistoryRule(),
            new ProductBandRule()));
    }

    /**
     * Evaluates an application.
     *
     * @param application a validated application
     * @return an approval, or a rejection naming the first rule that failed
     */
    public Decision evaluate(final LoanApplication application) {
        Objects.requireNonNull(application, "application is required");
        for (final EligibilityRule rule : rules) {
            final RuleOutcome outcome = rule.check(application);
            if (!outcome.passed()) {
                return Decision.rejected(outcome);
            }
        }
        return Decision.approved();
    }
}
