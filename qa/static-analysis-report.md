# MicroLend - Static Analysis Report (M2)

**Deliverable:** SE431 M2, "Static analysis report" · **Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Code analysed:** `feature/ledger-entry`, main source set: `common`, `ledger`, `persistence`, `web`.
- First run: `923755b` (14 Nov 2026, part-way through the ledger queue).
- Final M2 run: `dd4dd7e` (whole ledger queue L-01 to L-19).
- After fixes: `87c5caf`.

Finding IDs stay stable between runs; new findings get new IDs.

## 1. Tools and configuration

| Tool | Version | How it runs | Rules | Gate |
|---|---|---|---|---|
| Checkstyle | 10.18.2 | Gradle plugin, every CI run (`.\gradlew.bat checkstyleMain checkstyleTest`) | `config/checkstyle.xml`: project coding standard, including **C-01** (IllegalType `double`/`float`, IllegalToken `NUM_DOUBLE`/`NUM_FLOAT`), Javadoc on public API, final newline | `maxWarnings = 0`, `ignoreFailures = false`: any finding fails the build |
| PMD | 7.7.0 | Gradle `pmd` plugin applied from an init script, so `build.gradle` is unchanged (command below) | Java categories: best practices, error prone, design, multithreading, security, performance | Report only at M2 (`ignoreFailures = true`) |

```powershell
.\gradlew.bat --init-script config/pmd-init.gradle pmdMain checkstyleMain checkstyleTest
# reports: build/reports/pmd/main.xml|html, build/reports/checkstyle/main.xml|test.xml
```

`config/pmd-init.gradle` applies the `pmd` plugin with `toolVersion = '7.7.0'`, the six rule categories above and `ignoreFailures = true`.

Checkstyle is the gate that is already enforced. PMD was added for M2 because Checkstyle only checks form, while PMD finds error-prone and design problems.

## 2. Results

| Run | Commit | Checkstyle | PMD findings |
|---|---|---|---|
| First | `923755b` | 0 | 53 |
| Final M2 code | `dd4dd7e` | 0 | **66** |
| After fixes | `87c5caf` | 0 | 62 |

How the 66 findings on the final code were dispositioned:

| Disposition | Findings |
|---|---|
| Fixed | 4 |
| Accepted | 2 |
| Deferred | 12 |
| False positive | 48 |

Checkstyle reports zero because it is a CI gate: every commit on `feature/ledger-entry` passed it (see `qa/scm-evidence.md`, section 3.2). In particular, constraint C-01 holds: there is no `double` or `float` in the code base.

## 3. Triage

Disposition key:
- **Fixed**: a real defect or weakness, corrected through a CAPA in `qa/defect-log.md`.
- **Accepted**: a real finding kept on purpose, with the reason recorded.
- **Deferred**: real, but scheduled for a later change, which the finding is tracked to.
- **False positive**: the rule does not apply to this design. The reason is recorded so the decision can be checked later.

