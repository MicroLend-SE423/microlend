# M2 handoff - Muhammad Ibrahim (`origination`, `delinquency`, `ledger.importing`; M2 Team Lead)

## How to use this file

Work through it in this order:
1. Read `README.md` first, then `docs/planning/team-decisions.md`, then the M1 design sections
   named in each item (`docs/M1-SE423-Proposal-OOD-Pattern-Plan.md`, especially section 3.6
   on concurrency and section 5.2 on the Adapter).
2. Do the **Team Lead checklist** first, then take the queue **top to bottom**, one test-first
   cycle each.
3. Do only your own modules. Ask Tughral before editing `ledger` (other than
   `ledger.importing`), `persistence`, `web` or `CompositionRoot`, and ask Hassan before
   editing `products`.

After each AI-assisted piece of work, add a row to `qa/m2-ai-usage-log.md`. The row records:
- date and tool;
- purpose and prompt summary;
- what was adopted and what you changed;
- how you verified it.

You must be able to explain every line in the viva.

**Deadline.** Items marked **M2-critical** must be merged into `develop` by the evening of
**17 Nov 2026**. Submission is 18-19 Nov. They make the Adapter pattern and the concurrent
end-of-day operation real, which SE423 M2 grades directly:
- "patterns", 20 marks;
- "concurrency and thread safety", 15 marks;
- "working core architecture", 20 marks.

## Team Lead checklist (decision D-05: you are M2 Team Lead and document lead)

1. **Review and merge PR #8** (`feature/ledger-entry` -> `develop`, author Tughral).
   - Approve on GitHub. The ruleset needs one non-author approval with every thread resolved.
   - Merge with **"Create a merge commit"**. Squash and rebase are disabled on purpose.
   - Do not push to the branch after approving; a new push dismisses the approval.
2. **Review and merge your teammates' later PRs into `develop`** the same way. Hassan's are
   listed in `docs/planning/m2-handoff-hassan.md`.
3. **When `develop` holds the M2 build:**
   - open a PR `develop` -> `main` titled "M2 baseline";
   - have Tughral or Hassan approve it, then merge it with a merge commit;
   - tag the merge commit on `main`, annotated:

   ```powershell
   git fetch origin
   git tag -a m2-baseline origin/main -m "Milestone 2 baseline (Week 10): working architecture, patterns, concurrency, verification baseline (SE423); inspection, static analysis, V&V, test design, SCM baseline (SE431)"
   git push origin m2-baseline
   ```

4. **Rebuild the PDFs** from the merged `develop` (or `main`) and commit them through a PR:

   ```powershell
   cd docs; npm install; npm run build
   ```

   The outputs are `docs/M2-SE423-MicroLend.pdf` and `docs/M2-SE431-MicroLend.pdf`.
   It needs pandoc on PATH; Node 22 is fine.
5. **Submit on Teams:** one submission per team per course. Upload the PDF plus the repository
   link `github.com/MicroLend-SE423/microlend`. Be ready to demo with `.\gradlew.bat run`, which
   serves the teller page on `http://127.0.0.1:8080`, local only.
6. **Before submission,** make sure each member has signed their part of the AI usage log
   declaration. Nobody, including an AI tool, signs on another member's behalf. Every member
   also submits the confidential peer evaluation, including a self-evaluation.

### Document-lead duties

- **Read every M2 document** and check that it matches the code on `develop`:
  - `docs/M2-SE423-Architecture-Implementation.md`;
  - `qa/vv-plan.md`;
  - `qa/test-design/black-box.md` and `qa/test-design/white-box.md`;
  - `qa/static-analysis-report.md`;
  - `qa/scm-evidence.md`;
  - `qa/defect-log.md`;
  - `qa/inspections/`;
  - `qa/risk-register.md`;
  - `qa/rtm.md`.
- **Fill every subsection marked "owner to complete: Muhammad Ibrahim"** in
  `docs/M2-SE423-Architecture-Implementation.md`:
  - origination;
  - the Adapter rationale, with the real class names and Facade as the rejected alternative;
  - the concurrency strategy as built;
  - your test and coverage numbers.
- **Fagan inspection INS-01.** The instructor assigns the external inspector team. When they
  do, fill names, roles and dates in `qa/inspections/INS-01-repayment-path/` (plan and forms),
  hold the meeting, and log the real defects in `qa/defect-log.md`. Do not invent findings.
