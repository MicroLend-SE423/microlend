# MicroLend - Software Verification and Validation Plan (IEEE 1012)

**Milestone:** M2 (Week 10, due 18-19 Nov 2026) - revised at M3 (Week 13)
**Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Status:** M2 version, 2026-11-16 (refreshed against the final ledger build `701c161`)
**Related records:** `qa/rtm.md` (requirement status), `qa/risk-register.md`, `qa/defect-log.md`
(anomalies and CAPA), `qa/inspections/` (Fagan records), `docs/business-rules.md` (BR-01 to BR-43),
`docs/M1-SE431-Quality-Requirements-RTM-SQAP.md` (QR-01 to QR-06, SQAP outline)

## 1. Purpose and scope

This plan states which verification and validation (V&V) tasks the team performs on MicroLend,
when, by whom, with what tool, and where the evidence is kept in the repository. It covers
the five modules (`origination`, `products`, `ledger`, `delinquency`, `reporting`), the shared
`common` and `persistence` packages, and the local web interface, from M2 to the M3 acceptance
gate. The SQAP (IEEE 730) remains the umbrella document; this plan details its V&V part.

**Status at the time of writing (honest baseline, 2026-11-16).** On PR #8 (`701c161`, 182
tests passing, coverage 94.5% of lines and 86.5% of branches):
- `common`: `Money`, `LoanId`.
- `ledger`: the whole repayment path, which is `RepaymentAllocator` with `InstalmentDue`,
  `SettlementPolicy`, `CoreRepaymentPoster`, the Decorator chain (`TimingPoster`,
  `AuditLoggingPoster`, `IdempotencyGuardPoster`) and `CompensatingEntryService`.
- `persistence`: `SchemaMigrator`, `SqliteDatabase`, `SqliteLedgerRepository`,
  `SqliteLoanRepository`.
- The composition root and the local `web` teller page.

`products` has only `DueDateCalculator` in PR #6. `origination`, `delinquency` and `reporting`
are not built yet. Because of that, the running application cannot post a repayment end to end:
the instalment source and the BR-22 rebate are named placeholders until `products` and
`delinquency` exist. Rows below for code not yet built describe the V&V that will be applied as
the code lands.

## 2. Verification versus validation in this project

| | Verification | Validation |
|---|---|---|
| Question | Are we building the product right? Does each work product conform to its specification? | Are we building the right product? Does the system do what a microfinance lender actually needs? |
| Compared against | The business rules, the M1 design (patterns, schema, thread-safety strategy), coding standards C-01 to C-04 | The intended use: a teller posts repayments, end-of-day runs, reports reflect the portfolio |
| MicroLend examples | Unit test that `LedgerEntry` rejects a two-sided line (BR-43); Checkstyle fails the build on `double` (C-01); Fagan inspection of `RepaymentAllocator` against BR-18 to BR-20; schema `CHECK` constraints tested by `SchemaMigratorTest` | End-to-end demo: post a teller repayment through the web form, run end-of-day, read the statement and PAR30 report; golden schedules hand-computed independently of the code (QR-02); team decision on the BR-19 allocation reading |
| Methods | Inspection, static analysis, unit and integration tests, traceability analysis | Scenario tests on the integrated build, demonstration, review of business-rule interpretations with the team |

Validation questions and their state (they are decisions, not defects):
- **Closed.** The BR-18/BR-19 reading. Tier by tier was adopted by the team on 2026-11-16 as decision D-01, recorded by Tughral Hussain under BR-19 in `docs/business-rules.md`.
- **Open.** The per-product rebate for BR-22, which belongs to `products`.
- **Open.** What happens to money paid beyond a full payoff. Until a rule exists, the poster refuses it.
- **Closed (D-02, 16 Nov).** The instalment source lists only instalments due by the business date (`docs/planning/team-decisions.md`); `products` implements it.

## 3. Software integrity levels

IEEE 1012 scales V&V rigour to the consequence of failure. We use four levels (4 = highest).

