# Milestone 2 - SE423 Software Construction and Development

**Team Lead / Integrator (M2):** Muhammad Ibrahim
**Companion documents:** `docs/M1-SE423-Proposal-OOD-Pattern-Plan.md` (the design this build is measured against), `docs/M2-SE431-Verification-Baseline.md` (the quality evidence for the same build).
**Build described:** commit `5328937` (21 Nov 2026), the head of `feature/delinquency-origination-import`, merged into `develop` by pull request #9 on top of the ledger work of pull request #8. Every number below comes from that commit's build output, its CI runs or its Git history. None is estimated.

## 0. Module status at writing

| Module / package | Owner | State on 21 Nov 2026 | Where |
|---|---|---|---|
| `common` | shared | Done: `Money`, `LoanId`, `LoanDomainException` and two subtypes; `LockRegistry` (per-account locks) | `develop` |
| `ledger` | Tughral Hussain | Done: allocation waterfall, settlement (partial, see 1.3), posting pipeline with Decorator chain, compensating reversals | PR #8 |
| `ledger.importing` (Adapter) | Muhammad Ibrahim | Done: `HblStatementAdapter` and `McbStatementAdapter` over Apache Commons CSV, `StatementImportJob`, reachable from the application | PR #9 |
| `delinquency` (concurrent end-of-day engine) | Muhammad Ibrahim | Done: days past due, classification, penalty, provisioning, write-off threshold, `EndOfDayEngine` on four threads, classification-change events. Posting a write-off is M3 (needs the `LoanAccount` write-off transition, open item T-03) | PR #9 |
| `origination` | Muhammad Ibrahim | Done: eligibility rules BR-01 to BR-08, risk grade and amount cap BR-09 and BR-10. Creating the loan row and schedule (I-03) waits for `products` | PR #9 |
| `persistence` | Tughral Hussain; `SqliteDailyResultStore` and schema v2 by Muhammad Ibrahim | Done: schema v1 (7 tables) and v2 (`PENALTY_INCOME`, one reversal per entry), `SqliteLedgerRepository`, `SqliteLoanRepository`, `SqliteDailyResultStore` | PR #8, PR #9 |
| `web` and composition root | Tughral Hussain; end-of-day page, `LockingPoster` wiring and statement import by Muhammad Ibrahim | Done: teller page and end-of-day page on `127.0.0.1:8080`, four request threads, `CompositionRoot` wiring both chains, the engine and the import | PR #8, PR #9 |
| `products` (Factory Method) | Hassan Khalid | Only `DueDateCalculator` and its test, in PR #6 | PR #6 |
| `reporting` | Hassan Khalid | Not started | - |

Sections 2.2 and the `products` parts of 1.2 remain Hassan Khalid's to complete.

## 1. Working core architecture

### 1.1 Built packages against the M1 component view

The M1 component view (M1 section 3.3) put `common` at the centre. Domain modules depend inward on it. `persistence` implements interfaces that the domain declares, and `web` sits on the outside. The built code follows that rule, and the rule is now enforced by the build, not just stated.

```mermaid
flowchart TB
    APP[MicroLendApplication]
    ROOT[CompositionRoot]
    WEB[web: TellerWebServer, OperatorContext]
    ORG[origination: eligibility rules, risk grade]
    DEL[delinquency: EndOfDayEngine, calculators]
    IMP[ledger.importing: bank statement adapters]
    LED[ledger: allocation, settlement, posting pipeline]
    PER[persistence: SqliteDatabase, SchemaMigrator, Sqlite* stores]
    COM[common: Money, LoanId, LockRegistry, LoanDomainException]
    APP --> ROOT
    APP --> WEB
    ROOT --> DEL
    ROOT --> IMP
    ROOT --> LED
    ROOT --> PER
    WEB --> LED
    WEB --> DEL
    DEL --> LED
    IMP --> LED
    ORG --> COM
    DEL --> COM
    LED --> COM
    PER -.implements LedgerWriter, LedgerReader, LoanRepository, DailyResultStore.-> LED
    PER --> COM
```

`src/test/java/pk/edu/giki/microlend/ArchitectureTest.java` (ArchUnit 1.3.0) runs inside `.\gradlew.bat build` and in CI. It checks five rules:

| Rule | ArchUnit check | Source |
|---|---|---|
| No cycles between module packages | `slices().matching("pk.edu.giki.microlend.(*)..").should().beFreeOfCycles()` | Constraint C-03 |
| `common`, `ledger`, `origination` and `delinquency` never depend on `persistence` or `web` | `noClasses()...dependOnClassesThat().resideInAnyPackage(persistence, web)` | DIP, M1 section 4.5 |
| `common` depends on no module | `noClasses()` in `common` depend on any other module | M1 section 3.3 |
| `ledger` never depends on `origination` or `delinquency` | `noClasses()` in `ledger` depend on either | M1 section 3.3 (allowed directions) |
| `origination` depends only on the kernel | `noClasses()` in `origination` depend on `ledger` or `delinquency` | M1 section 3.3 |

The last three rules were added with the new modules (`5328937`), so the allowed directions of M1 section 3.3 are checked by the build rather than stated.

### 1.2 End-to-end path that runs today

`.\gradlew.bat run` starts `MicroLendApplication`. It builds `CompositionRoot.withPlaceholders(new SqliteDatabase("jdbc:sqlite:microlend.db"), operator)` and serves the teller and end-of-day pages on the loopback address only (`InetAddress.getLoopbackAddress()`), on four request threads. `.\gradlew.bat run --args="import HBL statement.csv"` imports a bank statement instead. The app is never deployed.

Three paths run through the production wiring:

| Path | Route through the code |
|---|---|
| Teller posting | `TellerWebServer` (form) -> `AuditLoggingPoster` -> `TimingPoster` -> `LockingPoster` (account lock) -> `CoreRepaymentPoster` -> `RepaymentAllocator` + `SettlementPolicy` -> `SqliteLedgerRepository.appendAll` (one transaction) -> `SqliteLoanRepository.save` (`BEGIN IMMEDIATE`) |
| Statement import | `MicroLendApplication` -> `CompositionRoot.importStatement` -> `HblStatementAdapter` or `McbStatementAdapter` (over `CSVParser`) -> `StatementImportJob` -> `IdempotencyGuardPoster` -> `TimingPoster` -> `LockingPoster` -> `CoreRepaymentPoster` -> as above |
| End of day | `TellerWebServer` (`POST /end-of-day`) -> `EndOfDayEngine.run` -> four pool threads, one task per active loan, each under its account lock -> `SqliteDailyResultStore.record` (snapshot and penalty pair in one `BEGIN IMMEDIATE` transaction) |