- **After your code lands,** re-run the static analysis
  (`.\gradlew.bat --init-script config/pmd-init.gradle pmdMain`), add the new findings to
  `qa/static-analysis-report.md` with a disposition, and update the risk register rows you own
  (R3, R5, R6, R7, R9).

## Where things stand (16 Nov 2026)

- **Ledger, persistence, web and the composition root are complete** (PR #8). What exists for
  you to build on:
  - `ledger.RepaymentCommand`: `cash(LoanId, Money, LocalDate)` and
    `bank(LoanId, Money, LocalDate, String bank, String reference)`. It validates once, at
    creation, and throws `InvalidRepaymentException`.
  - `CompositionRoot.importPoster()`: the import Decorator chain
    `IdempotencyGuardPoster -> TimingPoster -> CoreRepaymentPoster`. A repeated (bank,
    reference) is refused with `DuplicatePaymentException` (BR-25).
  - `ledger.LedgerWriter`: `append(LedgerEntry)` and `appendAll(List<LedgerEntry>)`, all or
    nothing.
  - `ledger.LoanRepository`: `findById(LoanId)` and `save(LoanAccount)`. SQLite
    implementation: `persistence.SqliteLoanRepository`, `BEGIN IMMEDIATE`, busy timeout.
    `save` only **updates**; creating a loan row is origination's job (I-03).
  - `ledger.LoanAccount`: `ACTIVE`, `CLOSED` and `WRITTEN_OFF`, with `closeAsRepaid`. There is
    **no write-off transition yet** (open item T-03 for Tughral).
  - `ledger.EntryType` has `PENALTY_ACCRUAL` and `WRITE_OFF` but **no balancing types for
    them yet** (T-02 and T-03). Every posting must balance (BR-23, D-04).
  - **Schema v1** (`persistence/SchemaMigrator.java`):
    - `customer (id, full_name, date_of_birth, monthly_income)`;
    - `loan (..., status, risk_grade IN ('A','B','C'), disbursed_on, closed_on)`;
    - `delinquency_snapshot (loan_id, business_date, days_past_due, classification IN ('STANDARD','WATCH','SUBSTANDARD','DOUBTFUL','LOSS'), UNIQUE(loan_id, business_date))`.

    That unique key is the end-of-day idempotency guard (BR-33). Columns are added through a
    schema version 2 in `SchemaMigrator`; never edit version 1.
  - **Placeholders naming you:**
    - `CompositionRoot.withPlaceholders`: the penalty part of `InstalmentsDueSource`;
    - `web/TellerWebServer.endOfDay` (`TODO(delinquency, Muhammad Ibrahim)`): the
      `/end-of-day` page currently answers 503.
- **No `origination`, `delinquency` or `ledger.importing` package exists yet.**

## Rules for every item (same as `docs/planning/m2-code-todo.md`)

1. **Branch from `develop`** as `feature/origination-<name>`, `feature/delinquency-<name>` or
   `feature/importing-<name>`. Open one PR into `develop`, reviewed by a non-author (Tughral or
   Hassan), merged with a merge commit. Never squash.
2. **Commit order:** `test(red): ...` first, after running it and seeing it fail; then
   `feat(green): ...`; then any real `refactor: ...`.
3. **`.\gradlew.bat build` must pass before the green push.** Checkstyle fails on:
   - `double` or `float` (use `common.Money`; compare with `compareTo() == 0`);
   - missing Javadoc on public API;
   - a missing final newline.
4. **Architecture rules** (`ArchitectureTest`, every build): no package cycles (C-03), and
   domain modules must not import `persistence` or `web`. Allowed directions (M1 section 3.3):
   - `origination -> products`;
   - `delinquency -> ledger`;
   - `delinquency -> products`.

   `ledger` must never import `delinquency` or `origination`. Domain modules declare the
   interfaces they need; `persistence` implements them; `CompositionRoot` wires them.
5. **Exceptions crossing a module boundary extend `common.LoanDomainException`** and carry the
   data that explains the failure. No `catch (Exception e)` and no empty catch blocks.
6. **Update `qa/rtm.md`** for each FR you implement, and add a row to `qa/m2-ai-usage-log.md`.

## Queue

- [ ] **I-01 (M2-critical)** Origination rules.
  - **Types:**
    - `LoanApplication`, validated in its constructor;
    - `EligibilityRule` interface (`check(LoanApplication): RuleOutcome`);
    - `AgeRule`, `ActiveLoanLimitRule`, `BurdenRatioRule`, `WriteOffHistoryRule`,
      `ProductBandRule`;
    - `EligibilityEvaluator`, holding a `List<EligibilityRule>` and evaluating in order
      BR-01 to BR-07;
    - `Decision`, `RejectionReason`, `RiskGrade`;
    - `IneligibleApplicantException`.
  - **Use the exact boundary values** in `docs/business-rules.md` section 2. The
    black-box cases TC-ORG-01 to TC-ORG-07 are already designed in
    `qa/test-design/black-box.md` section 3.
  - **Where the burden ratio needs the new loan's instalment** (BR-04), take it as a
    constructor-injected `Money` provider. Do not compute schedules in `origination`.
  - BR-01 to BR-08; FR-01 to FR-05, FR-07.
- [ ] **I-02** Risk grade and amount cap.
  - Spreads and caps: A +0.00% / 100%, B +1.50% / 80%, C +3.00% / 60%.
  - A capped amount below the product minimum is rejected with `BELOW_PRODUCT_MINIMUM`.
  - BR-09, BR-10; FR-06.
- [ ] **I-03 (M2-critical for end to end)** Loan creation.
  - On approval, ask Hassan's `products.LoanProduct.generateSchedule(...)` for the schedule
    (his H-03), then store the customer, the loan row and the instalments.
  - **Declare** `origination.LoanOriginationRepository` (`create(customer, loan, schedule)`).
  - **Implement** it as `persistence.SqliteLoanOriginationRepository`, inserting customer,
    loan and instalment rows in **one** `BEGIN IMMEDIATE` transaction. Ask Tughral to review
    the persistence class.
  - **Also write** the `PRINCIPAL_DISBURSEMENT` ledger lines as a balanced pair through
    `LedgerWriter.appendAll`. Agree the credit leg with Tughral (D-04).
- [ ] **I-04 (M2-critical, the Adapter pattern)** Bank statement import in
  `pk.edu.giki.microlend.ledger.importing`. Follow M1 section 5.2 and View 2.
  - **Target:** `RepaymentSource` (`nextBatch(): List<RepaymentCommand>`).
  - **Adapters:** `HblStatementAdapter` and `McbStatementAdapter`. Use the M1 class names. Each one **holds**
    Apache Commons CSV `CSVParser` by composition, as the adaptee, and translates that bank's
    columns, date format, minor units and sign convention into
    `RepaymentCommand.bank(...)`.
  - **Dependency:** add `implementation 'org.apache.commons:commons-csv:1.12.0'` to
    `build.gradle`. It is free.
  - **Sample statements:** put one small CSV per bank under `src/test/resources/statements/`.
    The formats are ours to define; document the columns in the adapter Javadoc.
  - **Bad rows:** a malformed row raises a typed `InvalidStatementException extends
    LoanDomainException` naming the file and line.
  - **`StatementImportJob`:** feeds each command into `CompositionRoot.importPoster()`. It
    records one audit line per file, which is FR-31's import half; coordinate the audit sink
    with Tughral.
  - **Tests:**
    - each adapter's mapping;
    - a duplicate (bank, reference) in a second file is refused (BR-25);
    - "same reference from two banks" is accepted.
  - FR-20, FR-18; TC-LED-08, TC-LED-06.
- [ ] **I-05 (M2-critical)** Delinquency calculators, all stateless and in
  `pk.edu.giki.microlend.delinquency`.
  - `DaysPastDueCalculator.daysPastDue(...)`: counted from the day after the due date (BR-29).
  - `DelinquencyClassifier.classify(int dpd): Classification`, mapping 0 / 1-30 / 31-60 /
    61-90 / 91+ to Standard / Watch / Substandard / Doubtful / Loss (BR-30). It is the
    white-box target, with CC about 6.
  - `PenaltyAccruer`: 0.05% per day on overdue **principal only**, capped at 10% of the
    overdue amount (BR-27, BR-28). Use `Money.percentage`; never `double`.
  - `ProvisioningPolicy`: 1 / 5 / 25 / 50 / 100% of outstanding principal (BR-31).
  - `WriteOffPolicy.shouldWriteOff(int dpd)`: more than 180 days (BR-32).
  - **Test with the boundary values** in `docs/business-rules.md` section 5. Cases TC-DEL-01
    to TC-DEL-05 are designed in `qa/test-design/black-box.md` section 6.
  - FR-21 to FR-25.
- [ ] **I-06 (M2-critical, the concurrent operation)** `EndOfDayEngine.run(LocalDate businessDate): EodReport`.
  Follow M1 section 3.6 exactly.
  - **Thread pool:** `Executors.newFixedThreadPool(4)`, one task per active loan.
  - **`LockRegistry`:** `ConcurrentHashMap<LoanId, ReentrantLock>` with `lockFor(LoanId)`.
    Put it in `pk.edu.giki.microlend.common`, not `delinquency`. The teller posting chain in
    `ledger` must use the same registry, and `ledger` may not import `delinquency`.
  - **Per loan, under its lock:** read the loan and schedule, compute DPD, penalty and
    classification, then write the penalty ledger lines **and** the `delinquency_snapshot` row
    in **one** SQLite transaction.
    - Declare a role `delinquency.DailyResultStore` (`record(LoanId, LocalDate, int dpd,
      Classification, List<LedgerEntry> accruals)`).
    - Implement it in `persistence` with `BEGIN IMMEDIATE`.
    - The `UNIQUE (loan_id, business_date)` key makes a second run for the same date roll
      back, accrual included. Treat that as "already done", which is BR-33's idempotency.
  - **Never hold two account locks at once.**
  - **Penalty lines must balance:** wait for Tughral's balancing entry type (T-02), or agree it
    with him first.
  - **Penalty per instalment:** expose the accrued penalty per instalment through a small
    `delinquency.PenaltyLookup`. Hassan's `ScheduleInstalmentsDueSource` (H-08) takes it as a
    constructor argument (D-02).
  - **Tests**, latch-driven with `CountDownLatch` and never `Thread.sleep`:
    - all loans processed;
    - a second run for the same date adds nothing;
    - a task that throws does not block the pool.
  - BR-27 to BR-33; FR-21, FR-26, QR-06.
- [ ] **I-07 (M2-critical)** Teller and engine share the per-account lock.
  - Add a `LockingPoster` decorator (an `ledger.RepaymentPosterDecorator` subclass) that takes
    `common.LockRegistry` and holds the loan's lock around `super.post(...)`.
  - It is wired innermost, just outside `CoreRepaymentPoster`, in both chains. The class and
    the wiring are in Tughral's packages, so open the PR with him as reviewer, or ask him to
    add it.
  - **Concurrency test:** teller posts and an end-of-day run on the same loans at once give
    the same balances as running them one after the other. This is QR-06; 50 runs is the M3
    target, and fewer is fine for M2.
- [ ] **I-08 (M2-critical)** Wiring and the web page.
  - Build the engine in `CompositionRoot`, exposing for example `endOfDayEngine()`.
  - Replace the `TODO(delinquency, Muhammad Ibrahim)` in `TellerWebServer.endOfDay` so that
    POST `/end-of-day` runs it for a business date and shows the `EodReport`.
  - Plug your `PenaltyLookup` into Hassan's `ScheduleInstalmentsDueSource`.
  - Ask Tughral to review.
- [ ] **I-09** `ClassificationChangeEvent` raised on every change of classification, up or
  down.
  - Collect the events in the `EodReport` (BR-34; FR-27, TC-DEL-07).
- [ ] **I-10** Write-off.
  - When `WriteOffPolicy` says so, move outstanding principal to write-off with a balanced
    `WRITE_OFF` pair, and set the loan to `WRITTEN_OFF`.
  - Needs Tughral's T-03.
  - BR-32; FR-25.
- [ ] **I-11 (M3)** `EndOfDayPerformanceTest`: 10,000 loans in 20 seconds or less on 4 threads,
  median over 5 runs (QR-01). Then two injected failure scenarios for the M3 concurrency
  criterion (M1 section 3.6, last paragraph).
