package pk.edu.giki.microlend.ledger.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
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
 * Tests for {@link HblStatementAdapter} (queue item I-04, TC-LED-08; FR-20).
 */
class HblStatementAdapterTest {

    private static final String HEADER =
        "Txn Date,Reference No,Loan Ref,Narration,Amount (Paisa),Dr/Cr\n";
    private static final String GOOD_ROW = "04102026,HBL-1,LN-1,first payment,100,CR\n";

    static Reader resource(final String name) {
        return new InputStreamReader(
            HblStatementAdapterTest.class.getResourceAsStream("/statements/" + name),
            StandardCharsets.UTF_8);
    }

    private static HblStatementAdapter adapter(final String csv) throws IOException {
        return new HblStatementAdapter(new StringReader(csv), "bad.csv");
    }

    @Test
    @DisplayName("TC-LED-08.1: one command per CR row; paisa become rupees, ddMMyyyy a date; DR rows skipped")
    void mapsCreditRows() throws IOException {
        final HblStatementAdapter adapter =
            new HblStatementAdapter(resource("hbl-statement.csv"), "hbl-statement.csv");
        assertEquals(List.of(
            RepaymentCommand.bank(LoanId.of("LN-0001"), Money.of(new BigDecimal("2549.00")),
                LocalDate.of(2026, 10, 4), "HBL", "HBL-7781"),
            RepaymentCommand.bank(LoanId.of("LN-0002"), Money.of(new BigDecimal("1000.00")),
                LocalDate.of(2026, 10, 5), "HBL", "HBL-7782")),
            adapter.nextBatch());
        assertEquals(List.of(), adapter.nextBatch());
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {
        "05102026,HBL-2,LN-1,amount not a number,12x4,CR",
        "05102026,HBL-2,LN-1,negative amount,-500,CR",
        "05102026,HBL-2,LN-1,zero amount,0,CR",
        "31022026,HBL-2,LN-1,no such date,500,CR",
        "2026-10-05,HBL-2,LN-1,wrong date format,500,CR",
        "05102026,HBL-2,LN-1,unknown direction,500,XX",
        "05102026,,LN-1,blank reference,500,CR",
        "05102026,HBL-2,,blank loan,500,CR",
        "05102026,HBL-2,LN-1,row with missing values"
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
    @DisplayName("broken CSV quoting is reported as a typed statement error, not a parser exception")
    void brokenQuoting() {
        final InvalidStatementException thrown = assertThrows(InvalidStatementException.class,
            () -> adapter(HEADER + GOOD_ROW + "05102026,\"HBL-2,LN-1,x,500,CR\n").nextBatch());
        assertEquals("bad.csv", thrown.sourceName());
    }

    @Test
    @DisplayName("a missing column is rejected on line 1, before any row is read")
    void missingColumn() {
        final InvalidStatementException thrown = assertThrows(InvalidStatementException.class,
            () -> adapter("Txn Date,Reference No,Loan Ref,Narration,Dr/Cr\n" + GOOD_ROW));
        assertEquals(1, thrown.line());
        assertTrue(thrown.getMessage().contains("Amount (Paisa)"), thrown.getMessage());
    }

    @Test
    @DisplayName("TC-LED-08.3: an MCB statement is refused as not being in HBL format")
    void otherBanksFormat() {
        final InvalidStatementException thrown = assertThrows(InvalidStatementException.class,
            () -> new HblStatementAdapter(resource("mcb-statement.csv"), "mcb-statement.csv"));
        assertEquals("mcb-statement.csv", thrown.sourceName());
    }
}
