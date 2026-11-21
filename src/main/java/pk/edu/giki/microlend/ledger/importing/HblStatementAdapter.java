package pk.edu.giki.microlend.ledger.importing;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.RepaymentCommand;

/**
 * Adapts an HBL statement, read through Apache Commons CSV's
 * {@link CSVParser}, to {@link RepaymentSource}: an Object Adapter that holds
 * the parser by composition (queue item I-04; M1 design section 5.2; FR-20).
 *
 * <p>HBL format: comma-separated with a header line; columns are found by
 * name, so their order does not matter and extra columns are ignored.</p>
 * <ul>
 *   <li>{@code Txn Date}: the value date as {@code ddMMyyyy}, e.g. {@code 04102026};</li>
 *   <li>{@code Reference No}: HBL's transaction reference, half of the BR-25 key;</li>
 *   <li>{@code Loan Ref}: the MicroLend loan id the customer quoted;</li>
 *   <li>{@code Narration}: free text, not read;</li>
 *   <li>{@code Amount (Paisa)}: whole paisa without a sign, e.g. {@code 254900} is 2549.00;</li>
 *   <li>{@code Dr/Cr}: {@code CR} is money in, a repayment; {@code DR} is money out
 *       (bank charges) and is skipped.</li>
 * </ul>
 *
 * <p>This class is the only place that knows these conventions. The caller
 * owns the reader and closes it.</p>
 */
public final class HblStatementAdapter implements RepaymentSource {

    private static final String BANK = "HBL";
    private static final String DATE = "Txn Date";
    private static final String REFERENCE = "Reference No";
    private static final String LOAN = "Loan Ref";
    private static final String AMOUNT = "Amount (Paisa)";
    private static final String DIRECTION = "Dr/Cr";
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("ddMMuuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
        .setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(false).build();

    private final CSVParser parser;
    private final String sourceName;

    /**
     * Opens an HBL statement and checks its header.
     *
     * @param statement the statement text; the caller closes it
     * @param sourceName the file name, quoted in every error
     * @throws IOException if the header cannot be read
     * @throws InvalidStatementException if a required column is missing, which
     *     usually means the file is not an HBL statement
     */
    public HblStatementAdapter(final Reader statement, final String sourceName)
        throws IOException {
        this.sourceName = Objects.requireNonNull(sourceName, "source name is required");
        this.parser = FORMAT.parse(StatementText.withoutByteOrderMark(statement));
        final List<String> missing = new ArrayList<>(
            List.of(DATE, REFERENCE, LOAN, AMOUNT, DIRECTION));
        missing.removeAll(parser.getHeaderNames());
        if (!missing.isEmpty()) {
            throw new InvalidStatementException(sourceName, 1,
                "not an HBL statement, missing columns " + missing);
        }
    }

    /**
     * Reads the whole statement and returns its CR rows as one batch.
     *
     * <p>Every row is translated before anything is returned, so a malformed
     * file yields no commands and nothing from it is posted.</p>
     *
     * @return one command per CR row, in file order; empty once the statement has been read
     * @throws InvalidStatementException naming the file and line of the first malformed row
     */
    @Override
    public List<RepaymentCommand> nextBatch() {
        final List<RepaymentCommand> batch = new ArrayList<>();
        try {
            for (final CSVRecord row : parser) {
                if (StatementText.isBlank(row)) {
                    continue;
                }
                try {
                    if (isCredit(row.get(DIRECTION))) {
                        batch.add(toCommand(row));
                    }
                } catch (final IllegalArgumentException | DateTimeException
                               | LoanDomainException badRow) {
                    // + 1 for the header; blank lines are records too, so this is the
                    // row's physical line.
                    throw new InvalidStatementException(sourceName, row.getRecordNumber() + 1,
                        badRow.getMessage(), badRow);
                }
            }
        } catch (final UncheckedIOException brokenCsv) {
            throw new InvalidStatementException(sourceName, parser.getCurrentLineNumber(),
                brokenCsv.getCause().getMessage(), brokenCsv);
        }
        return List.copyOf(batch);
    }

    private static boolean isCredit(final String direction) {
        if ("CR".equals(direction)) {
            return true;
        }
        if ("DR".equals(direction)) {
            return false;
        }
        throw new IllegalArgumentException(DIRECTION + " must be CR or DR, was '" + direction + "'");
    }

    private static RepaymentCommand toCommand(final CSVRecord row) {
        final long paisa = Long.parseLong(row.get(AMOUNT));
        return RepaymentCommand.bank(LoanId.of(row.get(LOAN)),
            Money.of(BigDecimal.valueOf(paisa, Money.SCALE)),
            LocalDate.parse(row.get(DATE), DATE_FORMAT), BANK, row.get(REFERENCE));
    }
}
