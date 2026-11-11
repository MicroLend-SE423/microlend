package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.CompensatingEntryService;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.LedgerEntry;

/**
 * Tests for {@link SqliteLedgerRepository}, the SQLite implementation of the
 * {@code LedgerWriter} and {@code LedgerReader} roles (queue item L-14, QR-05).
 */
class SqliteLedgerRepositoryTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LoanId OTHER = LoanId.of("LN-2");
    private static final Instant T1 = Instant.parse("2026-10-04T09:00:00Z");
    private static final Instant T2 = Instant.parse("2026-10-05T09:00:00.123456Z");

    @TempDir
    private Path dir;

    private SqliteDatabase database;
    private SqliteLedgerRepository repository;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("ledger.db"));
        repository = new SqliteLedgerRepository(database);
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            for (final String id : List.of("LN-1", "LN-2")) {
                statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code,"
                    + " principal, outstanding, status, risk_grade, disbursed_on)"
                    + " VALUES ('" + id + "', 'C-1', 'FLAT', 5000000, 5000000, 'ACTIVE', 'A',"
                    + " '2026-10-01')");
            }
        }
    }

    private List<Long> rawColumn(final String sql) throws SQLException {
        final List<Long> values = new ArrayList<>();
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement();
             ResultSet rows = statement.executeQuery(sql)) {
            while (rows.next()) {
                values.add(rows.getObject(1) == null ? null : rows.getLong(1));
            }
        }
        return values;
    }

    @Test
    @DisplayName("entries read back equal, in append order, for the requested loan only")
    void roundTripsInAppendOrder() {
        final LedgerEntry disbursement =
            LedgerEntry.debit(LOAN, EntryType.PRINCIPAL_DISBURSEMENT, money("50000.00"), T1);
        final LedgerEntry other =
            LedgerEntry.debit(OTHER, EntryType.PRINCIPAL_DISBURSEMENT, money("100.00"), T1);
        final LedgerEntry repayment =
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, money("2549.00"), T2);
        repository.append(disbursement);
        repository.append(other);
        repository.append(repayment);
        assertEquals(List.of(disbursement, repayment), repository.entriesFor(LOAN));
        assertEquals(List.of(other), repository.entriesFor(OTHER));
    }

    @Test
    @DisplayName("an unknown loan reads as an empty, unmodifiable list")
    void unknownLoanIsEmpty() {
        final List<LedgerEntry> entries = repository.entriesFor(LoanId.of("LN-404"));
        assertTrue(entries.isEmpty());
        assertThrows(UnsupportedOperationException.class, () -> entries.add(null));
    }

    @Test
    @DisplayName("money is stored as exact INTEGER paisa (C-04)")
    void moneyStoredAsPaisa() throws SQLException {
        repository.append(LedgerEntry.credit(LOAN, EntryType.INTEREST_REPAYMENT, money("0.01"), T1));
        repository.append(
            LedgerEntry.credit(LOAN, EntryType.INTEREST_REPAYMENT, money("12345678.99"), T1));
        assertEquals(List.of(1L, 1234567899L),
            rawColumn("SELECT credit FROM ledger_entry ORDER BY id"));
        assertEquals(money("12345678.99"), repository.entriesFor(LOAN).get(1).credit());
    }

    @Test
    @DisplayName("a reversal is stored against its original's row and read back with it")
    void reversalRoundTrip() throws SQLException {
        final CompensatingEntryService service = new CompensatingEntryService(
            repository, Clock.fixed(T2, ZoneOffset.UTC));
        final LedgerEntry original =
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, money("1000.00"), T1);
        repository.append(original);
        final LedgerEntry reversal = service.reverse(original, "wrong loan");

        final List<LedgerEntry> entries = repository.entriesFor(LOAN);
        assertEquals(List.of(original, reversal), entries);
        assertEquals(original, entries.get(1).reverses().orElseThrow());
        assertEquals(List.of(1L), rawColumn(
            "SELECT reverses_entry_id FROM ledger_entry WHERE entry_type = 'REVERSAL'"));
    }

    @Test
    @DisplayName("reversing two identical entries references two different rows")
    void identicalOriginalsAreReversedSeparately() throws SQLException {
        final LedgerEntry original =
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, money("500.00"), T1);
        repository.append(original);
        repository.append(original);
        repository.append(LedgerEntry.reversalOf(original, "first slip", T2));
        repository.append(LedgerEntry.reversalOf(original, "second slip", T2));
        assertEquals(List.of(1L, 2L), rawColumn("SELECT reverses_entry_id FROM ledger_entry"
            + " WHERE entry_type = 'REVERSAL' ORDER BY id"));
    }

    @Test
    @DisplayName("a reversal of an entry that is not in the ledger is refused and nothing is written")
    void reversalOfUnknownEntryIsRefused() throws SQLException {
        final LedgerEntry neverPosted =
            LedgerEntry.credit(LOAN, EntryType.PRINCIPAL_REPAYMENT, money("500.00"), T1);
        assertThrows(PersistenceException.class,
            () -> repository.append(LedgerEntry.reversalOf(neverPosted, "x", T2)));
        assertEquals(List.of(0L), rawColumn("SELECT COUNT(*) FROM ledger_entry"));
    }

    @Test
    @DisplayName("a storage failure (unknown loan) surfaces as a domain exception")
    void storageFailureIsTranslated() {
        final LedgerEntry orphan = LedgerEntry.debit(LoanId.of("LN-404"),
            EntryType.PENALTY_ACCRUAL, money("1.00"), T1);
        final PersistenceException thrown =
            assertThrows(PersistenceException.class, () -> repository.append(orphan));
        assertTrue(thrown instanceof LoanDomainException);
    }

    @Test
    @DisplayName("entries survive a new repository on the same file")
    void entriesAreDurable() {
        final LedgerEntry entry =
            LedgerEntry.debit(LOAN, EntryType.PENALTY_ACCRUAL, money("2.50"), T1);
        repository.append(entry);
        final SqliteLedgerRepository reopened = new SqliteLedgerRepository(
            new SqliteDatabase("jdbc:sqlite:" + dir.resolve("ledger.db")));
        assertEquals(List.of(entry), reopened.entriesFor(LOAN));
    }

    @Test
    @DisplayName("null arguments are rejected as the role contracts state")
    void rejectsNulls() {
        assertThrows(NullPointerException.class, () -> repository.append(null));
        assertThrows(NullPointerException.class, () -> repository.entriesFor(null));
        assertThrows(NullPointerException.class, () -> new SqliteLedgerRepository(null));
    }

    @Test
    @DisplayName("no UPDATE or DELETE against ledger_entry exists in production code (QR-05)")
    void noMutatingSqlAgainstLedger() throws IOException {
        final Pattern mutating = Pattern.compile(
            "(update\\s+ledger_entry|delete\\s+from\\s+ledger_entry)");
        final List<Path> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(Path.of("src", "main"))) {
            for (final Path file : files.filter(Files::isRegularFile).toList()) {
                final String text = Files.readString(file).toLowerCase(Locale.ROOT);
                if (mutating.matcher(text).find()) {
                    offenders.add(file);
                }
            }
        }
        assertEquals(List.of(), offenders);
    }
}
