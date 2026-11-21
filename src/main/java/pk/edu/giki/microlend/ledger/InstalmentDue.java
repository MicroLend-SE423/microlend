package pk.edu.giki.microlend.ledger;

import java.time.LocalDate;
import pk.edu.giki.microlend.common.Money;

/**
 * What is still owed on one instalment, tier by tier: the minimal immutable
 * input the {@link RepaymentAllocator} needs (queue item L-05).
 *
 * <p>This is a ledger-owned type on purpose. The schedule's
 * {@code products.Instalment} (Hassan Khalid) does not exist yet and, in the
 * M1 design, carries principal and interest only; once it exists, the posting
 * path maps it, together with the penalty accrued by the delinquency module,
 * to this record. The ledger therefore does not depend on {@code products}.</p>
 *
 * @param sequence the instalment's position in the schedule, from 1
 * @param dueDate the date the instalment fell due; orders instalments oldest first
 * @param penalty penalty accrued and still unpaid on this instalment
 * @param fees fees still unpaid (always zero at M1, BR-18)
 * @param interest interest still unpaid
 * @param principal principal still unpaid
 */
public record InstalmentDue(int sequence, LocalDate dueDate, Money penalty, Money fees,
                            Money interest, Money principal) {

    /**
     * Validates the instalment.
     *
     * @throws InvalidAllocationException if the sequence is below 1 or any field is null
     */
    public InstalmentDue {
        if (sequence < 1) {
            throw new InvalidAllocationException(
                "an instalment sequence starts at 1, got " + sequence);
        }
        if (dueDate == null || penalty == null || fees == null
            || interest == null || principal == null) {
            throw new InvalidAllocationException(
                "instalment " + sequence + " needs a due date and all four tiers");
        }
    }

    /**
     * Returns everything still owed on this instalment.
     *
     * @return penalty + fees + interest + principal
     */
    public Money total() {
        return penalty.plus(fees).plus(interest).plus(principal);
    }
}
