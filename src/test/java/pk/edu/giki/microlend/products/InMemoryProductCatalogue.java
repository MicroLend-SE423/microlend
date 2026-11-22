package pk.edu.giki.microlend.products;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;

/** Test fake of {@link ProductCatalogue}: products by code, and which loan is on which. */
public final class InMemoryProductCatalogue implements ProductCatalogue {

    private final Map<String, LoanProduct> products = new HashMap<>();
    private final Map<LoanId, String> loanProducts = new HashMap<>();

    /**
     * Adds a product to the catalogue.
     *
     * @param product the product
     */
    public void add(final LoanProduct product) {
        products.put(product.code(), product);
    }

    /**
     * Records that a loan was made on a product.
     *
     * @param loanId the loan
     * @param productCode the product's code
     */
    public void assign(final LoanId loanId, final String productCode) {
        loanProducts.put(loanId, productCode);
    }

    @Override
    public Optional<LoanProduct> find(final String code) {
        return Optional.ofNullable(products.get(code));
    }

    @Override
    public Optional<LoanProduct> findForLoan(final LoanId loanId) {
        return Optional.ofNullable(loanProducts.get(loanId)).map(products::get);
    }
}
