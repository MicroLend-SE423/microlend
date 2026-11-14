# MicroLend - Static Analysis Report (M2)

**Deliverable:** SE431 M2, "Static analysis report" Â· **Document lead:** to be assigned (M2 leads not yet chosen)
**Code analysed:** `feature/ledger-entry` at `923755b` (14 Nov 2026): `common`, `ledger`, `persistence` (main source set).
Re-run on the final M2 build before submission; the finding IDs stay stable and new findings get new IDs.

## 1. Tools and configuration

| Tool | Version | How it runs | Rules | Gate |
|---|---|---|---|---|
| Checkstyle | 10.18.2 | Gradle plugin, every CI run (`.\gradlew.bat checkstyleMain checkstyleTest`) | `config/checkstyle.xml`: project coding standard, including **C-01** (IllegalType `double`/`float`, IllegalToken `NUM_DOUBLE`/`NUM_FLOAT`), Javadoc on public API, final newline | `maxWarnings = 0`, `ignoreFailures = false`: any finding fails the build |
| PMD | 7.7.0 | Gradle `pmd` plugin applied from an init script, so `build.gradle` is unchanged (command below) | Java categories: best practices, error prone, design, multithreading, security, performance | Report only at M2 (`ignoreFailures = true`) |

```powershell
.\gradlew.bat --init-script pmd-init.gradle pmdMain checkstyleMain checkstyleTest
# reports: build/reports/pmd/main.xml|html, build/reports/checkstyle/main.xml|test.xml
```

Checkstyle is the gate that is already enforced. PMD was added for M2 because Checkstyle only checks form, while PMD finds error-prone and design problems.

## 2. Results

| Tool | Findings | Fixed | Fix scheduled | Deferred | False positive / accepted |
|---|---|---|---|---|---|
| Checkstyle (main + test) | **0** | - | - | - | - |
| PMD (main) | **53** | 0 | 3 | 14 | 36 (35 false positive, 1 accepted) |

Checkstyle reports zero because it is a CI gate: every commit on `feature/ledger-entry` passed it (see `qa/scm-evidence.md`, section 3.2). In particular, constraint C-01 holds: there is no `double` or `float` in the code base.

## 3. Triage

Disposition key:
- **Fix**: a real defect or weakness, corrected with a CAPA in `qa/defect-log.md`.
- **Deferred**: real, but the code it concerns is scheduled to change. The finding is tracked to that change.
- **False positive**: the rule does not apply to this design. The reason is recorded so the decision can be checked later.

