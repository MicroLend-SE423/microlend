# MicroLend — Risk Register

**Project:** MicroLend — Community Microfinance Loan Servicing & Delinquency Engine
**Courses:** SE423 (Construction) × SE431 (SQA) · **Team:** 3 members
**Owner of this document:** rotates per milestone (M1: Muhammad Ibrahim)
**Status:** living document — updated at **every** milestone and whenever a risk materialises or closes.

> This register is initiated at Milestone 1 and updated at M2 and M3. The M3 rubric
> checks that it *evolved* across milestones, so every change is recorded in the
> change log at the bottom and in this file's Git history. Do not rewrite history;
> add rows and update statuses.

## Scoring

Exposure is computed, not asserted (Laporte & April §11.3.1):

**E = P × C**, where **P** = probability (1 = rare … 5 = almost certain) and **C** = consequence, i.e. impact (1 = negligible … 5 = severe).

| Exposure | Priority | Response |
|---|---|---|
| 17–25 | Critical | Act now; mitigation owned and tracked weekly |
| 10–16 | High | Mitigation planned and started this milestone |
| 5–9 | Medium | Monitor; mitigate if exposure rises |
| 1–4 | Low | Accept; review each milestone |

## Register

| ID | Risk (condition → consequence) | Category | P | C | E | Priority | Mitigation | Owner | Status | Residual E |
|---|---|---|---|---|---|---|---|---|---|---|
| R1 | Money/date arithmetic in schedule generation or the allocation waterfall is subtly wrong → every downstream figure (penalties, ageing, provisioning, reports) is wrong, and the defect is found late | Product | 4 | 5 | 20 | Critical | Hand-compute 30 golden schedules in a spreadsheet **before** coding and commit them as the first tests; `BigDecimal` only, enforced by Checkstyle constraint C-01; property test that Σ instalments = principal + total interest | Hassan Khalid | Open | — |
| R2 | M1 work starts late → compressed schedule pushes work into the M2 window, and M2 (Week 10) carries 35% | Project | 5 | 3 | 15 | High | Freeze scope at 5 modules, no new features; M1 documents drafted from the existing analysis rather than from scratch; repo and tooling set up in parallel with document writing | Tughral Hussain | Open | — |
| R3 | The external Fagan inspector required at M2 is not arranged in time → the inspection criterion (20% of SE431 M2) cannot be fully evidenced | Process | 3 | 4 | 12 | High | Request the instructor's team pairing immediately after M1 submission; agree a date in Week 8; prepare the inspection package (checklist, 250 LOC of money-path code, defect log template) in advance | Muhammad Ibrahim | Open | — |
| R4 | Commits are squashed, or tests are written after the code → the red-green-refactor trail cannot be reconstructed, losing TDD marks at M2 (10%) and M3 (15%) | Process | 3 | 4 | 12 | High | Squash-merge disabled in repository settings on day one; commit convention `test(red): / feat(green): / refactor:`; merges use `--no-ff`; PR template asks for the red commit hash | Tughral Hussain | Open | — |
| R5 | Concurrency defects in the end-of-day engine (lost update, double penalty on retry, torn read in reporting) appear only under load → late, hard-to-reproduce failures near the M3 concurrency-correctness check | Product | 3 | 4 | 12 | High | Immutable `Money`/`LedgerEntry`; per-account locks, one held at a time; unique `(loan_id, business_date)` as an idempotency key, inserted in the same transaction as the accrual, so a double accrual rolls back; deterministic latch-driven concurrency tests, no `sleep` | Muhammad Ibrahim | Open | — |
| R6 | Individual contribution is not attributable (uneven commits, one member integrating everything) → individual evidence score suffers, which is 20% of each member's milestone mark | Project | 3 | 3 | 9 | Medium | Module ownership = directory ownership; every change via PR reviewed by a non-owner; SE431 deliverable ownership rotates each milestone so each member leads each artifact type once; AI Usage Log maintained continuously | Tughral Hussain | Open | — |
| R7 | Several M2- and M3-graded techniques (concurrency, TDD, Fagan inspection, EP/BVA, CFG/cyclomatic complexity, static analysis, SCM baselines, GQM) are not taught until after they must be designed for → the team designs ahead of the lectures and gets it wrong | Project | 4 | 4 | 16 | High | Design the structures now but defer detailed technique work until taught; start the evidence streams that cannot be reconstructed later (TDD trail, SCM baselines, static-analysis reports) at M1, and GQM data with the first feature branch at M2; re-check each design against the lecture when it is delivered | Muhammad Ibrahim | Open | — |
| R8 | AI-assisted work is submitted that a member cannot explain in the viva → reduced or zero individual marks for that part, and a possible academic integrity concern | Project | 3 | 5 | 15 | High | AI Usage Log updated continuously (tool, purpose, prompt summary, what was adopted, modifications, verification); owner reviews and rewrites generated code in their own module; dry-run Q&A inside the team before each demo | Hassan Khalid | Open | — |

