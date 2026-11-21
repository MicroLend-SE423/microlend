# Milestone 2 - SE431 Software Quality Assurance

**Companion document:** `docs/M2-SE423-Architecture-Implementation.md`, the construction-side report for the same build.
**Build verified:** `feature/delinquency-origination-import` at `7784a6a` (21 Nov 2026). It contains the ledger work of PR #8, merged into `develop` on 21 Nov, plus the `origination`, `delinquency` and statement-import code. The ledger-only figures of 16 Nov were measured at `701c161`. PR number and CI run for `7784a6a`: *to be filled by the lead*. **Document lead:** Muhammad Ibrahim (M2 Team Lead).
**Submission:** 22 Nov 2026 (date extended by the instructor).
**Repository:** github.com/MicroLend-SE423/microlend. Every artifact below is a file in the repository's `qa/` folder. This PDF is built from those files by `docs/build-pdf.mjs`, so the submitted text and the repository cannot drift apart.

## 1. Overview

### 1.1 What this milestone verifies

M2 is the first milestone with running code. The quality work below is applied to that code, not to a plan of it. At the time of writing, these modules exist:
- `common`: money, identifiers and the per-account `LockRegistry`.
- `ledger`: allocation, settlement, the posting pipeline and its decorators, including `LockingPoster`.
- `ledger.importing`: the HBL and MCB bank-statement Adapters and the import job.
- `origination`: eligibility rules BR-01 to BR-08 and risk grade and cap BR-09/BR-10. Loan creation is not built.
- `delinquency`: days past due, classification, penalty, provisioning and write-off policies, and the concurrent end-of-day engine.
- `persistence`: SQLite schema (version 2) and repositories, including the end-of-day result store.
- `web`: the local teller page and the end-of-day page.

The following modules are owned by Hassan Khalid and have no code on `develop`:
- `products` (only `DueDateCalculator`, in open PR #6);
- `reporting`.

Each artifact states its scope and marks every row that depends on those modules as *planned*, rather than reporting it as done. Until `products` exists, the running application's postings and end-of-day tasks report that they are waiting for it. The posting path and the engine are verified end to end in tests with stub sources.

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

The instructor assigns the inspecting team. **On 21 Nov 2026 no inspector from another team had been assigned, so inspection INS-01 has not been held** (risk R3, now materialised). The inspection package (sections 2 to 5) is complete: the process, the plan for inspection INS-01 of the repayment path, a 34-item project checklist and the recording forms. The plan's scope is now sized on the merged code (meeting A about 228 NCLOC). The forms are completed at and after the meeting, with the external inspector's name. The defects found will go into the defect log (section 6), each with its severity and CAPA. Until the meeting, the defect log holds the defects found by static analysis, by the SCM review and by the code review of PR #8. None of the logged defects is reported as an inspection result.

### 1.4 Key results at this baseline

| Measure | Value | Source |
|---|---|---|
| Automated tests | 366 in 45 test classes, all passing (182 at `701c161`) | `./gradlew build jacocoTestReport` at `7784a6a`; PR CI *to be filled by the lead* |
| Line / branch / method coverage (code that exists) | 95.1% / 89.0% / 98.6% (1,266/1,331; 422/474; 286/290) | JaCoCo report totals, section 10 |
| Checkstyle findings (gate, includes C-01 no `double`/`float`) | 0 | section 7 |
| PMD 7.7.0 findings at `7784a6a` | 98 open: 10 accepted, 20 deferred, 68 false positive. 37 are new in the 21 Nov code (8 accepted, 9 deferred, 20 false positive). The ledger code had 66, of which 4 were fixed | section 7 |
| Highest cyclomatic complexity | 12 (`origination.Applicant` constructor); on the money path 10 (`RepaymentAllocator.allocate`; hand-drawn CFG equals JaCoCo); mean 1.8; none above 15 | sections 7 and 10 |
| White-box designs (CFG, V(G) computed two ways, basis paths) | 6 methods: `RepaymentAllocator.allocate` (10), `LoanAccount.restore` (8), `LedgerEntry.requireValid` (6), `CoreRepaymentPoster.post` (6), `DelinquencyClassifier.classify` (6), `EndOfDayEngine.collect` (7) | section 10 |
| Black-box test cases (EP, BVA) | 186 designed, 152 automated (71 at `701c161`) | section 9 |
| RTM | 26 of 38 requirements with executed, passing tests (9 of them only partly met); 89 of 89 production classes traced; no rogue code | section 12 |
| CI runs (4 Oct to 14 Nov) | 50: 45 passed, 3 failed with causes recorded, 2 in progress | section 11 |
| Defects logged / CAPA opened | 17 / 8 (DEF-009 to DEF-016 from the code review of PR #8; 6 Major in all; 6 fixed or verified, 5 deferred, 6 open; 6 CAPA open) | section 6 |
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
