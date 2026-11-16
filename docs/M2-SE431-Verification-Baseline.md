# Milestone 2 - SE431 Software Quality Assurance

**Companion document:** `docs/M2-SE423-Architecture-Implementation.md`, the construction-side report for the same build.
**Build verified:** `feature/ledger-entry` (PR #8 into `develop`) at `87c5caf`. **Document lead:** Muhammad Ibrahim (M2 Team Lead).
**Repository:** github.com/MicroLend-SE423/microlend. Every artifact below is a file in the repository's `qa/` folder. This PDF is built from those files by `docs/build-pdf.mjs`, so the submitted text and the repository cannot drift apart.

## 1. Overview

### 1.1 What this milestone verifies

M2 is the first milestone with running code. The quality work below is applied to that code, not to a plan of it. At the time of writing, these modules exist:
- `common`: money and identifiers.
- `ledger`: allocation, settlement, the posting pipeline and its decorators.
- `persistence`: SQLite schema and repositories.
- `web`: the local teller page.

The following modules are owned by Muhammad Ibrahim and Hassan Khalid and were still being built when this document was assembled:
- `origination`;
- `products`;
- `delinquency`;
- `reporting`;
- the bank-statement adapters.

Each artifact states its scope and marks every row that depends on those modules as *planned*, rather than reporting it as done.

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

The instructor assigns the inspecting team. The inspection package (sections 2 to 5) is complete: the process, the plan for inspection INS-01 of the repayment path, a 34-item project checklist and the recording forms. The forms are completed at and after the meeting, with the external inspector's name. The defects found go into the defect log (section 6), each with its severity and CAPA. Until the meeting, the defect log holds the defects found by static analysis and by the SCM review.

### 1.4 Key results at this baseline

| Measure | Value | Source |
|---|---|---|
| Automated tests | 182, all passing | `.\gradlew.bat build` at `87c5caf`; PR #8 CI |
| Line / branch coverage (code that exists) | 94.5% / 86.5% | JaCoCo, section 10 |
| Checkstyle findings (CI gate, includes C-01 no `double`/`float`) | 0 | section 7 |
| PMD 7.7.0 findings on the final M2 code | 66: 4 fixed, 2 accepted, 12 deferred, 48 false positive | section 7 |
| Highest cyclomatic complexity on the money path | 10 (`RepaymentAllocator.allocate`; hand-drawn CFG equals JaCoCo) | section 10 |
| CI runs (4 Oct to 14 Nov) | 50: 45 passed, 3 failed with causes recorded, 2 in progress | section 11 |
| Defects logged / CAPA opened | 8 / 3 | section 6 |

### 1.5 How to reproduce

```powershell
.\gradlew.bat build                                   # tests, Checkstyle, JaCoCo
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
