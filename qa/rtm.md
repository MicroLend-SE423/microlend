# MicroLend — Requirements Traceability Matrix

**Version:** 1.1 (Milestone 1) · **Owner at M1:** Hassan Khalid · ownership rotates each milestone
**Status at M1:** requirements defined and verification approach planned. Design, code and test-result columns are filled from M2 onward.

This matrix is maintained in the repository and versioned with the code, so the state of traceability at any baseline tag can be reproduced.

## How it is used

Traceability runs in **both directions**, and each direction finds a defect the other cannot:

| Direction | Question answered | Defect it exposes |
|---|---|---|
| **Forward** — requirement → design → code → test | *Did we build and verify everything that was asked for?* | **Test gap**: a requirement implemented but never verified, so there is no objective evidence of conformity |
| **Backward** — code → design → requirement | *Why does this code exist?* | **Rogue code / gold plating**: a module traceable to no approved requirement — unbudgeted maintenance and enlarged attack surface |

**Audit rule, applied at every milestone:** every requirement must have at least one test, and every production class must trace to at least one requirement. Both checks are run and their results recorded in the milestone report.

## Legend

- **Status:** `planned` (M1) → `implemented` → `verified` (test passing) → `baselined` (tagged)
- **BR-xx** references are rules in `docs/business-rules.md`
- Test design techniques: **EP** equivalence partitioning · **BVA** boundary value analysis · **BP** basis path · **INV** invariant/property test · **INSP** inspection checklist item

---

## 1. Functional requirements

