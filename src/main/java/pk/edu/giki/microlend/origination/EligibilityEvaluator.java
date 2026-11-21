package pk.edu.giki.microlend.origination;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import pk.edu.giki.microlend.common.Money;

/**
 * Decides whether a loan application is approved, and on what terms (FR-05,
 * FR-06).
 *
 * <p>The evaluator holds no rules of its own. It checks its list of
 * {@link EligibilityRule}s in order and <strong>stops at the first rule that
 * fails</strong>, because BR-08 requires "a typed rejection reason naming the
 * first rule that failed, evaluated in the order BR-01 … BR-07". A rejected
 * {@link Decision} therefore carries exactly one reason, and later rules are
 * not consulted.</p>
 *
 * <p>An application that passes every rule is given a {@link RiskGrade} from
 * its BR-04 burden and repayment history (BR-09), and is approved for the
 * amount that grade caps it at. If that capped amount is below the product
 * minimum, it is rejected with {@link RejectionReason#BELOW_PRODUCT_MINIMUM}
 * (BR-10).</p>
 */
public final class EligibilityEvaluator {

    private final List<EligibilityRule> rules;
    private final Function<LoanApplication, Money> newInstalment;

    /**
     * Creates an evaluator over the given rules.
     *
     * @param rules the rules, in the order they are to be checked
     * @param newInstalment returns the new loan's monthly instalment at the
     *     product's base rate, for the burden the risk grade uses (BR-04, BR-09)
     */
    public EligibilityEvaluator(final List<EligibilityRule> rules,
                                final Function<LoanApplication, Money> newInstalment) {
        this.rules = List.copyOf(rules);
        this.newInstalment = Objects.requireNonNull(newInstalment, "newInstalment is required");
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
            new ProductBandRule()), newInstalment);
    }

    /**
     * Evaluates an application.
     *
     * @param application a validated application
     * @return an approval with the grade and capped amount, or a rejection
     *     naming the first rule that failed
     */
    public Decision evaluate(final LoanApplication application) {
        Objects.requireNonNull(application, "application is required");
        for (final EligibilityRule rule : rules) {
            final RuleOutcome outcome = rule.check(application);
            if (!outcome.passed()) {
                return Decision.rejected(outcome);
            }
        }
        final Applicant applicant = application.applicant();
        final RiskGrade grade = RiskGrade.assign(
            application.burdenPercent(newInstalment.apply(application)),
            applicant.earlierLoans(), applicant.worstDaysPastDue());
        final Money capped = grade.cap(application.requestedAmount());
        final Money minimum = application.product().minAmount();
        if (capped.compareTo(minimum) < 0) {
            return Decision.rejected(RuleOutcome.fail(RejectionReason.BELOW_PRODUCT_MINIMUM,
                "BR-10: grade " + grade + " caps the amount at " + capped.toPlainString()
                    + ", below the product minimum " + minimum.toPlainString()));
        }
        return Decision.approved(grade, capped);
    }
}
