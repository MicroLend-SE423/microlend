package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link ActiveLoanLimitRule}: FR-02, BR-03, black-box cases
 * TC-ORG-02.1 to TC-ORG-02.6. The input is the number of existing active loans,
 * not counting the one applied for; a loan in arrears is active, a written-off
 * loan is not.
 */
class ActiveLoanLimitRuleTest {

    private final ActiveLoanLimitRule rule = new ActiveLoanLimitRule();

    @ParameterizedTest(name = "{0}: {1} existing active loan(s), passes BR-03 = {2}")
    @CsvSource({
        "TC-ORG-02.1, 0, true",
        "TC-ORG-02.2, 1, true",
        "TC-ORG-02.3, 2, false",
        "TC-ORG-02.4, 3, false"})
    void existingActiveLoans(final String testCase, final int active, final boolean passes) {
        final LoanApplication application =
            new ApplicationBuilder().history(active, active, 0, false).build();
        assertEquals(passes ? null : RejectionReason.ACTIVE_LOAN_LIMIT,
            rule.check(application).reason());
    }

    @Test
    @DisplayName("TC-ORG-02.5: 1 active + 1 written off passes BR-03 (a written-off loan is not active)")
    void writtenOffLoanIsNotActive() {
        final LoanApplication application =
            new ApplicationBuilder().history(1, 2, 0, true).build();
        assertTrue(rule.check(application).passed());
    }

    @Test
    @DisplayName("TC-ORG-02.6: 1 active in arrears + 1 active is 2 active loans, rejected")
    void loanInArrearsIsActive() {
        final LoanApplication application =
            new ApplicationBuilder().history(2, 2, 45, false).build();
        assertEquals(RejectionReason.ACTIVE_LOAN_LIMIT, rule.check(application).reason());
    }
}
