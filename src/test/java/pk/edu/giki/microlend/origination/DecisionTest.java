package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanDomainException;

/**
 * Tests for {@link Decision}, {@link RuleOutcome} and
 * {@link IneligibleApplicantException}: a rejection carries one typed reason
 * (BR-08), and a caller that needs an exception gets one carrying the outcome.
 */
class DecisionTest {

    private static final RuleOutcome AGE_FAILURE =
        RuleOutcome.fail(RejectionReason.AGE, "BR-01: age 17 at disbursement is below 18");

    @Test
    @DisplayName("a rejected decision names its reason and is not approved")
    void rejectedDecision() {
        final Decision decision = Decision.rejected(AGE_FAILURE);
        assertFalse(decision.isApproved());
        assertEquals(Optional.of(RejectionReason.AGE), decision.reason());
    }

    @Test
    @DisplayName("requireApproved throws IneligibleApplicantException carrying the failed outcome")
    void requireApprovedThrowsForRejection() {
        final IneligibleApplicantException thrown = assertThrows(
            IneligibleApplicantException.class, Decision.rejected(AGE_FAILURE)::requireApproved);
        assertSame(AGE_FAILURE, thrown.outcome());
        assertTrue(thrown.getMessage().contains("BR-01"), thrown.getMessage());
        assertInstanceOf(LoanDomainException.class, thrown);
    }

    @Test
    @DisplayName("requireApproved returns an approved decision unchanged")
    void requireApprovedReturnsApproval() {
        final Decision approved = Decision.approved();
        assertSame(approved, approved.requireApproved());
    }

    @Test
    @DisplayName("a passed outcome cannot be used to reject an application")
    void rejectedNeedsAFailure() {
        assertThrows(IllegalArgumentException.class, () -> Decision.rejected(RuleOutcome.pass()));
    }

    @Test
    @DisplayName("a passed outcome has no reason; a failed one must have one")
    void ruleOutcome() {
        assertTrue(RuleOutcome.pass().passed());
        assertNull(RuleOutcome.pass().reason());
        assertFalse(AGE_FAILURE.passed());
        assertThrows(NullPointerException.class, () -> RuleOutcome.fail(null, "no reason"));
    }
}
