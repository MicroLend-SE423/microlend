# MicroLend - Static Analysis Report (M2)

**Deliverable:** SE431 M2, "Static analysis report" · **Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Code analysed:** main source set.
- First run: `c80b9c9` on `feature/ledger-entry` (14 Nov 2026, part-way through the ledger queue).
- Ledger run: `e0f9ecb` (whole ledger queue L-01 to L-19): `common`, `ledger`, `persistence`, `web`.
- After fixes: `701c161`; a re-run after PR #8 merged into `develop` (21 Nov) gives the same 62 findings.
- M2 integrated runs on `feature/delinquency-origination-import`, 21 Nov 2026:
  - `7784a6a` adds `origination`, `delinquency`, `ledger.importing`, `common.LockRegistry`, `ledger.LockingPoster`, `persistence.SqliteDailyResultStore`, schema version 2 in `SchemaMigrator`, and end-of-day changes to `CompositionRoot`, `MicroLendApplication` and `TellerWebServer`.
  - `4067d2f`, the code head triaged below, adds the independent code-review fixes (`delinquency.DailySnapshot`, `ledger.importing.StatementText`; changes to `PenaltyAccruer`, `EndOfDayEngine`, `SqliteDailyResultStore`, both adapters and `TellerWebServer`) and the durable reference store `persistence.SqlitePaymentReferences` (table in `SchemaMigrator`, wiring in `CompositionRoot`).

Finding IDs are stable between runs; the 21 Nov runs add SA-15 onward.

## 1. Tools and configuration

| Tool | Version | How it runs | Rules | Gate |
|---|---|---|---|---|
| Checkstyle | 10.18.2 | Gradle plugin, every CI run (`.\gradlew.bat checkstyleMain checkstyleTest`) | `config/checkstyle.xml`: project coding standard, including **C-01** (IllegalType `double`/`float`, IllegalToken `NUM_DOUBLE`/`NUM_FLOAT`), Javadoc on public API, final newline | `maxWarnings = 0`, `ignoreFailures = false`: any finding fails the build |
| PMD | 7.7.0 | Gradle `pmd` plugin applied from an init script, so `build.gradle` is unchanged (command below) | Java categories: best practices, error prone, design, multithreading, security, performance | Report only at M2 (`ignoreFailures = true`) |

```powershell
.\gradlew.bat --init-script config/pmd-init.gradle pmdMain checkstyleMain checkstyleTest
# reports: build/reports/pmd/main.xml|html, build/reports/checkstyle/main.xml|test.xml
```

`config/pmd-init.gradle` sets `toolVersion = '7.7.0'`, the six categories and `ignoreFailures = true`. PMD was added for M2 because Checkstyle, the existing gate, checks only form; PMD finds error-prone and design problems.

## 2. Results

| Run | Commit | Checkstyle | PMD findings |
|---|---|---|---|
| First | `c80b9c9` | 0 | 53 |
| Ledger code | `e0f9ecb` | 0 | 66 |
| After fixes | `701c161` | 0 | 62 |
| M2 integrated build | `7784a6a` | 0 | 98 |
| M2 integrated build after the review fixes | `4067d2f` | 0 | **100** |

Disposition of the 66 ledger-code findings:

| Disposition | Findings |
|---|---|
| Fixed | 4 |
| Accepted | 2 |
| Deferred | 12 |
| False positive | 48 |

**21 Nov runs.** The rise from 62 to 100 is 39 new findings less 1 old SA-07 literal in `TellerWebServer.endOfDay` that is no longer reported (see SA-07). 37 of the 39 appeared at `7784a6a`; the other 2 are the `CLAIM` and `RELEASE` constants of `SqlitePaymentReferences` at `4067d2f` (SA-17). The review fixes added or removed nothing else; only line numbers moved. Triage of the 39 (SA-15 to SA-28):

| Disposition (new findings, 21 Nov) | Findings |
|---|---|
| Fixed | 0 (code is not changed by this report) |
| Accepted | 8 |
| Deferred | 9 |
| False positive | 22 |
| **Total new** | **39** |

Open at `4067d2f`: 61 carried over from the ledger triage plus the 39 new:

| Disposition (open at `4067d2f`) | Findings |
|---|---|
| Accepted | 2 + 8 = 10 |
| Deferred | 11 + 9 = 20 |
| False positive | 48 + 22 = 70 |
| **Total** | **100** |

Checkstyle reports zero because it is a gate: `./gradlew build` on `4067d2f` passed `checkstyleMain` and `checkstyleTest` with `maxWarnings = 0`, as did every `feature/ledger-entry` commit in CI (`qa/scm-evidence.md` section 3.2), so C-01 holds: no `double` or `float`, new modules included.

