# Milestone 2 - SE431 Software Quality Assurance

**Companion document:** `docs/M2-SE423-Architecture-Implementation.md` (construction report for the same build).
**Build verified:** `develop` at `e7ecce6` (22 Nov 2026): the ledger (PR #8), `origination`, `delinquency` and statement import (PR #9, code head `4067d2f`, 21 Nov), and Hassan Khalid's `products` and `reporting` (PRs #11 to #13, 22 Nov). `develop` passed CI after the last merge (run 45030126014). Figures at `4067d2f` (21 Nov) and `701c161` (ledger only, 16 Nov) are kept where a table compares. **Document lead:** Muhammad Ibrahim (M2 Team Lead).
**Submission:** 22 Nov 2026 (date extended by the instructor).
**Repository:** github.com/MicroLend-SE423/microlend. Every artifact below is a file in `qa/`, and `docs/build-pdf.mjs` builds this PDF from those files, so the submitted text and the repository cannot drift apart.

<div class="glance">
<div class="glance-title">Verification baseline at a glance (22 Nov build)</div>
<div class="tiles">
<div class="tile"><div class="v">492</div><div class="l">automated tests</div><div class="s">0 failures, 0 skipped</div></div>
<div class="tile"><div class="v">95.4%</div><div class="l">line coverage</div><div class="s">87.7% branch, 99.0% method</div></div>
<div class="tile"><div class="v">97</div><div class="l">CI runs</div><div class="s">94 passed, 3 failures explained (11)</div></div>
<div class="tile"><div class="v">6 / 6</div><div class="l">PRs through the review gate</div><div class="s">non-author approval, reviewer merged (11)</div></div>
<div class="tile"><div class="v">186</div><div class="l">black-box cases</div><div class="s">174 automated; EP and BVA (9)</div></div>
<div class="tile"><div class="v">6</div><div class="l">white-box methods</div><div class="s">CFG, V(G) two ways, basis paths (10)</div></div>
<div class="tile"><div class="v">33 / 38</div><div class="l">requirements tested</div><div class="s">21 FR and 2 constraints verified in CI (12)</div></div>
<div class="tile"><div class="v">0</div><div class="l">Checkstyle findings</div><div class="s">zero-warning gate; C-01 holds (7)</div></div>
</div>
<div class="segs">
<div class="seg"><div class="seg-head"><span>PMD 7.7.0 findings, triaged</span><span class="t">122 open (7)</span></div>
<div class="seg-bar"><span class="k1" style="flex:10 1 0">10</span><span class="k2" style="flex:28 1 0">28</span><span class="k3" style="flex:84 1 0">84</span></div>
<div class="seg-legend"><span><i class="k1"></i>Accepted</span><span><i class="k2"></i>Deferred</span><span><i class="k3"></i>False positive</span></div></div>
<div class="seg"><div class="seg-head"><span>Defects logged</span><span class="t">30, with 11 CAPA (6)</span></div>
<div class="seg-bar"><span class="k1" style="flex:11 1 0">11</span><span class="k2" style="flex:7 1 0">7</span><span class="k3" style="flex:12 1 0">12</span></div>
<div class="seg-legend"><span><i class="k1"></i>Fixed or verified</span><span><i class="k2"></i>Deferred</span><span><i class="k3"></i>Open</span></div></div>
</div>
</div>

## 1. Overview

### 1.1 What this milestone verifies

M2 is the first milestone with running code, and the quality work below is applied to it. Built (detail in SE423 section 0): `common` with the per-account `LockRegistry`; `ledger` with its decorators, including `LockingPoster`; `ledger.importing` (HBL and MCB Adapters; claimed bank references kept in SQLite, so a duplicate is refused across restarts); `origination` (BR-01 to BR-08, BR-09/BR-10; loan creation not built); `delinquency` with the concurrent end-of-day engine; `products` (the Factory Method, schedules, rebate); `reporting` (the customer statement only); `persistence` (schema version 2); and `web` (teller and end-of-day pages).

`products` and `reporting` arrived on 22 Nov (PRs #11 to #13) and are verified here with the rest: tests and coverage (sections 9 and 12), PMD triage (7), the black-box TC-PRD and TC-REP cases (9), the RTM (12) and the defect log (DEF-024 to DEF-030, section 6). The white-box selection (10) and the INS-01 scope (3) were fixed before they arrived and do not include them; both are extended at M3. Nothing creates a loan or schedule until loan creation (I-03, M3), so the running application's postings and end-of-day tasks are refused for lack of a schedule; every path is verified end to end in tests over a saved schedule or a stub source.

### 1.2 Where each rubric criterion is evidenced

| SE431 M2 criterion | Weight | Section | Repository source |
|---|---|---|---|
| Fagan inspection report | 20 | 2 to 5, 6 | `qa/inspections/`, `qa/defect-log.md` |
| Static analysis report | 15 | 7 | `qa/static-analysis-report.md`, `config/pmd-init.gradle`, `config/checkstyle.xml` |
| V&V plan (IEEE 1012) | 15 | 8 | `qa/vv-plan.md` |
| Black-box test design (EP, BVA) | 15 | 9 | `qa/test-design/black-box.md` |
| White-box test design (CFG, CC, basis paths) | 15 | 10 | `qa/test-design/white-box.md` |
| SCM quality-gate evidence | 10 | 11 | `qa/scm-evidence.md` |
| RTM, risk and defect/CAPA update | 10 | 12, 13, 6 | `qa/rtm.md`, `qa/risk-register.md`, `qa/defect-log.md` |

### 1.3 Status of the Fagan inspection

The instructor assigns the inspecting team. **On 21 Nov 2026 no inspector from another team had been assigned, so inspection INS-01 has not been held** (risk R3, now materialised). The package in sections 2 to 5 is complete: the process, the INS-01 plan for the repayment path (sized on the merged code; meeting A about 228 NCLOC), a 39-item project checklist and the recording forms, which are completed at and after the meeting with the external inspector's name. Its defects will enter the defect log (section 6) with severity and CAPA. Until then the log holds defects from static analysis, the SCM review, the reviews of PRs #8, #11 and #12 and an independent code review of the M2 code; none is reported as an inspection result.

### 1.4 Key results at this baseline

| Measure | Value | Source |
|---|---|---|
| Automated tests | 492 in 69 test classes, 0 failures, 0 skipped (384 at `4067d2f`, 182 at `701c161`) | `./gradlew clean build jacocoTestReport` on the 22 Nov build; `develop` CI run 45030126014 at `e7ecce6` passed |
| Line / branch / method coverage (code that exists) | 95.4% / 87.7% / 99.0% (1,680/1,761; 563/642; 390/394); `products` 211/221 lines, `reporting` 65/65 | JaCoCo report totals, section 10 |
| Checkstyle findings (gate, includes C-01 no `double`/`float`) | 0 | section 7 |
| PMD 7.7.0 findings on the 22 Nov build | 122 open: 10 accepted, 28 deferred, 84 false positive. 22 are new in `products` and `reporting` (8 deferred as DEF-029 and DEF-030, 14 false positive); 39 were new in the 21 Nov code; the ledger code had 66, of which 4 were fixed | section 7 |
| Highest cyclomatic complexity | 12 (`origination.Applicant` constructor); on the money path 10 (`RepaymentAllocator.allocate`; hand-drawn CFG equals JaCoCo); mean 1.8; none above 15 | sections 7 and 10 |
| White-box designs (CFG, V(G) computed two ways, basis paths) | 6 methods, chosen before `products` merged: `RepaymentAllocator.allocate` (10), `LoanAccount.restore` (8), `LedgerEntry.requireValid` (6), `CoreRepaymentPoster.post` (6), `DelinquencyClassifier.classify` (6), `EndOfDayEngine.collect` (7) | section 10 |
| Black-box test cases (EP, BVA) | 186 designed, 174 automated (152 at `4067d2f`, 71 at `701c161`); `products` 20 of 21 | section 9 |
| RTM | 33 of 38 requirements with executed, passing tests (11 of them only partly met; 26 at `4067d2f`); 21 functional requirements and 2 constraints *verified* in CI; 122 of 122 production classes traced; no rogue code | section 12 |
| CI runs (4 Oct to 22 Nov) | 97: 94 passed, 3 failed with causes recorded | section 11 |
| Pull requests since the review gate (4 Nov) | 6 merged (PRs #8 to #13), each with one non-author approval and merged by its reviewer; PRs #11 to #13 retargeted and re-authored before review (SCM-05); PR #6 open, superseded by #11 | section 11 |
| TDD trail | 19 red/green pairs in PR #8, 9 in PR #9, 9 in PRs #11 and #12 | section 11 |
| Defects logged / CAPA opened | 30 / 11. DEF-009 to DEF-016 come from the code review of PR #8, DEF-018 to DEF-023 from the independent code review, DEF-024 to DEF-028 from the review of PRs #11 and #12, DEF-029 and DEF-030 from static analysis of `products`. 12 Major in all; 11 fixed or verified, 7 deferred, 12 open; 8 CAPA open | section 6 |
| Open money-path limitations carried to M3 | Imports must run with the server stopped (DEF-022); the BR-22 rebate line is not written (DEF-013); the write-off is listed, not posted (FR-25); paid instalments still count as owed until T-01 (DEF-024); the statement balance is wrong on balanced postings (DEF-028). DEF-024 to DEF-028 cannot be reached until loan creation (I-03) | sections 6 and 12 |
| Fagan inspection INS-01 | Planned, not held (no external inspector assigned) | sections 2 to 5 |

### 1.5 How to reproduce

```powershell
.\gradlew.bat build jacocoTestReport                  # tests, Checkstyle, JaCoCo
.\gradlew.bat --init-script config/pmd-init.gradle pmdMain
cd docs; npm install; npm run build                   # rebuilds this PDF and the SE423 one
```

<!-- include 2 ../qa/inspections/README.md -->

<!-- include 3 ../qa/inspections/INS-01-repayment-path/plan.md -->

<!-- include 4 ../qa/inspections/INS-01-repayment-path/checklist.md -->

<!-- include 5 ../qa/inspections/INS-01-repayment-path/forms.md -->

<!-- include 6 ../qa/defect-log.md -->

<!-- include 7 ../qa/static-analysis-report.md -->

<!-- include 8 ../qa/vv-plan.md -->

<!-- include 9 ../qa/test-design/black-box.md -->

<!-- include 10 ../qa/test-design/white-box.md -->

<!-- include 11 ../qa/scm-evidence.md -->

<!-- include 12 ../qa/rtm.md -->

<!-- include 13 ../qa/risk-register.md -->