| Req ID | Requirement | Business rule | Module | Design element | Code artifact | Test case ID | Technique | Status |
|---|---|---|---|---|---|---|---|---|
| FR-01 | Applicant at least 18 at disbursement, at most 65 at maturity | BR-01, BR-02 | origination | `AgeRule` | *planned* | TC-ORG-01 | BVA | planned |
| FR-02 | At most 2 active loans, counting the one applied for | BR-03 | origination | `ActiveLoanLimitRule` | *planned* | TC-ORG-02 | EP + BVA | planned |
| FR-03 | Instalment burden at most 40% of declared income | BR-04 | origination | `BurdenRatioRule` | *planned* | TC-ORG-03 | BVA | planned |
| FR-04 | Prior write-off rejects the application | BR-05 | origination | `WriteOffHistoryRule` | *planned* | TC-ORG-04 | EP | planned |
| FR-05 | Rejection states a typed reason, evaluated in rule order | BR-08 | origination | `EligibilityEvaluator`, `Decision`, `RejectionReason` | *planned* | TC-ORG-05 | EP | planned |
| FR-06 | Risk grade assigned from burden ratio and repayment history; adjusts rate and caps approved amount | BR-09, BR-10 | origination | `RiskGrade`, `Decision` | *planned* | TC-ORG-06 | EP + BVA | planned |
| FR-07 | Amount and tenure must be within the product's bands | BR-06, BR-07 | origination | `ProductBandRule` | *planned* | TC-ORG-07 | BVA | planned |
| FR-08 | Schedules generated for flat-rate, reducing-balance and bullet products | BR-11, BR-17 | products | `LoanProduct` + 3 subclasses; `InterestCalculator` + 3 calculators | *planned* | TC-PRD-01 | Golden file | planned |
| FR-09 | Instalment dates follow disbursement day-of-month, clamped for short months | BR-12 | products | `DueDateCalculator` | *planned* | TC-PRD-02 | BVA | planned |
| FR-10 | Grace period defers the first instalment while interest accrues | BR-13 | products | `GracePeriodPolicy` | *planned* | TC-PRD-03 | EP + BVA | planned |
| FR-11 | Instalments sum exactly to principal plus total interest | BR-14, BR-15 | products | `ScheduleAssembler`, `Schedule`, `Instalment` | *planned* | TC-PRD-04 | INV | planned |
| FR-12 | A schedule contains exactly `tenureMonths` instalments | BR-16 | products | `ScheduleAssembler` | *planned* | TC-PRD-05 | EP | planned |
| FR-13 | Payments applied penalty → fees → interest → principal, oldest first | BR-18, BR-19 | ledger | `RepaymentAllocator.allocate()`, `Allocation` | `ledger/RepaymentAllocator.java`, `InstalmentDue.java`, `AllocationResult.java` | TC-LED-01 → `RepaymentAllocatorTest` (TierOrder, OldestFirst) | **BP** (CC ≈ 11) | implemented (2026-11-14; awaiting PR CI) |
| FR-14 | Partial payment applied as far as it reaches | BR-20 | ledger | `RepaymentAllocator` | `ledger/RepaymentAllocator.java` | TC-LED-02 → `RepaymentAllocatorTest.PartialPayment` | BVA | implemented (2026-11-14; awaiting PR CI) |
| FR-15 | Overpayment becomes advance credit, or triggers early settlement | BR-21, BR-22 | ledger | `SettlementPolicy` | `ledger/SettlementPolicy.java`, `SettlementOutcome.java`, `EarlySettlementRebate.java` (rebate amount supplied by `products`, not yet implemented) | TC-LED-03 → `SettlementPolicyTest` | BVA | partial (2026-11-14; per-product rebate outstanding) |
| FR-16 | Every posting writes balanced debit and credit lines | BR-23 | ledger | `CoreRepaymentPoster` | `ledger/CoreRepaymentPoster.java`, `LedgerImbalanceException.java`; `LedgerWriter.appendAll` (`persistence/SqliteLedgerRepository.java`) | TC-LED-04 → `CoreRepaymentPosterTest`, `SqliteLedgerRepositoryTest.appendAllIsAtomic` | INV | implemented for repayment postings (2026-11-14; awaiting PR CI); rebate and end-of-day postings not covered |
| FR-17 | Ledger entries are immutable; corrections are compensating entries | BR-24 | ledger | `LedgerEntry`, `CompensatingEntryService` | *planned* | TC-LED-05 | INSP + unit | planned |
| FR-18 | Duplicate (bank, reference) pair is rejected | BR-25 | ledger | `IdempotencyGuardPoster` | *planned* | TC-LED-06 | EP | planned |
| FR-19 | Payment against a closed or written-off loan is rejected | BR-26 | ledger | `LoanAccount`, `LoanClosedException` | `ledger/LoanAccount.java`, `LoanStatus.java`, `LoanClosedException.java` | TC-LED-07 → `LoanAccountTest` | EP | verified |
| FR-20 | Repayments importable from each supported bank's statement format | — | ledger.importing | `HblStatementAdapter`, `McbStatementAdapter` | *planned* | TC-LED-08 | EP | planned |
| FR-21 | Penalty accrues at 0.05%/day on overdue principal, capped at 10% | BR-27, BR-28 | delinquency | `PenaltyAccruer` | *planned* | TC-DEL-01 | BVA | planned |
| FR-22 | Days past due counted from the day after the due date | BR-29 | delinquency | `DaysPastDueCalculator` | *planned* | TC-DEL-02 | BVA | planned |
| FR-23 | Accounts bucketed 0 / 1–30 / 31–60 / 61–90 / 91+ days past due and classified | BR-30 | delinquency | `DelinquencyClassifier.classify()` | *planned* | TC-DEL-03 | **BP** (CC ≈ 6) + BVA | planned |
| FR-24 | Provision percentage applied per classification | BR-31 | delinquency | `ProvisioningPolicy` | *planned* | TC-DEL-04 | EP | planned |
| FR-25 | Loans past 180 days overdue are written off automatically | BR-32 | delinquency | `WriteOffPolicy` | *planned* | TC-DEL-05 | BVA | planned |
| FR-26 | End-of-day processing is idempotent per business date | BR-33 | delinquency | `EndOfDayEngine`, `delinquency_snapshot` unique key | *planned* | TC-DEL-06 | INV | planned |
| FR-27 | Classification changes raise an alert event | BR-34 | delinquency | `ClassificationChangeEvent` | *planned* | TC-DEL-07 | EP | planned |
| FR-28 | Customer statement for a date range with running balance | BR-35 | reporting | `StatementRenderer` | *planned* | TC-REP-01 | EP | planned |
| FR-29 | Portfolio reports: PAR30, provision coverage, collection efficiency | BR-36–BR-38 | reporting | `PortfolioReporter` | *planned* | TC-REP-02 | Golden file | planned |
| FR-30 | Reports reproducible as at any past business date | BR-39 | reporting | `PortfolioReporter` | *planned* | TC-REP-03 | EP | planned |
| FR-31 | Teller postings record an audit line naming the operator; imports record one line per file | — | ledger | `AuditLoggingPoster` (teller); import job (per file) | *planned* | TC-LED-09 | EP | planned |
| FR-32 | Portfolio reports exportable as CSV | — | reporting | `ReportExporter`, `CsvReportExporter` | *planned* | TC-REP-04 | EP | planned |

