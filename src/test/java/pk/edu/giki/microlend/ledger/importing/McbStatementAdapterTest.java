package pk.edu.giki.microlend.ledger.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pk.edu.giki.microlend.ledger.importing.HblStatementAdapterTest.resource;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.RepaymentCommand;

/**
 * Tests for {@link McbStatementAdapter} (queue item I-04, TC-LED-08; FR-20).
 */
class McbStatementAdapterTest {

    private static final String HEADER =
        "Transaction ID,Value Date,Description,Customer Reference,Amount (PKR)\n";
    private static final String GOOD_ROW = "MCB-1,04/10/2026,first payment,LN-1,100.00\n";

    private static McbStatementAdapter adapter(final String csv) throws IOException {
        return new McbStatementAdapter(new StringReader(csv), "bad.csv");
    }

    @Test
    @DisplayName("blank lines are skipped, and an error names the row's physical line in the file")
    void physicalLineNumbers() throws IOException {
        assertEquals(1, adapter(HEADER + "\n" + GOOD_ROW + "\n").nextBatch().size());
        final InvalidStatementException thrown = assertThrows(InvalidStatementException.class,
            () -> adapter(HEADER + "\n" + GOOD_ROW + "\n"
                + "MCB-2,31/02/2026,no such date,LN-1,5.00\n").nextBatch());
        assertEquals(5, thrown.line());
    }

    @Test
    @DisplayName("a UTF-8 byte-order mark before the header, as spreadsheet exports write, is ignored")
    void byteOrderMark() throws IOException {
        assertEquals(1, adapter("\uFEFF" + HEADER + GOOD_ROW).nextBatch().size());
    }

    @Test
    @DisplayName("TC-LED-08.2: one command per positive row; rupees kept, dd/MM/yyyy a date; negative rows skipped")
    void mapsCreditRows() throws IOException {
        final McbStatementAdapter adapter =
            new McbStatementAdapter(resource("mcb-statement.csv"), "mcb-statement.csv");
        assertEquals(List.of(
            RepaymentCommand.bank(LoanId.of("LN-0003"), Money.of(new BigDecimal("1250.50")),
                LocalDate.of(2026, 10, 4), "MCB", "MCB000451"),
            RepaymentCommand.bank(LoanId.of("LN-0001"), Money.of(new BigDecimal("3000.00")),
                LocalDate.of(2026, 10, 6), "MCB", "MCB000452")),
            adapter.nextBatch());
        assertEquals(List.of(), adapter.nextBatch());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "MCB-2,06/10/2026,amount not a number,LN-1,12x4",
        "MCB-2,06/10/2026,amount below one paisa,LN-1,10.005",
        "MCB-2,06/10/2026,zero amount,LN-1,0.00",
        "MCB-2,31/02/2026,no such date,LN-1,500.00",
        "MCB-2,2026-10-06,wrong date format,LN-1,500.00",
        "MCB-2,06/10/2026,row with missing values"
    })
    @DisplayName("TC-LED-08.4: a malformed row is rejected naming the file and its line")
    void malformedRow(final String row) {
        final InvalidStatementException thrown = assertThrows(InvalidStatementException.class,
            () -> adapter(HEADER + GOOD_ROW + row + "\n").nextBatch());
        assertEquals("bad.csv", thrown.sourceName());
        assertEquals(3, thrown.line());
        assertTrue(thrown.getMessage().startsWith("bad.csv line 3: "), thrown.getMessage());
    }

    @Test
    @DisplayName("TC-LED-08.3: an HBL statement is refused as not being in MCB format")
    void otherBanksFormat() {
        final InvalidStatementException thrown = assertThrows(InvalidStatementException.class,
            () -> new McbStatementAdapter(resource("hbl-statement.csv"), "hbl-statement.csv"));
        assertEquals(1, thrown.line());
        assertTrue(thrown.getMessage().contains("Customer Reference"), thrown.getMessage());
    }
}