| ID | PMD rule | Count | Location | Disposition | Reasoning / action |
|---|---|---|---|---|---|
| SA-01 | InefficientEmptyStringCheck | 2 | `LedgerEntry.java:69` (`reversalOf`), `RepaymentCommand.java:96` (`requireText`) | **Fix** | `value.trim().isEmpty()` allocates a trimmed copy only to test for blank text. Use `String.isBlank()` (Java 11+). Behaviour is the same, so the existing tests cover it. DEF-001, CAPA-002 |
| SA-02 | CheckResultSet | 1 | `SchemaMigrator.java:121` (`currentVersion`) | **Fix** | `rows.next()` is ignored. `PRAGMA user_version` always returns one row in SQLite, but if the driver ever returned none, the code would read a closed cursor and fail with a raw `SQLException` instead of a clear `PersistenceException`. Check the result and throw `PersistenceException`. DEF-002, CAPA-002 |
| SA-03 | SystemPrintln | 9 | `MicroLendApplication.java:29-37` | **Deferred** to L-17/L-18 | This is the M1 console stub. The composition root (L-17) and local web interface (L-18) replace it. Diagnostic output then goes through `java.util.logging`, which is in the JDK and costs nothing |
| SA-04 | CyclomaticComplexity | 1 | `RepaymentAllocator.allocate` (PMD 13; McCabe 10) | **Accepted** | Kept as one method on purpose: it is the Fagan inspection target, and the queue requires it to read top to bottom. PMD counts the 3 `throw` statements as extra decisions. Its graph complexity of 10 is at, not above, McCabe's commonly used limit of 10, and all 18 branches are covered (`qa/test-design/white-box.md`). Re-assessed after inspection INS-01 |
| SA-05 | CyclomaticComplexity | 2 | `LoanAccount.restore` (13; McCabe 8), `LedgerEntry.requireValid` (11; McCabe 6) | **Deferred** to M3 refactoring | These are sequential guard clauses, each with its own message. They are a candidate for the M3 "refactoring and code smells" evidence: extract a validation helper and measure before and after |
| SA-06 | TooManyMethods | 2 | `LedgerEntry`, `RepaymentCommand` | **Deferred** to M3 | These are immutable value types whose methods are mostly accessors. They are reviewed together with SA-05 at M3 |
| SA-07 | AvoidLiteralsInIfCondition | 1 | `InstalmentDue.java:32` (`sequence < 1`) | **Deferred** | The literal `1` is the domain rule "instalments are numbered from 1". If the INS-01 inspection agrees, name it `FIRST_SEQUENCE` |
| SA-08 | AvoidFieldNameMatchingMethodName | 27 | Value types: `Allocation`, `LedgerEntry`, `LoanAccount`, `LoanId`, `RepaymentCommand`, `LoanClosedException`, `InvalidRepaymentException` | **False positive** | Record-style accessors (`amount()` returning field `amount`) are the project's deliberate convention for immutable types, the same convention Java records use |
| SA-09 | SimplifyBooleanReturns | 2 | `LoanId.equals`, `Money.equals` | **False positive** | This is the conventional `equals` shape (identity check, type check, then compare). The rewrite PMD suggests is harder to read and changes no behaviour |
| SA-10 | AvoidDuplicateLiterals | 2 | `SchemaMigrator` DDL (`" id INTEGER PRIMARY KEY,"`, `" loan_id TEXT NOT NULL REFERENCES loan (id),"`) | **False positive** | The DDL is kept literal so that each table definition can be read and inspected against the schema in the M1 design (section 3.7). Constants would hide the schema |
| SA-11 | UseConcurrentHashMap | 2 | `SqliteLedgerRepository.java:112`, `RepaymentAllocator.java:77` | **False positive** | Both maps are local variables, created and discarded inside one call and never shared between threads. `LinkedHashMap` is needed to keep insertion order |
| SA-12 | AvoidUsingVolatile, AvoidSynchronizedStatement | 2 | `SqliteDatabase` (`migrated` flag, `ensureMigrated`) | **False positive** | This is the correct double-checked locking idiom: a `volatile` flag plus a `synchronized` block, so the schema is migrated exactly once even if several end-of-day workers connect at the same moment (concurrency strategy, M1 section 3.6). The per-loan `ReentrantLock` registry is a separate mechanism for posting |

| Disposition | IDs | Findings |
|---|---|---|
| Fix | SA-01, SA-02 | 3 |
| Deferred | SA-03, SA-05, SA-06, SA-07 | 9 + 2 + 2 + 1 = 14 |
| Accepted | SA-04 | 1 |
| False positive | SA-08 to SA-12 | 27 + 2 + 2 + 2 + 2 = 35 |
| **Total** | | **53** |

## 4. Significant findings linked to corrective action

| Finding | Defect | CAPA | Owner | Due | Verification |
|---|---|---|---|---|---|
| SA-01 | DEF-001 | CAPA-002: replace `trim().isEmpty()` with `isBlank()`; add a PMD rule check to the review checklist (INS-01 checklist, validation section) | Tughral Hussain | before `m2-baseline` | PMD re-run shows 0 InefficientEmptyStringCheck; `LedgerEntryTest`, `RepaymentCommandTest` pass |
| SA-02 | DEF-002 | CAPA-002: check `ResultSet.next()` and fail with `PersistenceException` | Tughral Hussain | before `m2-baseline` | PMD re-run shows 0 CheckResultSet; `SchemaMigratorTest` passes |
| SA-03 | DEF-003 | CAPA-003: removed by L-17/L-18 | Tughral Hussain | M2 build | PMD re-run shows 0 SystemPrintln |

## 5. Not yet analysed

`origination`, `products`, `delinquency` and `reporting` have no code yet, and Hassan's `DueDateCalculator` is in open PR #6. They will be analysed when they reach `develop`. Recommendation for M3: wire PMD into `build.gradle` as a CI step with an agreed rule set, so findings are triaged per pull request rather than once per milestone.