## 2. Quality requirements

| Req ID | Requirement | Target | Verification method | Test / evidence artifact | Status |
|---|---|---|---|---|---|
| QR-01 | End-of-day run over 10,000 loans completes within 20 s on 4 threads | median ≤ 20 s over 5 runs; no run > 25 s | Test | `EndOfDayPerformanceTest` | planned |
| QR-02 | Schedule generation reproduces reference schedules exactly | 0 discrepancy against 30 golden schedules | Test | `GoldenScheduleTest` | planned |
| QR-03 | A failure during posting leaves the ledger balanced | Σ debits = Σ credits at ≥ 10 injected failure points; 0 orphans | Test + demonstration | `PostingFaultInjectionTest` | planned |
| QR-04 | Core methods stay within complexity and size limits | mean CC ≤ 5; no method > 15; no file > 400 lines | Analysis | Static-analysis metric report in CI | planned |
| QR-05 | Ledger entries are not alterable | 0 mutators; 0 `UPDATE`/`DELETE` against `ledger_entry`; all corrections compensating | Inspection | Fagan checklist item + `LedgerImmutabilityTest` | planned |
| QR-06 | End-of-day results identical with and without concurrent posting | 50 runs: 0 lost updates, 0 double accruals, balances match single-threaded run | Test | `EndOfDayConcurrencyTest` (`EndOfDayEngine`, `LockRegistry`) | planned |

## 3. Constraints

| ID | Constraint | Enforcement | Status |
|---|---|---|---|
| C-01 | No `double` or `float` for monetary values | Checkstyle rule, build fails | **active** |
| C-02 | `BigDecimal` compared with `compareTo() == 0` | Inspection checklist (no automated rule at M1) | active |
| C-03 | No cyclic dependencies between module packages | ArchUnit test in CI from M2 | planned |
| C-04 | Monetary columns stored as integer minor units | Schema DDL review; `SchemaMigratorTest.moneyColumnsAreInteger` (`persistence/SchemaMigrator.java`); `SqliteLedgerRepositoryTest.moneyStoredAsPaisa` | verified (PR CI run 42982638575) |

## 4. Coverage summary

M2 incremental evidence (2026-11-06): `LoanId`, `LedgerEntry`, `EntryType`,
`LedgerWriter` and `LedgerReader` now implement the L-01/L-02 foundations.
`LoanIdTest`, `LedgerEntryTest` and `InMemoryLedgerRepositoryTest` pass locally.
The test-only repository verifies loan isolation, append order, immutable
snapshots, repeated movements and null rejection. This is partial evidence for
FR-16, FR-17 and QR-05: balanced transactional posting, persistence and compensating
reversals remain unimplemented, so those requirements retain their planned status.

