package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.CoreRepaymentPoster;
import pk.edu.giki.microlend.ledger.EntryType;
import pk.edu.giki.microlend.ledger.InstalmentDue;
import pk.edu.giki.microlend.ledger.LedgerEntry;
import pk.edu.giki.microlend.ledger.LedgerWriter;
import pk.edu.giki.microlend.ledger.LoanAccount;
import pk.edu.giki.microlend.ledger.LoanRepository;
import pk.edu.giki.microlend.ledger.RepaymentAllocator;
import pk.edu.giki.microlend.ledger.RepaymentCommand;
import pk.edu.giki.microlend.ledger.SettlementPolicy;

/**
 * Fault injection into the posting path on a real SQLite file (queue item
 * L-16; QR-03: Σ debits = Σ credits at ten or more injected failure points,
 * zero orphan lines).
 *
 * <p>A failure is injected at each line of a posting's ledger write (the line
 * is replaced by one the database refuses) and at the loan save that follows
 * it. After every failure the ledger must balance, the loan must be unchanged,
 * and no line of the failed posting may stand without a reversal.</p>
 */
class PostingFaultInjectionTest {

    private static final LoanId LOAN = LoanId.of("LN-1");
    private static final LocalDate TODAY = LocalDate.of(2026, 3, 20);
    private static final Instant NOW = Instant.parse("2026-03-20T10:00:00Z");
    private static final int SAVE = -1;
    private static final List<InstalmentDue> OWING = List.of(
        new InstalmentDue(1, LocalDate.of(2026, 1, 15), money("10.00"), Money.ZERO,
            money("100.00"), money("1000.00")),
        new InstalmentDue(2, LocalDate.of(2026, 2, 15), Money.ZERO, Money.ZERO,
            money("100.00"), money("1000.00")));

    @TempDir
    private Path dir;

    private SqliteDatabase database;
    private SqliteLedgerRepository ledger;
    private SqliteLoanRepository loans;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    /**
     * Postings of three and four lines; every line index plus the loan save.
     * 3 + 4 + 4 line points and 3 save points: 14 failure points.
     */
    static Stream<Arguments> failurePoints() {
        final List<Arguments> points = new ArrayList<>();
        final String[][] postings = {{"60.00", "3"}, {"1300.00", "4"}, {"2210.00", "4"}};
        for (final String[] posting : postings) {
            for (int line = 0; line < Integer.parseInt(posting[1]); line++) {
                points.add(Arguments.of(posting[0], line));
            }
            points.add(Arguments.of(posting[0], SAVE));
        }
        return points.stream();
    }

    @BeforeEach
    void seed() throws SQLException {
        database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("faults.db"));
        ledger = new SqliteLedgerRepository(database);
        loans = new SqliteLoanRepository(database);
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type,"
                + " annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)"
                + " VALUES ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24)");
            statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code, principal,"
                + " outstanding, status, risk_grade, disbursed_on) VALUES ('LN-1', 'C-1', 'FLAT',"
                + " 200000, 200000, 'ACTIVE', 'A', '2026-01-01')");
        }
    }

    private CoreRepaymentPoster poster(final LedgerWriter writer, final LoanRepository repository) {
        return new CoreRepaymentPoster(new RepaymentAllocator(),
            new SettlementPolicy((loan, date) -> Money.ZERO), repository,
            (loan, date) -> OWING, writer, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    /** Replaces line {@code point} of the next posting with one the database refuses. */
    private LedgerWriter failingAtLine(final int point) {
        return new LedgerWriter() {
            @Override
            public void append(final LedgerEntry entry) {
                ledger.append(entry);
            }

            @Override
            public void appendAll(final List<LedgerEntry> entries) {
                final List<LedgerEntry> poisoned = new ArrayList<>(entries);
                if (point >= 0 && point < poisoned.size()) {
                    poisoned.set(point, LedgerEntry.debit(LoanId.of("LN-404"),
                        EntryType.PAYMENT_RECEIVED, Money.of(new BigDecimal("1.00")), NOW));
                }
                ledger.appendAll(poisoned);
            }
        };
    }

    /** A loan repository whose save fails when {@code point} is {@link #SAVE}. */
    private LoanRepository failingAtSave(final int point) {
        return new LoanRepository() {
            @Override
            public Optional<LoanAccount> findById(final LoanId loanId) {
                return loans.findById(loanId);
            }

            @Override
            public void save(final LoanAccount account) {
                if (point == SAVE) {
                    throw new PersistenceException("injected failure saving " + account.id());
                }
                loans.save(account);
            }
        };
    }

    @ParameterizedTest(name = "pay {0}, fail at {1} (-1 = loan save)")
    @MethodSource("failurePoints")
    void failureLeavesLedgerBalancedWithNoOrphans(final String amount, final int point) {
        // A successful posting first, so the ledger is not empty when the fault hits.
        poster(ledger, loans).post(RepaymentCommand.cash(LOAN, money("5.00"), TODAY));
        final List<LedgerEntry> before = ledger.entriesFor(LOAN);
        final Money principalBefore = loans.findById(LOAN).orElseThrow().outstandingPrincipal();

        assertThrows(LoanDomainException.class, () -> poster(failingAtLine(point),
            failingAtSave(point)).post(RepaymentCommand.cash(LOAN, money(amount), TODAY)));

        final List<LedgerEntry> after = ledger.entriesFor(LOAN);
        Money debits = Money.ZERO;
        Money credits = Money.ZERO;
        for (final LedgerEntry entry : after) {
            debits = debits.plus(entry.debit());
            credits = credits.plus(entry.credit());
        }
        assertEquals(debits, credits, "the ledger must balance");

        final List<LedgerEntry> added = after.subList(before.size(), after.size());
        final long reversals = added.stream().filter(LedgerEntry::isReversal).count();
        assertEquals(added.size() - reversals, reversals,
            "every line of the failed posting must be reversed: " + added);
        assertTrue(added.stream().filter(LedgerEntry::isReversal)
            .allMatch(r -> added.contains(r.reverses().orElseThrow())));

        assertEquals(principalBefore, loans.findById(LOAN).orElseThrow().outstandingPrincipal(),
            "the loan must be unchanged");
    }
}
