package pk.edu.giki.microlend.origination;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.DISBURSEMENT;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.INCOME;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.PRODUCT;
import static pk.edu.giki.microlend.origination.ApplicationBuilder.money;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import pk.edu.giki.microlend.common.LoanDomainException;
import pk.edu.giki.microlend.common.Money;

/**
 * Tests for {@link LoanApplication}, {@link Applicant} and {@link ProductBand}:
 * an invalid application cannot be constructed (M1 design section 3.5), and the
 * derived values the rules use (age, maturity date, burden ratio).
 */
class LoanApplicationTest {

    private static final LocalDate BORN = LocalDate.of(1990, 6, 15);

    @Test
    @DisplayName("a valid application keeps its inputs; maturity is disbursement plus tenure")
    void validApplication() {
        final LoanApplication application = new ApplicationBuilder().build();
        assertEquals(money("50000.00"), application.requestedAmount());
        assertEquals(12, application.tenureMonths());
        assertEquals(DISBURSEMENT, application.disbursementDate());
        assertEquals(LocalDate.of(2027, 12, 1), application.maturityDate());
    }

    @Test
    @DisplayName("age is counted in completed years: the day before the 18th birthday is 17")
    void ageInCompletedYears() {
        final Applicant applicant =
            new ApplicationBuilder().bornOn(LocalDate.of(2008, 12, 2)).applicant();
        assertEquals(17, applicant.ageOn(LocalDate.of(2026, 12, 1)));
        assertEquals(18, applicant.ageOn(LocalDate.of(2026, 12, 2)));
    }

    @Test
    @DisplayName("BR-04: the burden adds the new instalment to the existing ones")
    void burdenIncludesExistingInstalments() {
        final LoanApplication application =
            new ApplicationBuilder().existingInstalments("30000.00").build();
        final BigDecimal burden = application.burdenPercent(money("10000.00"));
        assertEquals(0, burden.compareTo(new BigDecimal("40.00")));
    }

    @Test
    @DisplayName("BR-04: a burden one paisa over 40% is rounded up to 40.01, never down to 40.00")
    void burdenRoundsUp() {
        final LoanApplication application = new ApplicationBuilder().build();
        final BigDecimal burden = application.burdenPercent(money("40000.01"));
        assertEquals(0, burden.compareTo(new BigDecimal("40.01")));
    }

    @Test
    @DisplayName("an applicant needs a date of birth, an income and existing instalments")
    void rejectsMissingApplicantField() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new Applicant(null, INCOME, Money.ZERO, 0, 0, 0, false));
    }

    @Test
    @DisplayName("rule gap EC-20: a declared income of 0.00 is refused, since BR-04 divides by it")
    void rejectsZeroIncome() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new Applicant(BORN, Money.ZERO, Money.ZERO, 0, 0, 0, false));
    }

    @ParameterizedTest(name = "active {0}, earlier {1}, worst DPD {2} is refused")
    @CsvSource({"-1, 0, 0", "0, -1, 0", "0, 0, -1"})
    @DisplayName("loan counts and days past due may not be negative")
    void rejectsNegativeHistory(final int active, final int earlier, final int worstDpd) {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new Applicant(BORN, INCOME, Money.ZERO, active, earlier, worstDpd, false));
    }

    @Test
    @DisplayName("every active loan is also an earlier loan")
    void rejectsActiveLoansMissingFromEarlier() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new Applicant(BORN, INCOME, Money.ZERO, 2, 1, 0, false));
    }

    @Test
    @DisplayName("a written-off loan is an earlier loan that is not active (BR-03)")
    void rejectsWriteOffMissingFromEarlier() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new Applicant(BORN, INCOME, Money.ZERO, 1, 1, 0, true));
        assertDoesNotThrow(() -> new Applicant(BORN, INCOME, Money.ZERO, 1, 2, 0, true));
    }

    @Test
    @DisplayName("an applicant with no earlier loan cannot have days past due")
    void rejectsDaysPastDueWithoutEarlierLoan() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new Applicant(BORN, INCOME, Money.ZERO, 0, 0, 5, false));
    }

    @Test
    @DisplayName("a product band needs both amounts, and its minimum may not exceed its maximum")
    void rejectsInvalidAmountBand() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new ProductBand(null, money("100000.00"), 6, 24));
        assertThrows(InvalidLoanApplicationException.class,
            () -> new ProductBand(money("100000.01"), money("100000.00"), 6, 24));
        assertDoesNotThrow(() -> new ProductBand(money("50000.00"), money("50000.00"), 6, 24));
    }

    @ParameterizedTest(name = "tenure band {0} to {1} months is refused")
    @CsvSource({"0, 24", "25, 24"})
    @DisplayName("a tenure band starts at one month or more and its minimum may not exceed its maximum")
    void rejectsInvalidTenureBand(final int minimum, final int maximum) {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new ProductBand(money("10000.00"), money("100000.00"), minimum, maximum));
    }

    @Test
    @DisplayName("an application needs an applicant, an amount, a disbursement date and a product")
    void rejectsMissingApplicationField() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new LoanApplication(null, money("50000.00"), 12, DISBURSEMENT, PRODUCT));
    }

    @Test
    @DisplayName("the requested amount must be greater than zero")
    void rejectsZeroRequestedAmount() {
        final Applicant applicant = new ApplicationBuilder().applicant();
        assertThrows(InvalidLoanApplicationException.class,
            () -> new LoanApplication(applicant, Money.ZERO, 12, DISBURSEMENT, PRODUCT));
    }

    @Test
    @DisplayName("the tenure must be at least one month")
    void rejectsZeroTenure() {
        final Applicant applicant = new ApplicationBuilder().applicant();
        assertThrows(InvalidLoanApplicationException.class,
            () -> new LoanApplication(applicant, money("50000.00"), 0, DISBURSEMENT, PRODUCT));
    }

    @Test
    @DisplayName("the disbursement date must be after the date of birth")
    void rejectsDisbursementBeforeBirth() {
        assertThrows(InvalidLoanApplicationException.class,
            () -> new ApplicationBuilder().bornOn(DISBURSEMENT).build());
    }

    @Test
    @DisplayName("an invalid application is a domain exception, so it may cross a module boundary")
    void invalidApplicationIsDomainException() {
        final InvalidLoanApplicationException thrown = assertThrows(
            InvalidLoanApplicationException.class,
            () -> new ApplicationBuilder().tenure(0).build());
        assertInstanceOf(LoanDomainException.class, thrown);
    }
}
