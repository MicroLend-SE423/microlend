package pk.edu.giki.microlend.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link LoanId}.
 *
 * <p>A loan identifier is the key every ledger entry, instalment and lock is
 * attached to, so it must be impossible to construct an empty one and two
 * identifiers with the same value must be interchangeable.</p>
 */
class LoanIdTest {

    @Test
    @DisplayName("a null value is rejected")
    void rejectsNull() {
        assertThrows(InvalidLoanIdException.class, () -> LoanId.of(null));
    }

    @Test
    @DisplayName("a blank value is rejected")
    void rejectsBlank() {
        assertThrows(InvalidLoanIdException.class, () -> LoanId.of("   "));
    }

    @Test
    @DisplayName("surrounding whitespace is trimmed")
    void trimsWhitespace() {
        assertEquals("LN-0001", LoanId.of("  LN-0001 ").value());
    }

    @Test
    @DisplayName("identifiers compare by value")
    void comparesByValue() {
        assertEquals(LoanId.of("LN-0001"), LoanId.of("LN-0001"));
        assertEquals(LoanId.of("LN-0001").hashCode(), LoanId.of("LN-0001").hashCode());
        assertNotEquals(LoanId.of("LN-0001"), LoanId.of("LN-0002"));
    }
}
