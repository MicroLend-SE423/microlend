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
| FR-13 | Payments applied penalty → fees → interest → principal, oldest first | BR-18, BR-19 | ledger | `RepaymentAllocator.allocate()`, `Allocation` | *planned* | TC-LED-01 | **BP** (CC ≈ 11) | planned |
| FR-14 | Partial payment applied as far as it reaches | BR-20 | ledger | `RepaymentAllocator` | *planned* | TC-LED-02 | BVA | planned |
| FR-15 | Overpayment becomes advance credit, or triggers early settlement | BR-21, BR-22 | ledger | `SettlementPolicy` | *planned* | TC-LED-03 | BVA | planned |
| FR-16 | Every posting writes balanced debit and credit lines | BR-23 | ledger | `CoreRepaymentPoster` | *planned* | TC-LED-04 | INV | planned |
| FR-17 | Ledger entries are immutable; corrections are compensating entries | BR-24 | ledger | `LedgerEntry`, `CompensatingEntryService` | *planned* | TC-LED-05 | INSP + unit | planned |
| FR-18 | Duplicate (bank, reference) pair is rejected | BR-25 | ledger | `IdempotencyGuardPoster` | *planned* | TC-LED-06 | EP | planned |
| FR-19 | Payment against a closed or written-off loan is rejected | BR-26 | ledger | `LoanAccount`, `LoanClosedException` | *planned* | TC-LED-07 | EP | planned |
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
| C-04 | Monetary columns stored as integer minor units | Schema DDL review | planned |

## 4. Coverage summary

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