| Level | Modules / components | Why | Minimum V&V rigour |
|---|---|---|---|
| **4** | `ledger` posting path (`RepaymentAllocator`, `CoreRepaymentPoster`, `SettlementPolicy`, `CompensatingEntryService`), `persistence` ledger tables, `delinquency` (`PenaltyAccruer`, `DelinquencyClassifier`, `EndOfDayEngine`, `LockRegistry`) | Wrong money or a corrupted ledger propagates to every balance, provision and report, and cannot be repaired by rerunning (R1, R5). The concurrent engine can lose updates or double-accrue | Fagan inspection; 100% decision coverage (SQAP 4.4); EP + BVA + basis-path tests; invariant tests (sum of debits = sum of credits); fault injection (QR-03); repeated concurrency tests (QR-06) |
| **3** | `products` (schedule generation), `common` (`Money`) | Errors are systematic but are caught by golden schedules before posting | Golden-file tests (QR-02), BVA, invariant (sum of instalments = principal + interest) |
| **2** | `origination`, `reporting` | A wrong decision or report is visible and recoverable; it does not alter the ledger | EP + BVA unit tests, golden report figures, review |
| **1** | Local web interface, composition root, CSV export | Thin layer over verified services; never deployed | Smoke test in the end-to-end demo, code review in the PR |

## 4. V&V activities by life-cycle phase

Timing: Week 10 = M2 submission (18-19 Nov 2026); Week 13 = M3. "Owner" is the module owner
for module-level tasks; M2 document tasks are led by Muhammad Ibrahim as M2 Team Lead.