M2 incremental evidence (2026-11-08): `LoanAccount`, `LoanStatus` and
`LoanClosedException` implement FR-19. `LoanAccountTest` (TC-LED-07) covers the
three equivalence classes of BR-26 (active, closed, written-off) and passes
locally; FR-19 moves to *verified* once PR CI passes. The rejection is only
reachable from a posting path once `CoreRepaymentPoster` (queue item L-08) calls it.

M2 incremental evidence (2026-11-09): FR-19 is *verified*: PR #8 CI run
42471721763 passed for ff8d352. `Allocation` (the four BR-18 tiers, value
equality, every tier required) and `LoanAccount.applyAllocation()` (principal
tier reduces outstanding principal, over-allocation rejected, closed loans refuse
it) are implemented and tested in `AllocationTest` and `LoanAccountTest`. This is
supporting work for FR-13 only: `RepaymentAllocator.allocate()`, which applies the
waterfall and the oldest-first order (TC-LED-01, TC-LED-02), is not implemented, so
FR-13 and FR-14 stay *planned*. `applyAllocation` returns a new account instead of
the `void` shown in M1 View 2, because `LoanAccount` is immutable.

M2 incremental evidence (2026-11-11): `RepaymentCommand` and
`InvalidRepaymentException` (queue item L-07) give the posting pipeline one
validated input. `RepaymentCommandTest` covers the cash and bank equivalence
classes of BR-25, the 0.00 / 0.01 amount boundary, missing fields and a blank
bank or reference. The command only *carries* the (bank, reference) pair; the
duplicate check itself is `IdempotencyGuardPoster` (L-11), so FR-18 stays
*planned*.

M2 incremental evidence (2026-11-11): `CompensatingEntryService.reverse()` and
`LedgerEntry.reversalOf()` (queue item L-12) implement correction by reversal:
a `REVERSAL` entry in the opposite column that references the original and
records a non-blank reason (BR-24). `CompensatingEntryServiceTest` (TC-LED-05)
and `LedgerImmutabilityTest` show the write role offers only `append` and a
correction leaves the original in place with debits equal to credits. This is
partial evidence for FR-17 and QR-05 only: the SQLite repository (L-14) and its
"no `UPDATE`/`DELETE` against `ledger_entry`" check, the Fagan checklist item and
the static-analysis rule do not exist yet, so both keep their planned status.

M2 incremental evidence (2026-11-11): `SchemaMigrator` and `SqliteDatabase`
(queue item L-13) create the seven tables of M1 section 3.7 on the first
connection and turn on foreign keys for every connection. `SchemaMigratorTest`
checks that the database itself refuses rows the code would never write:
negative, zero-zero or two-sided ledger lines (BR-43), a reversal without an
existing original or a reason (BR-24), a repeated (bank, reference) pair while
cash rows and the same reference from another bank are accepted (BR-25), a
second snapshot for the same loan and business date (BR-33), and a loan whose
status and closure date disagree (FR-19). Money columns are `INTEGER` paisa
(C-04). This is schema support only: FR-18 (the guard), FR-26/BR-33 (the
engine) and QR-05 (the repository with no `UPDATE`/`DELETE`, L-14) keep their
planned status.

M2 incremental evidence (2026-11-14): C-04 is *verified*: PR #8 CI run
42982638575 passed for 1091877. `SqliteLedgerRepository` (queue item L-14,
`persistence/SqliteLedgerRepository.java`) implements `LedgerWriter` and
`LedgerReader` against SQLite. `SqliteLedgerRepositoryTest` checks round trips
in append order per loan, paisa storage at the 0.01 and 12,345,678.99
boundaries, reversals stored against the original's row (identical originals
reversed separately), refusal of a reversal whose original is not in the
ledger, translation of storage failures into `PersistenceException`, durability
across instances, and a source scan finding no `UPDATE`/`DELETE` against
`ledger_entry` in production code. This is partial evidence for QR-05 (the
repository half); the Fagan checklist item is still owed, so QR-05 stays
*planned*. FR-17 likewise stays *planned* until the posting path uses it.

