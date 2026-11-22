package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import pk.edu.giki.microlend.common.Money;

/**
 * Flat-rate interest: charged on the original principal for the whole tenure (BR-11).
 *
 * <p>Total interest is {@code principal x rate x tenure / 12}, rounded once. It is spread
 * evenly, and the last month takes whatever rounding is left so the months add up to the
 * total exactly (BR-14). Principal is repaid in equal parts.</p>
 */
public final class FlatRateCalculator implements InterestCalculator {

    @Override
    public List<Money> interestSchedule(final Money principal, final BigDecimal annualRatePercent,
                                        final int tenureMonths) {
        Rounding.requireValid(annualRatePercent, tenureMonths);
        final Money total = Rounding.round(
            Rounding.monthlyInterest(principal, annualRatePercent)
                .multiply(BigDecimal.valueOf(tenureMonths)));
        return evenly(total, tenureMonths);
    }

    @Override
    public List<Money> principalSchedule(final Money principal, final BigDecimal annualRatePercent,
                                         final int tenureMonths) {
        Rounding.requireValid(annualRatePercent, tenureMonths);
        return evenly(principal, tenureMonths);
    }

    /** Splits an amount into equal rounded parts, the last one taking the residue. */
    private static List<Money> evenly(final Money total, final int parts) {
        final Money each = Rounding.round(
            Rounding.exact(total).divide(BigDecimal.valueOf(parts), Rounding.PRECISION));
        final List<Money> amounts = new ArrayList<>(parts);
        Money used = Money.ZERO;
        for (int i = 1; i < parts; i++) {
            amounts.add(each);
            used = used.plus(each);
        }
        amounts.add(residue(total, used, each));
        return List.copyOf(amounts);
    }

    /** What is left of the total; if rounding overshot, the last part falls back to the even share. */
    private static Money residue(final Money total, final Money used, final Money each) {
        return total.isGreaterThan(used) || total.compareTo(used) == 0 ? total.minus(used) : each;
    }
}
