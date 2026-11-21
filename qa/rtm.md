# MicroLend — Requirements Traceability Matrix

**Version:** 2.1 (Milestone 2, integrated build `4067d2f` on `feature/delinquency-origination-import`; 2.0 was the ledger build `701c161`) · **Owner at M1:** Hassan Khalid · **Lead at M2:** Muhammad Ibrahim (Team Lead) · ownership rotates each milestone
**Status at M1:** requirements defined and verification approach planned. Design, code and test-result columns are filled from M2 onward.
**Status at M2 (21 Nov 2026):** the `ledger`, `persistence` and `web` rows carry code, tests and CI evidence from PR #8, which was merged into `develop` on 21 Nov. The `origination`, `delinquency` and `ledger.importing` rows carry code and tests that pass in a local build of `4067d2f` (384 tests). Following the rule used since 14 Nov, they are *implemented* until PR CI passes, and then *verified*. PR number and CI run: *to be filled by the lead*. `products` and `reporting` rows stay *planned* until their owner's code reaches `develop`.

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
| FR-01 | Applicant at least 18 at disbursement, at most 65 at maturity | BR-01, BR-02 | origination | `AgeRule` | `origination/AgeRule.java`, `Applicant.java` (`ageOn`, completed years), `LoanApplication.java` (`maturityDate`) | TC-ORG-01 → `AgeRuleTest` (01.1 to 01.7); `EligibilityEvaluatorTest` (05.1, 05.2) | BVA | implemented (tests pass at `4067d2f`; CI pending) |
| FR-02 | At most 2 active loans, counting the one applied for | BR-03 | origination | `ActiveLoanLimitRule` | `origination/ActiveLoanLimitRule.java` | TC-ORG-02 → `ActiveLoanLimitRuleTest` (02.1 to 02.6); `EligibilityEvaluatorTest.writtenOffLoanRejectedByWriteOffRule` | EP + BVA | implemented (tests pass at `4067d2f`; CI pending) |
| FR-03 | Instalment burden at most 40% of declared income | BR-04 | origination | `BurdenRatioRule` | `origination/BurdenRatioRule.java`, `LoanApplication.burdenPercent`; the new loan's instalment is an injected function until `products` supplies it | TC-ORG-03 → `BurdenRatioRuleTest` (03.1 to 03.3), `EligibilityEvaluatorTest.gradeCAtTheBurdenLimit` (03.4), `LoanApplicationTest` (burden rounding, zero income refused) | BVA | implemented (tests pass at `4067d2f`; CI pending) |
| FR-04 | Prior write-off rejects the application | BR-05 | origination | `WriteOffHistoryRule` | `origination/WriteOffHistoryRule.java` | TC-ORG-04 → `WriteOffHistoryRuleTest` (04.1 to 04.3) | EP | implemented (tests pass at `4067d2f`; CI pending) |
| FR-05 | Rejection states a typed reason, evaluated in rule order | BR-08 | origination | `EligibilityEvaluator`, `Decision`, `RejectionReason` | `origination/EligibilityEvaluator.java`, `EligibilityRule.java`, `RuleOutcome.java`, `Decision.java`, `RejectionReason.java`, `IneligibleApplicantException.java` | TC-ORG-05 → `EligibilityEvaluatorTest` (05.1 to 05.9, `stopsAtFirstFailure`), `DecisionTest` | EP | implemented (tests pass at `4067d2f`; CI pending) |
| FR-06 | Risk grade assigned from burden ratio and repayment history; adjusts rate and caps approved amount | BR-09, BR-10 | origination | `RiskGrade`, `Decision` | `origination/RiskGrade.java` (grade, spread, cap), `Decision.java`; the spread is applied to a loan's rate at loan creation (I-03), not built because `products` does not exist | TC-ORG-06 → `RiskGradeTest` (06.1 to 06.7, spread and cap per grade), `EligibilityEvaluatorTest` (06.8 to 06.11) | EP + BVA | implemented for grade, spread and cap (tests pass at `4067d2f`; CI pending); rate applied at loan creation not built |
| FR-07 | Amount and tenure must be within the product's bands | BR-06, BR-07 | origination | `ProductBandRule` | `origination/ProductBandRule.java`, `ProductBand.java`; band values come from the product, supplied by the caller until `products` exists | TC-ORG-07 → `ProductBandRuleTest` (07.1 to 07.7), `LoanApplicationTest` (invalid bands refused); schema `SchemaMigratorTest.productBands` | BVA | implemented (tests pass at `4067d2f`; CI pending) |
| FR-08 | Schedules generated for flat-rate, reducing-balance and bullet products | BR-11, BR-17 | products | `LoanProduct` + 3 subclasses; `InterestCalculator` + 3 calculators | *planned* | TC-PRD-01 | Golden file | planned |
| FR-09 | Instalment dates follow disbursement day-of-month, clamped for short months | BR-12 | products | `DueDateCalculator` | *planned* | TC-PRD-02 | BVA | planned |
| FR-10 | Grace period defers the first instalment while interest accrues | BR-13 | products | `GracePeriodPolicy` | *planned* | TC-PRD-03 | EP + BVA | planned |
| FR-11 | Instalments sum exactly to principal plus total interest | BR-14, BR-15 | products | `ScheduleAssembler`, `Schedule`, `Instalment` | *planned* | TC-PRD-04 | INV | planned |
| FR-12 | A schedule contains exactly `tenureMonths` instalments | BR-16 | products | `ScheduleAssembler` | *planned* | TC-PRD-05 | EP | planned |
| FR-13 | Payments applied penalty → fees → interest → principal, oldest first | BR-18, BR-19 | ledger | `RepaymentAllocator.allocate()`, `Allocation` | `ledger/RepaymentAllocator.java`, `InstalmentDue.java`, `AllocationResult.java` | TC-LED-01 → `RepaymentAllocatorTest` (TierOrder, OldestFirst) | **BP** (V(G) = 10 measured; M1 estimate was about 11) | verified (PR #8 CI passed for c17052f); BR-19 reading is team decision D-01 |
| FR-14 | Partial payment applied as far as it reaches | BR-20 | ledger | `RepaymentAllocator` | `ledger/RepaymentAllocator.java` | TC-LED-02 → `RepaymentAllocatorTest.PartialPayment` | BVA | verified (PR #8 CI passed for c17052f) |
| FR-15 | Overpayment becomes advance credit, or triggers early settlement | BR-21, BR-22 | ledger | `SettlementPolicy` | `ledger/SettlementPolicy.java`, `SettlementOutcome.java`, `EarlySettlementRebate.java` (rebate amount supplied by `products`, not yet implemented) | TC-LED-03 → `SettlementPolicyTest` | BVA | partial (2026-11-14; per-product rebate outstanding; the rebate ledger line is not written, DEF-013) |
| FR-16 | Every posting writes balanced debit and credit lines | BR-23 | ledger | `CoreRepaymentPoster` | `ledger/CoreRepaymentPoster.java`, `LedgerImbalanceException.java`; `LedgerWriter.appendAll` (`persistence/SqliteLedgerRepository.java`); end-of-day penalty pair `PENALTY_ACCRUAL` / `PENALTY_INCOME` (`delinquency/EndOfDayEngine.java`, `persistence/SqliteDailyResultStore.java`) | TC-LED-04 → `CoreRepaymentPosterTest`, `SqliteLedgerRepositoryTest.appendAllIsAtomic`; end of day: `EndOfDayEngineTest.processesEveryLoan`, `SqliteDailyResultStoreTest.oneTransaction`, `ConcurrentTellerAndEndOfDayTest` (Σ debits = Σ credits over the whole ledger) | INV | implemented for repayment postings (tests pass in PR #8 CI for 34a7bb7) and end-of-day penalty postings (tests pass at `4067d2f`); the BR-22 rebate line is not written (DEF-013), so not yet *verified* as a whole |
| FR-17 | Ledger entries are immutable; corrections are compensating entries | BR-24 | ledger | `LedgerEntry`, `CompensatingEntryService` | `ledger/LedgerEntry.java`, `CompensatingEntryService.java`; no UPDATE/DELETE in `persistence/SqliteLedgerRepository.java`; schema version 2 unique index `ledger_entry_reversed_once` (`persistence/SchemaMigrator.java`, DEF-010) | TC-LED-05 → `LedgerEntryTest`, `CompensatingEntryServiceTest`, `LedgerImmutabilityTest`; `SchemaVersion2Test.reversedOnlyOnce` | INSP + unit | implemented; unit tests pass in PR #8 CI, the reversed-once test at `4067d2f`; inspection item (INS-01 checklist) pending. A version-1 database that already holds a double reversal cannot be upgraded until it is removed (DEF-023, open) |
| FR-18 | Duplicate (bank, reference) pair is rejected | BR-25 | ledger | `IdempotencyGuardPoster` | `ledger/IdempotencyGuardPoster.java`, `PaymentReferences.java`, `InMemoryPaymentReferences.java`, `DuplicatePaymentException.java`; durable store `persistence/SqlitePaymentReferences.java` over the `payment_reference` table (schema version 2), wired into the import chain by `CompositionRoot`; used by the import job (`ledger/importing/StatementImportJob.java`) | TC-LED-06 → `IdempotencyGuardPosterTest`, `SqlitePaymentReferencesTest`; from statement files: `StatementImportJobTest`, `StatementImportWiringTest.secondImportIsRefused`, `.secondImportAfterRestartIsRefused` | EP | implemented: guard verified in PR #8 CI (71d8b3f); durable store (red `5b758e9`, green `8401031`, DEF-012 fixed) passes at `4067d2f`, CI pending. Limitation: a crash between the claim and the posting leaves the pair claimed, so that row would be refused later; `payment` rows written inside the posting transaction are M3 work |
| FR-19 | Payment against a closed or written-off loan is rejected | BR-26 | ledger | `LoanAccount`, `LoanClosedException` | `ledger/LoanAccount.java`, `LoanStatus.java`, `LoanClosedException.java` | TC-LED-07 → `LoanAccountTest` | EP | verified |
| FR-20 | Repayments importable from each supported bank's statement format | — | ledger.importing | `HblStatementAdapter`, `McbStatementAdapter` | `ledger/importing/RepaymentSource.java` (Target), `HblStatementAdapter.java`, `McbStatementAdapter.java` (Adapters over Commons CSV `CSVParser`), `StatementImportJob.java` (Client), `ImportReport.java`, `InvalidStatementException.java`, `StatementText.java` (byte-order mark, blank lines); `CompositionRoot.importStatement`; `MicroLendApplication` `import` mode | TC-LED-08 → `HblStatementAdapterTest`, `McbStatementAdapterTest` (incl. `physicalLineNumbers`, `byteOrderMark`: DEF-019, DEF-020 fixed), `StatementImportJobTest`, `StatementImportWiringTest` (production chain on SQLite) | EP | implemented (tests pass at `4067d2f`; CI pending). Run imports with the web server stopped until DEF-022 is fixed |
| FR-21 | Penalty accrues at 0.05%/day on overdue principal, capped at 10% | BR-27, BR-28 | delinquency | `PenaltyAccruer` | `delinquency/PenaltyAccruer.java`; posted by `EndOfDayEngine` as a `PENALTY_ACCRUAL` / `PENALTY_INCOME` pair (`ledger/EntryType.java`, schema version 2) | TC-DEL-01 → `PenaltyAccruerTest` (01.1 to 01.7, daily sums equal the cumulative penalty, `catchesUpSkippedDates`, `firstRunAccruesEveryOverdueDay`); `EndOfDayEngineTest.processesEveryLoan`, `.catchesUpSkippedDates` | BVA | implemented (tests pass at `4067d2f`; CI pending). Skipped dates are caught up from the loan's last snapshot (DEF-018 fixed in `6d30af9`) |
| FR-22 | Days past due counted from the day after the due date | BR-29 | delinquency | `DaysPastDueCalculator` | `delinquency/DaysPastDueCalculator.java` | TC-DEL-02 → `DaysPastDueCalculatorTest` (02.1 to 02.3, oldest unpaid instalment, penalty-only instalment) | BVA | implemented (tests pass at `4067d2f`; CI pending) |
| FR-23 | Accounts bucketed 0 / 1–30 / 31–60 / 61–90 / 91+ days past due and classified | BR-30 | delinquency | `DelinquencyClassifier.classify()` | `delinquency/DelinquencyClassifier.java`, `Classification.java`, `InvalidDaysPastDueException.java` | TC-DEL-03 → `DelinquencyClassifierTest` (03.1 to 03.6; 6 basis paths, white-box section 6) | **BP** (V(G) = 6 measured, 10/10 branches; M1 estimate about 6) + BVA | implemented (tests pass at `4067d2f`; CI pending) |
| FR-24 | Provision percentage applied per classification | BR-31 | delinquency | `ProvisioningPolicy` | `delinquency/ProvisioningPolicy.java`; not yet called by end of day or a report (first caller: FR-29, `reporting`) | TC-DEL-04 → `ProvisioningAndWriteOffTest.provisionByClassification` (04.1 to 04.5) | EP | implemented (tests pass at `4067d2f`; CI pending) |
| FR-25 | Loans past 180 days overdue are written off automatically | BR-32 | delinquency | `WriteOffPolicy` | `delinquency/WriteOffPolicy.java`; `EndOfDayEngine` lists the loans due in `EodReport.writeOffsDue`, shown on the end-of-day page; the write-off posting is not built (team decision open item T-03) | TC-DEL-05 → `ProvisioningAndWriteOffTest` (05.1 to 05.3), `EndOfDayEngineTest.writeOffDueIsReported` | BVA | partial: the decision and the listing pass at `4067d2f`; the loan is not yet moved to written off |
| FR-26 | End-of-day processing is idempotent per business date | BR-33 | delinquency | `EndOfDayEngine`, `delinquency_snapshot` unique key | `delinquency/EndOfDayEngine.java`, `DailyResultStore.java`, `DailySnapshot.java`, `EodReport.java`; `persistence/SqliteDailyResultStore.java` (snapshot and penalty pair in one `BEGIN IMMEDIATE` transaction); `UNIQUE (loan_id, business_date)` | TC-DEL-06 → `EndOfDayEngineTest.processesEveryLoan`, `.idempotentPerBusinessDate`, `.earlierDateAfterALaterRunIsSkipped`; `SqliteDailyResultStoreTest` (`secondRecordIsANoOp`, `oneTransaction`, `concurrentRecordsForTheSameDate`, `lastSnapshot`); `SchemaMigratorTest.snapshotPerLoanPerDate`; `TellerWebServerTest.endOfDayFailureIsReported` (DEF-021) | INV | implemented (tests pass at `4067d2f`; CI pending) |
| FR-27 | Classification changes raise an alert event | BR-34 | delinquency | `ClassificationChangeEvent` | `delinquency/ClassificationChangeEvent.java`, listed in `EodReport` and on the end-of-day page (`web/TellerWebServer.java`) | TC-DEL-07 → `EndOfDayEngineTest.classificationChangeEvents` (07.1 to 07.3), `SqliteDailyResultStoreTest.lastSnapshot`, `TellerWebServerTest.endOfDayRuns` | EP | implemented (tests pass at `4067d2f`; CI pending) |
| FR-28 | Customer statement for a date range with running balance | BR-35 | reporting | `StatementRenderer` | *planned* | TC-REP-01 | EP | planned |
| FR-29 | Portfolio reports: PAR30, provision coverage, collection efficiency | BR-36–BR-38 | reporting | `PortfolioReporter` | *planned* | TC-REP-02 | Golden file | planned |
| FR-30 | Reports reproducible as at any past business date | BR-39 | reporting | `PortfolioReporter` | *planned* | TC-REP-03 | EP | planned |
| FR-31 | Teller postings record an audit line naming the operator; imports record one line per file | — | ledger | `AuditLoggingPoster` (teller); import job (per file) | `ledger/AuditLoggingPoster.java`, `AuditRecord.java`, `AuditJournal.java` (teller half only) | TC-LED-09 → `AuditLoggingPosterTest` | EP | partial: teller half verified in PR #8 CI (a43c681); the import job exists (`StatementImportJob`, FR-20) but writes no audit line per file (Ibrahim) |
| FR-32 | Portfolio reports exportable as CSV | — | reporting | `ReportExporter`, `CsvReportExporter` | *planned* | TC-REP-04 | EP | planned |

## 2. Quality requirements

| Req ID | Requirement | Target | Verification method | Test / evidence artifact | Status |
|---|---|---|---|---|---|
| QR-01 | End-of-day run over 10,000 loans completes within 20 s on 4 threads | median ≤ 20 s over 5 runs; no run > 25 s | Test | `EndOfDayPerformanceTest` | planned |
| QR-02 | Schedule generation reproduces reference schedules exactly | 0 discrepancy against 30 golden schedules | Test | `GoldenScheduleTest` | planned |
| QR-03 | A failure during posting leaves the ledger balanced | Σ debits = Σ credits at ≥ 10 injected failure points; 0 orphans | Test + demonstration | `PostingFaultInjectionTest` (14 points on SQLite; `CoreRepaymentPoster`, `SqliteLedgerRepository.appendAll`) | test passes in PR #8 CI (5502ccf, run 43596151001); demonstration not yet given |
| QR-04 | Core methods stay within complexity and size limits | mean CC ≤ 5; no method > 15; no file > 400 lines | Analysis | Static-analysis metric report in CI | measured on `4067d2f` (JaCoCo, `qa/test-design/white-box.md`, `qa/static-analysis-report.md`): mean 1.8 (544 complexity over 298 methods); highest 12 (`origination.Applicant` constructor), then 10 (`RepaymentAllocator.allocate`, `LedgerEntry.equals`); no method above 15; file length held at 400 by Checkstyle with 0 findings. CI gate not yet wired |
| QR-05 | Ledger entries are not alterable | 0 mutators; 0 `UPDATE`/`DELETE` against `ledger_entry`; all corrections compensating | Inspection | Fagan checklist item + `LedgerImmutabilityTest` | planned: automated checks pass (`LedgerImmutabilityTest`, the source scan in `SqliteLedgerRepositoryTest`, `SchemaVersion2Test.reversedOnlyOnce`); INS-01 checklist item still owed. Note for inspectors: schema version 2 rebuilds `ledger_entry` once by copy and rename (`INSERT ... SELECT`, ids and reversal links kept, `SchemaVersion2Test.upgradesVersion1InPlace`) and issues no `UPDATE` or `DELETE` |
| QR-06 | End-of-day results identical with and without concurrent posting | 50 runs: 0 lost updates, 0 double accruals, balances match single-threaded run | Test | `ConcurrentTellerAndEndOfDayTest` (production wiring on SQLite: `EndOfDayEngine`, `LockRegistry`, `LockingPoster`); supporting `EndOfDayEngineTest`, `LockingPosterTest`, `LockRegistryTest`, `SqliteDailyResultStoreTest.concurrentRecordsForTheSameDate` | implemented: passes in every build at `4067d2f` (one run per build: 4 tellers × 5 rounds × 5 loans while end of day runs; principal as expected, Σ debits = Σ credits, one penalty pair per loan). With `LockingPoster` removed it failed in 3 of 3 runs with lost postings. The 50-run repetition is not yet set up. The lock is in-process only: an `import` run as a separate process while the server runs can lose a balance update (DEF-022, open for M3; run imports with the server stopped) |

## 3. Constraints

| ID | Constraint | Enforcement | Status |
|---|---|---|---|
| C-01 | No `double` or `float` for monetary values | Checkstyle rule, build fails | **active** |
| C-02 | `BigDecimal` compared with `compareTo() == 0` | Inspection checklist (no automated rule at M1) | active |
| C-03 | No cyclic dependencies between module packages | ArchUnit test in CI from M2: `ArchitectureTest.noCyclesBetweenModules`, run by `./gradlew build`; since 21 Nov also `ledgerDoesNotDependOnItsClients` (no `ledger` dependency on `origination` or `delinquency`) and `originationDependsOnlyOnTheKernel` | verified (PR #8 CI run 43596655380 for e0f9ecb); the rules covering `origination` and `delinquency` pass at `4067d2f` (CI pending) |
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
42471721763 passed for 61febe9. `Allocation` (the four BR-18 tiers, value
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
42982638575 passed for c04b41c. `SqliteLedgerRepository` (queue item L-14,
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
first in each tier, the BR-19 reading adopted on 2026-11-14 and confirmed as
team decision D-01 on 2026-11-16. Its input is the ledger-owned `InstalmentDue`
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

M2 incremental evidence (2026-11-14): `RepaymentPosterDecorator` and
`TimingPoster` (queue item L-09) implement the Decorator of M1 section 5.3.
`TimingPosterTest` checks the IS-A/HAS-A structure by reflection, unchanged
delegation, and that both successful and refused postings are timed. Timing
traces to the design (GQM measurement), not to a numbered FR; no requirement
status changes.

M2 incremental evidence (2026-11-14): `AuditLoggingPoster` (queue item L-10)
journals one line per teller posting naming the trimmed operator, for posted
and refused attempts, and refuses a posting with no operator before it is
forwarded. `AuditLoggingPosterTest` (TC-LED-09) covers named, blank, null and
whitespace operators. FR-31 is *partial*: the one-line-per-file audit of
imports belongs to the import job, which does not exist yet.

M2 incremental evidence (2026-11-14): `IdempotencyGuardPoster` (queue item
L-11) claims the (bank, reference) pair atomically before forwarding and
releases it if the posting fails. `IdempotencyGuardPosterTest` (TC-LED-06)
covers the BR-25 classes (same pair twice, same reference from two banks, cash)
plus a failed-then-retried posting and eight concurrent duplicates forwarded
exactly once. FR-18 is *partial*: the claimed pairs live in
`InMemoryPaymentReferences` and are lost on restart; a store on the `payment`
table's `UNIQUE (source_bank, bank_reference)` is still to be built.

M2 incremental evidence (2026-11-14): `SqliteLoanRepository` (queue item L-15)
implements the ledger's `LoanRepository` role on the `loan` table: status,
outstanding principal (paisa) and closure date, each save in a
`BEGIN IMMEDIATE` transaction, refusing to insert a loan origination never
created. `SqliteDatabase` now sets `PRAGMA busy_timeout = 5000` on every
connection. `SqliteLoanRepositoryTest` shows a save waiting for another
writer's lock and then succeeding. This supports the concurrency strategy of
M1 section 3.6 and QR-06, which stays *planned* until the end-of-day engine
(Ibrahim) and `EndOfDayConcurrencyTest` exist. The ledger write and the loan
save are still two transactions.

M2 incremental evidence (2026-11-14): `PostingFaultInjectionTest` (queue item
L-16) injects a failure at each line of three postings (3, 4 and 4 lines) and
at the loan save after each, 14 points on a real SQLite file. After every
failure the ledger balances, the loan is unchanged and no line of the failed
posting stands unreversed. On the red run the 11 line points already passed
(atomic `appendAll`, L-08) and the 3 save points failed with orphan lines; the
fix reverses a posting's lines (BR-24) when its loan cannot be saved. QR-03's
test target is met locally; the "demonstration" half of its verification
method is still owed, and a failure of the compensating reversal itself is
only reported (suppressed exception), not recovered.

M2 incremental evidence (2026-11-14): `CompositionRoot` (queue item L-17)
builds the two chains of M1 section 5.3 over one `CoreRepaymentPoster` on
SQLite. `CompositionRootTest` shows the teller chain posting, auditing the
operator (FR-31) and timing, and the import chain refusing a duplicate before
it is timed (guard outermost) with no audit line. The instalment source and
the BR-22 rebate are named placeholders that throw
`ModuleNotAvailableException` until `products`/`delinquency` exist, so the
running application cannot post a repayment end to end yet. FR-13 and FR-14
are *verified*: PR #8 CI passed for c17052f.

M2 incremental evidence (2026-11-14): `TellerWebServer` and `OperatorContext`
(queue item L-18, `web` package) serve a teller posting form and an end-of-day
page on 127.0.0.1 only, using the JDK's `com.sun.net.httpserver`; `./gradlew
run` starts it through `CompositionRoot.withPlaceholders`. `TellerWebServerTest`
covers loopback binding, posting as the operator named on the form (FR-31 input)
without leaking it to the next request, 400 for malformed amount, date or a
zero amount, 404/409 for domain refusals, HTML escaping, and 404/405 routing.
The end-of-day page states that the engine (Muhammad Ibrahim) is not available
yet and a trigger returns 503, so FR-26 and the delinquency FRs are unaffected.

M2 incremental evidence (2026-11-14): `ArchitectureTest` (queue item L-19,
ArchUnit 1.3.0 as a test dependency) enforces C-03 on every build: the module
packages (`common`, `ledger`, `persistence`, `web`) are free of cycles. Two
further rules restate M1 section 4.5: `common` and `ledger` never depend on
`persistence` or `web`, and `common` depends on no module. The rules passed on
their first compiled run (the red commit failed only because ArchUnit was not
on the classpath), so no cycle existed to remove. Teammates' packages are
covered automatically once they exist under `pk.edu.giki.microlend`.

M2 incremental evidence (2026-11-21): the M2 integrated build `4067d2f`
(`feature/delinquency-origination-import`) adds Ibrahim's modules. All 384 tests pass
locally (`./gradlew build jacocoTestReport`); PR CI is pending.
- `origination`: eligibility rules BR-01 to BR-08 and risk grade and cap BR-09/BR-10.
  FR-01 to FR-07 move to *implemented*. Loan creation (I-03) is not built because
  `products` does not exist, so nothing applies the spread to a loan's rate yet, and the
  evaluator is not yet called from the running application.
- `delinquency`: the end-of-day engine (four threads, one task per loan under that
  loan's lock, idempotent per business date) and its calculators. FR-21 to FR-24, FR-26
  and FR-27 move to *implemented*. FR-25 is *partial*, because loans due for write-off
  are listed but not moved to written off.
- `ledger.importing` (Adapter): FR-20 moves to *implemented*.
- QR-06: `ConcurrentTellerAndEndOfDayTest` runs four tellers and end of day on the same
  loans through the production wiring. `LockingPoster` (shared `LockRegistry`) fixes the
  lost update found in the PR #8 review (DEF-009). QR-06 moves to *implemented*; the
  50-run target is not yet evidenced.
- FR-18: `SqlitePaymentReferences` keeps claimed (bank, reference) pairs in the
  `payment_reference` table, so a duplicate is refused across restarts (DEF-012 fixed;
  red `5b758e9`, green `8401031`). FR-18 moves from *partial* to *implemented*.
- The independent code review of the M2 code (21 Nov) found six defects, DEF-018 to
  DEF-023. Four are fixed in `6d30af9` (red `5541f0b`): a skipped end-of-day date
  under-accrued the penalty (FR-21), wrong import line numbers and a rejected byte-order
  mark (FR-20), and a dropped connection when end of day failed (FR-26). Two are open: an
  import run as a separate process is outside the account lock (QR-06, DEF-022), and an
  old double reversal blocks the schema upgrade (FR-17, DEF-023).
- In the running application (`CompositionRoot.withPlaceholders`), the instalment source
  is still a placeholder until `products` exists. Each end-of-day task therefore reports
  `ModuleNotAvailableException`, and the engine is exercised end to end only in tests.
- Backward check: 92 production classes. 76 name an FR, BR, QR, C or M1 design section
  in their Javadoc. The other 16 are the entry point (`MicroLendApplication`) and
  supporting exceptions, identifiers, records, helpers and role interfaces of traced
  classes (for example `LoanId`, `InstalmentsDueSource`, `DailySnapshot`,
  `StatementText`). No rogue code was found.

| Measure | M1 | M2 | M3 |
|---|---|---|---|
| Functional requirements defined | 32 | 32 | |
| Quality requirements defined | 6 | 6 | |
| Constraints recorded | 4 | 4 | |
| Requirements with a planned verification approach | 38 / 38 (100%) | 38 / 38 (100%) | |
| Requirements with executed, passing tests | 0 (planning milestone) | 26 / 38 (68%): FR-01 to FR-07, FR-13 to FR-27, FR-31, QR-03, QR-05, QR-06. In PR #8 CI: the ledger rows. In the local build of `4067d2f` only (CI pending): FR-01 to FR-07, FR-20 to FR-27 | |
| ... of which only partly met | n/a | 8: FR-06 (rate applied at loan creation), FR-15 (rebate), FR-16 (rebate line), FR-25 (write-off posting), FR-31 (per-file import audit), QR-03 (demonstration), QR-05 (inspection item), QR-06 (50 runs; separate-process import, DEF-022) | |
| Requirements with status *verified* | n/a | 3 FR (FR-13, FR-14, FR-19) and 2 constraints (C-03, C-04) | |
| Requirements with no executed verification yet | n/a | 11: FR-08 to FR-12, FR-28 to FR-30, FR-32 (`products`, `reporting`), QR-01, QR-02. QR-04 is met by analysis (measured), not by a test | |
| Production classes traceable to a requirement | n/a | 92 / 92 (76 directly; 16 as supporting types of traced classes) | |
| Test gaps found | n/a | No implemented requirement is without a test. 3 partial gaps: QR-06 runs once per build, not 50 times; TC-LED-04.2 does not assert the ledger balance after an import; `EndOfDayEngine.collect` paths BP-2 and BP-4 have no test (white-box section 7) | |
| Rogue code found | n/a | 0 | |

## Change log

| Date | Version | Change |
|---|---|---|
| 2026-10-04 | 1.0 | Matrix created at M1: 30 functional requirements, 6 quality requirements, 4 constraints, each traced to a business rule and a planned verification approach |
| 2026-10-07 | 1.1 | Pre-submission review: FR-31 (audit journal) and FR-32 (CSV export) added so `AuditLoggingPoster` and `ReportExporter` are not rogue code; FR-13 design element corrected to `allocate()`; FR-20 names the bank adapters; QR-02 and QR-04 targets aligned with the SE423 design and the Checkstyle gate; design elements added so every planned class traces (FR-05, FR-08, FR-11, FR-13, QR-06); FR-23 complexity corrected to ≈ 6 |
| 2026-11-14 to 2026-11-16 | 2.0 | M2 ledger build (`701c161`): code, tests and CI evidence for FR-13 to FR-19, FR-31 (teller half), QR-03, QR-04, C-03 and C-04, recorded in the dated evidence notes in section 4 |
| 2026-11-21 | 2.1 | M2 integrated build (code head `4067d2f`): FR-01 to FR-07, FR-18 (durable references), FR-20 to FR-24, FR-26, FR-27 and QR-06 implemented; FR-25 partial; FR-16, FR-17, FR-18, FR-31 and C-03 rows extended with the new code and tests; independent-review defects DEF-018 to DEF-023 referenced in FR-17, FR-20, FR-21, FR-26 and QR-06; QR-06 test renamed to the real `ConcurrentTellerAndEndOfDayTest`; QR-04 re-measured (highest 12, `Applicant` constructor); measured complexities replace the M1 estimates (FR-13 V(G) 10, FR-23 V(G) 6); M2 column of the coverage summary filled; PR #8 review defects referenced (DEF-009, DEF-010, DEF-012, DEF-013) |
