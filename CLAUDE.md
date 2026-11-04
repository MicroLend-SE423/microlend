# CLAUDE.md — MicroLend

Context for Claude Code sessions opened in this repository. It replaces the long
workspace chat in which M1 was produced (Sept–Oct 2026). Read this first, then
`README.md`, then the files it points to.

## What this is

MicroLend is the joint semester project for **SE423 Software Construction & Development**
and **SE431 Software Quality Assurance** (GIKI, Fall 2026). Both courses grade the
**same running system**: SE423 grades how it is designed and built, SE431 grades how its
quality is verified, measured and documented. SQA work only counts if it is applied to
the code actually built in the same milestone, so keep the two sides in step.

A community microfinance **loan servicing and delinquency engine**: originate small
loans, generate repayment schedules, post repayments through an allocation waterfall
into an append-only ledger, and run a concurrent end-of-day engine (penalties,
delinquency buckets, provisioning, write-off), plus statements and portfolio reports.

- Instructor requirements and every milestone rubric: `docs/course/joint-project-requirements.md`
  (condensed from the official guidelines PDF, `[Updated] SE423_SE431_Joint_Project_Guidelines.pdf`,
  which each member has from Teams).
- Why MicroLend was chosen, and the rejected alternatives: `docs/planning/Project-Idea-Analysis.md`.

## Hard constraints (from the team — do not break)

- **Nothing that costs money or needs extra resources.** No hosting, no cloud services,
  no paid tools, no accounts beyond GitHub. A local web interface served from a local
  process against a file-based SQLite database; the app is **never deployed**. GitHub
  Actions is fine because it is free on this public repo.
- **C-01: money is never `double` or `float`.** `BigDecimal` inside the `Money` value
  object with an explicit `RoundingMode`; compare with `compareTo() == 0`. Checkstyle
  (`config/checkstyle.xml`, IllegalType + IllegalToken on NUM_DOUBLE/NUM_FLOAT) fails the build.
- Patterns must be ones **formally taught** in SE423 and must be the real GoF form
  (e.g. Factory Method, not a Simple Factory `create(type)` switch — Lecture 11 says the
  latter is not the GoF pattern).
- The repo is **public**: no personal emails, tokens or private course files in commits.

## Team and ownership

| Member | Reg. # | GitHub | SE423 modules | Pattern owned |
|---|---|---|---|---|
| Muhammad Ibrahim | 2023446 | mIBRAHIM707 | `origination`, `delinquency` (the concurrent module) | Adapter |
| Hassan Khalid | 2023242 | Hassan242-kk | `products`, `reporting` | Factory Method |
| Tughral Hussain | 2023532 | tughral1 | `ledger` | Decorator |

- M1 Team Lead / Integrator: Muhammad Ibrahim (role may rotate each milestone).
- SE431 deliverable leads rotate every milestone so each member leads each kind of
  artifact once. M1 leads: Ibrahim (SQAP outline, risk register), Hassan (25010 quality
  requirements, RTM v1), Tughral (standards awareness, Cost of Quality, repo setup).
  **M2 leads have not been assigned yet** — ask the team rather than assuming.
- The person running Claude here is usually **Tughral**. Individual marks depend on
  attributable commits and on each member being able to defend their own work, so do
  not write large parts of another member's module unless asked, and keep commits
  attributable to the person doing the work.

## Design decisions already made (M1 — see `docs/M1-SE423-Proposal-OOD-Pattern-Plan.md`)

- Java 21, Gradle wrapper, SQLite (`org.xerial:sqlite-jdbc`), JUnit 5, JaCoCo, Checkstyle,
  GitHub Actions. Package root `pk.edu.giki.microlend`, one package per module.
- Patterns: **Factory Method** (`LoanProduct` subclasses create their interest/schedule
  calculator; creational), **Adapter** (`CsvStatementAdapter` per bank statement format;
  structural), **Decorator** (`RepaymentPoster` wrapped with audit / timing / idempotency;
  structural). Facade is an optional fourth; Strategy was deferred.
- Concurrency (doc §3.6): end-of-day engine on `Executors.newFixedThreadPool(4)`, one task
  per loan, while teller threads post repayments. Races to prevent: lost update, double
  penalty accrual for the same business date, torn reads in reports. Strategy: immutable
  core types (`Money`, `LedgerEntry`, `Instalment`), per-account `ReentrantLock` from a
  `LockRegistry` (`ConcurrentHashMap<LoanId, ReentrantLock>`), each read→allocate→write in
  one SQLite transaction (`BEGIN IMMEDIATE`, busy timeout, one connection per worker),
  idempotency on (loan, business date).
- Persistence (doc §3.7): SQLite, seven tables, schema re-states invariants
  (`CHECK`, `UNIQUE`, FKs with `PRAGMA foreign_keys = ON` on every connection).
- Business rules: `docs/business-rules.md` (43 numbered rules BR-xx with boundary values —
  the source for EP/BVA test design at M2).
