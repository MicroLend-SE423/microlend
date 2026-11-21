package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.DISBURSEMENT;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.money;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link EligibilityEvaluator}: FR-05, BR-08, black-box cases
 * TC-ORG-05.1 to TC-ORG-05.9. The evaluator stops at the first failing rule,
 * so a rejection names exactly one reason, in the order BR-01 to BR-07. An
 * application that passes is graded and capped: FR-06, BR-09, BR-10,
 * TC-ORG-06.8 to TC-ORG-06.11 and TC-ORG-03.4.
 *
 * <p>The new loan's instalment is 10,000.00 on income 100,000.00, a 10% burden,
 * unless a test adds existing instalments.</p>
 */
class EligibilityEvaluatorTest {

    private static final Money NEW_INSTALMENT = money("10000.00");

    private final EligibilityEvaluator evaluator =
        EligibilityEvaluator.standard(application -> NEW_INSTALMENT);

    @Test
    @DisplayName("TC-ORG-05 (EC-24): an application that passes every rule is approved")
    void approvesEligibleApplication() {
        final Decision decision = evaluator.evaluate(new ApplicationBuilder().build());
        assertTrue(decision.isApproved());
        assertEquals(Optional.empty(), decision.reason());
    }

    @Test
    @DisplayName("TC-ORG-05.1: BR-01 failing alone (age 17) names AGE")
    void ageAtDisbursementAlone() {
        assertRejected(new ApplicationBuilder().bornOn(DISBURSEMENT.minusYears(17)),
            RejectionReason.AGE, "BR-01");
    }

    @Test
    @DisplayName("TC-ORG-05.2: BR-02 failing alone (65 now, 66 at maturity) names AGE")
    void ageAtMaturityAlone() {
        assertRejected(new ApplicationBuilder().bornOn(DISBURSEMENT.plusMonths(12).minusYears(66)),
            RejectionReason.AGE, "BR-02");
    }

    @Test
    @DisplayName("TC-ORG-05.3: BR-03 failing alone (2 existing active loans) names ACTIVE_LOAN_LIMIT")
    void activeLoanLimitAlone() {
        assertRejected(new ApplicationBuilder().history(2, 2, 0, false),
            RejectionReason.ACTIVE_LOAN_LIMIT, "BR-03");
    }

    @Test
    @DisplayName("TC-ORG-05.4: BR-04 failing alone (burden 45%) names BURDEN_RATIO")
    void burdenRatioAlone() {
        assertRejected(new ApplicationBuilder().existingInstalments("35000.00"),
            RejectionReason.BURDEN_RATIO, "BR-04");
    }

    @Test
    @DisplayName("TC-ORG-05.5: BR-05 failing alone (a prior write-off) names PRIOR_WRITE_OFF")
    void priorWriteOffAlone() {
        assertRejected(new ApplicationBuilder().history(0, 1, 181, true),
            RejectionReason.PRIOR_WRITE_OFF, "BR-05");
    }

    @Test
    @DisplayName("TC-ORG-05.6: BR-06 failing alone (amount above the maximum) names OUTSIDE_PRODUCT_BAND")
    void amountBandAlone() {
        assertRejected(new ApplicationBuilder().requested("100000.01"),
            RejectionReason.OUTSIDE_PRODUCT_BAND, "BR-06");
    }

    @Test
    @DisplayName("TC-ORG-05.7: BR-07 failing alone (tenure above the band) names OUTSIDE_PRODUCT_BAND")
    void tenureBandAlone() {
        assertRejected(new ApplicationBuilder().tenure(25),
            RejectionReason.OUTSIDE_PRODUCT_BAND, "BR-07");
    }

    @Test
    @DisplayName("TC-ORG-05.8: age 17 and a prior write-off names BR-01, which is evaluated first")
    void ageBeforeWriteOff() {
        assertRejected(new ApplicationBuilder()
                .bornOn(DISBURSEMENT.minusYears(17)).history(0, 1, 181, true),
            RejectionReason.AGE, "BR-01");
    }

    @Test
    @DisplayName("TC-ORG-05.9: 2 active loans and burden 45% names BR-03, which is evaluated first")
    void activeLoansBeforeBurden() {
        assertRejected(new ApplicationBuilder()
                .history(2, 2, 0, false).existingInstalments("35000.00"),
            RejectionReason.ACTIVE_LOAN_LIMIT, "BR-03");
    }

