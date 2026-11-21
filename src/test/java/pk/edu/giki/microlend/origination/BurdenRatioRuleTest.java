package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.money;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link BurdenRatioRule}: FR-03, BR-04, black-box cases TC-ORG-03.1
 * to TC-ORG-03.3 (TC-ORG-03.4 needs a risk grade and is in
 * {@link EligibilityEvaluatorTest}). Declared income is 100,000.00, so 1,000.00
 * of instalments is one percentage point. The new loan's instalment comes from
 * the injected provider, as {@code products} will supply it.
 */
class BurdenRatioRuleTest {

    @ParameterizedTest(name = "{0}: existing {1} + new {2} on income 100,000.00, passes BR-04 = {3}")
    @CsvSource({
        "TC-ORG-03.1, 0.00, 39990.00, true",
        "TC-ORG-03.2, 30000.00, 10000.00, true",
        "TC-ORG-03.3, 0.00, 40010.00, false"})
    void burdenAtTheLimit(final String testCase, final String existing,
                          final String newInstalment, final boolean passes) {
        final BurdenRatioRule rule = new BurdenRatioRule(application -> money(newInstalment));
        final LoanApplication application =
            new ApplicationBuilder().existingInstalments(existing).build();
        assertEquals(passes ? null : RejectionReason.BURDEN_RATIO,
            rule.check(application).reason());
    }

    @Test
    @DisplayName("BR-04: one paisa over 40% exceeds the limit and is rejected")
    void onePaisaOverTheLimit() {
        final BurdenRatioRule rule = new BurdenRatioRule(application -> money("40000.01"));
        assertEquals(RejectionReason.BURDEN_RATIO,
            rule.check(new ApplicationBuilder().build()).reason());
    }
}
