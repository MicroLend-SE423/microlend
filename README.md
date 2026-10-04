# MicroLend

Community microfinance **loan servicing and delinquency engine**.
Joint semester project for **SE423 — Software Construction & Development** and **SE431 — Software Quality Assurance**, Fall 2026.

MicroLend originates small loans against a product catalogue, generates repayment
schedules, posts repayments through a strict allocation waterfall into an
append-only ledger, and runs a concurrent end-of-day engine that accrues
penalties, ages accounts into delinquency buckets and provisions the portfolio.

## Modules

| Module | Responsibility | Owner |
|---|---|---|
| `origination` | Eligibility assessment and loan approval: age bounds, active-loan limit, instalment-to-income burden, prior write-off check, risk grading | *TBC* |
| `products` | Product catalogue and schedule generation: flat, reducing-balance and bullet products; due-date calendar rule; rounding residue absorbed into the final instalment | *TBC* |
| `ledger` | Repayment posting and the append-only ledger: allocation waterfall (penalty → fees → interest → principal), partial payment, overpayment, compensating entries | *TBC* |
| `delinquency` | End-of-day engine: penalty accrual, days-past-due bucketing, classification and provisioning, auto write-off. **The concurrent module.** | *TBC* |
| `reporting` | Statements, portfolio reports (PAR30, collection efficiency, provision coverage), CSV export | *TBC* |

## Technology

Java · SQLite · JUnit 5 · Checkstyle · GitHub Actions. Command-line interface;
no network access is required to build, test or demonstrate the system.

## Build and run

> Populated once the build is scaffolded at the start of M2 development.

```bash
# build and run the test suite
./gradlew build

# run the static analysis gate
./gradlew checkstyleMain

# start the CLI
./gradlew run
```

## Repository layout

```
src/main/java/      production code, one package per module
src/test/java/      JUnit tests, mirroring the production package structure
config/             checkstyle.xml — the project coding standard (SQAP 4.4)
qa/                 SQA records: risk register, RTM, defect log, inspection records
docs/               design documentation, diagrams, milestone reports
.github/workflows/  CI pipeline
```

## Branching strategy

A **three-tier** model. `main` is the milestone baseline and is never written to directly.

| Branch | Purpose | Rules |
|---|---|---|
| `main` | Released, baselined code. Tagged `m1-baseline`, `m2-baseline`, `m3-baseline` | Protected. No direct pushes. Changes arrive only by reviewed pull request |
| `develop` | Integration branch; the current working state of the system | Changes arrive by pull request from feature branches |
| `feature/<module>-<short-name>` | One unit of work, owned by one member | Branched from `develop`, merged back by PR |

**Rules of the road**

1. **`main` is never touched directly.** Every change to it is a pull request.
2. **Every pull request is reviewed by another team member before merge.** The author does not merge their own work; the reviewer merges it.
3. **Changes accumulate in the pull request.** Push follow-up commits to the same branch rather than opening a new PR for each fix, so the PR records the whole conversation.
4. **Merges use `--no-ff` and squash-merging is disabled**, so the red-green-refactor commit sequence survives in history. This is deliberate: TDD evidence is assessed from repository history and cannot be reconstructed later.
5. **Module ownership is directory ownership.** A member's primary module is their own; cross-module edits still need the owner's review.

### Commit convention

TDD cycles are visible in the log:

```
test(red): instalment rounding residue lands on the final instalment
feat(green): absorb rounding residue in ScheduleGenerator
refactor: extract RoundingPolicy from ScheduleGenerator
```

Other prefixes: `docs:`, `chore:`, `fix:`, `test:` (for tests that are not part of a red-green cycle).

### Baselines

Each milestone submission is tagged on `main`:

| Tag | Milestone | Week |
|---|---|---|
| `m1-baseline` | M1 — proposal, OOD, pattern plan, SQA planning | 6 |
| `m2-baseline` | M2 — working architecture, inspection, test design | 10 |
| `m3-baseline` | M3 — final system, test execution, final SQAP | 13 |

## Coding standards

`config/checkstyle.xml` is the project coding standard, recorded in SQAP section 4.4.

**Constraint C-01 — money is never a binary floating-point type.** `double` and
`float` are rejected by the build. All monetary values use `BigDecimal` inside the
`Money` value object with an explicit `RoundingMode`. `0.1 + 0.2 != 0.3` in IEEE
754, and a fraction of a rupee per instalment accumulates across a portfolio into
a ledger that does not balance.

Compare `BigDecimal` with `compareTo() == 0`, never `equals()`, which also
compares scale (`2.50` is not `equals` to `2.5`).

## SQA records

| File | Contents |
|---|---|
| `qa/risk-register.md` | Living risk register, initiated at M1, updated at every milestone |
| `qa/rtm.md` | Requirements Traceability Matrix: requirement → design → code → test → result |
| `qa/defect-log.md` | Inspection defects and corrective action tracking (from M2) |
| `qa/inspections/` | Fagan inspection records: checklists, logging meeting minutes, follow-up |
| `qa/ai-usage-log.md` | AI Usage Log, submitted with every milestone |

## Team

| Member | Primary modules (SE423) | SE431 deliverable lead (M1) |
|---|---|---|
| *TBC* | `origination`, `delinquency` | SQAP and risk register |
| *TBC* | `products`, `reporting` | Quality requirements and RTM |
| *TBC* | `ledger` | Standards awareness and Cost of Quality |

SE431 deliverable ownership rotates each milestone, so every member leads each
kind of quality artifact at least once.
