package pk.edu.giki.microlend;

import java.time.LocalDate;
import java.util.Objects;
import pk.edu.giki.microlend.common.LoanId;
import pk.edu.giki.microlend.common.Money;
import pk.edu.giki.microlend.ledger.EarlySettlementRebate;
import pk.edu.giki.microlend.products.LoanProduct;
import pk.edu.giki.microlend.products.ProductCatalogue;
import pk.edu.giki.microlend.products.Schedule;
import pk.edu.giki.microlend.products.ScheduleNotFoundException;
import pk.edu.giki.microlend.products.ScheduleRepository;

/**
 * Supplies the ledger's {@link EarlySettlementRebate} role from {@code products} (decision
 * D-03, BR-22, FR-15).
 *
 * <p>It finds the loan's product and schedule and delegates the rule to the product, so there
 * is no switch on product type here. It sits in the root package, next to
 * {@link CompositionRoot}, because implementing a ledger interface inside {@code products}
 * would make {@code products} depend on {@code ledger}, and M1 section 3.3 allows only
 * {@code ledger -> products}.</p>
 */
public final class ProductEarlySettlementRebate implements EarlySettlementRebate {

    private final ProductCatalogue catalogue;
    private final ScheduleRepository schedules;

    /**
     * Creates the adapter.
     *
     * @param catalogue where a loan's product is looked up
     * @param schedules where a loan's schedule is looked up
     * @throws NullPointerException if an argument is null
     */
    public ProductEarlySettlementRebate(final ProductCatalogue catalogue,
                                        final ScheduleRepository schedules) {
        this.catalogue = Objects.requireNonNull(catalogue, "catalogue is required");
        this.schedules = Objects.requireNonNull(schedules, "schedule repository is required");
    }

    @Override
    public Money rebateFor(final LoanId loanId, final LocalDate settlementDate) {
        final LoanProduct product = catalogue.findForLoan(loanId)
            .orElseThrow(() -> new ScheduleNotFoundException(loanId, "product"));
        final Schedule schedule = schedules.scheduleFor(loanId)
            .orElseThrow(() -> new ScheduleNotFoundException(loanId, "schedule"));
        return product.earlySettlementRebate(schedule, settlementDate);
    }
}
