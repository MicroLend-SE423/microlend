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

## Change log

| Date | Milestone | Change |
|---|---|---|
| 2026-10-04 | M1 | Register initiated with 8 risks (R1–R8), scored with E = P × C. |
| 2026-10-07 | M1 | Pre-submission review against the updated joint guidelines (three milestones, issued 7 October 2026): R5 wording matched to the stated concurrency design; criterion weights in R2–R4 checked; review schedule weeks set to 10 and 13. No scores changed. |
| 2026-11-14 | M2 | M2 re-assessment added (draft, document lead to be assigned). R2 materialised; R6 materialising; R5 and R8 rising; R1, R4 and R7 reduced on evidence. New risks: R9 (teammates' modules and two patterns missing before M2, Critical), R10 (BR-18/BR-19 reading pending confirmation), R11 (unattended runner reliability). M1 rows left unchanged. |
| 2026-11-16 | M2 | R10 closed: the tier-by-tier BR-18/BR-19 reading was adopted as team decision D-01. M2 document lead assigned: Muhammad Ibrahim (Team Lead). |

## Review schedule

- **M1 (Week 6):** register created; ≥5 risks with probability, impact, priority, mitigation and owner. *(Rubric: 10 marks.)*
- **M2 (Week 10):** re-score every risk using evidence from architecture, inspection, static analysis, testing and SCM; add risks arising from implementation; record residual exposure.
- **M3 (Week 13):** demonstrate continuous updating M1 → M3; evaluate mitigation effectiveness using actual project evidence; record residual risk for every risk.