M2 incremental evidence (2026-11-14): `RepaymentAllocator.allocate()` (queue
item L-05) applies the BR-18 waterfall tier by tier with the oldest instalment
first in each tier, the BR-19 reading adopted by Tughral on 2026-11-14 and
pending team confirmation. Its input is the ledger-owned `InstalmentDue`
record; Hassan's `products.Instalment` is mapped to it once that class exists,
and the accrued penalty is an input amount (Ibrahim's `PenaltyAccruer` not
required). `RepaymentAllocatorTest` covers part of each tier, fees before
interest, two and three overdue instalments (input out of order), and the
BR-20 boundaries 0 / 0.01 / due − 0.01 / exactly due, plus due + 0.01 returned
as unapplied (the BR-21 hand-off). FR-13 and FR-14 move to *implemented*; they
become *verified* when PR CI passes. The allocator is not yet called from a
posting path (L-08).

M2 incremental evidence (2026-11-14): `SettlementPolicy` (queue item L-06,
partial) turns a surplus into advance credit while principal is owed (BR-21,
boundary due + 0.01), and on payoff closes the account through
`LoanAccount.closeAsRepaid()` and takes the rebate from a caller-supplied
`EarlySettlementRebate` (BR-22, boundaries exact payoff and payoff + 0.01).
`SettlementPolicyTest` (TC-LED-03) covers those boundaries with a stub rebate.
The per-product rebate arithmetic is `products` knowledge and is not
implemented, so FR-15 is recorded as *partial*, not *implemented*.

M2 incremental evidence (2026-11-14): `CoreRepaymentPoster` (queue item L-08)
reads the loan, allocates, settles, and writes one `PAYMENT_RECEIVED` debit
balanced by tier and advance-credit credits through `LedgerWriter.appendAll`,
which the SQLite repository runs in one transaction. `CoreRepaymentPosterTest`
(TC-LED-04) asserts Σ debits = Σ credits after every posting in a sequence,
`LedgerImbalanceException` on unbalanced lines, refusal of closed and unknown
loans, and that the loan is not saved when the ledger write fails;
`SqliteLedgerRepositoryTest.appendAllIsAtomic` shows a failing last line leaves
no line written. FR-16 is *implemented* for repayment postings only: the BR-22
rebate line and end-of-day postings are not written by this class. QR-03 stays
*planned* until the fault-injection test (L-16). `EntryType.PAYMENT_RECEIVED`
and its schema `CHECK` value were added for the debit leg; schema version 1 was
edited in place because it has not been merged or deployed anywhere.

| Measure | M1 | M2 | M3 |
|---|---|---|---|
| Functional requirements defined | 32 | | |
| Quality requirements defined | 6 | | |
| Constraints recorded | 4 | | |
| Requirements with a planned verification approach | 38 / 38 (100%) | | |
| Requirements with executed, passing tests | 0 (planning milestone) | | |
| Production classes traceable to a requirement | n/a | | |
| Test gaps found | n/a | | |
| Rogue code found | n/a | | |

## Change log

| Date | Version | Change |
|---|---|---|
| 2026-10-04 | 1.0 | Matrix created at M1: 30 functional requirements, 6 quality requirements, 4 constraints, each traced to a business rule and a planned verification approach |
| 2026-10-07 | 1.1 | Pre-submission review: FR-31 (audit journal) and FR-32 (CSV export) added so `AuditLoggingPoster` and `ReportExporter` are not rogue code; FR-13 design element corrected to `allocate()`; FR-20 names the bank adapters; QR-02 and QR-04 targets aligned with the SE423 design and the Checkstyle gate; design elements added so every planned class traces (FR-05, FR-08, FR-11, FR-13, QR-06); FR-23 complexity corrected to ≈ 6 |
