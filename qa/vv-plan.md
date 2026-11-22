# MicroLend - Software Verification and Validation Plan (IEEE 1012)

**Milestone:** M2 (Week 10; originally due 18-19 Nov 2026, extended by the instructor to 22 Nov 2026) - revised at M3 (Week 13)
**Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Status:** M2 version, 2026-11-22, checked against `develop` with `products` and `reporting` merged (PRs #11 and #12); the 2026-11-21 version used the integrated build `4067d2f`, the 2026-11-16 version the ledger build `701c161`
**Related records:** `qa/rtm.md` (requirement status), `qa/risk-register.md`, `qa/defect-log.md`
(anomalies and CAPA), `qa/inspections/` (Fagan records), `docs/business-rules.md` (BR-01 to BR-43),
`docs/M1-SE431-Quality-Requirements-RTM-SQAP.md` (QR-01 to QR-06, SQAP outline)

## 1. Purpose and scope

Each V&V task below has an owner, timing, tool and evidence location (sections 4 and 8), so
every requirement traces to a check and a record. Scope: the five modules, `common`,
`persistence` and the local web interface, from M2 to the M3 acceptance gate, as the V&V part
of the SQAP (IEEE 730).

**Baseline (2026-11-21).** PR #8 (the ledger) was merged into `develop` on 21 Nov.
`feature/delinquency-origination-import` at `4067d2f` passes 384 tests locally and contains:
- `common`: `Money`, `LoanId`, `LockRegistry`.
- `ledger` repayment path: `RepaymentAllocator` with `InstalmentDue`, `SettlementPolicy`,
  `CoreRepaymentPoster`, the Decorator chain (`TimingPoster`, `AuditLoggingPoster`,
  `IdempotencyGuardPoster`, `LockingPoster` innermost), `CompensatingEntryService`.
- `ledger.importing`: HBL and MCB statement Adapters, `StatementImportJob`.
- `origination`: eligibility BR-01 to BR-08, risk grade and cap BR-09/BR-10; no loan creation.
- `delinquency`: BR-27 to BR-32 calculators, concurrent `EndOfDayEngine`.
- `persistence`: `SchemaMigrator` (schema version 2), `SqliteDatabase`,
  `SqliteLedgerRepository`, `SqliteLoanRepository`, `SqliteDailyResultStore`.
- Composition root and local `web` pages for teller posting and end of day.

**Update (2026-11-22).** PRs #11 and #12 added `products` (Factory Method, schedules, SQLite
storage, the BR-22 rebate) and `reporting` (`StatementRenderer`); `develop` then passes 492 tests.
`CompositionRoot.production` wires the schedule source and the rebate, but nothing creates a loan
or schedule yet (I-03), so the running application cannot yet post a repayment or run end of day
on real data. Tests verify both paths end to end with stub sources
(`ConcurrentTellerAndEndOfDayTest`, `StatementImportWiringTest`) and with a saved schedule
(`CompositionRootTest`). Rows for unbuilt code give the V&V to apply as it lands.

## 2. Verification versus validation in this project

| | Verification | Validation |
|---|---|---|
| Question | Are we building the product right? Does each work product conform to its specification? | Are we building the right product? Does the system do what a microfinance lender actually needs? |
| Compared against | The business rules, the M1 design (patterns, schema, thread-safety strategy), coding standards C-01 to C-04 | The intended use: a teller posts repayments, end-of-day runs, reports reflect the portfolio |
| MicroLend examples | Unit test that `LedgerEntry` rejects a two-sided line (BR-43); Checkstyle fails the build on `double` (C-01); Fagan inspection of `RepaymentAllocator` against BR-18 to BR-20; schema `CHECK` constraints tested by `SchemaMigratorTest` | End-to-end demo: post a teller repayment through the web form, run end-of-day, read the statement and PAR30 report; golden schedules hand-computed independently of the code (QR-02); team decision on the BR-19 allocation reading |
| Methods | Inspection, static analysis, unit and integration tests, traceability analysis | Scenario tests on the integrated build, demonstration, review of business-rule interpretations with the team |

