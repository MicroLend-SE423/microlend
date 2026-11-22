# MicroLend - Team Decision Record (M2)

Decisions that other members' code depends on. Each one names the code it is built into,
who has to act, and when it can be reopened. A decision is changed by adding a dated row to
its "History" line and a change-log entry, never by rewriting the text, so the record shows
how it evolved (the same rule as `qa/risk-register.md`).

**Confirmed:** 2026-11-16 by Tughral Hussain on behalf of the team. Muhammad Ibrahim and
Hassan Khalid were out of town. Either of them can reopen any item using the reopen condition
stated with it. Until then, build against these decisions.

---

## D-01 Allocation order: tier by tier (BR-18, BR-19)

- **Context.** BR-18 fixes the tier order (penalty -> fees -> interest -> principal). BR-19 says
  "within that order, the oldest unpaid instalment is settled first". That can be read two ways:
  1. settle each instalment fully before moving to the next; or
  2. pay each tier across all instalments, oldest first, before moving to the next tier.
- **Decision.** Reading 2, tier by tier: all penalties oldest first, then all fees oldest first,
  then all interest oldest first, then all principal oldest first. This is what
  `ledger/RepaymentAllocator.allocate(Money, List<InstalmentDue>)` implements. The
  clarification is recorded under BR-19 in `docs/business-rules.md`.
- **Consequences.**
  - `RepaymentAllocatorTest` (BR-18 to BR-20 cases) and the black-box cases TC-LED-01 and
    TC-LED-02 assume this reading.
  - A payment never pays an older instalment's principal while a newer instalment still owes
    penalty or interest.
  - The fees tier stays zero until a fee type is introduced by a recorded deviation (BR-18).
- **Who must act.** Nobody. It is already implemented. Ibrahim and Hassan only need to know
  it, because penalty (delinquency) and interest (products) are consumed in this order.
- **Reopen if.** A member or the instructor reads BR-19 as "instalment by instalment". The
  change is then confined to `RepaymentAllocator.allocate` and its tests; nothing else changes.
- **History.** 2026-11-14 adopted by Tughral, pending confirmation; 2026-11-16 confirmed.

## D-02 How other modules tell the ledger what is owed (`InstalmentsDueSource`)

- **Context.** The ledger must not depend on `products` or `delinquency` (C-03 and
  `ArchitectureTest`), but the allocator needs principal and interest per instalment (from
  Hassan's schedule) and accrued penalty (from Ibrahim's end-of-day engine). The ledger
  declares the role it needs:

  ```java
  // pk.edu.giki.microlend.ledger.InstalmentsDueSource
  List<InstalmentDue> owing(LoanId loanId, LocalDate businessDate);

  // pk.edu.giki.microlend.ledger.InstalmentDue (record)
  InstalmentDue(int sequence, LocalDate dueDate, Money penalty, Money fees,
                Money interest, Money principal)   // amounts still OUTSTANDING
  ```

- **Decision.**
  - `products.Instalment` plus the accrued penalty are mapped to `ledger.InstalmentDue` by one
    implementation of `InstalmentsDueSource`.
  - That class lives in the root package `pk.edu.giki.microlend`, next to `CompositionRoot`,
    because it reads from both `products` and `delinquency`. Putting it in either module would
    make one depend on the other.
  - It replaces the `instalmentsNotAvailable` placeholder in
    `CompositionRoot.withPlaceholders`, where a TODO names Hassan and Ibrahim.
  - Hassan writes it: schedule side first, with penalty `Money.ZERO`. Ibrahim then plugs in
    the penalty.
- **Open sub-question: which instalments are listed?** The choice is between only those due by
  the business date, or every remaining one. Reading `SettlementPolicy.settle` settles it:
  - Payoff (BR-22, `Kind.PAID_OFF`) is detected only when the account's
    `outstandingPrincipal()` reaches zero after the allocation.
  - If the source lists only instalments due by the business date, a payment larger than
    what is due becomes advance credit (BR-21). The loan closes only when its final
    instalment's principal is paid. **This matches BR-21, and it never charges interest that
    is not yet earned.**
  - If the source lists every remaining instalment, tier-by-tier allocation (D-01) would pay
    all future interest before any overdue principal. That contradicts BR-21 and would charge
    unearned interest on every ordinary payment.
  - **Recommendation, adopted for M2:** list only instalments with `dueDate <= businessDate`,
    amounts net of what has already been paid.
  - Early settlement before maturity (BR-22) then needs its own explicit "settle loan"
    operation. That operation lists all remaining principal plus the product's settlement
    interest. It is an M3 item for Tughral (ledger) and Hassan (products, rebate). It is not
    needed for M2.
- **Gap this exposes (action for Tughral).** "Net of what has already been paid" needs
  paid-to-date per instalment, and nothing records it yet:
  - `ledger_entry` has no instalment column;
  - `PostingResult.allocation()` holds the split per sequence, but it is not persisted.
  - **Agreed fix:** `CoreRepaymentPoster` records each posting's per-instalment split through
    a new ledger role (for example `InstalmentPaymentsRecorder`). It is backed by a new
    append-only table `instalment_payment (loan_id, sequence, posted_at, penalty, fees,
    interest, principal)` in schema version 2. The source sums that table.
  - Until it lands, the source may treat nothing as paid. That is good enough for a demo of
    the first posting on a fresh loan, not for repeated postings.
- **Who must act.** Hassan: the source, schedule side. Ibrahim: the penalty per instalment.
  Tughral: paid-to-date recording.
- **Reopen if.** The team wants overpayments to prepay principal instead of becoming advance
  credit. That would be a change to BR-21 itself.
