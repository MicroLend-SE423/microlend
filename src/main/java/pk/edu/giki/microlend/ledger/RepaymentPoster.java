package pk.edu.giki.microlend.ledger;

/**
 * Posts one repayment: the Component of the Decorator pipeline (M1 design
 * section 5.3). {@link CoreRepaymentPoster} does the posting; decorators add
 * timing, audit and idempotency around it.
 */
public interface RepaymentPoster {

    /**
     * Posts a validated repayment.
     *
     * @param command the repayment
     * @return what was posted
     * @throws pk.edu.giki.microlend.common.LoanDomainException if the posting is refused
     */
    PostingResult post(RepaymentCommand command);
}
