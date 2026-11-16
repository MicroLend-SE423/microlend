# M2 handoff - Hassan Khalid (`products`, `reporting`)

## How to use this file with your AI assistant

Give your assistant this file and tell it to do the following:
1. Read `CLAUDE.md` first, then `docs/planning/team-decisions.md`, then the M1 design sections
   named in each item (`docs/M1-SE423-Proposal-OOD-Pattern-Plan.md`).
2. Take the items below **top to bottom**, one test-first cycle each.
3. Do only your own modules. Never edit `ledger`, `persistence` or `web` classes without asking
   Tughral, and never edit `origination` or `delinquency` classes without asking Ibrahim.

After each AI-assisted piece of work, add a row to `qa/m2-ai-usage-log.md`. The row records:
- date and tool;
- purpose and prompt summary;
- what was adopted and what you changed;
- how you verified it.

You must be able to explain every line in the viva. If you cannot, rewrite it.

**Deadline.** Items marked **M2-critical** must be merged into `develop` by the evening of
**17 Nov 2026**. Submission is 18-19 Nov. They are the minimum that makes the Factory Method
pattern real and lets a repayment be posted end to end. Everything else can follow for M3.

## Where things stand (16 Nov 2026)

- **Ledger, persistence and web are complete** (Tughral, PR #8 into `develop`). The posting
  pipeline is ready to consume your schedule through two ledger interfaces. Today the
  composition root fills both with placeholders that throw `ModuleNotAvailableException`
  naming you, at `CompositionRoot.withPlaceholders`, `src/main/java/pk/edu/giki/microlend/CompositionRoot.java`:
  - `InstalmentsDueSource`: `List<InstalmentDue> owing(LoanId, LocalDate)`;
  - `EarlySettlementRebate`: `Money rebateFor(LoanId, LocalDate)`.
- **Your PR #6** (`feat/hassan-product-schedule-tests`, `DueDateCalculator` and its test)
  targets `main` instead of `develop`, and its CI check fails.
- **No `products` or `reporting` package exists yet.** Package root is
  `pk.edu.giki.microlend.products` and `pk.edu.giki.microlend.reporting`.
- **The schema already has your tables** (`persistence/SchemaMigrator.java`, version 1):
  - `loan_product (code, product_type IN ('FLAT_RATE','REDUCING_BALANCE','BULLET'), annual_rate_bp, min_amount, max_amount, min_tenure_months, max_tenure_months)`;
  - `instalment (loan_id, sequence >= 1, due_date, principal, interest, UNIQUE(loan_id, sequence))`.
  - Money columns are INTEGER paisa: use `persistence.Paisa` to convert.

## Rules for every item (same as `docs/planning/m2-code-todo.md`)

1. **Branch from `develop`** as `feature/products-<short-name>` or `feature/reporting-<short-name>`.
   Open one PR into `develop`. A non-author must review it (Ibrahim or Tughral), and it is
   merged with a **merge commit**. Never squash or rebase-merge; the TDD history is graded.
2. **Commit order:**
   - the failing test first: `test(red): ...`, after running it and seeing it fail;
   - then the implementation: `feat(green): ...`;
   - then any real `refactor: ...`.
3. **`.\gradlew.bat build` must pass before you push the green commit.** Checkstyle fails the
   build on:
   - any `double` or `float` (money is `common.Money`, BigDecimal with HALF_EVEN; compare with
     `compareTo() == 0`);
   - a public type or method without Javadoc;
   - a file without a final newline.
4. **Respect the architecture rules.** `ArchitectureTest` runs in every build:
   - no cycles between module packages (C-03);
   - `common` and `ledger` must not depend on `persistence` or `web`.

   `products` may depend on `common` only (M1 section 3.3). It must **not** import `ledger`,
   `origination` or `delinquency`. A class that needs both `products` and `ledger` types goes
   in the root package `pk.edu.giki.microlend` (see H-08).
5. **Update `qa/rtm.md`** code and test columns when an FR moves from *planned* to
   *implemented*, and to *verified* when its tests pass in PR CI.
6. **Add a row to `qa/m2-ai-usage-log.md`** for each AI-assisted piece.

## Queue

- [ ] **H-01 (M2-critical)** Fix and land PR #6 (`DueDateCalculator.dueDates(LocalDate disbursement, int count)`).
  - **Retarget:** `gh pr edit 6 --base develop`.
  - **Fix the failing check.** It was the Checkstyle final-newline rule; run
    `.\gradlew.bat build` locally and fix whatever it reports.
  - **Move the class** into `pk.edu.giki.microlend.products` if it is not already there.
  - **Test BR-12 boundaries:** disbursement on the 28th, 29th, 30th and 31st, and February in
    leap and non-leap years.
  - BR-12; FR-09, TC-PRD-02.
- [ ] **H-02 (M2-critical)** `InterestCalculator` interface plus `FlatRateCalculator`,
  `ReducingBalanceCalculator` and `BulletCalculator`.
  - **The contract**, stated in the interface Javadoc and checked by one **shared contract
    test** that every implementation runs (M1 section 4.3, LSP):
    - non-negative `Money`, never null;
    - HALF_EVEN at every step (BR-15).
  - **Test values at .005** boundaries.
  - BR-11, BR-15, BR-17; FR-08.
- [ ] **H-03 (M2-critical, the Factory Method pattern)** The Factory Method structure. Follow
  M1 section 5.1 exactly, because the graders check the real GoF form, not a
  `create(type)` switch.
  - **Creator:** abstract `LoanProduct` with the stable workflow
    `generateSchedule(Money principal, int tenureMonths, LocalDate disbursement): Schedule`
    and `protected abstract InterestCalculator createInterestCalculator()`, the factory method.
  - **ConcreteCreators:** `FlatRateProduct`, `ReducingBalanceProduct`, `BulletProduct`.
  - **Supporting types:**
    - `Schedule` is immutable; `instalments()` returns an unmodifiable list.
    - `Instalment` is immutable: `sequence`, `dueDate`, `principal`, `interest`.
    - `ScheduleAssembler` absorbs the rounding residue into the final instalment (BR-14) and
      guarantees exactly `tenureMonths` instalments (BR-16).
  - **Tests:**
    - one schedule per product;
    - the invariant that instalments sum to principal plus total interest;
    - tenure 1, 6 and 36.
  - BR-11, BR-14, BR-16; FR-08, FR-11, FR-12; TC-PRD-01, TC-PRD-04, TC-PRD-05.
- [ ] **H-04 (M2-critical, minimum 3 golden schedules)** `GoldenScheduleTest`, with
  hand-computed schedules as CSV or JSON under `src/test/resources/golden/`.
  - For M2: one per product, computed in a spreadsheet **before** running the code.
  - The 30 golden schedules for QR-02 are an M3 target. They mitigate risk R1.
  - QR-02.
- [ ] **H-05** `GracePeriodPolicy.firstDueDate(LocalDate, int graceMonths)`.
  - Grace of 0 to 3 months defers the first instalment. Interest accrues during grace and is
    added to the first instalment. A grace of 4 is rejected with a typed exception that
    extends `common.LoanDomainException`.
  - Wire it into `generateSchedule`.
  - BR-13; FR-10, TC-PRD-03.
- [ ] **H-06 (M2-critical for end to end)** Schedule persistence.
  - **Declare the roles** in `products`:
    - `ScheduleRepository` (`save(LoanId, Schedule)` and `scheduleFor(LoanId)`); it also
      serves Ibrahim's `ScheduleProvider` role (M1 View 3);
    - `ProductCatalogue` (`find(String code): Optional<LoanProduct>`).
  - **Implement them** as `persistence.SqliteScheduleRepository` and
    `persistence.SqliteProductCatalogue` over the existing `instalment` and `loan_product`
    tables. Copy the style of `persistence/SqliteLoanRepository.java`:
    - one connection per call;
    - `BEGIN IMMEDIATE` for writes;
    - `PersistenceException` for SQL failures.
  - **Ask Tughral to review**, because `persistence` is his package. Test against a temporary
    SQLite file, as `SqliteLoanRepositoryTest` does.
  - FR-08, FR-11.
- [ ] **H-07** BR-22 early-settlement rebate per product (decision D-03).
  - **Add an abstract method** to `LoanProduct`, for example
    `Money earlySettlementRebate(Schedule, LocalDate settlementDate)`, implemented by each
    subclass:
    - reducing-balance refunds unearned interest;
    - flat-rate refunds nothing;
    - bullet refunds unaccrued monthly interest only.
  - **Add one class** `pk.edu.giki.microlend.ProductEarlySettlementRebate implements ledger.EarlySettlementRebate`
    in the root package, next to `CompositionRoot`. It looks up the loan's product and schedule
    through `ProductCatalogue` and `ScheduleRepository` (H-06) and delegates to the product.
    - It belongs in the root package, not in `products`, because implementing a ledger
      interface inside `products` would make `products` depend on `ledger`. M1 section 3.3
      allows only `ledger -> products`.
  - **Tests:** exact payoff and payoff + 0.01, per product.
  - BR-22; FR-15, TC-LED-03.
- [ ] **H-08 (M2-critical for end to end)** `InstalmentsDueSource` implementation (decision
  D-02).
  - **Create** `pk.edu.giki.microlend.ScheduleInstalmentsDueSource implements ledger.InstalmentsDueSource`
    in the root package.
  - **It maps** each `products.Instalment` with `dueDate <= businessDate` to
    `ledger.InstalmentDue(sequence, dueDate, penalty, Money.ZERO /*fees*/, interest, principal)`,
    with amounts **still outstanding**.
  - **Penalty:** pass `Money.ZERO` until Ibrahim's penalty lookup exists (his I-06). Give
    the class a constructor parameter for it, so he can plug it in without editing your logic.
  - **Paid-to-date:** until Tughral's T-01 (`instalment_payment` table) lands, treat nothing
    as paid, and say so in the Javadoc.
  - **Wire it** in `CompositionRoot`: replace the `instalmentsNotAvailable` placeholder, and
    the `rebateNotAvailable` placeholder with H-07. Ask Tughral to review the
    `CompositionRoot` change.
  - **Test** with an in-memory schedule:
    - due-date filtering (due yesterday, today, tomorrow);
    - the mapping of each field.
  - BR-19, BR-20; FR-13, FR-14.
- [ ] **H-09** `reporting` minimum: `StatementRenderer.render(LoanId, LocalDate from, LocalDate to)`.
  - It returns an immutable `Statement` listing every ledger entry in the range with a running
    balance.
  - It reads only through `ledger.LedgerReader` (`entriesFor(LoanId)`), never `LedgerWriter`.
  - Test it with the in-memory ledger fake used in `src/test/java/.../ledger/InMemoryLedgerRepository.java`.
  - BR-35; FR-28, TC-REP-01.
- [ ] **H-10 (M3)** `PortfolioReporter.reportAsAt(LocalDate)` with PAR30, provision coverage and
  collection efficiency; then `ReportExporter` and `CsvReportExporter`.
  - BR-36 to BR-39; FR-29, FR-30, FR-32. Needs Ibrahim's delinquency snapshot data.

## What the M2 documents need from you

Muhammad Ibrahim is document lead (D-05). In `docs/M2-SE423-Architecture-Implementation.md`,
fill the subsections marked "owner to complete: Hassan Khalid":
- the products architecture as built;
- the Factory Method rationale, with the real class names and the rejected alternatives
  (Simple Factory, Abstract Factory, Singleton);
- your test and coverage numbers.

When your code lands, Tughral or Ibrahim re-runs the static analysis
(`.\gradlew.bat --init-script config/pmd-init.gradle pmdMain`) and adds your findings to
`qa/static-analysis-report.md`. The black-box cases for your module already exist in
`qa/test-design/black-box.md` (section 5); implement the TC-PRD cases listed there.
