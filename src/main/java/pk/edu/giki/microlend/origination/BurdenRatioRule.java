package pk.edu.giki.microlend.origination;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.function.Function;
import pk.edu.giki.microlend.common.Money;

/**
 * BR-04 (FR-03): the applicant's monthly instalments, including the new loan,
 * must not exceed 40% of declared monthly income.
 *
 * <p>The new loan's instalment is computed on the requested amount at the
 * product's base rate, before any risk-grade spread or cap. Schedules belong to
 * {@code products}, so this rule does not compute it: it is given a function
 * that returns it, injected through the constructor.</p>
 */
public final class BurdenRatioRule implements EligibilityRule {

    private static final BigDecimal MAXIMUM_BURDEN_PERCENT = new BigDecimal("40");

    private final Function<LoanApplication, Money> newInstalment;

    /**
     * Creates the rule.
     *
     * @param newInstalment returns the monthly instalment of the loan applied for,
     *     on the requested amount at the product's base rate
     */
    public BurdenRatioRule(final Function<LoanApplication, Money> newInstalment) {
        this.newInstalment = Objects.requireNonNull(newInstalment, "newInstalment is required");
    }

    @Override
    public RuleOutcome check(final LoanApplication application) {
        final BigDecimal burden = application.burdenPercent(newInstalment.apply(application));
        if (burden.compareTo(MAXIMUM_BURDEN_PERCENT) > 0) {
            return RuleOutcome.fail(RejectionReason.BURDEN_RATIO, "BR-04: instalment burden "
                + burden.toPlainString() + "% is above " + MAXIMUM_BURDEN_PERCENT + "%");
        }
        return RuleOutcome.pass();
    }
}
