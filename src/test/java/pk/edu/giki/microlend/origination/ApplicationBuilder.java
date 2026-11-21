package pk.edu.giki.microlend.origination;

import java.math.BigDecimal;
import java.time.LocalDate;
import pk.edu.giki.microlend.common.Money;

/**
 * Builds loan applications for the origination tests.
 *
 * <p>The defaults pass every eligibility rule: a 36-year-old first-time borrower
 * with no other loans, declared income 100,000.00 (the income the black-box
 * design uses throughout), asking for 50,000.00 over 12 months from a product
 * whose band is 10,000.00 to 100,000.00 and 6 to 24 months. Each method changes
 * one input, so a test states only what it is about.</p>
 */
final class ApplicationBuilder {

    static final LocalDate DISBURSEMENT = LocalDate.of(2026, 12, 1);
    static final Money INCOME = money("100000.00");
    static final ProductBand PRODUCT =
        new ProductBand(money("10000.00"), money("100000.00"), 6, 24);

    private LocalDate dateOfBirth = LocalDate.of(1990, 6, 15);
    private Money existingInstalments = Money.ZERO;
    private int activeLoans;
    private int earlierLoans;
    private int worstDaysPastDue;
    private boolean priorWriteOff;
    private Money requestedAmount = money("50000.00");
    private int tenureMonths = 12;

    static Money money(final String amount) {
        return Money.of(new BigDecimal(amount));
    }

    ApplicationBuilder bornOn(final LocalDate date) {
        this.dateOfBirth = date;
        return this;
    }

    ApplicationBuilder existingInstalments(final String amount) {
        this.existingInstalments = money(amount);
        return this;
    }

    ApplicationBuilder history(final int active, final int earlier, final int worstDpd,
                               final boolean writeOff) {
        this.activeLoans = active;
        this.earlierLoans = earlier;
        this.worstDaysPastDue = worstDpd;
        this.priorWriteOff = writeOff;
        return this;
    }

    ApplicationBuilder requested(final String amount) {
        this.requestedAmount = money(amount);
        return this;
    }

    ApplicationBuilder tenure(final int months) {
        this.tenureMonths = months;
        return this;
    }

    Applicant applicant() {
        return new Applicant(dateOfBirth, INCOME, existingInstalments,
            activeLoans, earlierLoans, worstDaysPastDue, priorWriteOff);
    }

    LoanApplication build() {
        return new LoanApplication(applicant(), requestedAmount, tenureMonths, DISBURSEMENT, PRODUCT);
    }
}
