package pk.edu.giki.microlend.delinquency;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import pk.edu.giki.microlend.common.Money;

/**
 * Loan-loss provision by classification (BR-31, FR-24): Standard 1%, Watch 5%,
 * Substandard 25%, Doubtful 50%, Loss 100% of outstanding principal. Stateless.
 */
public final class ProvisioningPolicy {

    private static final Map<Classification, BigDecimal> PERCENT = Map.of(
        Classification.STANDARD, new BigDecimal("1"),
        Classification.WATCH, new BigDecimal("5"),
        Classification.SUBSTANDARD, new BigDecimal("25"),
        Classification.DOUBTFUL, new BigDecimal("50"),
        Classification.LOSS, new BigDecimal("100"));

    /**
     * The provision required for a loan.
     *
     * @param classification the loan's classification
     * @param outstandingPrincipal the loan's outstanding principal
     * @return the provision amount
     */
    public Money provisionFor(final Classification classification,
                              final Money outstandingPrincipal) {
        Objects.requireNonNull(classification, "classification is required");
        Objects.requireNonNull(outstandingPrincipal, "outstanding principal is required");
        return outstandingPrincipal.percentage(PERCENT.get(classification));
    }
}