Two inputs come from `products`, which is not built yet:
- the instalments owed per loan (`InstalmentsDueSource`, decision D-02), which the posting allocator and the end-of-day engine both read;
- the early-settlement rebate (`EarlySettlementRebate`, decision D-03).

`withPlaceholders` fills both with lambdas that throw `ModuleNotAvailableException`, which names the owner. In the running application a teller posting is therefore refused with that message, and each end-of-day task reports it in the page's "Failed" list, having written nothing. Every path above is exercised end to end against a real SQLite file, with a test double only for the instalments owed:
- `CompositionRootTest`: the teller chain posts, audits and times; the import chain refuses a duplicate;
- `ConcurrentTellerAndEndOfDayTest`: four teller threads and the engine on the same loans at once (section 4.3);
- `StatementImportWiringTest`: an HBL statement imported through `CompositionRoot.importStatement`, then refused as duplicates on a second import;
- `TellerWebServerTest`: the end-of-day page runs the engine for a business date and shows its report.

When Hassan Khalid's `InstalmentsDueSource` (H-08) and rebate (H-07) replace the two placeholders, the same paths run against real schedules with no other change.

### 1.3 Deviations from the M1 design, with reasons

| Deviation | M1 said | Built | Why |
|---|---|---|---|
| Allocator input type | `RepaymentAllocator.allocate()` over `products.Instalment` | Takes `ledger.InstalmentDue` (record: sequence, due date, outstanding penalty, fees, interest, principal) | M1's `Instalment` holds only principal and interest. The allocator also needs what is still owed per tier, including the penalty accrued by `delinquency`. A ledger-owned input keeps `ledger` independent of `products`, so it compiles and tests now and needs no `ledger -> products` dependency. `products.Instalment` is mapped onto it through `InstalmentsDueSource` |
| BR-18 / BR-19 order | "penalty -> fees -> interest -> principal, oldest first" | Tier by tier: all penalties oldest first, then fees, then interest, then principal | The rule text allows two readings. The literal one was adopted on 14 Nov 2026, recorded under BR-19 in `docs/business-rules.md`, and confirmed as team decision D-01 on 16 Nov 2026 (`docs/planning/team-decisions.md`). Only `RepaymentAllocator` and its test change if the team chooses otherwise |
| Debit leg of a repayment | Not specified | New `EntryType.PAYMENT_RECEIVED` debit balanced by one credit per tier | Without it a posting's lines cannot balance on their own (BR-23). The schema `CHECK` on `entry_type` lists it |
| Multi-line writes | `LedgerWriter.append(entry)` | Adds `LedgerWriter.appendAll(List<LedgerEntry>)`, all or nothing | A posting is 2 to 6 lines. Writing them one by one would leave a half-posted repayment if a write failed mid-way |
| Ledger write vs loan save | One transaction for read, allocate and write | Two transactions, both inside the account lock taken by `LockingPoster`; if the loan save fails, `CoreRepaymentPoster` appends a reversal of every line it wrote (BR-24) | The two repositories open their own connections. The lock (section 4.3) stops a concurrent posting from interleaving, and the compensating reversal keeps the ledger balanced, which `PostingFaultInjectionTest` proves at 14 failure points. Merging them into one transaction remains an open item for M3 |
| Settlement | `SettlementPolicy` covers BR-21 and BR-22 | BR-21 advance credit and payoff detection built; the BR-22 rebate is requested through `EarlySettlementRebate`; money above the payoff is refused | The rebate amount differs per product type, which is `products` knowledge. No rule says how an excess over the payoff is returned, so it is refused rather than invented |
| Duplicate-reference store | `UNIQUE (source_bank, bank_reference)` on `payment` | `PaymentReferences` role with `InMemoryPaymentReferences` | The guard is in place and the import now uses it, but references are forgotten when the process restarts. A durable store on the `payment` table is an open defect for M3 (`qa/defect-log.md`) |
| Package `persistence` | Implementations named, package implied | Explicit `pk.edu.giki.microlend.persistence` | It holds the SQLite code apart from the domain, so the ArchUnit rule in 1.1 can be stated |
| What the engine reads | `EndOfDayEngine --> ScheduleProvider`, `DaysPastDueCalculator.daysPastDue(Schedule, LocalDate)`, `PenaltyAccruer.accrue(LoanAccount, LocalDate)` | The engine reads `ledger.InstalmentsDueSource` (instalments due by the business date, with what is still owed per tier); both calculators take that list | Decision D-02 made `InstalmentsDueSource` the one place where a schedule meets payments. Reading the same source as the posting allocator means the engine and a teller can never disagree about what is overdue. `delinquency -> ledger` is an allowed direction (M1 section 3.3) |
| Penalty balancing leg | `PENALTY_ACCRUAL` only | New `EntryType.PENALTY_INCOME` credit; schema version 2 rebuilds `ledger_entry` with it in the `CHECK` | Without a credit leg a penalty accrual cannot balance (BR-23, decision D-04, open item T-02). Version 1 is never edited; existing databases are upgraded in place (`SchemaVersion2Test`) |
| Engine thread pool | `-ExecutorService pool` field | A pool created per `run` and shut down in `finally` | No pool thread outlives a run, so the application and the tests never leak threads |
| Write-off | Engine moves principal to write-off (BR-32) | Engine lists loans more than 180 days past due in `EodReport.writeOffsDue`; nothing is posted | Posting needs a `LoanAccount` write-off transition and a balanced `WRITE_OFF` pair (open item T-03, M3) |
| `ProvisioningPolicy` | `<<interface>>` | A final class mapping each `Classification` to its BR-31 percentage | Only one provisioning schedule exists. An interface with one implementation adds nothing until a second schedule is needed |
| Parser failures | Translated into `InvalidRepaymentException` | New `InvalidStatementException` naming the file and line | `InvalidRepaymentException` describes one repayment, not a file; the handoff asked for the file and line in the message |
| Origination types | `LoanApplication`, `Applicant`, rules, `Decision`, `RiskGrade` | Adds `ProductBand` (amount and tenure limits), `RuleOutcome` (reason and detail), `InvalidLoanApplicationException`; `Decision` also holds the approved amount | `products.LoanProduct` does not exist yet, so the limits arrive as a small record that the product will supply. A rule's detail names the rule that failed (BR-08) |