PMD reports no `CyclomaticComplexity` finding in the new code; its three (SA-04, SA-05) are ledger methods. The most complex new method by JaCoCo is the compact constructor of `origination.Applicant` (12: five validation guards and one conditional expression, 11 simple conditions), below the QR-04 limit of 15.

## 3. Triage

Disposition key: **Fixed**, a real defect or weakness corrected through a CAPA in `qa/defect-log.md`; **Accepted**, real but kept on purpose; **Deferred**, real and tracked to a later change; **False positive**, the rule does not apply to this design. Every row records its reason so the decision can be checked.

| ID | PMD rule | Count | Location | Disposition | Reasoning / action |
|---|---|---|---|---|---|
| SA-01 | InefficientEmptyStringCheck | 3 | `LedgerEntry.reversalOf`, `RepaymentCommand.requireText`, `AuditLoggingPoster` (operator name) | **Fixed** in `e3f585d` | `value.trim().isEmpty()` made a trimmed copy only to test for blank text. It now uses `String.isBlank()` (Java 11+). Behaviour is the same, and the existing tests pass. DEF-001, CAPA-002 |
| SA-02 | CheckResultSet | 1 | `SchemaMigrator.currentVersion` | **Fixed** in `701c161` | `rows.next()` was ignored. `PRAGMA user_version` always returns one row in SQLite, but if none came back, the code would have read an empty cursor and failed with a raw `SQLException`. It now raises an explicit error that `migrate()` wraps in `PersistenceException`. DEF-002, CAPA-002 |
| SA-03 | SystemPrintln | 1 (was 9) | `MicroLendApplication.main` | **Accepted** | L-17/L-18 replaced the M1 console stub, which removed 8 of the 9 findings (DEF-003). The one remaining line prints the local address of the teller page once at start-up. That message is meant for the operator at the console, so it is not diagnostic logging |
| SA-04 | CyclomaticComplexity | 1 | `RepaymentAllocator.allocate` (PMD 13; McCabe 10) | **Accepted** | Kept as one method on purpose: it is the Fagan inspection target, and the queue requires it to read top to bottom. PMD counts the 3 `throw` statements as extra decisions. Its graph complexity of 10 is at, not above, McCabe's commonly used limit of 10, and all 18 branches are covered (`qa/test-design/white-box.md`). Re-assessed after inspection INS-01 |
| SA-05 | CyclomaticComplexity | 2 | `LoanAccount.restore` (13; McCabe 8), `LedgerEntry.requireValid` (11; McCabe 6) | **Deferred** to M3 refactoring | These are sequential guard clauses, each with its own message. They are a candidate for the M3 "refactoring and code smells" evidence: extract a validation helper and measure before and after. DEF-004 |
| SA-06 | TooManyMethods | 3 | `LedgerEntry`, `RepaymentCommand`, `TellerWebServer` | **Deferred** to M3 | The first two are immutable value types whose methods are mostly accessors. `TellerWebServer` does routing, form parsing and HTML rendering in one class, which is a real cohesion smell: split it at M3. DEF-004 |
| SA-07 | AvoidLiteralsInIfCondition | 7 (6 from 21 Nov) | `InstalmentDue` (`sequence < 1`), `TellerWebServer` (5: paths and HTTP methods in `route`, `endOfDay`; 4 from 21 Nov, all in `route`), `SqliteLoanRepository.save` (`executeUpdate() != 1`) | **Deferred** to M3 | Name the routes and HTTP methods as constants when `TellerWebServer` is split (SA-06), and name `FIRST_SEQUENCE` if INS-01 agrees. "Exactly one row updated" is self-explanatory, but it is changed with the rest for consistency. DEF-005. On 21 Nov the `endOfDay` literal was no longer reported, because that handler was rewritten to run the engine |
| SA-08 | AvoidFieldNameMatchingMethodName | 34 | Value and exception types in `ledger` and `common` | **False positive** | Record-style accessors (`amount()` returning field `amount`) are the project's deliberate convention for immutable types, the same convention Java records use |
| SA-09 | SimplifyBooleanReturns | 2 | `LoanId.equals`, `Money.equals` | **False positive** | This is the conventional `equals` shape (identity check, type check, then compare). The rewrite PMD suggests is harder to read and changes no behaviour |
| SA-10 | AvoidDuplicateLiterals | 2 | `SchemaMigrator` DDL (`" id INTEGER PRIMARY KEY,"`, `" loan_id TEXT NOT NULL REFERENCES loan (id),"`) | **False positive** | The DDL is kept literal so that each table definition can be read and inspected against the schema in the M1 design (section 3.7). Constants would hide the schema |
| SA-11 | UseConcurrentHashMap | 3 | `SqliteLedgerRepository.readAll`, `RepaymentAllocator.allocate`, `TellerWebServer.readForm` | **False positive** | All three maps are local variables, created and discarded inside one call and never shared between threads. The map that **is** shared, the duplicate-reference store in `InMemoryPaymentReferences`, already uses `ConcurrentHashMap.newKeySet()`, and its claim is atomic. Since `4067d2f` production uses `SqlitePaymentReferences` instead, whose claim is made atomic by the table's primary key |
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

