package pk.edu.giki.microlend.ledger.importing;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import org.apache.commons.csv.CSVRecord;

/**
 * Text handling shared by the bank statement adapters.
 */
final class StatementText {

    /** U+FEFF, which spreadsheet "CSV UTF-8" exports write before the header. */
    private static final int BYTE_ORDER_MARK = 0xFEFF;

    private StatementText() {
    }

    /**
     * Returns the statement with a leading byte-order mark removed, if it has one;
     * otherwise the header's first column name would not match.
     *
     * @param statement the statement text
     * @return a reader positioned at the first character of the header
     * @throws IOException if the first character cannot be read
     */
    static Reader withoutByteOrderMark(final Reader statement) throws IOException {
        final PushbackReader reader = new PushbackReader(statement, 1);
        final int first = reader.read();
        if (first != -1 && first != BYTE_ORDER_MARK) {
            reader.unread(first);
        }
        return reader;
    }

    /**
     * Whether a record is a blank line. The adapters keep blank lines as records so that
     * a record's number plus one is its physical line in the file.
     *
     * @param row a parsed record
     * @return true for an empty or whitespace-only line
     */
    static boolean isBlank(final CSVRecord row) {
        return row.size() == 1 && row.get(0).isEmpty();
    }
}