## 2. Design patterns implemented

| Pattern | Kind | Owner | State |
|---|---|---|---|
| Decorator (`RepaymentPoster` pipeline) | Structural | Tughral Hussain (`LockingPoster`: Muhammad Ibrahim) | Implemented, integrated in both production chains, tested |
| Adapter (bank statement adapters) | Structural | Muhammad Ibrahim | Implemented, reachable from the application through `CompositionRoot.importStatement`, tested against the production import chain |
| Factory Method (`LoanProduct`) | Creational | Hassan Khalid | Not yet implemented (section 2.2) |

### 2.1 Decorator: the repayment posting pipeline

**Problem it solves.** A posting sometimes also has to:
- write an audit line naming the operator (FR-31);
- record its elapsed time (the GQM measure);
- refuse a bank reference already posted (BR-25).

Which of these apply depends on the call path, and they combine (M1 section 5.3, table of call paths).

**Participants in the code:**

| GoF role | Class (`src/main/java/pk/edu/giki/microlend/ledger/`) | Queue item, green commit |
|---|---|---|
| Component | `RepaymentPoster` (interface, `post(RepaymentCommand)`) | L-08, `34a7bb7` |
| ConcreteComponent | `CoreRepaymentPoster`: reads the loan, allocates, settles, writes balanced lines, saves | L-08, `34a7bb7` |
| Decorator | `RepaymentPosterDecorator`: abstract; IS-A `RepaymentPoster` and HAS-A `RepaymentPoster` (`private final RepaymentPoster wrapped`); `post` forwards | L-09, `9f7b423` |
| ConcreteDecorator | `TimingPoster`: measures with an injected nanosecond ticker and reports to `PostingTimeRecorder`, for both success and refusal | L-09, `9f7b423` |
| ConcreteDecorator | `AuditLoggingPoster`: refuses a posting with no operator; journals posted and refused attempts to `AuditJournal` | L-10, `a43c681` |
| ConcreteDecorator | `IdempotencyGuardPoster`: claims (bank, reference) atomically through `PaymentReferences`, and releases the claim if the inner posting fails; cash passes through | L-11, `71d8b3f` |
| ConcreteDecorator | `LockingPoster`: holds the loan's lock from the shared `LockRegistry` around the inner posting (section 4.3) | I-07, `3b167bf` |

```mermaid
classDiagram
    class RepaymentPoster {
      <<interface>>
      +post(RepaymentCommand) PostingResult
    }
    class CoreRepaymentPoster
    class RepaymentPosterDecorator {
      <<abstract>>
      -wrapped : RepaymentPoster
      +post(RepaymentCommand) PostingResult
    }
    class TimingPoster
    class AuditLoggingPoster
    class IdempotencyGuardPoster
    class LockingPoster
    RepaymentPoster <|.. CoreRepaymentPoster
    RepaymentPoster <|.. RepaymentPosterDecorator
    RepaymentPosterDecorator o--> RepaymentPoster : wraps
    RepaymentPosterDecorator <|-- TimingPoster
    RepaymentPosterDecorator <|-- AuditLoggingPoster
    RepaymentPosterDecorator <|-- IdempotencyGuardPoster
    RepaymentPosterDecorator <|-- LockingPoster
```

**Integrated in running code.** `CompositionRoot` builds the two chains planned at M1 over one shared `CoreRepaymentPoster`, now wrapped innermost by `LockingPoster`:

```java
final RepaymentPoster core = new LockingPoster(new CoreRepaymentPoster(
    new RepaymentAllocator(), new SettlementPolicy(rebates),
    new SqliteLoanRepository(database), instalments,
    new SqliteLedgerRepository(database), clock), locks);
this.importPoster = new IdempotencyGuardPoster(
    new TimingPoster(core, System::nanoTime, (c, elapsed, ok) -> timings.add(elapsed)),
    new InMemoryPaymentReferences());
this.tellerPoster = new AuditLoggingPoster(
    new TimingPoster(core, System::nanoTime, (c, elapsed, ok) -> timings.add(elapsed)),
    operator, auditTrail::add, clock);
```

The teller page posts through `tellerPoster()`, and a bank statement through `importPoster()` (section 2.3). The guard is outermost so a duplicate is refused before anything is timed or written. The lock is innermost so it covers only the read-allocate-write sequence, not auditing or timing. `CompositionRootTest` checks both chains end to end against SQLite:
- the teller chain posts, audits the operator and times the posting;
- the import chain refuses a duplicate before it is timed, and writes no audit line.

**Benefit, shown by the tests.** Each behaviour is one class with one test class: `TimingPosterTest`, `AuditLoggingPosterTest`, `IdempotencyGuardPosterTest` and `LockingPosterTest`. Each is tested against a stub inner poster, without a database. `CoreRepaymentPosterTest` tests the core with no decorator at all, which is the "unit tests" call path of M1 section 5.3. The pattern's open-closed claim was tested in practice at M2: account locking was added as one new subclass and one changed line in the root, without editing `CoreRepaymentPoster` or any other decorator.

**Rejected alternatives:**
- **Inheritance combinations.** Three optional behaviours would need eight subclasses, and the combination could not be chosen per call path.
- **Proxy.** Proxy controls access to one subject. The problem here is stacking several responsibilities in different combinations, which is the Decorator's intent (M1 section 5.3).

### 2.2 Factory Method: `LoanProduct` as Creator (planned at M1)

M1 design (section 5.1):
- **Creator:** `LoanProduct` holds the stable `generateSchedule()` workflow and declares `protected abstract InterestCalculator createInterestCalculator()`.
- **ConcreteCreators:** `FlatRateProduct`, `ReducingBalanceProduct` and `BulletProduct` each override that method.
- **Products:** `InterestCalculator` and its three concrete calculators.
- **Rejected:** a Simple Factory `create(type)` switch, which is not the GoF pattern, as well as Abstract Factory and Singleton.

