package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WriteOffHistoryRule}: FR-04, BR-05, black-box cases
 * TC-ORG-04.1 to TC-ORG-04.3.
 */
class WriteOffHistoryRuleTest {

    private final WriteOffHistoryRule rule = new WriteOffHistoryRule();

    @Test
    @DisplayName("TC-ORG-04.1: no loan history passes BR-05")
    void noHistory() {
        assertTrue(rule.check(new ApplicationBuilder().build()).passed());
    }

    @Test
    @DisplayName("TC-ORG-04.2: one written-off loan, all else valid, is rejected")
    void priorWriteOff() {
        final LoanApplication application =
            new ApplicationBuilder().history(0, 1, 181, true).build();
        assertEquals(RejectionReason.PRIOR_WRITE_OFF, rule.check(application).reason());
    }

    @Test
    @DisplayName("TC-ORG-04.3: a loan settled 45 days late but never written off passes BR-05")
    void settledLateIsNotAWriteOff() {
        final LoanApplication application =
            new ApplicationBuilder().history(0, 1, 45, false).build();
        assertTrue(rule.check(application).passed());
    }
}
