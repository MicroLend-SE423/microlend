package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.DISBURSEMENT;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link AgeRule}: FR-01, BR-01 and BR-02, black-box cases TC-ORG-01.1
 * to TC-ORG-01.7. Ages are completed years; the default tenure is 12 months, so
 * maturity is 1 Dec 2027.
 */
class AgeRuleTest {

    private static final LocalDate MATURITY = DISBURSEMENT.plusMonths(12);

    private final AgeRule rule = new AgeRule();

    @ParameterizedTest(name = "{0}: age {1} at disbursement, passes BR-01 = {2}")
    @CsvSource({"TC-ORG-01.1, 17, false", "TC-ORG-01.2, 18, true", "TC-ORG-01.3, 19, true"})
    void ageAtDisbursement(final String testCase, final int age, final boolean passes) {
        final LoanApplication application =
            new ApplicationBuilder().bornOn(DISBURSEMENT.minusYears(age)).build();
        assertEquals(passes ? null : RejectionReason.AGE, rule.check(application).reason());
    }

    @Test
    @DisplayName("BR-01: the day before the 18th birthday is still 17, rejected")
    void dayBeforeEighteenthBirthday() {
        final LoanApplication application = new ApplicationBuilder()
            .bornOn(DISBURSEMENT.minusYears(18).plusDays(1)).build();
        final RuleOutcome outcome = rule.check(application);
        assertEquals(RejectionReason.AGE, outcome.reason());
        assertTrue(outcome.detail().startsWith("BR-01"), outcome.detail());
    }

    @ParameterizedTest(name = "{0}: age {1} at maturity, passes BR-02 = {2}")
    @CsvSource({"TC-ORG-01.4, 64, true", "TC-ORG-01.5, 65, true", "TC-ORG-01.6, 66, false"})
    void ageAtMaturity(final String testCase, final int age, final boolean passes) {
        final LoanApplication application =
            new ApplicationBuilder().bornOn(MATURITY.minusYears(age)).build();
        assertEquals(passes ? null : RejectionReason.AGE, rule.check(application).reason());
    }

    @Test
    @DisplayName("BR-02: one day short of 66 at maturity is still 65 in completed years, passes")
    void dayBeforeSixtySixthBirthday() {
        final LoanApplication application = new ApplicationBuilder()
            .bornOn(MATURITY.minusYears(66).plusDays(1)).build();
        assertTrue(rule.check(application).passed());
    }

    @Test
    @DisplayName("TC-ORG-01.7: age 40 at application but 66 at maturity is rejected under BR-02")
    void ageIsTestedAtMaturityNotApplication() {
        final LoanApplication application = new ApplicationBuilder()
            .bornOn(DISBURSEMENT.minusYears(40)).tenure(26 * 12).build();
        final RuleOutcome outcome = rule.check(application);
        assertEquals(RejectionReason.AGE, outcome.reason());
        assertTrue(outcome.detail().startsWith("BR-02"), outcome.detail());
    }
}