**Status at writing (16 Nov 2026): not yet implemented. Owner to complete this subsection (Hassan Khalid).** It should give:
- the classes and their commit hashes;
- a class diagram;
- the test showing the workflow never names a concrete calculator;
- the shared `InterestCalculator` contract test (LSP evidence, M1 section 4.3);
- how `products.Instalment` is mapped to `ledger.InstalmentDue` (see 1.3).

### 2.3 Adapter: bank statement adapters

**Problem it solves.** Repayments also arrive as bank statements (FR-20). Each bank exports a different file: its own column names and order, date format, amount unit and way of marking money in or out. The posting pipeline expects one thing, a validated `RepaymentCommand`. Without an adapter, either every bank's quirks leak into the posting code, or the core is changed to one bank's format and breaks for the next.

**Participants in the code** (`src/main/java/pk/edu/giki/microlend/ledger/importing/`, owner Muhammad Ibrahim):

| GoF role | Class | Commits |
|---|---|---|
| Target | `RepaymentSource`: `nextBatch(): List<RepaymentCommand>`; an empty batch means the statement is finished | red `d0a156d`, green `c216a87` |
| Adaptee | Apache Commons CSV `CSVParser` (third-party, `commons-csv` 1.12.0) | - |
| Adapter | `HblStatementAdapter` and `McbStatementAdapter`: each implements `RepaymentSource` and holds a `CSVParser` field (composition) | red `d0a156d`, green `c216a87` |
| Client | `StatementImportJob`: sees only a `RepaymentSource` and a `RepaymentPoster`, never a bank format | red `d0a156d`, green `c216a87` |
| Integration | `CompositionRoot.importStatement(bank, reader, fileName)` picks the adapter and runs the job over `importPoster()`; `MicroLendApplication` accepts `import <HBL|MCB> <file>` | red `a206b83`, green `ec445cc` |

The two formats differ in every way an adapter has to absorb:

| | HBL | MCB |
|---|---|---|
| Columns used | `Txn Date`, `Reference No`, `Loan Ref`, `Amount (Paisa)`, `Dr/Cr` | `Transaction ID`, `Value Date`, `Customer Reference`, `Amount (PKR)` |
| Date | `ddMMyyyy`, strict | `dd/MM/yyyy`, strict |
| Amount | whole paisa, unsigned (`254900` is 2,549.00) | rupees with at most two decimals |
| Money in or out | `CR` is a repayment; `DR` (bank charges) is skipped | positive is a repayment; negative is skipped |

Both look columns up by name, so column order and extra columns do not matter. Each converts amounts only through `Money.of(BigDecimal)` (C-01) and builds `RepaymentCommand.bank(loanId, amount, date, bank, reference)`, which validates once more.

```mermaid
classDiagram
    class RepaymentSource {
      <<interface>>
      +nextBatch() List~RepaymentCommand~
    }
    class HblStatementAdapter {
      -parser : CSVParser
      +nextBatch() List~RepaymentCommand~
    }
    class McbStatementAdapter {
      -parser : CSVParser
      +nextBatch() List~RepaymentCommand~
    }
    class CSVParser {
      <<third-party, final>>
    }
    class StatementImportJob {
      +run() ImportReport
    }
    class RepaymentPoster {
      <<interface>>
    }
    RepaymentSource <|.. HblStatementAdapter
    RepaymentSource <|.. McbStatementAdapter
    HblStatementAdapter o-- CSVParser : adaptee
    McbStatementAdapter o-- CSVParser : adaptee
    StatementImportJob --> RepaymentSource
    StatementImportJob --> RepaymentPoster
```

