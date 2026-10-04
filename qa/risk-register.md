# MicroLend — Risk Register

**Project:** MicroLend — Community Microfinance Loan Servicing & Delinquency Engine
**Courses:** SE423 (Construction) × SE431 (SQA) · **Team:** 3 members
**Owner of this document:** rotates per milestone (M1: *TBC*)
**Status:** living document — updated at **every** milestone and whenever a risk materialises or closes.

> This register is initiated at Milestone 1 and updated at M2 and M3. The M3 rubric
> checks that it *evolved* across milestones, so every change is recorded in the
> change log at the bottom and in this file's Git history. Do not rewrite history;
> add rows and update statuses.

## Scoring

Exposure is computed, not asserted (Laporte & April §11.3.1):

**E = P × C**, where **P** = probability (1 = rare … 5 = almost certain) and **C** = consequence (1 = negligible … 5 = severe).

| Exposure | Priority | Response |
|---|---|---|
| 17–25 | Critical | Act now; mitigation owned and tracked weekly |
| 10–16 | High | Mitigation planned and started this milestone |
| 5–9 | Medium | Monitor; mitigate if exposure rises |
| 1–4 | Low | Accept; review each milestone |

## Register

| ID | Risk (condition → consequence) | Category | P | C | E | Priority | Mitigation | Owner | Status | Residual E |
|---|---|---|---|---|---|---|---|---|---|---|
| R1 | Money/date arithmetic in schedule generation or the allocation waterfall is subtly wrong → every downstream figure (penalties, ageing, provisioning, reports) is wrong, and the defect is found late | Product | 4 | 5 | 20 | Critical | Hand-compute 30 golden schedules in a spreadsheet **before** coding and commit them as the first tests; `BigDecimal` only, enforced by Checkstyle constraint C-01; property test that Σ instalments = principal + total interest | TBC | Open | — |
| R2 | Team starts M1 late (Week 6 deliverable begun in Week 7) → compressed schedule pushes work into the M2 window, and M2 is 35% | Project | 5 | 3 | 15 | High | Freeze scope at 5 modules, no new features; M1 documents drafted from the existing analysis rather than from scratch; repo and tooling set up in parallel with document writing | TBC | Open | — |
| R3 | The external Fagan inspector required at M2 is not arranged in time → the inspection criterion (20% of SE431 M2) cannot be fully evidenced | Process | 3 | 4 | 12 | High | Request the instructor's team pairing immediately after M1 submission; agree a date in Week 8; prepare the inspection package (checklist, 250 LOC of money-path code, defect log template) in advance | TBC | Open | — |
| R4 | Commits are squashed, or tests are written after the code → the red-green-refactor trail cannot be reconstructed, losing TDD marks at M2 (10%) and M3 (15%) | Process | 3 | 4 | 12 | High | Squash-merge disabled in repository settings on day one; commit convention `test(red): / feat(green): / refactor:`; merges use `--no-ff`; PR template asks for the red commit hash | TBC | Open | — |
| R5 | Concurrency defects in the end-of-day engine (lost update, double penalty on retry, torn read in reporting) appear only under load → late, hard-to-reproduce failures near M3 | Product | 3 | 4 | 12 | High | Immutable `Money`/`LedgerEntry`; per-account striped locks with ascending lock ordering; unique `(loan_id, business_date)` as an idempotency key so double accrual is impossible; deterministic latch-driven concurrency tests, no `sleep` | TBC | Open | — |
| R6 | Individual contribution is not attributable (uneven commits, one member integrating everything) → individual evidence score suffers, which is 20% of each member's milestone mark | Project | 3 | 3 | 9 | Medium | Module ownership = directory ownership; every change via PR reviewed by a non-owner; SE431 deliverable ownership rotates each milestone so each member leads each artifact type once; AI Usage Log maintained continuously | TBC | Open | — |
| R7 | Several M2-graded techniques (concurrency, TDD, Fagan inspection, EP/BVA, CFG/cyclomatic complexity, static analysis, SCM baselines, GQM) are not taught until after they must be designed for → the team designs ahead of the lectures and gets it wrong | Project | 4 | 4 | 16 | High | Design the structures now but defer detailed technique work until taught; start the four non-reconstructable evidence streams (TDD trail, SCM baselines, static-analysis baseline, GQM data) at M1; re-check each design against the lecture when it is delivered | TBC | Open | — |
| R8 | AI-assisted work is submitted that a member cannot explain in the viva → reduced or zero individual marks for that part, and a possible academic integrity concern | Project | 3 | 5 | 15 | High | AI Usage Log updated continuously (tool, purpose, prompt summary, what was adopted, modifications, verification); owner reviews and rewrites generated code in their own module; dry-run Q&A inside the team before each demo | TBC | Open | — |

## Change log

| Date | Milestone | Change |
|---|---|---|
| 2026-10-04 | M1 | Register initiated with 8 risks (R1–R8), scored with E = P × C. |

## Review schedule

- **M1 (Week 6):** register created; ≥5 risks with probability, impact, priority, mitigation and owner. *(Rubric: 10 marks.)*
- **M2 (Week 10):** re-score every risk using evidence from architecture, inspection, static analysis, testing and SCM; add risks arising from implementation; record residual exposure.
- **M3 (Week 13):** demonstrate continuous updating M1 → M3; evaluate mitigation effectiveness using actual project data; record residual risk.
