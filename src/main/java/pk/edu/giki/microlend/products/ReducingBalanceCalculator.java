package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import pk.edu.giki.microlend.common.Money;

/**
 * Reducing-balance loan: interest is charged on the balance still outstanding and the
 * borrower pays an equal instalment each month (BR-11).
 *
 * <p>The instalment is {@code P x i x (1+i)^n / ((1+i)^n - 1)} with {@code i} the monthly
 * rate, rounded once. Each month's interest is the balance times {@code i}, rounded; the
 * rest of the instalment repays principal. The final month repays whatever balance is left,
 * so the principal always adds up exactly and the rounding shows up in that last instalment
 * (BR-14).</p>
 */
public final class ReducingBalanceCalculator implements InterestCalculator {

    private static final BigDecimal TWELVE_HUNDRED = new BigDecimal("1200");

    @Override
    public List<Money> interestSchedule(final Money principal, final BigDecimal annualRatePercent,
                                        final int tenureMonths) {
        Rounding.requireValid(annualRatePercent, tenureMonths);
        final List<Money> interest = new ArrayList<>(tenureMonths);
        amortise(principal, annualRatePercent, tenureMonths, interest);
        return List.copyOf(interest);
    }

    @Override
    public List<Money> principalSchedule(final Money principal, final BigDecimal annualRatePercent,
                                         final int tenureMonths) {
        Rounding.requireValid(annualRatePercent, tenureMonths);
        return List.copyOf(amortise(principal, annualRatePercent, tenureMonths, new ArrayList<>()));
    }

    /** Runs the amortisation, filling {@code interestOut} and returning the principal parts. */
    private static List<Money> amortise(final Money principal, final BigDecimal annualRatePercent,
                                        final int tenureMonths, final List<Money> interestOut) {
        final Money instalment = instalment(principal, annualRatePercent, tenureMonths);
        final List<Money> principalOut = new ArrayList<>(tenureMonths);
        Money balance = principal;
        for (int month = 1; month <= tenureMonths; month++) {
            final Money interest = Rounding.round(Rounding.monthlyInterest(balance, annualRatePercent));
            Money repaid = balance;
            if (month < tenureMonths) {
                repaid = instalment.isGreaterThan(interest) ? instalment.minus(interest) : Money.ZERO;
                if (repaid.isGreaterThan(balance)) {
                    repaid = balance;
                }
            }
            interestOut.add(interest);
            principalOut.add(repaid);
            balance = balance.minus(repaid);
        }
        return principalOut;
    }

    /** The equal monthly payment; with no interest it is simply the even share of principal. */
    private static Money instalment(final Money principal, final BigDecimal annualRatePercent,
                                    final int tenureMonths) {
        if (annualRatePercent.signum() == 0) {
            return Rounding.round(Rounding.exact(principal)
                .divide(BigDecimal.valueOf(tenureMonths), Rounding.PRECISION));
        }
        final BigDecimal monthlyRate = annualRatePercent.divide(TWELVE_HUNDRED, Rounding.PRECISION);
        final BigDecimal growth = BigDecimal.ONE.add(monthlyRate).pow(tenureMonths, Rounding.PRECISION);
        return Rounding.round(Rounding.exact(principal).multiply(monthlyRate).multiply(growth)
            .divide(growth.subtract(BigDecimal.ONE), Rounding.PRECISION));
    }
}
