package pk.edu.giki.microlend.products;

import java.math.BigDecimal;
import java.util.List;
import pk.edu.giki.microlend.common.Money;

/**
 * The Product of the Factory Method in {@link LoanProduct}: how one kind of loan splits its
 * repayments into interest and principal (BR-11).
 *
 * <p><strong>The contract every implementation honours</strong> (M1 design section 4.3). It is
 * checked by one shared test that each implementation runs, so substitution is verified by the
 * build and not by good intentions:</p>
 * <ul>
 *   <li>any non-negative principal and any tenure of at least 1 month are accepted; a tenure
 *       below 1 or a negative rate is refused with {@link InvalidScheduleException};</li>
 *   <li>each method returns exactly {@code tenureMonths} amounts, never {@code null}, and no
 *       element is {@code null}; {@link Money} cannot be negative (BR-16);</li>
 *   <li>every amount is rounded to two decimal places HALF_EVEN at the step that produces it
 *       (BR-15);</li>
 *   <li>the principal amounts add up to the principal, give or take rounding of at most one
 *       paisa per instalment; {@link ScheduleAssembler} absorbs that residue into the final
 *       instalment (BR-14);</li>
 *   <li>a zero rate yields zero interest, and a zero principal yields zeros throughout.</li>
 * </ul>
 *
 * <p>The rate is fixed for the life of the loan (BR-17), so it is a plain argument and the
 * calculator keeps no state.</p>
 */
public interface InterestCalculator {

    /**
     * Returns the interest due in each month of the loan.
     *
     * @param principal the amount lent
     * @param annualRatePercent the yearly rate in percent, for example {@code 24.00}
     * @param tenureMonths the number of monthly instalments, at least 1
     * @return {@code tenureMonths} interest amounts in instalment order
     * @throws InvalidScheduleException if the tenure or the rate is invalid
     */
    List<Money> interestSchedule(Money principal, BigDecimal annualRatePercent, int tenureMonths);

    /**
     * Returns the principal due in each month of the loan.
     *
     * @param principal the amount lent
     * @param annualRatePercent the yearly rate in percent, for example {@code 24.00}
     * @param tenureMonths the number of monthly instalments, at least 1
     * @return {@code tenureMonths} principal amounts in instalment order
     * @throws InvalidScheduleException if the tenure or the rate is invalid
     */
    List<Money> principalSchedule(Money principal, BigDecimal annualRatePercent, int tenureMonths);
}
