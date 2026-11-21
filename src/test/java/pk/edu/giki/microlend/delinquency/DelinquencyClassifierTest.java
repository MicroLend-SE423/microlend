package pk.edu.giki.microlend.delinquency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link DelinquencyClassifier} (BR-30, FR-23, TC-DEL-03).
 */
class DelinquencyClassifierTest {

    private final DelinquencyClassifier classifier = new DelinquencyClassifier();

    @Test
    @DisplayName("TC-DEL-03.1: negative days past due are rejected")
    void negativeRejected() {
        assertThrows(InvalidDaysPastDueException.class, () -> classifier.classify(-1));
    }

    @ParameterizedTest(name = "TC-DEL-03: {0} days past due -> {1}")
    @CsvSource({
        "0, STANDARD",
        "1, WATCH",
        "30, WATCH",
        "31, SUBSTANDARD",
        "60, SUBSTANDARD",
        "61, DOUBTFUL",
        "90, DOUBTFUL",
        "91, LOSS",
        "400, LOSS",
    })
    void bucketBoundaries(final int daysPastDue, final Classification expected) {
        assertEquals(expected, classifier.classify(daysPastDue));
    }
}
