package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link ProductBandRule}: FR-07, BR-06 and BR-07, black-box cases
 * TC-ORG-07.1 to TC-ORG-07.7. The test product's band is 10,000.00 to
 * 100,000.00 and 6 to 24 months, both inclusive.
 */
class ProductBandRuleTest {

    private final ProductBandRule rule = new ProductBandRule();

    @ParameterizedTest(name = "{0}: amount {1}, passes BR-06 = {2}")
    @CsvSource({
        "TC-ORG-07.1, 9999.99, false",
        "TC-ORG-07.2, 10000.00, true",
        "TC-ORG-07.3, 100000.00, true",
        "TC-ORG-07.4, 100000.01, false"})
    void amountBand(final String testCase, final String amount, final boolean passes) {
        final RuleOutcome outcome = rule.check(new ApplicationBuilder().requested(amount).build());
        assertEquals(passes ? null : RejectionReason.OUTSIDE_PRODUCT_BAND, outcome.reason());
        assertTrue(passes || outcome.detail().startsWith("BR-06"), outcome.detail());
    }

    @ParameterizedTest(name = "{0}: tenure {1} months, passes BR-07 = {2}")
    @CsvSource({
        "TC-ORG-07.5, 5, false",
        "TC-ORG-07.6, 6, true",
        "TC-ORG-07.6, 24, true",
        "TC-ORG-07.7, 25, false"})
    void tenureBand(final String testCase, final int months, final boolean passes) {
        final RuleOutcome outcome = rule.check(new ApplicationBuilder().tenure(months).build());
        assertEquals(passes ? null : RejectionReason.OUTSIDE_PRODUCT_BAND, outcome.reason());
        assertTrue(passes || outcome.detail().startsWith("BR-07"), outcome.detail());
    }
}
