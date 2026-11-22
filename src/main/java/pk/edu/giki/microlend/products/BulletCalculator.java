package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import pk.edu.giki.microlend.common.Money;

/**
 * Bullet loan: interest is paid monthly on the full principal and the whole principal falls
 * due with the final instalment (BR-11).
 */
public final class BulletCalculator implements InterestCalculator {

    @Override
    public List<Money> interestSchedule(final Money principal, final BigDecimal annualRatePercent,
                                        final int tenureMonths) {
        Rounding.requireValid(annualRatePercent, tenureMonths);
        final Money monthly = Rounding.round(Rounding.monthlyInterest(principal, annualRatePercent));
        final List<Money> amounts = new ArrayList<>(tenureMonths);
        for (int i = 0; i < tenureMonths; i++) {
            amounts.add(monthly);
        }
        return List.copyOf(amounts);
    }

    @Override
    public List<Money> principalSchedule(final Money principal, final BigDecimal annualRatePercent,
                                         final int tenureMonths) {
        Rounding.requireValid(annualRatePercent, tenureMonths);
        final List<Money> amounts = new ArrayList<>(tenureMonths);
        for (int i = 1; i < tenureMonths; i++) {
            amounts.add(Money.ZERO);
        }
        amounts.add(principal);
        return List.copyOf(amounts);
    }
}
