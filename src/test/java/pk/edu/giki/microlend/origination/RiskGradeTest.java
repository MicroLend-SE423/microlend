package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.money;

import java.math.BigDecimal;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Tests for {@link RiskGrade}: FR-06, BR-09 and BR-10, black-box cases
 * TC-ORG-06.1 to TC-ORG-06.7 (assignment) and the spread and cap of each grade.
 */
class RiskGradeTest {

    @ParameterizedTest(name = "{0}: burden {1}%, {2} earlier loan(s), worst DPD {3} is grade {4}")
    @CsvSource({
        "TC-ORG-06.1, 25.00, 1, 30, A",
        "TC-ORG-06.2, 25.01, 1, 30, B",
        "TC-ORG-06.3, 20.00, 1, 31, B",
        "TC-ORG-06.4, 20.00, 0, 0, B",
        "TC-ORG-06.5, 35.00, 1, 60, B",
        "TC-ORG-06.6, 35.01, 1, 0, C",
        "TC-ORG-06.7, 20.00, 1, 61, C"})
    void assignsGrade(final String testCase, final BigDecimal burden, final int earlierLoans,
                      final int worstDaysPastDue, final RiskGrade expected) {
        assertEquals(expected, RiskGrade.assign(burden, earlierLoans, worstDaysPastDue));
    }

    @ParameterizedTest(name = "grade {0}: spread +{1}%, 50,000.00 requested is capped at {2}")
    @CsvSource({"A, 0.00, 50000.00", "B, 1.50, 40000.00", "C, 3.00, 30000.00"})
    void spreadAndCap(final RiskGrade grade, final BigDecimal spread, final String cap) {
        assertEquals(0, grade.spreadPercent().compareTo(spread));
        assertEquals(money(cap), grade.cap(money("50000.00")));
    }
}