    @Test
    @DisplayName("TC-ORG-02.5: 1 active + 1 written off passes BR-03 and is rejected by BR-05")
    void writtenOffLoanRejectedByWriteOffRule() {
        assertRejected(new ApplicationBuilder().history(1, 2, 0, true),
            RejectionReason.PRIOR_WRITE_OFF, "BR-05");
    }

    @Test
    @DisplayName("the evaluator stops at the first failing rule; later rules are not consulted")
    void stopsAtFirstFailure() {
        final EligibilityRule passes = application -> RuleOutcome.pass();
        final EligibilityRule failsFirst =
            application -> RuleOutcome.fail(RejectionReason.ACTIVE_LOAN_LIMIT, "first");
        final EligibilityRule mustNotRun =
            application -> fail("a rule after the first failure was evaluated");
        final EligibilityEvaluator stubbed = new EligibilityEvaluator(
            List.of(passes, failsFirst, mustNotRun), application -> NEW_INSTALMENT);
        final Decision decision = stubbed.evaluate(new ApplicationBuilder().build());
        assertEquals(Optional.of(RejectionReason.ACTIVE_LOAN_LIMIT), decision.reason());
    }

    @Test
    @DisplayName("TC-ORG-06.8: grade B, requested 50,000.00, is approved for 40,000.00 (80%)")
    void gradeBIsCappedAtEightyPercent() {
        assertApproved(new ApplicationBuilder(), RiskGrade.B, "40000.00");
    }

    @Test
    @DisplayName("TC-ORG-06.9: grade A, requested 50,000.00, is approved for 50,000.00")
    void gradeAIsNotCapped() {
        assertApproved(new ApplicationBuilder().history(0, 1, 0, false), RiskGrade.A, "50000.00");
    }

    @Test
    @DisplayName("TC-ORG-06.10: grade C caps 16,666.00 at 9,999.60, below the minimum: BELOW_PRODUCT_MINIMUM")
    void capBelowProductMinimum() {
        assertRejected(new ApplicationBuilder().existingInstalments("28000.00").requested("16666.00"),
            RejectionReason.BELOW_PRODUCT_MINIMUM, "BR-10");
    }

    @Test
    @DisplayName("TC-ORG-06.11: grade C caps 16,666.67 at 10,000.00, the minimum itself: approved")
    void capAtProductMinimum() {
        assertApproved(new ApplicationBuilder().existingInstalments("28000.00").requested("16666.67"),
            RiskGrade.C, "10000.00");
    }

    @Test
    @DisplayName("TC-ORG-03.4: burden 40.00% at the base rate passes BR-04 as grade C; the spread does not count")
    void gradeCAtTheBurdenLimit() {
        assertApproved(new ApplicationBuilder().existingInstalments("30000.00"),
            RiskGrade.C, "30000.00");
    }

    @Test
    @DisplayName("BR-09: the grade uses the BR-04 burden; 25.00% with an earlier loan is A, 25.01% is B")
    void gradeUsesTheBurdenRatio() {
        assertApproved(new ApplicationBuilder().history(0, 1, 0, false)
            .existingInstalments("15000.00"), RiskGrade.A, "50000.00");
        assertApproved(new ApplicationBuilder().history(0, 1, 0, false)
            .existingInstalments("15010.00"), RiskGrade.B, "40000.00");
    }

    private void assertApproved(final ApplicationBuilder builder, final RiskGrade grade,
                                final String amount) {
        final Decision decision = evaluator.evaluate(builder.build());
        assertTrue(decision.isApproved(), () -> "rejected: " + decision.reason());
        assertEquals(Optional.of(grade), decision.grade());
        assertEquals(Optional.of(money(amount)), decision.approvedAmount());
    }

    private void assertRejected(final ApplicationBuilder builder, final RejectionReason reason,
                                final String rule) {
        final Decision decision = evaluator.evaluate(builder.build());
        assertEquals(Optional.of(reason), decision.reason());
        final IneligibleApplicantException thrown =
            assertThrows(IneligibleApplicantException.class, decision::requireApproved);
        assertTrue(thrown.outcome().detail().startsWith(rule), thrown.outcome().detail());
    }
}
