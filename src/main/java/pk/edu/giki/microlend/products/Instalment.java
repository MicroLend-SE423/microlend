package pk.edu.giki.microlend.products;

import java.time.LocalDate;
import pk.edu.giki.microlend.common.Money;

/**
 * One repayment due on a loan: how much principal and how much interest, and when.
 *
 * <p>Immutable. This is the schedule's view of an instalment; what is still unpaid, and any
 * penalty, belong to the ledger and delinquency modules and are not held here.</p>
 *
 * @param sequence the instalment's position in the schedule, from 1
 * @param dueDate the date the instalment falls due
 * @param principal the principal due
 * @param interest the interest due
 */
public record Instalment(int sequence, LocalDate dueDate, Money principal, Money interest) {

    /**
     * Validates the instalment.
     *
     * @throws InvalidScheduleException if the sequence is below 1 or any other field is null
     */
    public Instalment {
        if (sequence < 1) {
            throw new InvalidScheduleException("an instalment sequence starts at 1, got " + sequence);
        }
        if (dueDate == null || principal == null || interest == null) {
            throw new InvalidScheduleException(
                "instalment " + sequence + " needs a due date, principal and interest");
        }
    }

    /**
     * Returns everything due on this instalment.
     *
     * @return principal plus interest
     */
    public Money total() {
        return principal.plus(interest);
    }
}