| Phase | Input | V&V task | Method / tool | Output and evidence location | Owner | Timing |
|---|---|---|---|---|---|---|
| Requirements | FR-01 to FR-32, QR-01 to QR-06, BR-01 to BR-43 | Traceability analysis: every requirement has a test case ID; every production class traces to a requirement | RTM audit rule (forward and backward) | `qa/rtm.md` sections 1 to 4 | Muhammad Ibrahim (M2 Team Lead); module owners fill their rows | M2 update, M3 final |
| Requirements | `docs/business-rules.md` | Requirements validation of ambiguous rules (BR-18/19, BR-22) | Team review, recorded decision | Dated note under the rule (BR-19: decision D-01, 2026-11-16); queue notes in `docs/planning/m2-code-todo.md` | All three | BR-19 done; BR-22 rebate before M3 |
| Design | M1 design (patterns, schema section 3.7, concurrency section 3.6) | Design conformance: code matches module boundaries and pattern structure | Review in each PR; `ArchitectureTest` (ArchUnit): no cycles between module packages (C-03), domain does not depend on `persistence` or `web`, `common` depends on no module | PR reviews on GitHub; `ArchitectureTest` runs in every `./gradlew build` | Each module owner; reviewer is a non-author | Every PR (ArchUnit in place since L-19) |
| Design | Schema DDL | Schema re-states invariants (CHECK, UNIQUE, FK, INTEGER paisa, C-04) | Unit tests against a real SQLite file | `SchemaMigratorTest` | Tughral | Done (CI run 42982638575) |
| Implementation | Source code | Coding standard and C-01 gate | Checkstyle 10.18.2 (`config/checkstyle.xml`), build fails on violation | CI job `build` (`.github/workflows/build.yml`), `build/reports/checkstyle/` | All | Every push |
| Implementation | Source code | Static analysis with triage fixed / accepted / deferred / false positive (QR-04 metrics) | Checkstyle 10.18.2 plus PMD 7.7.0 applied by `config/pmd-init.gradle` (`.\gradlew.bat --init-script config/pmd-init.gradle pmdMain`); JaCoCo complexity per method | `qa/static-analysis-report.md` (66 PMD findings on the final ledger code, all triaged, 4 fixed); defects DEF-001 to DEF-005 in `qa/defect-log.md` | Muhammad Ibrahim (M2 Team Lead); fixes by the module owner | M2 done for `common`, `ledger`, `persistence`, `web`; re-run as other modules land |
| Implementation | Repayment path (`RepaymentAllocator`, `CoreRepaymentPoster`, ledger and schema), `DelinquencyClassifier` | Fagan inspection with an inspector from another team (R3) | Six-stage Fagan process, checklist incl. QR-05 and C-02 items | `qa/inspections/` (INS-01 plan, 34-item checklist, forms), `qa/defect-log.md` | Moderators per SQAP 3.1: Ibrahim for the repayment path, Hassan for `DelinquencyClassifier`; inspecting team chosen by the instructor | Week 10 (INS-01); `DelinquencyClassifier` when built |
| Implementation | Commits | TDD evidence: failing test committed before code | Commit convention `test(red)` / `feat(green)` / `refactor`; no squash (ruleset) | Git history of PR #8 and later PRs | Each author | Every item |
| Test | BR boundary values | Black-box design: EP and BVA traced to RTM IDs | EP/BVA tables | `qa/test-design/black-box.md` (186 cases, 71 automated; ledger 54 of 62); JUnit tests | Module owners | M2 design, M3 executed |
| Test | Integrity-level 4 methods | White-box design: CFG, cyclomatic complexity, basis paths | Hand CFG; JaCoCo complexity counter | `qa/test-design/white-box.md` (`RepaymentAllocator.allocate` V(G) 10, `CoreRepaymentPoster.post`, `LoanAccount.restore`, `LedgerEntry.requireValid`) | Module owners | M2 design (ledger done), M3 executed |
| Test | Whole suite | Coverage measurement | JaCoCo (`jacocoTestReport`); `coverageGate` task, branch coverage >= 60% | `build/reports/jacoco/test/html/` uploaded by CI; at `701c161`: 94.5% lines, 86.5% branches (existing modules only) | Integrator | Measured M2; 60% gate enforced M3 |
| Test | Posting path | Fault injection: ledger stays balanced at every write point (QR-03) | `PostingFaultInjectionTest` (L-16): 14 failure points on a real SQLite file | CI test report (PR #8 run 43596151001) | Tughral | Done at M2; live demonstration at M3 |
| Test | End-of-day engine | Concurrency: no lost update, no double accrual (QR-06) | Latch-driven `EndOfDayConcurrencyTest`, 50 repetitions | Scheduled CI job (to be added with the engine) | Ibrahim | M2 code, M3 evidence |
| Test | End-of-day engine | Performance (QR-01) | `EndOfDayPerformanceTest` on 10,000 generated loans | Timings attached to milestone report | Ibrahim | M3 |
| M3 acceptance | Integrated build at `m3-baseline` | Acceptance gate (SQAP 5.1.3): all FRs traced to passing tests; >= 60% core coverage, 100% decision coverage on money path; zero open critical defects | RTM final status; JaCoCo; defect log | `qa/rtm.md`, CI artifacts, `qa/defect-log.md` | All three | Week 13 |
| M3 acceptance | Running system | Validation demo: teller posting, end-of-day, statement, PAR30 report; two injected failure scenarios | Live demonstration from the submitted build | Demo script and recording notes | All three | Week 13 |

## 5. Requirement to V&V activity

Technique codes: EP, BVA, BP (basis path), INV (invariant), INSP (inspection), GOLD (golden file),
SA (static analysis), CONC (concurrency test), FI (fault injection), DEMO (validation demo).
Status is taken from `qa/rtm.md` on 2026-11-16.

| Req | Rule | Verification | Validation | Owner | Status |
|---|---|---|---|---|---|
| FR-01 | BR-01, BR-02 | TC-ORG-01: BVA on ages 17/18, 65/66 at maturity | DEMO (loan application) | Ibrahim | planned |
| FR-02 | BR-03 | TC-ORG-02: EP + BVA on 1/2/3 active loans | DEMO | Ibrahim | planned |
| FR-03 | BR-04 | TC-ORG-03: BVA on burden 40% / 40.01% | DEMO | Ibrahim | planned |
| FR-04 | BR-05 | TC-ORG-04: EP prior write-off yes/no | - | Ibrahim | planned |
| FR-05 | BR-08 | TC-ORG-05: EP, rule-order cases | Review of rejection reasons | Ibrahim | planned |
| FR-06 | BR-09, BR-10 | TC-ORG-06: EP + BVA per grade | - | Ibrahim | planned |
| FR-07 | BR-06, BR-07 | TC-ORG-07: BVA on product bands | - | Ibrahim | planned |
| FR-08 | BR-11, BR-17 | TC-PRD-01: GOLD, 10 schedules per product | Schedules hand-computed independently | Hassan | planned |
| FR-09 | BR-12 | TC-PRD-02: BVA on 28/29/30/31 day months | - | Hassan | in PR #6 (CI failing) |
| FR-10 | BR-13 | TC-PRD-03: EP + BVA grace 0 / 1 / max | - | Hassan | planned |
| FR-11 | BR-14, BR-15 | TC-PRD-04: INV sum of instalments | - | Hassan | planned |
| FR-12 | BR-16 | TC-PRD-05: EP tenure 1 / 6 / 36 | - | Hassan | planned |
| FR-13 | BR-18, BR-19 | TC-LED-01: BP on `RepaymentAllocator.allocate()` (V(G) 10, 18/18 branches); INSP (Fagan INS-01) | BR-19 reading decided by the team (D-01, 2026-11-16) | Tughral | verified (PR #8 CI for c17052f); inspection pending |
| FR-14 | BR-20 | TC-LED-02: BVA 0 / 0.01 / due - 0.01 / due / due + 0.01 | - | Tughral | verified (PR #8 CI for c17052f) |
| FR-15 | BR-21, BR-22 | TC-LED-03: BVA due + 0.01, exact payoff, payoff + 0.01 | Rebate per product confirmed with Hassan | Tughral | partial: advance credit and payoff detection tested; per-product rebate outstanding (`products`) |
| FR-16 | BR-23 | TC-LED-04: INV debits = credits after every posting; `LedgerImbalanceException`; atomic `appendAll` | DEMO | Tughral | implemented for repayment postings (PR #8 CI for 34a7bb7); rebate and end-of-day postings not yet covered |
| FR-17 | BR-24 | TC-LED-05: unit + INSP | - | Tughral | implemented; unit tests pass in PR #8 CI; INS-01 checklist item pending |
| FR-18 | BR-25 | TC-LED-06: EP same pair / other bank / cash / failed-then-retried; 8 concurrent duplicates | - | Tughral | partial: guard verified in PR #8 CI (71d8b3f); pairs held in memory only, durable store pending |
| FR-19 | BR-26 | TC-LED-07: EP active / closed / written-off, at the account, the poster and the settlement policy | - | Tughral | verified (CI 42471721763) |
| FR-20 | - | TC-LED-08: EP per bank format | Sample statements | Ibrahim | planned |
| FR-21 | BR-27, BR-28 | TC-DEL-01: BVA on 10% cap | - | Ibrahim | planned |
| FR-22 | BR-29 | TC-DEL-02: BVA day 0 / 1 | - | Ibrahim | planned |
| FR-23 | BR-30 | TC-DEL-03: BP (CC about 6) + BVA 0/1/30/31/60/61/90/91; INSP | - | Ibrahim | planned |
| FR-24 | BR-31 | TC-DEL-04: EP per class | - | Ibrahim | planned |
| FR-25 | BR-32 | TC-DEL-05: BVA 180 / 181 days | - | Ibrahim | planned |
| FR-26 | BR-33 | TC-DEL-06: INV rerun same date; schema UNIQUE | DEMO (rerun end-of-day) | Ibrahim | planned (schema tested) |
| FR-27 | BR-34 | TC-DEL-07: EP class changed / unchanged | - | Ibrahim | planned |
| FR-28 | BR-35 | TC-REP-01: EP date ranges | DEMO (statement) | Hassan | planned |
| FR-29 | BR-36 to BR-38 | TC-REP-02: GOLD report figures | DEMO (PAR30) | Hassan | planned |
| FR-30 | BR-39 | TC-REP-03: EP current / past / before first loan | - | Hassan | planned |
| FR-31 | - | TC-LED-09: EP named / blank operator, refused posting, web form operator; per-file import audit | - | Tughral (teller), Ibrahim (import) | partial: teller half verified in PR #8 CI (a43c681); per-file import line not built |
| FR-32 | - | TC-REP-04: EP empty / one / full portfolio | - | Hassan | planned |
| QR-01 | - | Test: `EndOfDayPerformanceTest` | Timings on demo machine | Ibrahim | planned |
| QR-02 | - | GOLD: `GoldenScheduleTest`, 30 schedules | Independent hand computation | Hassan | planned |
| QR-03 | - | FI: `PostingFaultInjectionTest`, 14 points (target >= 10) | Live failure scenario at M3 | Tughral | test passes in PR #8 CI (5502ccf, run 43596151001); demonstration not yet given |
| QR-04 | - | SA: complexity and size metrics from JaCoCo and PMD | - | Muhammad Ibrahim (M2 Team Lead) | measured at M2: highest McCabe 10 (`RepaymentAllocator.allocate`); CI gate not yet wired |
| QR-05 | - | INSP checklist item + `LedgerImmutabilityTest` + source scan in `SqliteLedgerRepositoryTest` | - | Tughral | planned: automated checks pass; INS-01 checklist item still owed |
| QR-06 | - | CONC: `EndOfDayConcurrencyTest`, 50 runs | Rerun with concurrent tellers in demo | Ibrahim | planned |
| C-01 | - | SA: Checkstyle IllegalType / IllegalToken, build fails | - | All | active |
| C-03 | - | `ArchitectureTest#noCyclesBetweenModules` (ArchUnit 1.3.0) in every build | - | Tughral | verified (PR #8 CI run 43596655380 for e0f9ecb) |
| C-04 | - | `SchemaMigratorTest`, `SqliteLedgerRepositoryTest` | - | Tughral | verified (CI 42982638575) |

## 6. Anomaly reporting and CAPA

Every anomaly found by any V&V task (inspection, static analysis, failing test on `develop`,
review comment that requires a change, validation disagreement) is recorded in
`qa/defect-log.md` with: ID, date, source activity, artifact and location, requirement ID,
description, type, severity, phase injected and phase found, corrective action, preventive
action, owner, status, and the commit that closed it. The log records the defect, not who
wrote it (SQAP 4.4 quality-culture statement).

| Severity | Meaning in MicroLend | Required response |
|---|---|---|
| Critical | Wrong money, unbalanced or altered ledger, data loss, race that changes balances | Fix before the milestone baseline; regression test added; preventive action recorded |
| Major | Business rule violated in an edge case, missing validation at a module boundary, unhandled exception | Fix in the current milestone or defer with a stated reason and owner |
| Minor | Standards, naming, Javadoc, readability, complexity above target | Fix or justify; may be batched |

CAPA loop: corrective action fixes the instance; preventive action changes the process
(a checklist item, a Checkstyle rule, a test pattern) so the class of defect does not recur.
CAPA items opened at M2 are reviewed in the M3 final audit and closed, or kept open with a reason.

## 7. V&V reporting and exit criteria

| Report | Content | When |
|---|---|---|
| PR description and CI run | Scope, red/green commit hashes, CI result, reports artifact | Every PR |
| RTM update | Status per requirement with CI run IDs as evidence | Each milestone |
| Inspection report | Fagan stages, roles, preparation time, defects, rates | M2 |
| Static-analysis report | Findings and triage | M2, refreshed M3 |
| V&V summary | Results against this plan, open anomalies, residual risk | M3 |

**M2 exit criteria (verification baseline):** CI green on `develop` with all module PRs merged;
Fagan inspection held with an external inspector and its defects logged; static-analysis
findings triaged; EP/BVA and basis-path designs traced to RTM IDs; coverage measured and
reported (no threshold); RTM, risk register and defect log updated; `m2-baseline` tag on `main`.

**M3 exit criteria (acceptance):** the SQAP 5.1.3 gate in section 4 above; designed tests
executed with results recorded against RTM IDs; QR-01, QR-03 and QR-06 evidenced; all M2 CAPA
items closed or justified; `m3-baseline` tag on `main`.

**Known gaps in the plan as of 2026-11-16:**
- CI does not yet run `coverageGate`, only `jacocoTestReport` (DEF-008, deferred to M3).
- The scheduled 50-run concurrency job for QR-06 does not exist, because the end-of-day
  engine is not built (owner: Ibrahim).
- PMD runs from `config/pmd-init.gradle` on demand and is not yet a CI step. The M2 findings
  are triaged in `qa/static-analysis-report.md`, and wiring PMD into CI is recommended for M3.
- `origination`, `products`, `delinquency` and `reporting` have no code to verify yet. Their
  rows above stay *planned*.

## 8. Responsibilities

| Role | Who | V&V responsibilities |
|---|---|---|
| M2 Team Lead / Integrator | Muhammad Ibrahim | Leads the M2 documents (this plan, inspection report, static-analysis report, test designs, RTM/risk/defect updates); merges reviewed PRs; tags `m2-baseline` |
| Module owner, `origination` + `delinquency` | Muhammad Ibrahim | Tests for FR-01 to FR-07, FR-20 to FR-27; QR-01, QR-06; moderates the repayment-path inspection (INS-01) |
| Module owner, `products` + `reporting` | Hassan Khalid | Tests for FR-08 to FR-12, FR-28 to FR-30, FR-32; QR-02; moderates the `DelinquencyClassifier` inspection |
| Module owner, `ledger` + `persistence` + web | Tughral Hussain | Tests for FR-13 to FR-19, FR-31; QR-03, QR-05; C-03, C-04 |
| PR reviewer | Any non-author member | Design conformance and standards check on every PR (ruleset: one approval) |
| External inspector | From the team the instructor pairs us with | Independent inspection of the money path |
