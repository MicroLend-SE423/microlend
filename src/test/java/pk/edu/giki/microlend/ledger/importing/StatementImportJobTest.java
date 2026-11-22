package pk.edu.giki.microlend.ledger.importing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static pk.edu.giki.microlend.ledger.importing.HblStatementAdapterTest.resource;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.IdempotencyGuardPoster;
import pk.edu.giki.microlend.ledger.InMemoryPaymentReferences;
import pk.edu.giki.microlend.ledger.RepaymentCommand;
import pk.edu.giki.microlend.ledger.RepaymentPoster;

/**
 * Tests for {@link StatementImportJob} through the real BR-25 guard
 * (queue item I-04, TC-LED-06, TC-LED-08; FR-18, FR-20).
 */
class StatementImportJobTest {

    private static final String HBL_HEADER =
        "Txn Date,Reference No,Loan Ref,Narration,Amount (Paisa),Dr/Cr\n";
    private static final String MCB_HEADER =
        "Transaction ID,Value Date,Description,Customer Reference,Amount (PKR)\n";

    private final List<RepaymentCommand> posted = new ArrayList<>();
    // Stands in for TimingPoster -> CoreRepaymentPoster; the job never reads the PostingResult.
    private final RepaymentPoster core = command -> {
        posted.add(command);
        return null;
    };
    private final RepaymentPoster importChain =
        new IdempotencyGuardPoster(core, new InMemoryPaymentReferences());

    private static RepaymentSource hbl(final String rows) throws IOException {
        return new HblStatementAdapter(new StringReader(HBL_HEADER + rows), "hbl.csv");
    }

    private static RepaymentSource mcb(final String rows) throws IOException {
        return new McbStatementAdapter(new StringReader(MCB_HEADER + rows), "mcb.csv");
    }

    @Test
    @DisplayName("every command from the statement is posted and counted")
    void postsEveryCommand() throws IOException {
        final ImportReport report = new StatementImportJob(
            new HblStatementAdapter(resource("hbl-statement.csv"), "hbl-statement.csv"),
            importChain).run();
        assertEquals(2, report.posted());
        assertEquals(List.of(), report.duplicateReferences());
        assertEquals(2, posted.size());
    }

    @Test
    @DisplayName("TC-LED-06.3: a (bank, reference) pair already imported is refused; the rest of the file is posted")
    void duplicateInSecondFileRefused() throws IOException {
        new StatementImportJob(hbl("04102026,HBL-1,LN-1,first file,100,CR\n"), importChain).run();
        final ImportReport second = new StatementImportJob(hbl(
            "04102026,HBL-1,LN-1,repeated from first file,100,CR\n"
                + "05102026,HBL-2,LN-2,new payment,200,CR\n"), importChain).run();
        assertEquals(1, second.posted());
        assertEquals(List.of("HBL-1"), second.duplicateReferences());
        assertEquals(List.of("HBL-1", "HBL-2"),
            posted.stream().map(c -> c.reference().orElseThrow()).toList());
    }

    @Test
    @DisplayName("TC-LED-06.4: the same reference from HBL and from MCB is two payments")
    void sameReferenceFromTwoBanksAccepted() throws IOException {
        new StatementImportJob(hbl("04102026,TX-100,LN-1,hbl,100,CR\n"), importChain).run();
        final ImportReport report = new StatementImportJob(
            mcb("TX-100,04/10/2026,mcb,LN-1,1.00\n"), importChain).run();
        assertEquals(1, report.posted());
        assertEquals(2, posted.size());
    }

    @Test
    @DisplayName("TC-LED-08.4: a file with a malformed row posts nothing")
    void malformedFilePostsNothing() {
        assertThrows(InvalidStatementException.class, () -> new StatementImportJob(hbl(
            "04102026,HBL-1,LN-1,good row,100,CR\n"
                + "05102026,HBL-2,LN-1,bad amount,12x4,CR\n"), importChain).run());
        assertTrue(posted.isEmpty());
    }

    @Test
    @DisplayName("the job reads batches until the source returns an empty one")
    void readsEveryBatch() {
        final RepaymentCommand first = RepaymentCommand.bank(LoanId.of("LN-1"),
            Money.of(new BigDecimal("1.00")), LocalDate.of(2026, 10, 4), "HBL", "B-1");
        final RepaymentCommand second = RepaymentCommand.bank(LoanId.of("LN-1"),
            Money.of(new BigDecimal("2.00")), LocalDate.of(2026, 10, 4), "HBL", "B-2");
        final Iterator<List<RepaymentCommand>> batches =
            List.of(List.of(first), List.of(second), List.<RepaymentCommand>of()).iterator();
        final ImportReport report = new StatementImportJob(batches::next, importChain).run();
        assertEquals(2, report.posted());
        assertEquals(List.of(first, second), posted);
    }

    @Test
    @DisplayName("the report is immutable and the job rejects missing collaborators")
    void reportAndNulls() {
        final List<String> references = new ArrayList<>(List.of("HBL-1"));
        final ImportReport report = new ImportReport(0, references);
        references.add("HBL-2");
        assertEquals(List.of("HBL-1"), report.duplicateReferences());
        assertThrows(UnsupportedOperationException.class,
            () -> report.duplicateReferences().add("HBL-3"));
        assertThrows(NullPointerException.class, () -> new StatementImportJob(null, importChain));
        assertThrows(NullPointerException.class,
            () -> new StatementImportJob(List::of, null));
    }
}
