# Milestone 2 - SE431 Software Quality Assurance

**Companion document:** `docs/M2-SE423-Architecture-Implementation.md` (construction report for the same build).
**Build verified:** `feature/delinquency-origination-import` at code head `4067d2f` (21 Nov 2026): the PR #8 ledger work (merged into `develop` 21 Nov) plus `origination`, `delinquency`, statement import, the durable bank-reference store and the independent code review's fixes. Ledger-only figures of 16 Nov were measured at `701c161`. PR #9, approved and merged by Tughral Hussain on 21 Nov 2026, passed CI in run 44930290499; `develop` passed after the merge (run 44931805244 at `ac14ba9`). **Document lead:** Muhammad Ibrahim (M2 Team Lead).
**Submission:** 22 Nov 2026 (date extended by the instructor).
**Repository:** github.com/MicroLend-SE423/microlend. Every artifact below is a file in `qa/`, and `docs/build-pdf.mjs` builds this PDF from those files, so the submitted text and the repository cannot drift apart.

<div class="glance">
<div class="glance-title">Verification baseline at a glance</div>
<div class="tiles">
<div class="tile"><div class="v">384</div><div class="l">automated tests</div><div class="s">0 failures, 0 skipped</div></div>
<div class="tile"><div class="v">95.2%</div><div class="l">line coverage</div><div class="s">88.8% branch, 98.7% method</div></div>
<div class="tile"><div class="v">81</div><div class="l">CI runs</div><div class="s">78 passed, 3 failures explained (11)</div></div>
<div class="tile"><div class="v">2 / 2</div><div class="l">PRs through the review gate</div><div class="s">non-author approval, reviewer merged (11)</div></div>
<div class="tile"><div class="v">186</div><div class="l">black-box cases</div><div class="s">152 automated; EP and BVA (9)</div></div>
<div class="tile"><div class="v">6</div><div class="l">white-box methods</div><div class="s">CFG, V(G) two ways, basis paths (10)</div></div>
<div class="tile"><div class="v">26 / 38</div><div class="l">requirements tested</div><div class="s">17 FR and 2 constraints verified in CI (12)</div></div>
<div class="tile"><div class="v">0</div><div class="l">Checkstyle findings</div><div class="s">zero-warning gate; C-01 holds (7)</div></div>
</div>
<div class="segs">
<div class="seg"><div class="seg-head"><span>PMD 7.7.0 findings, triaged</span><span class="t">100 open (7)</span></div>
<div class="seg-bar"><span class="k1" style="flex:10 1 0">10</span><span class="k2" style="flex:20 1 0">20</span><span class="k3" style="flex:70 1 0">70</span></div>
<div class="seg-legend"><span><i class="k1"></i>Accepted</span><span><i class="k2"></i>Deferred</span><span><i class="k3"></i>False positive</span></div></div>
<div class="seg"><div class="seg-head"><span>Defects logged</span><span class="t">23, with 9 CAPA (6)</span></div>
<div class="seg-bar"><span class="k1" style="flex:11 1 0">11</span><span class="k2" style="flex:5 1 0">5</span><span class="k3" style="flex:7 1 0">7</span></div>
<div class="seg-legend"><span><i class="k1"></i>Fixed or verified</span><span><i class="k2"></i>Deferred</span><span><i class="k3"></i>Open</span></div></div>
</div>
</div>

## 1. Overview

### 1.1 What this milestone verifies

M2 is the first milestone with running code, and the quality work below is applied to it. Built (detail in SE423 section 0): `common` with the per-account `LockRegistry`; `ledger` with its decorators, including `LockingPoster`; `ledger.importing` (HBL and MCB Adapters; claimed bank references kept in SQLite, so a duplicate is refused across restarts); `origination` (BR-01 to BR-08, BR-09/BR-10; loan creation not built); `delinquency` with the concurrent end-of-day engine; `persistence` (schema version 2); and `web` (teller and end-of-day pages).

Hassan Khalid's `products` (only `DueDateCalculator`, in open PR #6) and `reporting` have no code on `develop`, so every artifact marks rows that depend on them as *planned*. Until `products` exists, the running application's postings and end-of-day tasks report that they are waiting for it; the posting path and the engine are verified end to end in tests with stub sources.

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

The instructor assigns the inspecting team. **On 21 Nov 2026 no inspector from another team had been assigned, so inspection INS-01 has not been held** (risk R3, now materialised). The package in sections 2 to 5 is complete: the process, the INS-01 plan for the repayment path (sized on the merged code; meeting A about 228 NCLOC), a 39-item project checklist and the recording forms, which are completed at and after the meeting with the external inspector's name. Its defects will enter the defect log (section 6) with severity and CAPA. Until then the log holds defects from static analysis, the SCM review, the PR #8 code review and an independent code review of the M2 code; none is reported as an inspection result.

### 1.4 Key results at this baseline

| Measure | Value | Source |
|---|---|---|
| Automated tests | 384 in 46 test classes, 0 failures, 0 skipped (182 at `701c161`) | `./gradlew clean build jacocoTestReport` at `4067d2f`; PR #9 CI, run 44930290499 passed; `develop` run 44931805244 passed |
| Line / branch / method coverage (code that exists) | 95.2% / 88.8% / 98.7% (1,309/1,375; 437/492; 294/298) | JaCoCo report totals, section 10 |
| Checkstyle findings (gate, includes C-01 no `double`/`float`) | 0 | section 7 |
| PMD 7.7.0 findings at `4067d2f` | 100 open: 10 accepted, 20 deferred, 70 false positive. 39 are new in the 21 Nov code (8 accepted, 9 deferred, 22 false positive). The ledger code had 66, of which 4 were fixed | section 7 |
| Highest cyclomatic complexity | 12 (`origination.Applicant` constructor); on the money path 10 (`RepaymentAllocator.allocate`; hand-drawn CFG equals JaCoCo); mean 1.8; none above 15 | sections 7 and 10 |
| White-box designs (CFG, V(G) computed two ways, basis paths) | 6 methods: `RepaymentAllocator.allocate` (10), `LoanAccount.restore` (8), `LedgerEntry.requireValid` (6), `CoreRepaymentPoster.post` (6), `DelinquencyClassifier.classify` (6), `EndOfDayEngine.collect` (7) | section 10 |
| Black-box test cases (EP, BVA) | 186 designed, 152 automated (71 at `701c161`) | section 9 |
| RTM | 26 of 38 requirements with executed, passing tests (8 of them only partly met); 17 functional requirements and 2 constraints *verified* in CI; 92 of 92 production classes traced; no rogue code | section 12 |
| CI runs (4 Oct to 21 Nov) | 81: 78 passed, 3 failed with causes recorded | section 11 |
| Pull requests since the review gate (4 Nov) | 2 merged (PR #8, PR #9), each with one non-author approval and merged by its reviewer; PR #6 open with changes requested | section 11 |
| TDD trail | 19 red/green pairs in PR #8, 9 in PR #9 | section 11 |
| Defects logged / CAPA opened | 23 / 9. DEF-009 to DEF-016 come from the code review of PR #8 and DEF-018 to DEF-023 from the independent code review. 8 Major in all; 11 fixed or verified, 5 deferred, 7 open; 6 CAPA open | section 6 |
| Open money-path limitations carried to M3 | Imports must run with the server stopped (DEF-022); the BR-22 rebate line is not written (DEF-013); the write-off is listed, not posted (FR-25) | sections 6 and 12 |
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