### 3.1 New findings in the M2 integrated build (`4067d2f`, 21 Nov 2026)

Paths under `src/main/java/pk/edu/giki/microlend/`; line numbers from the PMD report at `4067d2f`.

| ID | PMD rule | Count | Location | Disposition | Reasoning / action |
|---|---|---|---|---|---|
| SA-15 | SystemPrintln | 1 | `MicroLendApplication.main`, line 49 (the `import` command-line mode) | **Accepted** | The command-line import prints one result line: rows posted and references refused as duplicates. It is meant for the operator, so the reason given for SA-03 applies here too |
| SA-16 | AvoidLiteralsInIfCondition | 6 | `CompositionRoot.importStatement` 136, 138 (`"HBL"`, `"MCB"`); `ledger/importing/HblStatementAdapter.isCredit` 117, 120 (`"CR"`, `"DR"`); `origination/LoanApplication` 38 (`tenureMonths < 1`); `persistence/SchemaMigrator.apply` 172 (`fromVersion < 1`) | **Deferred** to M3, with SA-07 | Real but minor. The bank codes repeat the adapters' own `BANK` constants, and `importStatement` should use those. `"CR"`/`"DR"` are the HBL file's direction codes, and `1` is a lower bound. Name them in the M3 refactoring pass. DEF-017 |
| SA-17 | AvoidFieldNameMatchingMethodName | 10 | `CompositionRoot.endOfDayEngine`; `ledger/importing/InvalidStatementException` (`sourceName`, `line`); `origination/Decision` (`grade`, `approvedAmount`); `origination/IneligibleApplicantException.outcome`; `origination/RiskGrade.spreadPercent`; `persistence/SqliteDailyResultStore.EXISTS`; `persistence/SqlitePaymentReferences` (`CLAIM`, `RELEASE`, new at `4067d2f`) | **False positive** | The first seven are record-style accessors, the project convention described in SA-08. `EXISTS`, `CLAIM` and `RELEASE` are SQL text constants, and `exists(...)`, `claim(...)` and `release(...)` are methods. Java naming separates a constant from a method, and the two never meet at a call site |
| SA-18 | AvoidFieldNameMatchingMethodName, AvoidDuplicateLiterals, ExcessiveImports | 3 | `web/TellerWebServer`: field `endOfDay` (the engine runner) next to the handler method `endOfDay(...)`, line 52; form field name `"businessDate"` written 4 times, line 125; the class's import count, line 1 | **Deferred** to M3, with SA-06 | All three come from the class doing routing, form parsing and rendering at once, the cohesion smell in SA-06. When it is split, the field is renamed, the form field names become constants that the form and the parser share, and the imports divide between the new classes. DEF-017 |
| SA-19 | AvoidDuplicateLiterals | 1 | `web/TellerWebServer`, `"</p>"` (7 times), line 129 | **False positive** | This is an HTML closing tag inside inline markup. Naming it as a constant would make the pages harder to read and would not remove a real duplication of meaning |
| SA-20 | UseConcurrentHashMap | 2 | `delinquency/EndOfDayEngine.run` 87 (`tasks`), `EndOfDayEngine.collect` 139 (`failures`) | **False positive** | Both maps are local to the thread that calls `run`. Workers never touch them: each task returns its result through its `Future`, and the calling thread fills both maps. `LinkedHashMap` is chosen to keep loan order in the report. The state the workers do share (`LockRegistry`, the database) is thread-safe |
| SA-21 | DoNotUseThreads | 6 | `delinquency/EndOfDayEngine` 85, 89, 93, 148 (pool, `submit`, `shutdownNow`, re-interrupt); `web/TellerWebServer` 55, 88 (request pool) | **False positive** | The rule protects Java EE containers, which manage threads themselves. MicroLend is a stand-alone Java SE application. The four-thread pool is the required design (M1 section 3.6, QR-01), and the 4 request threads let tellers at several counters post at once |
| SA-22 | CloseResource | 1 | `delinquency/EndOfDayEngine.run`, line 85 (`ExecutorService`) | **Accepted** | The pool is released by `shutdownNow()` in a `finally` block. On the normal path every task's `Future` has completed before that call. `close()` (Java 19+) would instead wait for termination, and on the interrupt path that would block the caller that was interrupted |
| SA-23 | CouplingBetweenObjects | 1 | `delinquency/EndOfDayEngine` (26 types; threshold 20) | **Accepted** | The engine is the end-of-day orchestrator, so it meets four calculators, the store, the lock registry and the ledger types. Most of the 26 types are JDK types (`Clock`, `Instant`, `LocalDate`, `ExecutorService`, `Future`, collections). Re-assessed at M3 together with QR-01 |
| SA-24 | ExcessiveImports | 1 | `CompositionRoot` | **False positive** | The composition root's job is to know every concrete class it wires (M1 section 4.5), the same reason SA-14 was triaged as a false positive. No other class needs these imports |
| SA-25 | BigIntegerInstantiation | 2 | `delinquency/PenaltyAccruer` 27 (`new BigDecimal("10")`), `delinquency/ProvisioningPolicy` 15 (`new BigDecimal("1")`) | **Accepted** | Each is a `static final` constant created once, so the allocation cost the rule targets does not occur. The percentages are written as string literals, like their neighbours (`"0.05"`, `"25"`, `"50"`), so each table reads as BR-28 and BR-31 state it. `BigDecimal.TEN` and `ONE` would be equal in value |
| SA-26 | ConsecutiveLiteralAppends (2), InsufficientStringBufferDeclaration (1) | 3 | `web/TellerWebServer.eodSummary`, lines 184 to 190 | **Accepted** | The end-of-day summary is built once per run on a local, single-user page. One `append` per label and value keeps the markup readable. Pre-sizing the builder or merging literals saves a few bytes, which nobody would notice |
| SA-27 | LooseCoupling | 1 | `common/LockRegistry`, line 19 (field typed `ConcurrentHashMap`) | **False positive** | The concrete type is deliberate. `ConcurrentHashMap.computeIfAbsent` guarantees the mapping function runs at most once per key, atomically, so every caller gets the same `ReentrantLock` for an account. The `Map` and `ConcurrentMap` interfaces do not promise that, and the per-account lock (M1 section 3.6, QR-06) depends on it |
| SA-28 | AvoidInstantiatingObjectsInLoops | 1 | `ledger/importing/McbStatementAdapter.nextBatch`, line 96 (`new BigDecimal(amount)`) | **False positive** | Each statement row carries one amount, so one `BigDecimal` per row is the minimum possible. There is nothing to hoist out of the loop |