| ID | PMD rule | Count | Location | Disposition | Reasoning / action |
|---|---|---|---|---|---|
| SA-01 | InefficientEmptyStringCheck | 3 | `LedgerEntry.reversalOf`, `RepaymentCommand.requireText`, `AuditLoggingPoster` (operator name) | **Fixed** in `f359765` | `value.trim().isEmpty()` made a trimmed copy only to test for blank text. It now uses `String.isBlank()` (Java 11+). Behaviour is the same, and the existing tests pass. DEF-001, CAPA-002 |
| SA-02 | CheckResultSet | 1 | `SchemaMigrator.currentVersion` | **Fixed** in `87c5caf` | `rows.next()` was ignored. `PRAGMA user_version` always returns one row in SQLite, but if none came back, the code would have read an empty cursor and failed with a raw `SQLException`. It now raises an explicit error that `migrate()` wraps in `PersistenceException`. DEF-002, CAPA-002 |
| SA-03 | SystemPrintln | 1 (was 9) | `MicroLendApplication.main` | **Accepted** | L-17/L-18 replaced the M1 console stub, which removed 8 of the 9 findings (DEF-003). The one remaining line prints the local address of the teller page once at start-up. That message is meant for the operator at the console, so it is not diagnostic logging |
| SA-04 | CyclomaticComplexity | 1 | `RepaymentAllocator.allocate` (PMD 13; McCabe 10) | **Accepted** | Kept as one method on purpose: it is the Fagan inspection target, and the queue requires it to read top to bottom. PMD counts the 3 `throw` statements as extra decisions. Its graph complexity of 10 is at, not above, McCabe's commonly used limit of 10, and all 18 branches are covered (`qa/test-design/white-box.md`). Re-assessed after inspection INS-01 |
| SA-05 | CyclomaticComplexity | 2 | `LoanAccount.restore` (13; McCabe 8), `LedgerEntry.requireValid` (11; McCabe 6) | **Deferred** to M3 refactoring | These are sequential guard clauses, each with its own message. They are a candidate for the M3 "refactoring and code smells" evidence: extract a validation helper and measure before and after. DEF-004 |
| SA-06 | TooManyMethods | 3 | `LedgerEntry`, `RepaymentCommand`, `TellerWebServer` | **Deferred** to M3 | The first two are immutable value types whose methods are mostly accessors. `TellerWebServer` does routing, form parsing and HTML rendering in one class, which is a real cohesion smell: split it at M3. DEF-004 |
| SA-07 | AvoidLiteralsInIfCondition | 7 | `InstalmentDue` (`sequence < 1`), `TellerWebServer` (5: paths and HTTP methods in `route`, `endOfDay`), `SqliteLoanRepository.save` (`executeUpdate() != 1`) | **Deferred** to M3 | Name the routes and HTTP methods as constants when `TellerWebServer` is split (SA-06), and name `FIRST_SEQUENCE` if INS-01 agrees. "Exactly one row updated" is self-explanatory, but it is changed with the rest for consistency. DEF-005 |
| SA-08 | AvoidFieldNameMatchingMethodName | 34 | Value and exception types in `ledger` and `common` | **False positive** | Record-style accessors (`amount()` returning field `amount`) are the project's deliberate convention for immutable types, the same convention Java records use |
| SA-09 | SimplifyBooleanReturns | 2 | `LoanId.equals`, `Money.equals` | **False positive** | This is the conventional `equals` shape (identity check, type check, then compare). The rewrite PMD suggests is harder to read and changes no behaviour |
| SA-10 | AvoidDuplicateLiterals | 2 | `SchemaMigrator` DDL (`" id INTEGER PRIMARY KEY,"`, `" loan_id TEXT NOT NULL REFERENCES loan (id),"`) | **False positive** | The DDL is kept literal so that each table definition can be read and inspected against the schema in the M1 design (section 3.7). Constants would hide the schema |
| SA-11 | UseConcurrentHashMap | 3 | `SqliteLedgerRepository.readAll`, `RepaymentAllocator.allocate`, `TellerWebServer.readForm` | **False positive** | All three maps are local variables, created and discarded inside one call and never shared between threads. The map that **is** shared, the duplicate-reference store in `InMemoryPaymentReferences`, already uses `ConcurrentHashMap.newKeySet()`, and its claim is atomic |
| SA-12 | AvoidUsingVolatile, AvoidSynchronizedStatement | 2 | `SqliteDatabase` (`migrated` flag, `ensureMigrated`) | **False positive** | This is the correct double-checked locking idiom: a `volatile` flag plus a `synchronized` block, so the schema is migrated exactly once even if several end-of-day workers connect at the same moment (concurrency strategy, M1 section 3.6). The per-loan `ReentrantLock` registry is a separate mechanism for posting |
| SA-13 | LawOfDemeter | 4 | `TellerWebServer` (`exchange.getRequestURI().getPath()`, `server.getAddress().getAddress()`, `exchange.getResponseHeaders()`) | **False positive** | These are calls into the JDK `HttpServer`/`HttpExchange` API, which is designed to be navigated this way. Wrapping it would add a layer with no behaviour |
| SA-14 | DataClass | 1 | `CompositionRoot` | **False positive** | A composition root's job is to build objects and hand out the wired chains (teller and import posters). Having accessors and no business logic is the intended design, not a misplaced-behaviour smell |

| Disposition | IDs | Findings |
|---|---|---|
| Fixed | SA-01, SA-02 | 3 + 1 = 4 |
| Accepted | SA-03, SA-04 | 1 + 1 = 2 |
| Deferred | SA-05, SA-06, SA-07 | 2 + 3 + 7 = 12 |
| False positive | SA-08 to SA-14 | 34 + 2 + 2 + 3 + 2 + 4 + 1 = 48 |
| **Total** | | **66** |

## 4. Significant findings linked to corrective action

| Finding | Defect | CAPA | Owner | Due | Verification |
|---|---|---|---|---|---|
| SA-01 | DEF-001 | CAPA-002: use `isBlank()`; the INS-01 checklist covers blank-text checks | Tughral Hussain | before `m2-baseline` | Done locally in `f359765`: PMD re-run has 0 InefficientEmptyStringCheck; 182 tests pass. CI on push still to be recorded |
| SA-02 | DEF-002 | CAPA-002: check `ResultSet.next()` | Tughral Hussain | before `m2-baseline` | Done locally in `87c5caf`: PMD re-run has 0 CheckResultSet; `SchemaMigratorTest` passes. CI on push still to be recorded |
| SA-03 | DEF-003 | CAPA-003: stub replaced by L-17/L-18 | Tughral Hussain | M2 build | Done in `2bb5355`: SystemPrintln went from 9 to 1, and the remaining line is accepted |
| SA-05 to SA-07 | DEF-004, DEF-005 | CAPA-003: M3 refactoring with before/after metrics | Tughral Hussain | M3 (Week 13) | PMD and JaCoCo complexity before and after; tests unchanged and passing |

## 5. Not yet analysed

`origination`, `products`, `delinquency` and `reporting` have no code yet, and Hassan's `DueDateCalculator` is in open PR #6. They will be analysed when they reach `develop`. Recommendation for M3: wire PMD into `build.gradle` as a CI step with an agreed rule set, so findings are triaged per pull request rather than once per milestone.
