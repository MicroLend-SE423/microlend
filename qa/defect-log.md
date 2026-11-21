# MicroLend - Defect log and CAPA register

Single register for defects found from M2 onward, whatever found them, and for the
corrective and preventive actions (CAPA) they lead to. Process and classification rules:
[`inspections/README.md`](inspections/README.md).

**Rules**

- IDs are sequential and never reused: `DEF-nnn` for defects, `CAPA-nnn` for actions.
- Rows are appended, not rewritten. A status change updates the status cell and is visible in Git history.
- **Source** is one of: `Inspection INS-nn`, `Static analysis (tool)`, `Test`, `CI`, `Review PR #n`.
- **Severity:** Major / Minor. **Type:** Logic / Data / Interface / Standards / Documentation.
- **Status:** Open -> Fixed -> Verified (closed), or Deferred (with justification and a CAPA) or Rejected (not a defect; reason given).
- A defect is closed only when the fix commit and the test or CI run that shows it are recorded.

Static-analysis findings triaged as *fixed* or *deferred* will be added here from
[`static-analysis-report.md`](static-analysis-report.md). Findings triaged as false
positives stay in that report only.

## Defects

| Defect ID | Source | Date found | Artifact and line | Description | Type | Severity | Found by | RTM / BR ref | Status | CAPA ID |
|---|---|---|---|---|---|---|---|---|---|---|
| DEF-001 | Static analysis (PMD 7.7.0, SA-01) | 2026-11-14 | `LedgerEntry.java:69`, `RepaymentCommand.java:96`, `AuditLoggingPoster.java:42` | Blank-text check uses `trim().isEmpty()`, allocating a copy; `isBlank()` is the intended test | Standards | Minor | Tughral Hussain | FR-17, BR-25 | Verified (`e3f585d`; CI run 43829233377 passed) | CAPA-002 |
| DEF-002 | Static analysis (PMD 7.7.0, SA-02) | 2026-11-14 | `SchemaMigrator.java:121` | Result of `ResultSet.next()` ignored when reading `PRAGMA user_version`; an empty result would surface as a raw `SQLException`, not `PersistenceException` | Logic | Minor | Tughral Hussain | QR-03 | Verified (`701c161`; CI run 43829233377 passed) | CAPA-002 |
| DEF-003 | Static analysis (PMD 7.7.0, SA-03) | 2026-11-14 | `MicroLendApplication.java:29-37` | Console stub prints with `System.out`; no structured logging | Standards | Minor | Tughral Hussain | - | Fixed (`9db0657`, 9 to 1 finding; remaining start-up line accepted as SA-03) | CAPA-003 |
| DEF-004 | Static analysis (PMD 7.7.0, SA-05, SA-06) | 2026-11-14 | `LoanAccount.restore`, `LedgerEntry.requireValid`; `LedgerEntry`, `RepaymentCommand`, `TellerWebServer` | Long guard-clause sequences (McCabe 8 and 6), many-method value types, and a web class that routes, parses and renders | Standards | Minor | Tughral Hussain | FR-17, FR-19 | Deferred (M3 refactoring) | CAPA-003 |
| DEF-005 | Static analysis (PMD 7.7.0, SA-07) | 2026-11-14 | `InstalmentDue.java:32`, `TellerWebServer` (5), `SqliteLoanRepository.save` | Literals in conditions: first instalment number, routes and HTTP methods, expected row count | Standards | Minor | Tughral Hussain | BR-19 | Deferred (decide in INS-01) | CAPA-003 |
| DEF-006 | Review of repository history (SCM evidence, SCM-01) | 2026-11-14 | PRs #1, #2, #3, #4, #5, #7 | Pull requests merged without an approving review, five of them by their own author, contrary to README branching rule 2 | Standards | Major | Tughral Hussain | SCM plan, risk R4/R6 | Fixed | CAPA-001 |
| DEF-007 | Review of repository history (SCM-02) | 2026-11-14 | PR #6 | Pull request targets `main` instead of `develop`, and its CI check fails | Standards | Minor | Tughral Hussain | FR-09 | Open (owner: Hassan Khalid) | - |
| DEF-008 | Review of CI configuration (SCM-03) | 2026-11-14 | `.github/workflows/build.yml` | `coverageGate` (branch coverage >= 60%) is defined in `build.gradle` but not run in CI | Standards | Minor | Tughral Hussain | QR (coverage), V&V plan | Deferred (M3) | - |

## CAPA

A CAPA is opened for every Major defect and for any defect type that recurs (3 or more
of one type).

| CAPA ID | Defect(s) | Root cause | Corrective action | Preventive action | Owner | Due | Status | Verification evidence |
|---|---|---|---|---|---|---|---|---|
| CAPA-001 | DEF-006 | The review rule was documented but the ruleset bypass list let admins merge, so it was not enforced | Ruleset `main-protection` re-applied on 2026-11-04 with an empty bypass list, covering `main` and `develop`, 1 approval, last-push approval, merge commits only | Every member reviews at least one PR per milestone; SCM evidence re-collected at every milestone | Tughral Hussain | 2026-11-04 | Corrective action verified | PR #8 shows `REVIEW_REQUIRED` and cannot be merged without a non-author approval (`qa/scm-evidence.md`) |
| CAPA-002 | DEF-001, DEF-002 | PMD was not part of the toolchain, so error-prone patterns Checkstyle cannot see went unnoticed | Use `isBlank()`; check `ResultSet.next()` and throw `PersistenceException` | Run PMD at each milestone (M3: as a CI step); INS-01 checklist covers blank checks and result-set handling | Tughral Hussain | before `m2-baseline` | Closed: verified by PMD re-run and CI run 43829233377 | PMD re-run with 0 InefficientEmptyStringCheck and 0 CheckResultSet; `LedgerEntryTest`, `RepaymentCommandTest`, `SchemaMigratorTest` pass |
| CAPA-003 | DEF-003, DEF-004, DEF-005 | Code written ahead of the composition root and before the refactoring phase | DEF-003 removed when L-17/L-18 replace the stub; DEF-004/005 refactored at M3 with before/after metrics | Complexity reported per method in `qa/test-design/white-box.md` at each milestone | Tughral Hussain | M3 (Week 13) | Open | PMD and JaCoCo complexity before and after; tests unchanged and passing |

## Summary by milestone

| Milestone | Defects found | Major | Minor | Fixed / verified | Deferred | Open CAPA |
|---|---|---|---|---|---|---|
| M2 (to 16 Nov) | 8 | 1 | 7 | 4 (3 verified, 1 fixed) | 3 | 1 (CAPA-003) |
| M3 | | | | | | |
