package pk.edu.giki.microlend.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.products.BulletProduct;
import pk.edu.giki.microlend.products.FlatRateProduct;
import pk.edu.giki.microlend.products.LoanProduct;
import pk.edu.giki.microlend.products.ReducingBalanceProduct;

/**
 * Tests for {@link SqliteProductCatalogue} over the {@code loan_product} table (FR-08;
 * queue item H-06).
 */
class SqliteProductCatalogueTest {

    @TempDir
    private Path dir;

    private SqliteProductCatalogue catalogue;

    private static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    @BeforeEach
    void seed() throws SQLException {
        final SqliteDatabase database = new SqliteDatabase("jdbc:sqlite:" + dir.resolve("products.db"));
        catalogue = new SqliteProductCatalogue(database);
        try (Connection connection = database.connect();
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("INSERT INTO customer (id, full_name, date_of_birth,"
                + " monthly_income) VALUES ('C-1', 'Test Borrower', '1990-01-15', 6000000)");
            statement.executeUpdate("INSERT INTO loan_product (code, product_type, annual_rate_bp,"
                + " min_amount, max_amount, min_tenure_months, max_tenure_months) VALUES"
                + " ('FLAT', 'FLAT_RATE', 2400, 1000000, 50000000, 3, 24),"
                + " ('REDUCING', 'REDUCING_BALANCE', 1800, 2000000, 80000000, 6, 36),"
                + " ('BULLET', 'BULLET', 3000, 500000, 10000000, 1, 12)");
            statement.executeUpdate("INSERT INTO loan (id, customer_id, product_code, principal,"
                + " outstanding, status, risk_grade, disbursed_on) VALUES ('LN-1', 'C-1', 'REDUCING',"
                + " 2000000, 2000000, 'ACTIVE', 'A', '2027-01-31')");
        }
    }

    @Test
    @DisplayName("each product type is built as its own ConcreteCreator")
    void mapsEachType() {
        assertInstanceOf(FlatRateProduct.class, catalogue.find("FLAT").orElseThrow());
        assertInstanceOf(ReducingBalanceProduct.class, catalogue.find("REDUCING").orElseThrow());
        assertInstanceOf(BulletProduct.class, catalogue.find("BULLET").orElseThrow());
    }

    @Test
    @DisplayName("the rate and bands are read from the row, converted from paisa")
    void readsCatalogueData() {
        final LoanProduct product = catalogue.find("REDUCING").orElseThrow();
        assertEquals("REDUCING", product.code());
        assertEquals(1800, product.annualRateBp());
        assertEquals(money("20000.00"), product.minAmount());
        assertEquals(money("800000.00"), product.maxAmount());
        assertEquals(6, product.minTenureMonths());
        assertEquals(36, product.maxTenureMonths());
    }

    @Test
    @DisplayName("an unknown code is empty, not an error")
    void unknownCode() {
        assertEquals(Optional.empty(), catalogue.find("NOPE"));
    }

    @Test
    @DisplayName("the product of a loan is found through the loan's product code")
    void productOfLoan() {
        assertEquals("REDUCING", catalogue.findForLoan(LoanId.of("LN-1")).orElseThrow().code());
        assertEquals(Optional.empty(), catalogue.findForLoan(LoanId.of("LN-404")));
    }

    @Test
    @DisplayName("a product read from the catalogue generates a schedule")
    void productIsUsable() {
        final LoanProduct product = catalogue.find("FLAT").orElseThrow();
        assertEquals(6,
            product.generateSchedule(money("12000.00"), 6, LocalDate.of(2027, 1, 31))
                .instalments().size());
    }
}
