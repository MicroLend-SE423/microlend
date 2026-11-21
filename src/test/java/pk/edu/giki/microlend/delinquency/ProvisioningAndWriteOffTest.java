package pk.edu.giki.microlend.delinquency;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link ProvisioningPolicy} (BR-31, FR-24, TC-DEL-04) and
 * {@link WriteOffPolicy} (BR-32, FR-25, TC-DEL-05).
 */
class ProvisioningAndWriteOffTest {

    private final ProvisioningPolicy provisioning = new ProvisioningPolicy();
    private final WriteOffPolicy writeOff = new WriteOffPolicy();

    @ParameterizedTest(name = "TC-DEL-04: {0} on 10,000.00 outstanding -> {1}")
    @CsvSource({
        "STANDARD, 100.00",
        "WATCH, 500.00",
        "SUBSTANDARD, 2500.00",
        "DOUBTFUL, 5000.00",
        "LOSS, 10000.00",
    })
    void provisionByClassification(final Classification classification, final String expected) {
        assertEquals(Money.of(new BigDecimal(expected)),
            provisioning.provisionFor(classification, Money.of(new BigDecimal("10000.00"))));
    }

    @Test
    @DisplayName("TC-DEL-05.1/05.2: 179 and 180 days are not written off")
    void notYetWrittenOff() {
        assertFalse(writeOff.shouldWriteOff(179));
        assertFalse(writeOff.shouldWriteOff(180));
    }

    @Test
    @DisplayName("TC-DEL-05.3: more than 180 days is written off")
    void writtenOffAfter180() {
        assertTrue(writeOff.shouldWriteOff(181));
    }

    @Test
    @DisplayName("negative days past due are rejected")
    void negativeRejected() {
        assertThrows(InvalidDaysPastDueException.class, () -> writeOff.shouldWriteOff(-1));
    }
}
