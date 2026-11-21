# INS-01 - Inspection checklist: repayment and ledger money path

Use during individual preparation and in the meeting. Answer each question for every
file in scope. A "No" is a candidate defect: log the file, line, checklist ID and a
one-line description in your preparation log.

References: **C-xx** constraints in CLAUDE.md and the M1 design; **BR-xx**
`docs/business-rules.md`; **FR/QR-xx** `qa/rtm.md`.

## A. Money and arithmetic

| ID | Question | Reference |
|---|---|---|
| A-01 | Is every monetary amount held as `Money` (or `BigDecimal` inside `Money`), with no `double`, `float`, `Double` or `Float` anywhere, including literals and casts? | C-01 |
| A-02 | Are amounts compared with `compareTo(...) == 0` (or `Money` methods built on it), never `equals` on `BigDecimal` or `==`? | C-01 |
| A-03 | Does every division or `setScale` name an explicit `RoundingMode`, and is it the one the design specifies (HALF_EVEN for `Money`)? | C-01 |
| A-04 | Is the paisa conversion exact in both directions (`longValueExact`, `valueOf(paisa, SCALE)`), with no silent truncation or overflow? | C-04 |
| A-05 | Are negative and zero amounts rejected where the rule forbids them, and allowed only where the rule needs them? | BR-20, BR-23 |

## B. Allocation waterfall

| ID | Question | Reference |
|---|---|---|
| B-01 | Is the tier order exactly penalty, then fees, then interest, then principal, with no tier skipped or swapped? | BR-18; FR-13 |
| B-02 | Within each tier, are instalments taken oldest unpaid first, by due date and then sequence, and is the input order not trusted blindly? | BR-19; FR-13 |
| B-03 | Does the code follow the adopted tier-by-tier reading (all penalties across instalments, then all fees, ...) consistently, and is the reading documented in Javadoc? | BR-18, BR-19 (reading of 2026-11-14) |
| B-04 | Is the fees tier handled even though it is always zero at M2, so introducing a fee later does not need a logic change? | BR-18 |
| B-05 | Is a partial payment applied as far as it reaches, leaving the remainder outstanding, at the boundaries 0, 0.01, due - 0.01 and exactly due? | BR-20; FR-14 |
| B-06 | Is the sum of the four `Allocation` parts always exactly equal to the amount applied (no money created or lost)? | BR-18; FR-13 |
| B-07 | Is an overpayment (amount above total due) handed on for advance credit or early settlement rather than silently dropped or applied to principal? | BR-21, BR-22; FR-15 |
| B-08 | Is principal applied never more than the outstanding principal on the account? | BR-18; FR-19 |
| B-09 | Is the allocator one readable method whose branches map one-to-one to the rules, with no hidden second loop that changes the order? | M1 design (allocator as inspection target); FR-13 (BP target) |

## C. Ledger integrity

| ID | Question | Reference |
|---|---|---|
| C-01 | Does every `LedgerEntry` have exactly one non-zero side (debit or credit), enforced in the constructor or factory and again by the schema CHECK? | BR-23; FR-16 |
| C-02 | Does every posting write balanced lines, so that the sum of debits equals the sum of credits for the posting? | BR-23; FR-16; QR-03 |
| C-03 | Is `LedgerEntry` immutable: final class, final fields, no setters, no exposed mutable state? | BR-24; FR-17; QR-05 |
| C-04 | Is a correction made only by a compensating reversal that references the original, with a mandatory reason? | BR-24; FR-17 |
| C-05 | Can an entry be reversed twice, or a reversal be reversed, and is that behaviour intended and documented? | BR-24 |
| C-06 | Is there no `UPDATE` or `DELETE` against `ledger_entry` anywhere in the code? | BR-24; QR-05 |
| C-07 | Is a payment against a CLOSED or WRITTEN_OFF loan rejected before any ledger line is written? | BR-26; FR-19 |
| C-08 | Is a repeated (bank, reference) pair rejected, and are cash payments (no reference) not falsely treated as duplicates? | BR-25; FR-18 |

## D. Validation and exceptions

| ID | Question | Reference |
|---|---|---|
| D-01 | Are all public method arguments checked for null and range at the module boundary, with a clear message? | Defensive programming (SE423 M2) |
| D-02 | Are failures reported with project exceptions (`LoanDomainException` subclasses, `PersistenceException`), not `RuntimeException`, `Exception` or `IllegalStateException` thrown without context? | Structured exceptions (SE423 M2) |
| D-03 | Does each exception message carry the identifying data (loan id, amount, entry) needed to diagnose it, without leaking anything sensitive? | Structured exceptions |
| D-04 | Is a caught `SQLException` wrapped with its cause preserved, never swallowed or logged and ignored? | Structured exceptions |

## E. Persistence and transactions

| ID | Question | Reference |
|---|---|---|
| E-01 | Are all SQL statements parameterised (`PreparedStatement` with `?`), with no string concatenation of user or domain values? | Security; QR-05 |
| E-02 | Is every `Connection`, `Statement` and `ResultSet` closed by try-with-resources on every path? | Resource handling |
| E-03 | Is `PRAGMA foreign_keys = ON` set on every connection, not just once at start-up? | M1 design 3.7 |
| E-04 | Does each read, allocate and write sequence run in one transaction (`BEGIN IMMEDIATE`, busy timeout), and is it rolled back on any failure? | M1 design 3.6, 3.7; QR-03 |
| E-05 | Do the schema CHECK, UNIQUE and FK constraints re-state the same invariants as the domain code (one-sided entry, reversal has reason, status vs closed_on, unique bank reference)? | M1 design 3.7; BR-23 to BR-26 |
| E-06 | Is the schema migration idempotent and atomic (one transaction per version, `user_version` updated in it)? | M1 design 3.7 |

## F. Concurrency assumptions

| ID | Question | Reference |
|---|---|---|
| F-01 | Are shared types immutable or confined to one thread, and is any shared mutable state documented with the lock that guards it? | M1 design 3.6; R5 |
| F-02 | Could the same loan be posted by a teller and by the end-of-day engine at once, and is the per-account lock or transaction what prevents a lost update here? | M1 design 3.6; BR-33 |

## G. Tests and documentation

| ID | Question | Reference |
|---|---|---|
| G-01 | Is there a test for each BVA boundary listed in the business rule (0, 0.01, due - 0.01, exactly due, due + 0.01)? | BR-20, BR-21; FR-14, FR-15 |
| G-02 | Do the tests cover every basis path of the allocator (cyclomatic complexity N needs at least N tests)? | FR-13 (BP) |
| G-03 | Do test names and assertions check the money values exactly, not just "no exception"? | TDD baseline |
| G-04 | Does every public class and method have Javadoc that matches what the code actually does and names the rule it implements? | Checkstyle; documentation |
| G-05 | Does the class trace to an RTM row (backward traceability), and does the RTM row point back to this class and its test? | `qa/rtm.md` audit rule |
