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
 * Adapts an MCB statement, read through Apache Commons CSV's
 * {@link CSVParser}, to {@link RepaymentSource}: an Object Adapter that holds
 * the parser by composition (queue item I-04; M1 design section 5.2; FR-20).
 *
 * <p>MCB format: comma-separated with a header line; columns are found by
 * name, so their order does not matter and extra columns are ignored.</p>
 * <ul>
 *   <li>{@code Transaction ID}: MCB's transaction reference, half of the BR-25 key;</li>
 *   <li>{@code Value Date}: the value date as {@code dd/MM/yyyy}, e.g. {@code 04/10/2026};</li>
 *   <li>{@code Description}: free text, not read;</li>
 *   <li>{@code Customer Reference}: the MicroLend loan id the customer quoted;</li>
 *   <li>{@code Amount (PKR)}: rupees with at most two decimals, signed: positive is
 *       money in, a repayment; negative is money out (bank charges) and is skipped.</li>
 * </ul>
 *
 * <p>This class is the only place that knows these conventions. The caller
 * owns the reader and closes it.</p>
 */
public final class McbStatementAdapter implements RepaymentSource {

    private static final String BANK = "MCB";
    private static final String REFERENCE = "Transaction ID";
    private static final String DATE = "Value Date";
    private static final String LOAN = "Customer Reference";
    private static final String AMOUNT = "Amount (PKR)";
    private static final DateTimeFormatter DATE_FORMAT =
        DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);
    private static final CSVFormat FORMAT = CSVFormat.DEFAULT.builder()
        .setHeader().setSkipHeaderRecord(true).setTrim(true).build();

    private final CSVParser parser;
    private final String sourceName;

    /**
     * Opens an MCB statement and checks its header.
     *
     * @param statement the statement text; the caller closes it
     * @param sourceName the file name, quoted in every error
     * @throws IOException if the header cannot be read
     * @throws InvalidStatementException if a required column is missing, which
     *     usually means the file is not an MCB statement
     */
    public McbStatementAdapter(final Reader statement, final String sourceName)
        throws IOException {
        this.sourceName = Objects.requireNonNull(sourceName, "source name is required");
        this.parser = FORMAT.parse(statement);
        final List<String> missing = new ArrayList<>(List.of(REFERENCE, DATE, LOAN, AMOUNT));
        missing.removeAll(parser.getHeaderNames());
        if (!missing.isEmpty()) {
            throw new InvalidStatementException(sourceName, 1,
                "not an MCB statement, missing columns " + missing);
        }
    }

    /**
     * Reads the whole statement and returns its money-in rows as one batch.
     *
     * <p>Every row is translated before anything is returned, so a malformed
     * file yields no commands and nothing from it is posted.</p>
     *
     * @return one command per row with a positive amount, in file order; empty
     *     once the statement has been read
     * @throws InvalidStatementException naming the file and line of the first malformed row
     */
    @Override
    public List<RepaymentCommand> nextBatch() {
        final List<RepaymentCommand> batch = new ArrayList<>();
        try {
            for (final CSVRecord row : parser) {
                try {
                    final BigDecimal amount = new BigDecimal(row.get(AMOUNT));
                    if (amount.signum() >= 0) {
                        batch.add(toCommand(row, amount));
                    }
                } catch (final IllegalArgumentException | DateTimeException
                               | LoanDomainException badRow) {
                    // + 1 because the header is line 1 and records are numbered from 1.
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

    private static RepaymentCommand toCommand(final CSVRecord row, final BigDecimal amount) {
        return RepaymentCommand.bank(LoanId.of(row.get(LOAN)), Money.of(amount),
            LocalDate.parse(row.get(DATE), DATE_FORMAT), BANK, row.get(REFERENCE));
    }
}