Validation questions are team decisions, not defects. Closed: BR-18/BR-19 are read tier by tier (D-01, recorded by Tughral Hussain under BR-19), and the instalment source lists only instalments due by the business date (D-02, 16 Nov, `docs/planning/team-decisions.md`; `products` implements it). Open: the BR-22 per-product rebate (`products`) and money paid beyond a full payoff, which the poster refuses until a rule exists.

## 3. Software integrity levels

Rigour scales with the consequence of failure (4 = highest).

| Level | Modules / components | Why | Minimum V&V rigour |
|---|---|---|---|
| **4** | `ledger` posting path (`RepaymentAllocator`, `CoreRepaymentPoster`, `SettlementPolicy`, `CompensatingEntryService`), `persistence` ledger tables, `delinquency` (`PenaltyAccruer`, `DelinquencyClassifier`, `EndOfDayEngine`, `LockRegistry`) | Wrong money or a corrupted ledger propagates to every balance, provision and report, and cannot be repaired by rerunning (R1, R5). The concurrent engine can lose updates or double-accrue | Fagan inspection; 100% decision coverage (SQAP 4.4); EP + BVA + basis-path tests; invariant tests (sum of debits = sum of credits); fault injection (QR-03); repeated concurrency tests (QR-06) |
| **3** | `products` (schedule generation), `common` (`Money`) | Errors are systematic but are caught by golden schedules before posting | Golden-file tests (QR-02), BVA, invariant (sum of instalments = principal + interest) |
| **2** | `origination`, `reporting` | A wrong decision or report is visible and recoverable; it does not alter the ledger | EP + BVA unit tests, golden report figures, review |
| **1** | Local web interface, composition root, CSV export | Thin layer over verified services; never deployed | Smoke test in the end-to-end demo, code review in the PR |

## 4. V&V activities by life-cycle phase

Week 10 = M2 submission (22 Nov 2026); Week 13 = M3.