## M2 re-assessment (14 Nov 2026, Week 10)

The M1 rows above are kept unchanged as the M1 baseline. Each risk is re-scored here from
M2 evidence. Evidence documents: `qa/scm-evidence.md`, `qa/static-analysis-report.md`,
`qa/test-design/white-box.md`, `qa/defect-log.md`, PR #8.

| ID | M1 E | M2 P | M2 C | M2 E | Priority | Evidence at M2 | Mitigation now | Owner | Status | Residual E |
|---|---|---|---|---|---|---|---|---|---|---|
| R1 | 20 | 3 | 5 | 15 | High | Ledger money path built with `Money`/`BigDecimal` only; Checkstyle C-01 gate has 0 findings; `RepaymentAllocator` has 18/18 branches covered with BVA cases at 0.00/0.01/due-0.01/due. Golden schedules (products) are not built yet | Keep the C-01 gate; Hassan's 30 golden schedules are still the main control for schedule arithmetic | Hassan Khalid | Open, partly mitigated | 10 |
| R2 | 15 | 5 | 4 | 20 | Critical | **Materialised.** Module code started on 6 Nov; on 14 Nov only `common`, `ledger` and `persistence` exist | Scope frozen to what M2 grades; daily integration into `develop`; ledger queue run as an expedited sprint | Tughral Hussain | Materialised | 12 |
| R3 | 12 | 3 | 4 | 12 | High | No inspector pairing recorded in the repository yet. The inspection package INS-01 (plan, 34-item checklist, forms) is ready | Confirm the paired team and a date; run INS-01 before submission | Muhammad Ibrahim | Open | 6 |
| R4 | 12 | 1 | 4 | 4 | Low | Squash and rebase merges are off, and the ruleset allows merge commits only; PR #8 has 8 `test(red)` / 8 `feat(green)` pairs. Six earlier PRs were merged without review (DEF-006) | Ruleset re-applied 4 Nov with no bypass (CAPA-001) | Tughral Hussain | Mitigated | 4 |
| R5 | 12 | 4 | 4 | 16 | High | The end-of-day engine, `LockRegistry` and concurrency tests are not built yet. `SqliteDatabase` migrates the schema exactly once under concurrent connects | Build the engine with latch-driven tests before the M2 demo; the posting path already uses one transaction per posting | Muhammad Ibrahim | Open, rising | 8 |
| R6 | 9 | 5 | 3 | 15 | High | **Materialising.** Commits on all branches: tughral1 39, Ibrahim 16, Hassan 1. Hassan's only M2 code is in PR #6 (targets `main`, CI failing) | Each member lands their own module through their own PR before the 17th; every member reviews another's PR | Muhammad Ibrahim | Materialising | 9 |
| R7 | 16 | 3 | 4 | 12 | High | The evidence streams are running: TDD trail, CI history (50 runs), static analysis, SCM evidence | Re-check the white-box and inspection documents against the lecture slides before submission | Muhammad Ibrahim | Open, reducing | 8 |
| R8 | 15 | 4 | 5 | 20 | Critical | Most ledger code and the M2 QA drafts were produced with Claude Code (`qa/m2-ai-usage-log.md`), so the risk of a member being unable to explain submitted work has gone **up** | The owner walks through every class and test before review; each AI-assisted commit has a usage-log row; team dry-run Q&A before the demo | Tughral Hussain (ledger), each member for own work | Open, rising | 10 |
| **R9** | new | 5 | 5 | **25** | Critical | `origination`, `products`, `delinquency` and `reporting` have no code on any branch except PR #6. Factory Method (Hassan) and Adapter (Ibrahim) are not implemented. Without them, SE423 M2 "working architecture" (20), "patterns" (20) and "concurrency" (15) cannot be fully evidenced | Minimum vertical slice per module by the 17th: one product with Factory Method, one bank adapter, end-of-day over the ledger. Integrate daily on `develop` | Muhammad Ibrahim (Team Lead) | Open | 15 |
| **R10** | new | 2 | 3 | 6 | Medium | BR-18/BR-19 were ambiguous. The tier-by-tier reading was adopted on 14 Nov pending team confirmation (`docs/business-rules.md`) | Confirm at the next team meeting. If the other reading is chosen, `RepaymentAllocator` and its tests change, but nothing else does | Tughral Hussain | Closed 2026-11-16 (decision D-01, `docs/planning/team-decisions.md`) | 2 |
| **R11** | new | 4 | 2 | 8 | Medium | The unattended daily coding runner produced no work on 4 of its first 8 scheduled days (7-13 Nov): model not available, laptop asleep, network drop, expired sign-in | Runner exits non-zero on failure; catch-up mode; long-lived sign-in token; failures visible in Task Scheduler | Tughral Hussain | Open | 4 |