- SQA side: `docs/M1-SE431-Quality-Requirements-RTM-SQAP.md`, `qa/rtm.md`,
  `qa/risk-register.md` (E = P × C, append-only change log — never rewrite earlier rows,
  M3 checks that it evolved), `qa/m1-ai-usage-log.md`.

## Repository rules (enforced on GitHub)

- Org repo: **github.com/MicroLend-SE423/microlend** (moved from tughral1/microlend; old
  URL redirects). Admins: tughral1, mIBRAHIM707. Hassan has write.
- Branches: `main` (milestone baselines, tagged `m1-baseline`, `m2-baseline`, `m3-baseline`;
  never pushed to directly) ← `develop` (integration) ← `feature/<module>-<short-name>`.
- Every PR needs one approval from a non-author. **Squash and rebase merges are disabled**
  so the TDD red-green-refactor trail survives — never squash. Auto-delete of head
  branches is off.
- Note: after the org transfer the ruleset's bypass list became empty, so nobody
  (including admins) can bypass the PR rule. Leave it unless the team asks.
- Commit convention (TDD evidence is graded from history):
  `test(red): …` → `feat(green): …` → `refactor: …`; also `docs:`, `chore:`, `fix:`, `test:`.
  Commit the failing test **before** the code that makes it pass.
- CI (`.github/workflows/build.yml`): compile, tests, Checkstyle, JaCoCo on every push to
  main/develop/feature/** and on PRs.

## Commands

Environment: Windows 11. **Git Bash is not installed — use PowerShell.** Node is at
`D:/Tools/nodejs/node.exe` (Node 22). JDK 21 must be installed locally for Gradle.

```powershell
.\gradlew.bat build                       # compile + tests + checkstyle + coverage
.\gradlew.bat test
.\gradlew.bat checkstyleMain checkstyleTest
.\gradlew.bat jacocoTestReport            # build/reports/jacoco/test/html/index.html
.\gradlew.bat coverageGate                # branch coverage >= 60%
.\gradlew.bat run
cd docs; npm install; npm run build       # rebuild the submission PDFs (Paged.js + mermaid-cli)
```

`gh` may have more than one account logged in; if a push or API call fails with a
permission error, run `gh auth switch --user tughral1`. If `./gradlew` fails in CI with
"Permission denied", the executable bit was lost: `git update-index --chmod=+x gradlew`.

## Where things stand (as of 4 Nov 2026)

**M1 (Week 6, 15%) — submitted.** Both PDFs in `docs/`, tag `m1-baseline` on main. Still
owed by the members themselves: signing the AI-log declaration (Claude must not sign or
tick it on their behalf).

**Code so far:** only `common/` — `Money`, `LoanDomainException`, `InvalidMoneyException`,
`MoneyTest` (11 tests), and a stub `MicroLendApplication`. None of the five modules is
implemented yet.

**Open PR #6 (Hassan, `feat/hassan-product-schedule-tests`): `DueDateCalculator` + test.**
It targets `main` instead of `develop`, its CI check is failing, and it has no review.
It needs retargeting to `develop`, a fix for the failing check, and a review — it is
Hassan's module, so help him rather than rewriting it.

**M2 (Week 10, 35%) is due now or within days — check the exact date on Teams.**
The full rubric is in `docs/course/joint-project-requirements.md`. In short:

SE423 M2: core architecture builds and runs end-to-end; ≥ 3 patterns implemented and
integrated in running code; input validation and structured exceptions at module
boundaries; the concurrent end-of-day operation implemented with its thread-safety
strategy; SOLID visible in code; tests with a genuine red-green-refactor history and a
measured coverage report (60% not required until M3); attributable commits from all three.

SE431 M2: Fagan inspection of real code/design with **an inspector from another team**
(instructor pairs teams), roles, defect log with severity and CAPA tracking; static
analysis report from a real tool (Checkstyle is already wired in) with findings triaged
fixed / deferred / false positive; V&V plan per IEEE 1012 tied to real requirements;
black-box test design (EP + BVA) traced to RTM IDs; white-box design (CFG, cyclomatic
complexity, basis paths) for key methods; SCM quality-gate evidence from the actual repo;
RTM updated to the M2 build, risk register updated, CAPA current.

Also for M2: a new **`qa/m2-ai-usage-log.md`** (same columns as the M1 log: date, tool,
purpose, prompt summary, material adopted, modifications, verification), plus
`qa/defect-log.md` and `qa/inspections/` as promised in the README; tag `m2-baseline` on
main after the M2 PR merges.

## How to work here

- Prefer small, owner-attributed PRs into `develop`; follow the TDD commit order.
- Log every significant AI-assisted piece of work in the current milestone's AI usage
  log as you go, with what was changed and how it was verified.
- Documents must be specific to this project, no filler; the team must be able to
  explain everything submitted.
- Ask before pushing, opening or merging PRs, or changing repository settings.