| Phase | Input | V&V task | Method / tool | Output and evidence location | Owner | Timing |
|---|---|---|---|---|---|---|
| Requirements | FR-01 to FR-32, QR-01 to QR-06, BR-01 to BR-43 | Traceability analysis: every requirement has a test case ID; every production class traces to a requirement | RTM audit rule (forward and backward) | `qa/rtm.md` sections 1 to 4 | Muhammad Ibrahim (M2 Team Lead); module owners fill their rows | M2 update, M3 final |
| Requirements | `docs/business-rules.md` | Requirements validation of ambiguous rules (BR-18/19, BR-22) | Team review, recorded decision | Dated note under the rule (BR-19: decision D-01, 2026-11-16); queue notes in `docs/planning/m2-code-todo.md` | All three | BR-19 done; BR-22 rebate before M3 |
| Design | M1 design (patterns, schema section 3.7, concurrency section 3.6) | Design conformance: code matches module boundaries and pattern structure | Review in each PR; `ArchitectureTest` (ArchUnit): no cycles between module packages (C-03), domain does not depend on `persistence` or `web`, `common` depends on no module | PR reviews on GitHub; `ArchitectureTest` runs in every `./gradlew build` | Each module owner; reviewer is a non-author | Every PR (ArchUnit in place since L-19) |
| Design | Schema DDL | Schema re-states invariants (CHECK, UNIQUE, FK, INTEGER paisa, C-04) | Unit tests against a real SQLite file | `SchemaMigratorTest` | Tughral | Done (CI run 42982638575) |
| Implementation | Source code | Coding standard and C-01 gate | Checkstyle 10.18.2 (`config/checkstyle.xml`), build fails on violation | CI job `build` (`.github/workflows/build.yml`), `build/reports/checkstyle/` | All | Every push |
| Implementation | Source code | Static analysis with triage fixed / accepted / deferred / false positive (QR-04 metrics) | Checkstyle 10.18.2 plus PMD 7.7.0 applied by `config/pmd-init.gradle` (`.\gradlew.bat --init-script config/pmd-init.gradle pmdMain`); JaCoCo complexity per method | `qa/static-analysis-report.md` (66 PMD findings on the ledger code, 4 fixed; re-run on `4067d2f`: 100 findings, all triaged, 39 of them new; integrated build, 22 Nov: 122, all triaged, 22 of them new); defects DEF-001 to DEF-005 and DEF-017 in `qa/defect-log.md` | Muhammad Ibrahim (M2 Team Lead); fixes by the module owner | M2 done for every module with code, `products` and `reporting` included; re-run at M3 |
| Implementation | Repayment path (`RepaymentAllocator`, `CoreRepaymentPoster`, ledger and schema), `DelinquencyClassifier` | Fagan inspection with an inspector from another team (R3) | Six-stage Fagan process, checklist incl. QR-05 and C-02 items | `qa/inspections/` (INS-01 plan, 39-item checklist, forms), `qa/defect-log.md` | Moderators per SQAP 3.1: Ibrahim for the repayment path, Hassan for `DelinquencyClassifier`; inspecting team chosen by the instructor | INS-01: not held as of 21 Nov, because no external inspector has been assigned; held when one is. `DelinquencyClassifier`: built 21 Nov, inspection not yet scheduled |
| Implementation | Pull requests | Code review by a non-author before merge | Review against the business rules and the M1 design | PR #8 review by Muhammad Ibrahim, 21 Nov: 8 findings logged as DEF-009 to DEF-016; PRs #11 and #12, 22 Nov: 5 as DEF-024 to DEF-028 | Any non-author member | Every PR |
| Implementation | M2 code on `feature/delinquency-origination-import` | Independent code review | Review against the business rules, the M1 design and the concurrency strategy | 21 Nov: 6 findings logged as DEF-018 to DEF-023; 4 fixed in `6d30af9` (red `5541f0b`), 2 open | Reviewer independent of the code's author | Once at M2; again before M3 |
| Implementation | Commits | TDD evidence: failing test committed before code | Commit convention `test(red)` / `feat(green)` / `refactor`; no squash (ruleset) | Git history of PR #8 and later PRs | Each author | Every item |
| Test | BR boundary values | Black-box design: EP and BVA traced to RTM IDs | EP/BVA tables | `qa/test-design/black-box.md` (186 cases, 174 automated in the integrated build, 152 at `4067d2f`: ledger 58 of 62, origination 47 of 47, delinquency 30 of 30, products 20 of 21, reporting 3 of 9); JUnit tests | Module owners | M2 design and automation; portfolio reports at M3 |
| Test | Integrity-level 4 methods | White-box design: CFG, cyclomatic complexity, basis paths | Hand CFG; JaCoCo complexity counter | `qa/test-design/white-box.md` (`RepaymentAllocator.allocate` V(G) 10, `CoreRepaymentPoster.post`, `LoanAccount.restore`, `LedgerEntry.requireValid`, `DelinquencyClassifier.classify` V(G) 6, `EndOfDayEngine.collect` V(G) 7) | Module owners | M2 design (ledger and delinquency done), M3 executed |
| Test | Whole suite | Coverage measurement | JaCoCo (`jacocoTestReport`); `coverageGate` task, branch coverage >= 60% | `build/reports/jacoco/test/html/` uploaded by CI; integrated build (492 tests): 95.4% lines, 87.7% branches, 99.0% methods (at `4067d2f`: 95.2%, 88.8%, 98.7%) | Integrator | Measured M2; 60% gate enforced M3 |
| Test | Posting path | Fault injection: ledger stays balanced at every write point (QR-03) | `PostingFaultInjectionTest` (L-16): 14 failure points on a real SQLite file | CI test report (PR #8 run 43596151001) | Tughral | Done at M2; live demonstration at M3 |
| Test | End-of-day engine | Concurrency: no lost update, no double accrual (QR-06) | Latch-driven `ConcurrentTellerAndEndOfDayTest` (4 tellers and end of day on the same loans, production wiring, SQLite) and `EndOfDayEngineTest`; 50 repetitions planned | Runs once in every build at `4067d2f`; it failed in 3 of 3 runs with `LockingPoster` removed. The scheduled 50-run job is not yet added | Ibrahim | Test at M2; 50-run evidence at M3 |
| Test | End-of-day engine | Performance (QR-01) | `EndOfDayPerformanceTest` on 10,000 generated loans | Timings attached to milestone report | Ibrahim | M3 |
| M3 acceptance | Integrated build at `m3-baseline` | Acceptance gate (SQAP 5.1.3): all FRs traced to passing tests; >= 60% core coverage, 100% decision coverage on money path; zero open critical defects | RTM final status; JaCoCo; defect log | `qa/rtm.md`, CI artifacts, `qa/defect-log.md` | All three | Week 13 |
| M3 acceptance | Running system | Validation demo: teller posting, end-of-day, statement, PAR30 report; two injected failure scenarios | Live demonstration from the submitted build | Demo script and recording notes | All three | Week 13 |

## 5. Requirement to V&V activity

Technique codes: EP, BVA, BP (basis path), INV (invariant), INSP (inspection), GOLD (golden file),
SA (static analysis), CONC (concurrency test), FI (fault injection), DEMO (validation demo).
Status is from `qa/rtm.md` v2.3 (2026-11-22). "Verified" means the tests pass in pull-request CI;
"implemented" means they pass in the build but a part of the requirement (named in the row) is not yet verified.

| Req | Rule | Verification | Validation | Owner | Status |
|---|---|---|---|---|---|
| FR-01 | BR-01, BR-02 | TC-ORG-01: BVA on ages 17/18, 65/66 at maturity (`AgeRuleTest`) | DEMO (loan application) | Ibrahim | verified (PR #9 CI) |
| FR-02 | BR-03 | TC-ORG-02: EP + BVA on 1/2/3 active loans (`ActiveLoanLimitRuleTest`) | DEMO | Ibrahim | verified (PR #9 CI) |
| FR-03 | BR-04 | TC-ORG-03: BVA on burden 40% / 40.01% (`BurdenRatioRuleTest`) | DEMO | Ibrahim | verified (PR #9 CI) |
| FR-04 | BR-05 | TC-ORG-04: EP prior write-off yes/no (`WriteOffHistoryRuleTest`) | - | Ibrahim | verified (PR #9 CI) |
| FR-05 | BR-08 | TC-ORG-05: EP, rule-order cases (`EligibilityEvaluatorTest`) | Review of rejection reasons | Ibrahim | verified (PR #9 CI) |
| FR-06 | BR-09, BR-10 | TC-ORG-06: EP + BVA per grade (`RiskGradeTest`, `EligibilityEvaluatorTest`) | - | Ibrahim | verified for grade, spread and cap (PR #9 CI); rate applied at loan creation not built (DEF-025) |
| FR-07 | BR-06, BR-07 | TC-ORG-07: BVA on product bands (`ProductBandRuleTest`) | - | Ibrahim | verified (PR #9 CI) |
| FR-08 | BR-11, BR-17 | TC-PRD-01: GOLD, 10 schedules per product | Schedules hand-computed independently | Hassan | verified (PR #11 CI); base rate only, grade spread not applied (DEF-025) |
| FR-09 | BR-12 | TC-PRD-02: BVA on 28/29/30/31 day months | - | Hassan | verified (PR #11 CI); TC-PRD-02.5 not automated |
| FR-10 | BR-13 | TC-PRD-03: EP + BVA grace 0 / 1 / max | - | Hassan | verified (PR #11 CI) |
| FR-11 | BR-14, BR-15 | TC-PRD-04: INV sum of instalments | - | Hassan | partial: flat-rate total wrong at very low rates (DEF-027) |
| FR-12 | BR-16 | TC-PRD-05: EP tenure 1 / 6 / 36 | - | Hassan | verified (PR #11 CI) |
| FR-13 | BR-18, BR-19 | TC-LED-01: BP on `RepaymentAllocator.allocate()` (V(G) 10, 18/18 branches); INSP (Fagan INS-01) | BR-19 reading decided by the team (D-01, 2026-11-16) | Tughral | verified (PR #8 CI for c17052f); inspection pending |
| FR-14 | BR-20 | TC-LED-02: BVA 0 / 0.01 / due - 0.01 / due / due + 0.01 | - | Tughral | verified (PR #8 CI for c17052f) |
| FR-15 | BR-21, BR-22 | TC-LED-03: BVA due + 0.01, exact payoff, payoff + 0.01 | Rebate per product confirmed with Hassan | Tughral | partial: advance credit, payoff detection and per-product rebate tested; rebate line not written (DEF-013); grace interest refunded (DEF-026) |
| FR-16 | BR-23 | TC-LED-04: INV debits = credits after every posting; `LedgerImbalanceException`; atomic `appendAll` | DEMO | Tughral (repayment), Ibrahim (end of day) | implemented for repayment postings (PR #8 CI for 34a7bb7) and end-of-day penalty pairs (`4067d2f`); rebate line not written (DEF-013) |
| FR-17 | BR-24 | TC-LED-05: unit + INSP; reversed at most once (`SchemaVersion2Test`) | - | Tughral | implemented; unit tests pass in PR #8 CI; INS-01 checklist item pending |
| FR-18 | BR-25 | TC-LED-06: EP same pair / other bank / cash / failed-then-retried; 8 concurrent duplicates; repeated statement import | - | Tughral (guard), Ibrahim (durable store) | implemented: guard verified in PR #8 CI (71d8b3f); pairs kept in SQLite since `8401031` (DEF-012 fixed; `SqlitePaymentReferencesTest`, `StatementImportWiringTest.secondImportAfterRestartIsRefused`). A crash between claim and posting leaves the pair claimed (M3) |
| FR-19 | BR-26 | TC-LED-07: EP active / closed / written-off, at the account, the poster and the settlement policy | - | Tughral | verified (CI 42471721763) |
| FR-20 | - | TC-LED-08: EP per bank format (`HblStatementAdapterTest`, `McbStatementAdapterTest`, `StatementImportJobTest`, `StatementImportWiringTest`) | Sample statements (`src/test/resources/statements/`) | Ibrahim | verified (PR #9 CI); imports run with the server stopped (DEF-022) |
| FR-21 | BR-27, BR-28 | TC-DEL-01: BVA on 10% cap (`PenaltyAccruerTest`); skipped dates caught up (`EndOfDayEngineTest.catchesUpSkippedDates`) | - | Ibrahim | verified (PR #9 CI; DEF-018 fixed) |
| FR-22 | BR-29 | TC-DEL-02: BVA day 0 / 1 (`DaysPastDueCalculatorTest`) | - | Ibrahim | verified (PR #9 CI); paid instalments not yet subtracted (DEF-024) |
| FR-23 | BR-30 | TC-DEL-03: BP (V(G) 6, 6 paths) + BVA 0/1/30/31/60/61/90/91 (`DelinquencyClassifierTest`); INSP | - | Ibrahim | verified (PR #9 CI); inspection not yet scheduled |
| FR-24 | BR-31 | TC-DEL-04: EP per class (`ProvisioningAndWriteOffTest`) | - | Ibrahim | verified (PR #9 CI); not yet called by end of day or reports |
| FR-25 | BR-32 | TC-DEL-05: BVA 179 / 180 / 181 days (`ProvisioningAndWriteOffTest`, `EndOfDayEngineTest`) | - | Ibrahim | partial: listed for write-off, not yet posted |
| FR-26 | BR-33 | TC-DEL-06: INV rerun same date; schema UNIQUE (`EndOfDayEngineTest`, `SqliteDailyResultStoreTest`) | DEMO (rerun end-of-day) | Ibrahim | verified (PR #9 CI) |
| FR-27 | BR-34 | TC-DEL-07: EP class changed / unchanged (`EndOfDayEngineTest`) | - | Ibrahim | verified (PR #9 CI) |
| FR-28 | BR-35 | TC-REP-01: EP date ranges | DEMO (statement) | Hassan | partial: tests pass in PR #12 CI; balance nil on balanced postings (DEF-028) |
| FR-29 | BR-36 to BR-38 | TC-REP-02: GOLD report figures | DEMO (PAR30) | Hassan | planned |
| FR-30 | BR-39 | TC-REP-03: EP current / past / before first loan | - | Hassan | planned |
| FR-31 | - | TC-LED-09: EP named / blank operator, refused posting, web form operator; per-file import audit | - | Tughral (teller), Ibrahim (import) | partial: teller half verified in PR #8 CI (a43c681); the import job writes no per-file audit line yet |
| FR-32 | - | TC-REP-04: EP empty / one / full portfolio | - | Hassan | planned |
| QR-01 | - | Test: `EndOfDayPerformanceTest` | Timings on demo machine | Ibrahim | planned |
| QR-02 | - | GOLD: `GoldenScheduleTest`, 30 schedules | Independent hand computation | Hassan | partial: 3 of 30 pass in PR #11 CI |
| QR-03 | - | FI: `PostingFaultInjectionTest`, 14 points (target >= 10) | Live failure scenario at M3 | Tughral | test passes in PR #8 CI (5502ccf, run 43596151001); demonstration not yet given |
| QR-04 | - | SA: complexity and size metrics from JaCoCo and PMD | - | Muhammad Ibrahim (M2 Team Lead) | measured on `4067d2f`: mean 1.8, highest 12 (`Applicant` constructor), none above 15; CI gate not yet wired |
| QR-05 | - | INSP checklist item + `LedgerImmutabilityTest` + source scan in `SqliteLedgerRepositoryTest` | - | Tughral | planned: automated checks pass; INS-01 checklist item still owed |
| QR-06 | - | CONC: `ConcurrentTellerAndEndOfDayTest`, 50 runs | Rerun with concurrent tellers in demo | Ibrahim | implemented: passes once per build; 50-run job not yet added; an import run as a separate process is outside the lock (DEF-022, M3; run imports with the server stopped) |
| C-01 | - | SA: Checkstyle IllegalType / IllegalToken, build fails | - | All | active |
| C-03 | - | `ArchitectureTest#noCyclesBetweenModules` (ArchUnit 1.3.0) in every build | - | Tughral | verified (PR #8 CI run 43596655380 for e0f9ecb) |
| C-04 | - | `SchemaMigratorTest`, `SqliteLedgerRepositoryTest` | - | Tughral | verified (CI 42982638575) |

## 6. Anomaly reporting and CAPA

Anomalies from any V&V task (inspection, static analysis, failing test on `develop`, review
comment requiring a change, validation disagreement) go into `qa/defect-log.md` with severity,
corrective and preventive action, owner, status and closing commit. The log records the defect,
not who wrote it (SQAP 4.4).

| Severity | Meaning in MicroLend | Required response |
|---|---|---|
| Critical | Wrong money, unbalanced or altered ledger, data loss, race that changes balances | Fix before the milestone baseline; regression test added; preventive action recorded |
| Major | Business rule violated in an edge case, missing validation at a module boundary, unhandled exception | Fix in the current milestone or defer with a stated reason and owner |
| Minor | Standards, naming, Javadoc, readability, complexity above target | Fix or justify; may be batched |

Preventive actions change the process (a checklist item, a Checkstyle rule, a test pattern).
M2 CAPA items are reviewed in the M3 final audit.

## 7. V&V reporting and exit criteria

| Report | Content | When |
|---|---|---|
| PR description and CI run | Scope, red/green commit hashes, CI result, reports artifact | Every PR |
| RTM update | Status per requirement with CI run IDs as evidence | Each milestone |
| Inspection report | Fagan stages, roles, preparation time, defects, rates | M2 |
| Static-analysis report | Findings and triage | M2, refreshed M3 |
| V&V summary | Results against this plan, open anomalies, residual risk | M3 |

**M2 exit criteria (verification baseline)**, status on 22 Nov 2026 (coverage has no threshold
at M2):

| Criterion | Status | Evidence |
|---|---|---|
| CI green on `develop` with all module PRs merged | **Met.** PR #8 (ledger) and PR #9 (`origination`, `delinquency`, `ledger.importing`; CI run 44930290499) were merged on 21 Nov, and `develop` passed CI after the merge (run 44931805244). PRs #11 (`products`) and #12 (`reporting`) passed CI on their re-authored heads and were merged on 22 Nov; the merged tree passes 492 tests. `develop` passed CI after the last merge; 94 of 97 workflow runs to date passed, and the 3 failures are old | `develop` CI runs 44931805244 at `ac14ba9` and 45030126014 at `e7ecce6`; PR #11 run 45028938297; PR #12 run 45028943565 |
| Fagan inspection held with an external inspector, defects logged | **Not met.** No inspector from another team has been assigned, so INS-01 has not been held (risk R3). The package is complete | `qa/inspections/` |
| Static-analysis findings triaged | Met | `qa/static-analysis-report.md`: 122 PMD findings in the integrated build (100 at `4067d2f`), each with a disposition; Checkstyle 0 |
| EP/BVA and basis-path designs traced to RTM IDs | Met for every module with code | `qa/test-design/black-box.md` (186 cases, 174 automated); `qa/test-design/white-box.md` (6 methods) |
| Coverage measured and reported | Met | 95.4% lines, 87.7% branches, 99.0% methods in the integrated build |
| RTM, risk register and defect log updated | Met | `qa/rtm.md` v2.3; `qa/risk-register.md` (22 Nov update); `qa/defect-log.md` (DEF-009 to DEF-030, CAPA-004 to CAPA-011) |
| `m2-baseline` tag on `main` | **Final submission step** | Annotated tag on the merge commit of the `develop` -> `main` pull request that carries these documents; `git rev-list -n 1 m2-baseline` gives the commit |

**M3 exit criteria (acceptance):** the SQAP 5.1.3 gate (section 4); designed tests executed
with results recorded against RTM IDs; QR-01, QR-03 and QR-06 evidenced; all M2 CAPA items
closed or justified; `m3-baseline` tag on `main`.

**Known gaps (2026-11-22):**
- CI runs `jacocoTestReport` but not `coverageGate` (DEF-008, deferred to M3).
- PMD runs on demand from `config/pmd-init.gradle`, not in CI; wiring it in is recommended for M3.
- Still open (sections 4 and 5): the 50-run QR-06 job, INS-01, the *planned* portfolio-report
  rows (FR-29, FR-30, FR-32), DEF-024 to DEF-030, and DEF-022 (until fixed, bank-statement
  imports run with the web server stopped).

## 8. Responsibilities

| Role | Who | V&V responsibilities |
|---|---|---|
| M2 Team Lead / Integrator | Muhammad Ibrahim | Leads the M2 documents (this plan, inspection report, static-analysis report, test designs, RTM/risk/defect updates); merges reviewed PRs; tags `m2-baseline` |
| Module owner, `origination` + `delinquency` | Muhammad Ibrahim | Tests for FR-01 to FR-07, FR-20 to FR-27; QR-01, QR-06; moderates the repayment-path inspection (INS-01) |
| Module owner, `products` + `reporting` | Hassan Khalid | Tests for FR-08 to FR-12, FR-28 to FR-30, FR-32; QR-02; moderates the `DelinquencyClassifier` inspection |
| Module owner, `ledger` + `persistence` + web | Tughral Hussain | Tests for FR-13 to FR-19, FR-31; QR-03, QR-05; C-03, C-04 |
| PR reviewer | Any non-author member | Design conformance and standards check on every PR (ruleset: one approval) |
| External inspector | From the team the instructor pairs us with | Independent inspection of the money path |
