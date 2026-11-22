package pk.edu.giki.microlend.products;

import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;

/**
 * Role through which loan products are looked up by code or by the loan that was made on them.
 * Declared here, implemented by the persistence layer.
 */
public interface ProductCatalogue {

    /**
     * Finds a product by its catalogue code.
     *
     * @param code the product code
     * @return the product, built as the concrete type its row names, or empty if there is none
     * @throws NullPointerException if the code is null
     */
    Optional<LoanProduct> find(String code);

    /**
     * Finds the product a loan was made on.
     *
     * @param loanId the loan
     * @return the loan's product, or empty if the loan does not exist
     * @throws NullPointerException if the loan is null
     */
    Optional<LoanProduct> findForLoan(LoanId loanId);
}
