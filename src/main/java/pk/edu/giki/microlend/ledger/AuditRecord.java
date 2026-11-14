package pk.edu.giki.microlend.ledger;

import java.time.Instant;

/**
 * One line of the teller audit journal (FR-31).
 *
 * @param operator the teller who made the posting
 * @param command the repayment that was attempted
 * @param at when the attempt finished
 * @param posted true if the posting succeeded
 * @param detail {@code "posted"}, or the reason the posting was refused
 */
public record AuditRecord(String operator, RepaymentCommand command, Instant at,
                          boolean posted, String detail) {
}
