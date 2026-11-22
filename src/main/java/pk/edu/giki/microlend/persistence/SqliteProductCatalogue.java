package pk.edu.giki.microlend.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.products.BulletProduct;
import pk.edu.giki.microlend.products.FlatRateProduct;
import pk.edu.giki.microlend.products.LoanProduct;
import pk.edu.giki.microlend.products.ProductCatalogue;
import pk.edu.giki.microlend.products.ReducingBalanceProduct;

/**
 * The SQLite implementation of {@link ProductCatalogue} over the {@code loan_product} table
 * (queue item H-06).
 *
 * <p>The {@code product_type} column names which ConcreteCreator to build. The mapping is a
 * lookup table from the stored type name to a constructor, not a conditional ladder, and it
 * lives here in the storage layer: the {@code products} workflow never sees a type name.</p>
 */
public final class SqliteProductCatalogue implements ProductCatalogue {

    private static final String COLUMNS = "p.code, p.product_type, p.annual_rate_bp, p.min_amount,"
        + " p.max_amount, p.min_tenure_months, p.max_tenure_months";
    private static final String BY_CODE = "SELECT " + COLUMNS
        + " FROM loan_product p WHERE p.code = ?";
    private static final String BY_LOAN = "SELECT " + COLUMNS
        + " FROM loan l JOIN loan_product p ON p.code = l.product_code WHERE l.id = ?";

    /** Builds a ConcreteCreator from the columns of a catalogue row. */
    @FunctionalInterface
    private interface ProductConstructor {
        LoanProduct create(String code, int annualRateBp, Money minAmount, Money maxAmount,
                           int minTenureMonths, int maxTenureMonths);
    }

    private static final Map<String, ProductConstructor> CONSTRUCTORS = Map.of(
        "FLAT_RATE", FlatRateProduct::new,
        "REDUCING_BALANCE", ReducingBalanceProduct::new,
        "BULLET", BulletProduct::new);

    private final SqliteDatabase database;

    /**
     * Creates the catalogue over a database handle.
     *
     * @param database the database holding the {@code loan_product} table
     * @throws NullPointerException if the database is null
     */
    public SqliteProductCatalogue(final SqliteDatabase database) {
        this.database = Objects.requireNonNull(database, "database is required");
    }

    @Override
    public Optional<LoanProduct> find(final String code) {
        Objects.requireNonNull(code, "product code is required");
        return query(BY_CODE, code, "product " + code);
    }

    @Override
    public Optional<LoanProduct> findForLoan(final LoanId loanId) {
        Objects.requireNonNull(loanId, "loan id is required");
        return query(BY_LOAN, loanId.value(), "the product of loan " + loanId);
    }

    private Optional<LoanProduct> query(final String sql, final String key, final String what) {
        try (Connection connection = database.connect();
             PreparedStatement select = connection.prepareStatement(sql)) {
            select.setString(1, key);
            try (ResultSet row = select.executeQuery()) {
                if (!row.next()) {
                    return Optional.empty();
                }
                final ProductConstructor constructor = CONSTRUCTORS.get(row.getString("product_type"));
                if (constructor == null) {
                    throw new PersistenceException(what + " has an unknown type "
                        + row.getString("product_type"));
                }
                return Optional.of(constructor.create(row.getString("code"),
                    row.getInt("annual_rate_bp"),
                    Paisa.toMoney(row.getLong("min_amount")), Paisa.toMoney(row.getLong("max_amount")),
                    row.getInt("min_tenure_months"), row.getInt("max_tenure_months")));
            }
        } catch (SQLException e) {
            throw new PersistenceException("could not read " + what, e);
        }
    }
}