| Disposition (21 Nov) | IDs | Findings |
|---|---|---|
| Accepted | SA-15, SA-22, SA-23, SA-25, SA-26 | 1 + 1 + 1 + 2 + 3 = 8 |
| Deferred | SA-16, SA-18 | 6 + 3 = 9 |
| False positive | SA-17, SA-19, SA-20, SA-21, SA-24, SA-27, SA-28 | 10 + 1 + 2 + 6 + 1 + 1 + 1 = 22 |
| **Total new** | | **39** |

## 4. Significant findings linked to corrective action

| Finding | Defect | CAPA | Owner | Due | Verification |
|---|---|---|---|---|---|
| SA-01 | DEF-001 | CAPA-002: use `isBlank()`; the INS-01 checklist covers blank-text checks | Tughral Hussain | before `m2-baseline` | Verified: `e3f585d`, PMD re-run has 0 InefficientEmptyStringCheck; 182 tests pass; CI run 43829233377 passed |
| SA-02 | DEF-002 | CAPA-002: check `ResultSet.next()` | Tughral Hussain | before `m2-baseline` | Verified: `701c161`, PMD re-run has 0 CheckResultSet; `SchemaMigratorTest` passes; CI run 43829233377 passed |
| SA-03 | DEF-003 | CAPA-003: stub replaced by L-17/L-18 | Tughral Hussain | M2 build | Done in `9db0657`: SystemPrintln went from 9 to 1, and the remaining line is accepted |
| SA-05 to SA-07 | DEF-004, DEF-005 | CAPA-003: M3 refactoring with before/after metrics | Tughral Hussain | M3 (Week 13) | PMD and JaCoCo complexity before and after; tests unchanged and passing |
| SA-16, SA-18 | DEF-017 | CAPA-003: named constants and the `TellerWebServer` split, in the same M3 refactoring | Muhammad Ibrahim (SA-16), Tughral Hussain (SA-18, `web` owner) | M3 (Week 13) | PMD re-run shows 0 findings for SA-16 and SA-18; tests unchanged and passing |

## 5. Not yet analysed

`products` and `reporting` have no code on any branch except Hassan's `DueDateCalculator` in open PR #6; they are analysed when they reach `develop`. M3 recommendation: wire PMD into `build.gradle` as a CI step with an agreed rule set, so findings are triaged per pull request.