## M2 re-assessment update (21 Nov 2026, before submission on 22 Nov)

The 14 Nov rows above are kept unchanged. This table re-scores only the risks that the
code built from 17 to 21 Nov, the PR #8 review, the independent code review of the M2 code
and the inspection status affect. Evidence: `feature/delinquency-origination-import` at
code head `4067d2f` (384 tests passing locally), `qa/defect-log.md` (DEF-009 to DEF-023),
`qa/rtm.md` v2.1, and the commit history (counts from `git shortlog -sn`).

| ID | 14 Nov E | P | C | E | Priority | Evidence on 21 Nov | Mitigation now | Owner | Status | Residual E |
|---|---|---|---|---|---|---|---|---|---|---|
| R3 | 12 | 5 | 4 | 20 | Critical | **Materialised.** On 21 Nov no inspector from another team has been assigned, and INS-01 has not been held. The package (plan, 34-item checklist, forms) is complete. The plan's scope is now sized from the merged code: meeting A is 228 NCLOC | Submit the package with INS-01 honestly marked as pending. Hold INS-01 as soon as the instructor assigns an inspector, and add its records and defects at M3. The defects found by code review of PR #8 are logged as review defects, not as inspection results | Muhammad Ibrahim | Materialised | 20 for M2; 8 once INS-01 is held |
| R4 | 4 | 2 | 4 | 8 | Medium | The PR #8 review found one trail defect: green commit `34a7bb7` changed a test assertion (DEF-015). Ibrahim's branch has 9 `test(red)` commits, each followed by the commit that makes it pass (8 `feat(green)` and 1 `fix:`, `6d30af9`), and none of those changes `src/test` | CAPA-008: the reviewer checks that no green commit changes `src/test`; history is not rewritten | Tughral Hussain | Open | 4 |
| R5 | 16 | 2 | 4 | 8 | Medium | `EndOfDayEngine` is built: four threads, one task per loan under that loan's `LockRegistry` lock, and each loan's snapshot and penalty pair in one `BEGIN IMMEDIATE` transaction keyed `UNIQUE (loan_id, business_date)`. `LockingPoster` makes tellers take the same lock. `ConcurrentTellerAndEndOfDayTest` passes through the production wiring, and it failed in 3 of 3 runs with `LockingPoster` removed. The PR #8 review found a lost update on the posting path, now fixed (DEF-009). The independent review found that a skipped end-of-day date under-accrued, now fixed (DEF-018). Still open: an `import` run as a separate process is outside the in-process lock, so a balance update can be lost (DEF-022); `appendAll` uses a deferred transaction (DEF-011); the QR-06 test runs once per build, not 50 times; QR-01 performance is not measured | Run imports with the server stopped until DEF-022 is fixed. Read, allocate and save in one transaction (CAPA-004, fixes DEF-011 and DEF-022); add the 50-run QR-06 job and `EndOfDayPerformanceTest` at M3 | Muhammad Ibrahim | Open, reduced | 4 |
| R6 | 15 | 5 | 3 | 15 | High | **Materialised for one member.** On `develop` (PR #8 merged): tughral1 71 commits (69 non-merge), Muhammad Ibrahim 16 (12 as `mIBRAHIM707`, plus 4 PR merges made on GitHub under the name `Muhammad Ibrahim`), Hassan Khalid 0. On `feature/delinquency-origination-import` since `develop`: `mIBRAHIM707` 27 (24 non-merge). Across all branches, Hassan Khalid has 1 commit (`db4353a`, 10 Oct, open PR #6) | Each member's evidence is their own module and PRs. Hassan's `products` and `reporting` work is the open gap (see R9). The two author names of one member should be mapped (for example with a `.mailmap`) so commit counts are attributed correctly | Muhammad Ibrahim | Materialised | 15 |
| R7 | 12 | 2 | 4 | 8 | Medium | The M2 techniques have now been applied to the new code. White-box designs for `DelinquencyClassifier.classify` and `EndOfDayEngine.collect` compute V(G) two ways and are cross-checked against JaCoCo; the only differences are the exception exits, which are explained. Black-box cases for `origination` and `delinquency` are mapped to automated tests. PMD was re-run, with every new finding triaged | Keep the cross-checks (hand CFG against JaCoCo; PMD triage) for M3 | Muhammad Ibrahim | Open, reducing | 4 |
| R9 | 25 | 5 | 4 | 20 | Critical | **Partly materialised.** Ibrahim's modules are built and tested: `origination` (BR-01 to BR-10), `delinquency` with the concurrent end-of-day engine, and the bank-statement Adapter (`ledger.importing`). `products` and `reporting` have no code on any branch, so Factory Method is not implemented. Because of that, loan creation (I-03) is not built, and the running application's end-of-day and posting wait on a `products` placeholder (`ModuleNotAvailableException`) | Submit with `products`/`reporting` rows honestly marked *planned*. Factory Method and `products` at M3 (owner: Hassan Khalid) | Muhammad Ibrahim (Team Lead) | Partly materialised | 20 for M2 |
| **R12** | new | 3 | 4 | 12 | High | The two reviews left money-path gaps open: no ledger line is written for the BR-22 rebate (DEF-013); `appendAll` uses a deferred transaction (DEF-011); an import run as a separate process can lose a balance update (DEF-022); an old double reversal blocks the schema upgrade (DEF-023). The restart double-posting gap (DEF-012) was closed on 21 Nov by `SqlitePaymentReferences`. A crash between claim and posting can still leave a reference claimed, which makes a later import refuse the row rather than post it twice. The rebate source is a placeholder in the M2 build, and imports can be run with the server stopped. All open items must be closed before the M3 demonstration | CAPA-004, CAPA-005, CAPA-006 | Tughral Hussain | Open | 4 |

## Change log

| Date | Milestone | Change |
|---|---|---|
| 2026-10-04 | M1 | Register initiated with 8 risks (R1–R8), scored with E = P × C. |
| 2026-10-07 | M1 | Pre-submission review against the updated joint guidelines (three milestones, issued 7 October 2026): R5 wording matched to the stated concurrency design; criterion weights in R2–R4 checked; review schedule weeks set to 10 and 13. No scores changed. |
| 2026-11-14 | M2 | M2 re-assessment added (draft, document lead to be assigned). R2 materialised; R6 materialising; R5 and R8 rising; R1, R4 and R7 reduced on evidence. New risks: R9 (teammates' modules and two patterns missing before M2, Critical), R10 (BR-18/BR-19 reading pending confirmation), R11 (unattended runner reliability). M1 rows left unchanged. |
| 2026-11-16 | M2 | R10 closed: the tier-by-tier BR-18/BR-19 reading was adopted as team decision D-01. M2 document lead assigned: Muhammad Ibrahim (Team Lead). |
| 2026-11-21 | M2 | M2 re-assessment update added; earlier rows left unchanged. R3 materialised (no external inspector assigned; INS-01 not held), 12 to 20. R5 reduced, 16 to 8: engine, shared account lock and QR-06 test built, and the lost update found in review fixed. R7 reduced, 12 to 8. R9 reduced, 25 to 20: Ibrahim's modules and the Adapter built, `products`/`reporting` and Factory Method still missing. R6 unchanged at 15 (materialised for one member). R4 raised, 4 to 8, by trail defect DEF-015. New risk R12 (open money-path gaps from the PR #8 review and the independent code review, 12; the restart double-posting gap closed the same day). Submission date moved to 22 Nov 2026 by the instructor. |

## Review schedule

- **M1 (Week 6):** register created; ≥5 risks with probability, impact, priority, mitigation and owner. *(Rubric: 10 marks.)*
- **M2 (Week 10):** re-score every risk using evidence from architecture, inspection, static analysis, testing and SCM; add risks arising from implementation; record residual exposure.
- **M3 (Week 13):** demonstrate continuous updating M1 → M3; evaluate mitigation effectiveness using actual project evidence; record residual risk for every risk.
