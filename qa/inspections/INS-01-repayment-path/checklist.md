# INS-01 - Inspection checklist: repayment and ledger money path

Answer each question for every file in scope. A "No" is a candidate defect: log file, line,
checklist ID and a one-line description in the preparation log.

References: **C-xx** constraints in the M1 design; **BR-xx**
`docs/business-rules.md`; **FR/QR-xx** `qa/rtm.md`.

## A. Money and arithmetic

| ID | Question | Reference |
|---|---|---|
| A-01 | Is every amount a `Money` (`BigDecimal` inside), with no `double`, `float`, `Double` or `Float`, literals and casts included? | C-01 |
| A-02 | Are amounts compared with `compareTo(...) == 0` or `Money` methods built on it, never `BigDecimal.equals` or `==`? | C-02 |
| A-03 | Does every division or `setScale` name an explicit `RoundingMode`, the designed one (HALF_EVEN for `Money`)? | C-01 |
| A-04 | Is the paisa conversion exact both ways (`longValueExact`, `valueOf(paisa, SCALE)`), with no silent truncation or overflow? | C-04 |
| A-05 | Are negative and zero amounts rejected where a rule forbids them and allowed only where one needs them? | BR-20, BR-23 |

## B. Allocation waterfall

| ID | Question | Reference |
|---|---|---|
| B-01 | Is the tier order exactly penalty, fees, interest, principal, with none skipped or swapped? | BR-18; FR-13 |
| B-02 | Within each tier, are instalments taken oldest unpaid first (due date, then sequence), without trusting the input order? | BR-19; FR-13 |
| B-03 | Is the adopted tier-by-tier reading (all penalties across instalments, then all fees, ...) followed throughout and documented in Javadoc? | BR-18, BR-19 (reading of 2026-11-14) |
| B-04 | Is the fees tier handled, though always zero at M2, so a later fee needs no logic change? | BR-18 |
| B-05 | Is a partial payment applied as far as it reaches, leaving the rest outstanding, at 0, 0.01, due - 0.01 and exactly due? | BR-20; FR-14 |
| B-06 | Do the four `Allocation` parts always sum exactly to the amount applied (no money created or lost)? | BR-18; FR-13 |
| B-07 | Is an overpayment (above total due) passed on for advance credit or early settlement, not silently dropped or applied to principal? | BR-21, BR-22; FR-15 |
| B-08 | Is applied principal never more than the account's outstanding principal? | BR-18; FR-19 |
| B-09 | Is the allocator one readable method whose branches map one-to-one to the rules, with no hidden second loop changing the order? | M1 design (allocator as inspection target); FR-13 (BP target) |

## C. Ledger integrity

| ID | Question | Reference |
|---|---|---|
| C-01 | Does every `LedgerEntry` have exactly one non-zero side (debit or credit), enforced by the constructor or factory and again by the schema CHECK? | BR-23; FR-16 |
| C-02 | Does every posting write balanced lines (debits equal credits)? | BR-23; FR-16; QR-03 |
| C-03 | Is `LedgerEntry` immutable: final class, final fields, no setters, no exposed mutable state? | BR-24; FR-17; QR-05 |
| C-04 | Is a correction only a compensating reversal that references the original, with a mandatory reason? | BR-24; FR-17 |
| C-05 | Can an entry be reversed twice, or a reversal reversed, and is that intended and documented? | BR-24 |
| C-06 | Is there no `UPDATE` or `DELETE` against `ledger_entry` anywhere in the code? | BR-24; QR-05 |
| C-07 | Is a payment against a CLOSED or WRITTEN_OFF loan rejected before any ledger line is written? | BR-26; FR-19 |
| C-08 | Is a repeated (bank, reference) pair rejected, without treating cash payments (no reference) as duplicates? | BR-25; FR-18 |

## D. Validation and exceptions

| ID | Question | Reference |
|---|---|---|
| D-01 | Are public method arguments checked for null and range at the module boundary, with a clear message? | Defensive programming (SE423 M2) |
| D-02 | Are failures project exceptions (`LoanDomainException` subclasses, `PersistenceException`), not a context-free `RuntimeException`, `Exception` or `IllegalStateException`? | Structured exceptions (SE423 M2) |
| D-03 | Does each exception message carry diagnostic data (loan id, amount, entry) without leaking anything sensitive? | Structured exceptions |
| D-04 | Is a caught `SQLException` wrapped with its cause preserved, never swallowed or logged and ignored? | Structured exceptions |

## E. Persistence and transactions

| ID | Question | Reference |
|---|---|---|
| E-01 | Is all SQL parameterised (`PreparedStatement` with `?`), with no concatenated user or domain values? | Security; QR-05 |
| E-02 | Is every `Connection`, `Statement` and `ResultSet` closed by try-with-resources on every path? | Resource handling |
| E-03 | Is `PRAGMA foreign_keys = ON` set on every connection, not just once at start-up? | M1 design 3.7 |
| E-04 | Does each read-allocate-write sequence run in one transaction (`BEGIN IMMEDIATE`, busy timeout), rolled back on any failure? | M1 design 3.6, 3.7; QR-03 |
| E-05 | Do the schema CHECK, UNIQUE and FK constraints restate the domain invariants (one-sided entry, reversal has reason, status vs closed_on, unique bank reference)? | M1 design 3.7; BR-23 to BR-26 |
| E-06 | Is the schema migration idempotent and atomic (one transaction per version, `user_version` updated in it)? | M1 design 3.7 |

## F. Concurrency assumptions

| ID | Question | Reference |
|---|---|---|
| F-01 | Are shared types immutable or thread-confined, and is shared mutable state documented with its guarding lock? | M1 design 3.6; R5 |
| F-02 | If a teller and the end-of-day engine post the same loan at once, does the per-account lock or transaction prevent a lost update? | M1 design 3.6; BR-33 |

## G. Tests and documentation

| ID | Question | Reference |
|---|---|---|
| G-01 | Is there a test for each BVA boundary in the rule (0, 0.01, due - 0.01, exactly due, due + 0.01)? | BR-20, BR-21; FR-14, FR-15 |
| G-02 | Do tests cover every allocator basis path (cyclomatic complexity N needs at least N tests)? | FR-13 (BP) |
| G-03 | Do test names and assertions check money values exactly, not just "no exception"? | TDD baseline |
| G-04 | Does every public class and method have Javadoc that matches the code and names the rule it implements? | Checkstyle; documentation |
| G-05 | Does the class trace to an RTM row (backward traceability), and the row back to the class and its test? | `qa/rtm.md` audit rule |