- **History.** 2026-11-14 interface added (L-08); 2026-11-16 decided.

## D-03 Early-settlement rebate comes from `products` (BR-22)

- **Context.** BR-22 differs per product:
  - reducing-balance refunds unearned interest;
  - flat-rate gets no rebate;
  - bullet refunds unaccrued monthly interest only.

  That is product knowledge. The ledger asks for the amount through:

  ```java
  // pk.edu.giki.microlend.ledger.EarlySettlementRebate
  Money rebateFor(LoanId loanId, LocalDate settlementDate);   // zero when none; never null
  ```

- **Decision.**
  - `products` implements BR-22 with **one implementation per product**, polymorphically:
    each `LoanProduct` subclass knows its own rebate rule.
  - One `products` class implementing `EarlySettlementRebate` looks up the loan's product
    and delegates to it. No `switch` on product type.
  - It replaces the `rebateNotAvailable` placeholder in `CompositionRoot.withPlaceholders`.
- **Consequences.**
  - `SettlementPolicy` already calls the source, and reports the amount in
    `SettlementOutcome`.
  - Posting the rebate as an `INTEREST_REBATE` ledger line is not built yet. It needs Tughral
    (ledger) once the amount is real.
  - FR-15 stays *partial* in the RTM until both are done.
- **Who must act.** Hassan: implementations and tests at the BR-22 boundaries (exact payoff,
  payoff + 0.01, per product). Tughral: the rebate ledger line.
- **Reopen if.** The team decides the rebate should be computed from the schedule alone in
  `ledger`. That is not recommended, because it puts product arithmetic in the ledger.
- **History.** 2026-11-14 interface added (L-06); 2026-11-16 decided.

## D-04 Ledger posting shape: `PAYMENT_RECEIVED` debit and `LedgerWriter.appendAll`

- **Context.** BR-23 requires every posting to balance. The M1 entry types had no leg to
  balance the tier credits against.
- **Decision.** Accepted as built in L-08:
  - each repayment writes one `PAYMENT_RECEIVED` **debit** for the amount received;
  - this is balanced by one **credit** per non-zero tier (`PENALTY_REPAYMENT`,
    `FEE_REPAYMENT`, `INTEREST_REPAYMENT`, `PRINCIPAL_REPAYMENT`, `ADVANCE_CREDIT`);
  - the lines are written with `LedgerWriter.appendAll(List<LedgerEntry>)`, all or nothing in
    one SQLite transaction;
  - schema v1 lists `PAYMENT_RECEIVED` in the `ledger_entry.entry_type` CHECK.
- **Consequences for teammates.**
  - Every module that writes money must use `appendAll` with lines whose debits equal credits.
  - **Penalty accrual (Ibrahim) has no balancing type yet.** Only `PENALTY_ACCRUAL` exists.
    Before the end-of-day engine writes penalties, Tughral adds a balancing credit type (for
    example `PENALTY_INCOME`) to `EntryType` and to the schema CHECK in version 2.
  - Write-off (BR-32) likewise needs a debit/credit pair around `WRITE_OFF`, and a
    `LoanAccount` transition to `WRITTEN_OFF`. `LoanAccount` has `closeAsRepaid` but no
    write-off method yet. Ibrahim asks Tughral for both, or adds them in a PR that Tughral
    reviews.
- **Who must act.** Tughral: the new entry types and the `LoanAccount` write-off transition.
  Ibrahim: uses them.
- **Reopen if.** An accounting review proposes a different chart of entry types. Old rows stay
  valid because the ledger is append-only.
- **History.** 2026-11-14 built (L-08, schema v1); 2026-11-16 accepted.

## D-05 M2 Team Lead and document lead

- **Context.** M2 document leads were not assigned. The M1 plan asks that leads be agreed by the
  team.
- **Decision.** Muhammad Ibrahim is the M2 Team Lead and Integrator, and the document lead
  for **all M2 documents**. That covers the SE423 M2 document and every SE431 M2 deliverable
  under `qa/`.
- **Consequences.**
  - Ibrahim signs off each document before submission and is the person who can explain the
    set as a whole.
  - Each module owner still writes and defends the parts about their own module.
  - The M1 plan was that each member leads each kind of SE431 artifact once across M1 to M3.
    M2 therefore does not count towards that rotation, and M3 should spread the leads across
    all three members.
- **Who must act.** Ibrahim: review every M2 document and fill the owner sections listed in
  `docs/planning/m2-handoff-ibrahim.md`.
- **Reopen if.** The team prefers per-document leads (for example Tughral for static analysis
  and white-box design, Hassan for black-box design). Change the "Document lead" lines and
  rebuild the PDFs.
- **History.** 2026-11-16 decided.

---

## Open items created by these decisions (owner: Tughral Hussain, ledger)

| ID | Item | Needed by | Blocks |
|---|---|---|---|
| T-01 | Record paid-to-date per instalment (`instalment_payment`, schema v2) from `CoreRepaymentPoster` | before repeated postings are demonstrated | D-02, H-08 |
| T-02 | Balancing entry type for penalty accrual; schema v2 CHECK | before I-06 writes penalties | D-04, I-06 |
| T-03 | `LoanAccount` write-off transition, plus `WRITE_OFF` debit/credit pair | before I-07 (write-off) | D-04, I-07 |
| T-04 | `INTEREST_REBATE` line for BR-22, once H-07 returns real amounts | M3 | D-03 |
| T-05 | Explicit early-settlement operation (all remaining principal plus settlement interest) | M3 | D-02 |

## Change log

| Date | Change |
|---|---|
| 2026-11-16 | Record created with D-01 to D-05 and open items T-01 to T-05 (confirmed by Tughral Hussain for the team). |