**Defensive behaviour at the boundary.** A file is untrusted input:
- a missing column (usually the wrong bank's file) is refused in the adapter's constructor, before any row is read;
- a malformed row (bad amount or date, zero amount, blank reference or loan id, broken quoting) raises `InvalidStatementException`, which names the file and the line and keeps the cause;
- each adapter translates the whole file before returning its batch, so a malformed file posts nothing.

**Duplicates (BR-25).** The job posts through the import chain, whose `IdempotencyGuardPoster` is outermost. A row whose (bank, reference) was already posted is refused, listed in the `ImportReport`, and the rest of the file still posts, because banks send statements whose dates overlap. The same reference from two banks is two different payments and both post. Any other refusal (for example a closed loan, BR-26) stops the import with that exception.

**Integrated in running code, shown by the tests:**
- `HblStatementAdapterTest` and `McbStatementAdapterTest`: each bank's mapping, skipped debit rows, and every malformed-row case with its line number (TC-LED-08);
- `StatementImportJobTest`: a duplicate in a second file is refused and the rest posts; the same reference from two banks is accepted (TC-LED-06), using the real `IdempotencyGuardPoster`;
- `StatementImportWiringTest`: an HBL statement imported through `CompositionRoot.importStatement` reaches SQLite and reduces the loan's principal; importing it again posts nothing and lists both references.

**Rejected alternatives:**
- **Class Adapter.** `CSVParser` is declared `final`, and a Java class can extend only one class, so the adapter must hold the parser rather than inherit from it.
- **Facade.** A facade simplifies a subsystem behind a new interface. Here the interface already exists (`RepaymentSource`) and the job is to convert one interface into it, which is the Adapter's intent.
- **One parser with a `switch` on bank.** Every new bank would reopen the same class. With adapters, a new bank is one new class and one line in `CompositionRoot.importStatement`.

**Trade-off accepted.** The two adapters repeat about 20 lines (the header check and the row loop) so that each reads as one complete translation for its bank. A shared base class is the planned refactoring when a third bank is added.

## 3. Defensive programming and exceptions

### 3.1 Invalid objects cannot be constructed

Every value type validates in its factory or compact constructor and has no setters:

| Type | Rejects | Exception |
|---|---|---|
| `Money` (`common`) | null, negative, more than 2 decimal places; rounding is `HALF_EVEN`, explicit (C-01) | `InvalidMoneyException` |
| `LoanId` | null or blank; trims the value | `InvalidLoanIdException` |
| `LedgerEntry` (`debit`, `credit`, `reversalOf`) | any null field, a zero amount, a reversal without a reason | `InvalidLedgerEntryException` |
| `Allocation`, `InstalmentDue` | null tiers, sequence below 1 | `InvalidAllocationException` |
| `LoanAccount.restore` | null fields, an ACTIVE loan with a closure date, a CLOSED or WRITTEN_OFF loan without one | `InvalidLoanAccountException` |
| `RepaymentCommand` (`cash`, `bank`) | zero or null amount, blank bank or reference on a bank payment | `InvalidRepaymentException`, which carries the offending value and its source |
| `RepaymentAllocator.allocate` | null payment, list or element; a duplicate instalment sequence | `InvalidAllocationException` |
| `LoanApplication`, `Applicant` (`origination`) | a requested amount of zero or less, a tenure under one month, a disbursement date before the date of birth, a monthly income of 0.00 (the burden ratio divides by it), a loan history that contradicts itself | `InvalidLoanApplicationException` |
| `DelinquencyClassifier`, `PenaltyAccruer`, `WriteOffPolicy` | negative days past due | `InvalidDaysPastDueException` |
| `HblStatementAdapter`, `McbStatementAdapter` | a missing column, then any malformed row | `InvalidStatementException`, naming the file and line |

The `persistence` layer re-states these as schema constraints (`SchemaMigrator`):
- `CHECK (debit >= 0 AND credit >= 0)`;
- a `CHECK` listing the allowed `entry_type` values;
- foreign keys enforced with `PRAGMA foreign_keys = ON` on every connection (`SqliteDatabase`);
- money stored as `INTEGER` paisa (C-04);
- `UNIQUE (loan_id, business_date)` on `delinquency_snapshot` (BR-33);
- since schema version 2, a unique index on `ledger_entry.reverses_entry_id`, so an entry can be reversed at most once (BR-24).

### 3.2 One structured hierarchy at the module boundary

All 16 domain exceptions extend the abstract `common.LoanDomainException`, which is a `RuntimeException`:

```
LoanDomainException (common, abstract)
|-- InvalidMoneyException, InvalidLoanIdException          (common)
|-- InvalidLedgerEntryException, InvalidAllocationException,
|   InvalidLoanAccountException, InvalidRepaymentException,
|   LoanClosedException, LoanNotFoundException,
|   LedgerImbalanceException, DuplicatePaymentException     (ledger)
|-- InvalidStatementException                               (ledger.importing)
|-- InvalidLoanApplicationException,
|   IneligibleApplicantException                            (origination)
|-- InvalidDaysPastDueException                             (delinquency)
|-- PersistenceException                                    (persistence)
`-- ModuleNotAvailableException                             (composition root)
```

Each one carries the data needed to explain the failure:
- `LoanClosedException` carries the loan id, status and closure date (BR-26);
- `LedgerImbalanceException` carries both totals (BR-23);
- `DuplicatePaymentException` carries the bank and reference (BR-25);
- `InvalidStatementException` carries the file name and line number;
- `IneligibleApplicantException` carries the failing rule's outcome, whose detail names the rule (BR-08);
- `ModuleNotAvailableException` names the missing module and its owner.

The end-of-day engine never lets one loan's failure escape the run: a task that throws is caught by its `Future`, reported in `EodReport.failures` with the cause, and has written nothing, because its storage transaction rolled back.

JDBC failures never cross the boundary. `SqliteLedgerRepository` and `SqliteLoanRepository` catch `SQLException` and rethrow `PersistenceException` with the cause attached. Rollback happens before the rethrow (`appendAll`, `save`). There is no `catch (Exception e)` and no empty catch in production code.

### 3.3 Error reporting

- `AuditLoggingPoster` records every refused teller posting with its reason before rethrowing.
- `TellerWebServer` turns a `LoanDomainException` into a page that shows the message, not a stack trace. The end-of-day page lists each failed loan with its reason, HTML-escaped.
- `CoreRepaymentPoster` attaches a failed reversal to the original failure as a suppressed exception, so neither is lost.

Structured logging beyond the audit journal is deferred to M3 (static analysis finding SA-03).

## 4. Concurrency and thread safety

### 4.1 The planned concurrent operation (M1 section 3.6)

The end-of-day engine runs on `Executors.newFixedThreadPool(4)`, one task per loan, while teller threads post repayments to the same accounts. The races to prevent are lost updates, double penalty accrual for one business date, and torn reads in reports. The planned strategy:
- immutable core types;
- a `LockRegistry` of per-account `ReentrantLock`s held one at a time;
- each read, allocate and write inside one `BEGIN IMMEDIATE` SQLite transaction with a busy timeout and one connection per worker;
- a unique `(loan_id, business_date)` key;
- latch-driven tests.

### 4.2 What the built code already guarantees

| Measure in the code | Where | Race it addresses |
|---|---|---|
| Immutable value types: `Money`, `LoanId`, `LedgerEntry`, `Allocation`, `InstalmentDue`, `RepaymentCommand`, `LoanAccount`. `applyAllocation` returns a new account | `common`, `ledger` | Nothing shared can change under another thread. `LedgerImmutabilityTest` checks by reflection that `LedgerEntry` is final with final fields and no mutators |
| Stateless services: `RepaymentAllocator`, `SettlementPolicy`, `CoreRepaymentPoster` hold only final collaborators | `ledger` | One instance is shared by both chains and by any number of teller threads |
| One connection per call: every repository method opens its own connection through `SqliteDatabase.connect()` and closes it | `persistence` | No JDBC connection is shared between threads |
| `BEGIN IMMEDIATE` for every loan save, with `PRAGMA busy_timeout = 5000` on every connection | `SqliteLoanRepository.save`, `SqliteDatabase` | Takes SQLite's write lock up front, so two writers queue for up to 5 s instead of one failing. On its own this does **not** prevent a lost update: `CoreRepaymentPoster` reads the loan before that transaction starts, so two postings can both read the same balance. The PR #8 review found this; the per-account lock in 4.3 closes it |
| All-or-nothing ledger writes: `appendAll` in one transaction, rolled back on any failure | `SqliteLedgerRepository` | No half-posted repayment is visible to a concurrent reader |
| Exactly-once schema migration: `volatile boolean migrated` with double-checked locking in `synchronized` | `SqliteDatabase.ensureMigrated` | Several workers connecting at start-up cannot migrate twice |
| Atomic duplicate claims: `ConcurrentHashMap.newKeySet()`; `claim` is a single `add`, so of two concurrent claims of one (bank, reference) exactly one wins | `InMemoryPaymentReferences`, `IdempotencyGuardPoster` | A bank payment imported twice at the same moment is posted once (BR-25) |
| Thread-safe collectors: audit trail and timings are `CopyOnWriteArrayList`; accessors return copies | `CompositionRoot` | Teller threads append while the web page reads |

### 4.3 End-of-day engine and `LockRegistry` (owner Muhammad Ibrahim)

**The operation.** `EndOfDayEngine.run(businessDate)` (`delinquency`, green commit `70428a8`) lists the active loans, then submits one task per loan to `Executors.newFixedThreadPool(4)`. For its loan, each task:
1. takes the loan's lock from the shared `LockRegistry`;
2. reads what is owed through `InstalmentsDueSource`;
3. computes days past due (`DaysPastDueCalculator`, BR-29), the classification (`DelinquencyClassifier`, BR-30), the day's penalty (`PenaltyAccruer`, BR-27 and BR-28) and whether a write-off is due (`WriteOffPolicy`, BR-32);
4. records the snapshot and a balanced `PENALTY_ACCRUAL`/`PENALTY_INCOME` pair through `DailyResultStore.record`, in one storage transaction;
5. releases the lock.

The engine then collects every task's `Future` into an `EodReport`: loans recorded, loans already recorded for that date, penalty accrued, classification changes (BR-34), loans due for write-off, and per-loan failures. The pool is shut down in `finally`.

**The three races of M1 section 3.6, and what stops each one in the built code:**

| Race | Prevented by | Where |
|---|---|---|
| Lost update: two postings, or a posting and the engine, read the same account and both write back | `LockRegistry` (`ConcurrentHashMap<LoanId, ReentrantLock>`, locks created atomically by `computeIfAbsent`). The engine's task and `LockingPoster`, innermost in both posting chains, take the same lock for the same loan, so the read-allocate-write sequence of one account never interleaves with another on that account. Different accounts run in parallel | `common.LockRegistry`, `ledger.LockingPoster` (`3b167bf`), `EndOfDayEngine.process` |
| Double penalty accrual: the engine run twice for one date (a retry, or two runs at once) | `SqliteDailyResultStore.record` opens `BEGIN IMMEDIATE` first, then checks for an existing snapshot, then inserts the snapshot and the penalty lines and commits. The write lock is held across the check, so two racing records cannot both pass it; `UNIQUE (loan_id, business_date)` is the second line of defence. A repeated date writes nothing and is counted as "already recorded" (BR-33) | `persistence.SqliteDailyResultStore` |
| Torn read: a report sums balances mid-update | Every write that belongs together commits in one transaction (`appendAll`; snapshot plus penalty pair). The portfolio report is `reporting` (Hassan Khalid) and is not built, so the snapshot-read half of this measure is M3 work | `SqliteLedgerRepository`, `SqliteDailyResultStore` |

**Why it cannot deadlock.** A deadlock needs two threads each holding a lock the other wants. A task and a posting each hold exactly one account lock, the lock of the loan they are working on, and nothing in scope moves money between accounts. Inside the lock the only other wait is SQLite's write lock, which is always released by commit or rollback before the account lock is released.

**What runs in parallel and what is serialised.** SQLite admits one writer at a time, so the four threads run the reading and the calculation in parallel, while the short commits queue on SQLite's write lock with a 5-second busy timeout. Whether this is fast enough for 10,000 loans is QR-01, measured at M3 rather than assumed.

**Tests** (all deterministic: latches and lock queues, never `Thread.sleep`):

| Test | What it shows |
|---|---|
| `EndOfDayEngineTest.runsFourTasksInParallel` | Four tasks are inside the engine at the same moment: each counts down a latch of 4 and waits for it, which only completes if the pool really runs four at once |
| `EndOfDayEngineTest.workRunsUnderTheAccountLock` | Every read and write of a loan happens while the task holds that loan's lock (`isHeldByCurrentThread`) |
| `EndOfDayEngineTest.idempotentPerBusinessDate` | A second run for the same date records nothing and accrues nothing; the next date accrues again (TC-DEL-06) |
| `EndOfDayEngineTest.failingTaskDoesNotBlockThePool` | One loan's task throws; the other nine are recorded and the failure is reported with its cause |
| `EndOfDayEngineTest.classificationChangeEvents` | A downgrade and an upgrade raise one event each; no change raises none (TC-DEL-07) |
| `LockingPosterTest.waitsForTheHolder` | A posting to a locked account queues on the lock and does not reach the core until the holder releases it; another account is not held up |
| `LockRegistryTest.concurrentRequestsShareOneLock` | Eight threads released together all receive the same lock for one account |
| `SqliteDailyResultStoreTest.concurrentRecordsForTheSameDate` | Two connections record the same loan and date at once against a real SQLite file: exactly one succeeds, one snapshot and one penalty pair exist |
| `SqliteDailyResultStoreTest.oneTransaction` | A failing penalty line rolls the snapshot back with it |
| `ConcurrentTellerAndEndOfDayTest` (QR-06) | Four teller threads post 100 payments across five loans through `CompositionRoot.tellerPoster()` while `endOfDayEngine().run` processes the same loans, all on one SQLite file. Afterwards every loan's principal is exactly the sequential result, debits equal credits across the whole ledger, and each loan has one snapshot and one penalty pair |

The last test was checked against the fault it targets. With `LockingPoster` removed from the chains, it failed in 3 of 3 runs, reporting `LN-1 lost a posting: expected 49800.00 but was 49900.00`: ten teller postings had overwritten one another. With the lock in place it passes. QR-06's M3 target is 50 consecutive clean runs, and the two injected failure scenarios of M1 section 3.6 are also M3 work.

**The teller side.** `TellerWebServer` previously used the JDK server's default executor, so it served one request at a time and "teller threads" existed only in tests. It now serves requests on four threads (`TellerWebServerTest.servesRequestsConcurrently` proves two postings are inside the poster at once). The operator name stays per request because `OperatorContext` is a `ThreadLocal`.

## 5. Code quality and SOLID

### 5.1 Each principle, in the built code

| Principle | Evidence in code | Why it matters here |
|---|---|---|
| **SRP** | `RepaymentAllocator` only computes an `AllocationResult`. `SettlementPolicy` only decides advance credit or payoff. `CoreRepaymentPoster` only writes lines and saves. Each decorator adds one behaviour. In `delinquency` each rule has its own class: `DaysPastDueCalculator` (BR-29), `DelinquencyClassifier` (BR-30), `PenaltyAccruer` (BR-27, BR-28), `ProvisioningPolicy` (BR-31), `WriteOffPolicy` (BR-32); `EndOfDayEngine` only orchestrates them under locks. Each origination rule is one `EligibilityRule` class | A change to the waterfall order (for example the BR-19 reading) or to one delinquency bucket touches one class and one test class |
| **OCP** | New posting behaviour is a new `RepaymentPosterDecorator` subclass plus one line in `CompositionRoot`: `LockingPoster` was added at M2 exactly that way. A new eligibility rule is a new `EligibilityRule` added to the evaluator's list; a new bank is a new `RepaymentSource` adapter. None of these edits an existing class | L-09 to L-11 and I-07 were added without reopening `CoreRepaymentPoster` |
| **LSP** | Every decorator and `CoreRepaymentPoster` honour the `RepaymentPoster` contract: same inputs, a `PostingResult`, or a `LoanDomainException` passed through unchanged. Both bank adapters honour `RepaymentSource` (validated commands; an empty batch means finished), so `StatementImportJob` runs either one unchanged. Every `EligibilityRule` returns a `RuleOutcome` and never throws for an ineligible applicant | `CompositionRootTest` uses both chains where a `RepaymentPoster` is expected; `StatementImportJobTest` runs the same job over both adapters. The `InterestCalculator` contract test planned at M1 belongs to `products` (2.2) |
| **ISP** | Role interfaces, not one DAO: `LedgerWriter` (append, appendAll), `LedgerReader` (entriesFor), `LoanRepository`, `PaymentReferences`, `AuditJournal`, `PostingTimeRecorder`, `InstalmentsDueSource`, `EarlySettlementRebate`, and for the engine `DailyResultStore` (three methods: active loans, previous classification, record) | `CoreRepaymentPoster` receives a `LedgerWriter` and cannot read the ledger. The engine's whole storage need is one small role, so its tests use a small in-memory store (`InMemoryDailyResultStore`) |
| **DIP** | `ledger` declares `LedgerWriter`, `LedgerReader` and `LoanRepository`; `delinquency` declares `DailyResultStore`. `persistence` implements them. Only `CompositionRoot` builds the infrastructure graph; the entry point `MicroLendApplication` only opens the database file and hands it over. No domain class imports `java.sql` | Enforced by `ArchitectureTest.domainDoesNotDependOnInfrastructure`, which now covers `origination` and `delinquency`. The ledger suite runs on in-memory repositories; the engine suite on `InMemoryDailyResultStore` |

### 5.2 Cohesion, coupling and measured quality

- **Coupling.** It is one-directional and checked by ArchUnit (1.1). The heaviest dependency is `persistence -> ledger`, which runs in the intended direction.
- **Complexity.** The most complex method is `RepaymentAllocator.allocate`, with McCabe V(G) = 10, which JaCoCo confirms. It is kept as one method on purpose as the Fagan inspection target, as planned at M1. `DelinquencyClassifier.classify`, the white-box target planned for the delinquency module, has V(G) = 6: one guard and five buckets. Full complexity tables are in `qa/test-design/white-box.md`.
- **Static analysis.** Checkstyle 10.18.2 is a zero-warning CI gate and reports 0 findings, so C-01 holds: no `double` or `float`. PMD 7.7.0 found 66 findings on the final code, all triaged in `qa/static-analysis-report.md`: 4 fixed (`e3f585d`, `701c161`), 2 accepted, 12 deferred to M3 refactoring, and 48 false positives with reasons.
- **Known weaknesses kept for M3 refactoring evidence.** These are the guard-clause runs in `LoanAccount.restore` and `LedgerEntry.requireValid`, and `TellerWebServer`, which does routing, parsing and rendering in one class (DEF-004, DEF-005).

## 6. TDD and test baseline

### 6.1 Red-green-refactor history

Each queue item in `docs/planning/m2-code-todo.md` was built as a `test(red):` commit with the failing test, followed by a `feat(green):` commit with the code. Pull request #8 has **19 red/green pairs**, one per item from L-01 to L-19. They are followed by 2 `refactor:` commits (`6a93ca4` shared `Paisa` converter, `e3f585d` `isBlank`) and 1 `fix:` commit (`701c161`).

| Item | Red | Green | Item | Red | Green |
|---|---|---|---|---|---|
| L-01 | `131164a` | `d09780d` | L-11 | `3a85d3e` | `71d8b3f` |
| L-02 | `ba36509` | `b6a0c4f` | L-12 | `d34dffa` | `48f9378` |
| L-03 | `c6bfa3e` | `61febe9` | L-13 | `32ea473` | `c04b41c` |
| L-04 | `89063cc` | `512cd86` | L-14 | `8be61ef` | `92210a7` |
| L-05 | `e386629` | `c17052f` | L-15 | `65266d9` | `c2e3879` |
| L-06 | `4a353b4` | `c80b9c9` | L-16 | `535fa82` | `5502ccf` |
| L-07 | `db4b263` | `40586be` | L-17 | `33efd9a` | `038bd08` |
| L-08 | `83934ce` | `34a7bb7` | L-18 | `69f3d76` | `9db0657` |
| L-09 | `81b1a2f` | `9f7b423` | L-19 | `5835019` | `e0f9ecb` |
| L-10 | `4f79a3c` | `a43c681` | | | |

Most red commits fail to compile, because the type under test does not exist yet. L-16 is a genuine behavioural red. `PostingFaultInjectionTest` failed at 3 of its 14 failure points, where a failed loan save left the posting's lines in the ledger. Green commit `5502ccf` added the compensating reversal. Squash and rebase merges are disabled on the repository, so this history reaches `develop` intact.

Pull request #9 (Muhammad Ibrahim) adds **7 red/green pairs** for the I-items:

| Item | What | Red | Green |
|---|---|---|---|
| I-05 | Delinquency calculators, BR-27 to BR-32 | `fd5e402` | `9cab692` |
| I-06, I-09, T-02 | End-of-day engine, `LockRegistry`, `SqliteDailyResultStore`, schema v2 | `87a7796` | `70428a8` |
| I-07, I-08 | `LockingPoster`, wiring, end-of-day page, four request threads | `16d8ea5` | `3b167bf` |
| I-04 | Bank statement adapters and import job | `d0a156d` | `c216a87` |
| I-04 | Import reachable from the application | `a206b83` | `ec445cc` |
| I-01 | Origination eligibility rules BR-01 to BR-08 | `d962c75` | `da2c929` |
| I-02 | Risk grade and capped amount BR-09, BR-10 | `a7681e3` | `1eb0f25` |

Every red commit failed to compile before its green commit. Between I-07's red and green, a `test:` commit (`11d2c7d`) corrected a test double that built an invalid `PostingResult`, without changing any assertion, so the green commit `3b167bf` contains production code only. A further `test:` commit (`5328937`) extends the architecture rules to the new modules. The adapter and origination work was done on its own branches and joined by merge commits, so each pair keeps its own position in the history.

### 6.2 Tests and coverage

`.\gradlew.bat build jacocoTestReport` at `5328937`, compared with the ledger baseline at `701c161`:

| Measure | `701c161` (PR #8) | `5328937` (PR #9) |
|---|---|---|
| Tests | 182, 0 failures, 0 skipped | 366, 0 failures, 0 skipped |
| Line coverage (JaCoCo) | 773 / 818 = 94.5% | 1266 / 1331 = 95.1% |
| Branch coverage | 244 / 282 = 86.5% | 422 / 474 = 89.0% |
| Method coverage | 184 / 188 = 97.9% | 286 / 290 = 98.6% |

Coverage of the new packages at `5328937`:

| Package | Lines | Branches |
|---|---|---|
| `delinquency` | 136 / 139 | 49 / 50 |
| `origination` | 159 / 159 | 84 / 90 |
| `ledger.importing` | 89 / 92 | 20 / 20 |

Tests run against real logic and, for persistence, a real SQLite file:
- boundary tests on the allocator at 0.00, 0.01, due - 0.01 and exactly due (BR-20);
- the BR-23 invariant after every posting (`CoreRepaymentPosterTest.ledgerBalancesAfterEveryPosting`), and across the whole ledger after concurrent tellers and end of day (`ConcurrentTellerAndEndOfDayTest`);
- fault injection at 14 write points (`PostingFaultInjectionTest`);
- every black-box boundary of the delinquency and origination rules, named by their TC id: TC-DEL-01 to TC-DEL-07 and TC-ORG-01 to TC-ORG-07 (`qa/test-design/black-box.md`);
- an in-place upgrade of a version 1 database to schema version 2 (`SchemaVersion2Test`);
- HTTP tests of the teller and end-of-day pages (`TellerWebServerTest`);
- the architecture rules (`ArchitectureTest`).

The 60% gate (`coverageGate`) is required at M3. The lowest-covered package is the root package (`CompositionRoot`, `MicroLendApplication`: 29 of 46 lines), because `main` and the placeholder lambdas only run in the live application.

## 7. Repository discipline

Full evidence is in `qa/scm-evidence.md`, collected from the GitHub API.

- **Branching.** `main` holds baselines (`m1-baseline` = `10688c2`), `develop` is for integration, and work happens on `feature/<module>-<name>` branches. The ledger work was on `feature/ledger-entry` (PR #8); Ibrahim's modules on `feature/delinquency-origination-import` (PR #9), with the adapter and origination work on their own branches merged into it.
- **Controls.** Ruleset `main-protection` covers `main` and `develop` and has no bypass. It requires:
  - one approval from someone other than the last pusher, with stale approvals dismissed;
  - review threads resolved;
  - merge commits only, with no force push or deletion.

  Squash and rebase are off at the repository level.
- **CI.** `.github/workflows/build.yml` runs compile, test, Checkstyle and JaCoCo on every push and pull request. Every push to `feature/ledger-entry` passed.
- **Attribution.** Every member's work is on their own branch and pull request, under their own GitHub account. The commit counts are uneven (risk R6, `qa/risk-register.md`); `products` and `reporting` are still to land through Hassan Khalid's own pull requests.
- **Honest finding.** Six earlier pull requests (#1 to #5 and #7) were merged without an approving review, five of them by their own author. This was possible because the ruleset's bypass list let admins through. It is logged as DEF-006. The corrective action was taken on 4 Nov 2026: the ruleset was re-applied with an empty bypass list (CAPA-001). Since then, no pull request can merge without a non-author approval. PR #8 is the first held by that gate.

**Integration at M2:**

| Step | Author | Reviewer and outcome | Merged |
|---|---|---|---|
| PR #8, `feature/ledger-entry` -> `develop` | Tughral Hussain | Muhammad Ibrahim, 21 Nov 2026: approved with seven follow-ups recorded in the review (the defects raised from it are in `qa/defect-log.md`) | 21 Nov 2026, merge commit `47c713e` |
| PR #9, `feature/delinquency-origination-import` -> `develop` | Muhammad Ibrahim | TODO-BASELINE | TODO-BASELINE |
| `develop` -> `main` ("M2 baseline") | Muhammad Ibrahim | TODO-BASELINE | TODO-BASELINE |
| Tag `m2-baseline` | Muhammad Ibrahim | annotated, on the merge commit on `main` | TODO-BASELINE |

**A controlled exception to the no-force-push rule.** On 21 Nov 2026, with the team lead's approval, the ruleset was switched off for a few minutes so that `develop` could be force-pushed once. The commit messages since the M1 baseline were rewritten to remove tool-generated trailer lines. File contents and commit order did not change: every rewritten commit has the same tree as the one it replaced, which was checked before the push. The ruleset was switched back on straight afterwards. Rewriting changes commit hashes, so every hash quoted in these documents and in `qa/` was updated to the new value in commit `7784a6a` (141 references). GitHub's page for PR #8 still shows that pull request's original hashes. The event is recorded in `qa/scm-evidence.md`.
