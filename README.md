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
| `origination` | Eligibility assessment and loan approval: age bounds, active-loan limit, instalment-to-income burden, prior write-off check, risk grading | Muhammad Ibrahim |
| `products` | Product catalogue and schedule generation: flat, reducing-balance and bullet products; due-date calendar rule; rounding residue absorbed into the final instalment | Hassan Khalid |
| `ledger` | Repayment posting and the append-only ledger: allocation waterfall (penalty → fees → interest → principal), partial payment, overpayment, compensating entries | Tughral Hussain |
| `delinquency` | End-of-day engine: penalty accrual, days-past-due bucketing, classification and provisioning, auto write-off. **The concurrent module.** | Muhammad Ibrahim |
| `reporting` | Statements, portfolio reports (PAR30, collection efficiency, provision coverage), CSV export | Hassan Khalid |

## Technology

Java 21 · Gradle · SQLite · JUnit 5 · JaCoCo · Checkstyle · GitHub Actions.

The application runs **locally only**. It is never deployed: no hosting, no cloud
service and no paid resource of any kind. A small web interface is served from a
local process against a file-based database, so the whole system runs on one
laptop. The only network access is Gradle fetching dependencies the first time
you build.

## Build and run

You need a JDK 21 (or newer) on your machine. Gradle itself is bundled, so there is
nothing else to install — the wrapper downloads what it needs on first use.

```bash
# build everything and run the test suite
./gradlew build            # Windows: gradlew.bat build

# tests only
./gradlew test

# static analysis — enforces constraint C-01 (no double/float for money)
./gradlew checkstyleMain checkstyleTest

# coverage report
./gradlew jacocoTestReport

# run the application
./gradlew run
```

Reports are written under `build/reports/`: JUnit results in `tests/test/`,
coverage in `jacoco/test/html/index.html`, and Checkstyle findings in
`checkstyle/`.

Every push runs the same four steps in GitHub Actions, so a green build locally
and a green build in CI mean the same thing.

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

| Member | Reg. # | Primary modules (SE423) | Pattern owned | SE431 deliverable lead (M1) |
|---|---|---|---|---|
| Muhammad Ibrahim | 2023446 | `origination`, `delinquency` | Adapter | SQAP scope & outline, risk register |
| Hassan Khalid | 2023242 | `products`, `reporting` | Factory Method | Quality requirements (25010), RTM v1 |
| Tughral Hussain | 2023532 | `ledger` | Decorator | Standards awareness, Cost of Quality |

**Team Lead / Integrator (M1):** Tughral Hussain — final integration and the version-control repository. This role rotates at each milestone.

SE431 deliverable ownership rotates each milestone, so every member leads each
kind of quality artifact at least once.
