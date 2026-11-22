package pk.edu.giki.microlend.products;

import java.util.Optional;
import pk.edu.giki.microlend.common.LoanId;

/**
 * Role through which the repayment schedule of a loan is stored and read back (M1 design
 * section 4.5, dependency inversion). Declared here, implemented by the persistence layer.
 *
 * <p>It also serves other modules' need to ask what a loan's schedule is (the
 * {@code ScheduleProvider} role of the M1 component view), so they read the same data the
 * schedule was saved as.</p>
 */
public interface ScheduleRepository {

    /**
     * Stores the schedule of a loan. A schedule is fixed for the life of the loan, so saving a
     * second one for the same loan is refused (BR-17).
     *
     * @param loanId the loan, which must already exist
     * @param schedule the schedule to store
     * @throws NullPointerException if an argument is null
     * @throws pk.edu.giki.microlend.common.LoanDomainException if the loan does not exist, already
     *     has a schedule, or the storage fails
     */
    void save(LoanId loanId, Schedule schedule);

    /**
     * Reads the schedule of a loan.
     *
     * @param loanId the loan
     * @return the schedule, or empty when none has been saved
     * @throws NullPointerException if the loan is null
     */
    Optional<Schedule> scheduleFor(LoanId loanId);
}
