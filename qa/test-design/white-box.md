# MicroLend - White-Box Test Design (M2)

**Deliverable:** SE431 M2, "White-box test design" · **Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Code baseline:** `feature/ledger-entry` at `701c161` (14 Nov 2026): the whole ledger queue (L-01 to L-19) plus the two static-analysis fixes. Re-check against the M2 build before submission.
**Tools:** control-flow graphs drawn by hand from the source; complexity cross-checked with JaCoCo 0.8 (`build/reports/jacoco/test/jacocoTestReport.xml`, `COMPLEXITY` counter) and PMD 7.7.0 (`CyclomaticComplexity` rule).

## 1. Method

1. Draw the control-flow graph (CFG) of the method. Each node is a block of statements with one entry and one exit. Compound conditions (`a || b`) are split into one decision node per simple condition, because each one is a separate branch in the bytecode and in the coverage report.
2. Compute cyclomatic complexity three ways and check that they agree:
   - `V(G) = E - N + 2` (edges, nodes, one connected component);
   - `V(G) = P + 1`, where P is the number of simple decision nodes;
   - JaCoCo's `COMPLEXITY` counter for the method.
3. Derive V(G) linearly independent basis paths. Start from a typical baseline path, then flip one decision at a time.
4. Give each feasible path a concrete test case, with inputs and the expected result, and name the automated test that executes it. Infeasible paths are stated and explained, not hidden.

### Selection of methods

These methods were chosen because they are on the money path and have the highest complexity on it. They are also the Fagan inspection target (`qa/inspections/INS-01-repayment-path/`).

| Method | Requirement | Why it is "key" |
|---|---|---|
| `RepaymentAllocator.allocate(Money, List<InstalmentDue>)` | FR-13, FR-14 (BR-18, BR-19, BR-20) | Decides where every rupee of every repayment goes; an error here corrupts penalties, ageing and provisioning downstream (risk R1) |
| `LoanAccount.restore(LoanId, LoanStatus, Money, LocalDate)` | FR-19 (BR-26) | Guards the status/closure-date invariant every repository load relies on |
| `LedgerEntry.requireValid(LoanId, EntryType, Money, Instant)` | FR-17 (BR-23, BR-24) | Gate for every ledger line; a zero or incomplete entry breaks the balanced-ledger invariant |
| `CoreRepaymentPoster.post(RepaymentCommand)` | FR-16, FR-19, QR-03 (BR-21, BR-23, BR-24, BR-26) | The one place a repayment changes both the ledger and the loan; its failure paths decide whether the ledger stays balanced (section 5) |

### Complexity summary

| Method | P (simple decisions) | E | N | V(G) = E - N + 2 | JaCoCo | PMD 7.7.0 | Branch coverage (JaCoCo) |
|---|---|---|---|---|---|---|---|
| `RepaymentAllocator.allocate` | 9 | 26 | 18 | **10** | 10 | 13 | 18 / 18 |
| `LoanAccount.restore` | 7 | 22 | 16 | **8** | 8 | 13 | 14 / 14 |
| `LedgerEntry.requireValid` | 5 | 16 | 12 | **6** | 6 | 11 | 10 / 10 |
| `CoreRepaymentPoster.post` | 5 (4 are exception exits) | 16 | 12 | **6** | 2 | below threshold | 2 / 2 |

PMD's figure is higher by exactly the number of `throw` statements in each method (3, 5 and 5). That is consistent with PMD counting each `throw` as an extra decision. The McCabe value from the graph, which JaCoCo confirms, is the one used for basis paths. The M1 RTM estimated "CC about 11" for the allocator, and the built method came in at 10.

---

## 2. `RepaymentAllocator.allocate`

Source: `src/main/java/pk/edu/giki/microlend/ledger/RepaymentAllocator.java`. Tier-by-tier reading of BR-18/BR-19, team decision D-01 (16 Nov 2026).

### 2.1 Nodes

| Node | Statement(s) | Decision |
|---|---|---|
| N1 | `if (payment == null` | P1 |
| N2 | second operand of the OR: `owing == null` | P2 |
| N3 | `throw` "a payment and a list ... are required" | |
| N4 | create `sequences`; `for (instalment : owing)`, has next? | P3 |
| N5 | `if (instalment == null)` | P4 |
| N6 | `throw` "... has a gap" | |
| N7 | `if (!sequences.add(instalment.sequence()))` | P5 |
| N8 | `throw` "instalment n appears twice" | |
| N9 | sort oldest first; create `applied`; `remaining = payment`; `tier < TIERS.size()`? | P6 |
| N10 | `i < oldestFirst.size()`? | P7 |
| N11 | `owed = tier(i)`; `remaining.compareTo(owed) < 0`? | P8 |
| N12 | `paid = remaining` (partial, BR-20) | |
| N13 | `paid = owed` (tier cleared) | |
| N14 | `applied[i][tier] = paid; remaining -= paid; i++` | |
| N15 | build `bySequence`, `total`; `i < oldestFirst.size()`? | P9 |
| N16 | add the instalment's `Allocation` and accumulate the total | |
| N17 | `return new AllocationResult(total, bySequence, remaining)` | |
| N18 | exit | |

### 2.2 CFG

```mermaid
flowchart TD
    N1{"N1 payment == null"} -->|T| N3["N3 throw: required"]
    N1 -->|F| N2{"N2 owing == null"}
    N2 -->|T| N3
    N2 -->|F| N4{"N4 next instalment?"}
    N4 -->|yes| N5{"N5 instalment == null"}
    N5 -->|T| N6["N6 throw: gap"]
    N5 -->|F| N7{"N7 duplicate sequence"}
    N7 -->|T| N8["N8 throw: appears twice"]
    N7 -->|F| N4
    N4 -->|no| N9{"N9 tier < 4"}
    N9 -->|T| N10{"N10 i < size"}
    N10 -->|T| N11{"N11 remaining < owed"}
    N11 -->|T| N12["N12 paid = remaining"]
    N11 -->|F| N13["N13 paid = owed"]
    N12 --> N14["N14 record, subtract, i++"]
    N13 --> N14
    N14 --> N10
    N10 -->|F, tier++| N9
    N9 -->|F| N15{"N15 i < size"}
    N15 -->|T| N16["N16 add Allocation, total"]
    N16 --> N15
    N15 -->|F| N17["N17 return result"]
    N3 --> N18(("N18 exit"))
    N6 --> N18
    N8 --> N18
    N17 --> N18
```

E = 26, N = 18, so **V(G) = 26 - 18 + 2 = 10**, which equals P + 1 = 9 + 1.

### 2.3 Basis paths and test cases

The baseline is BP-1: one instalment, and a payment that clears it exactly. Each later path flips one decision from a path already listed.

| Path | Route | Decision flipped | Test input (money in PKR) | Expected result | Closest automated test in `RepaymentAllocatorTest` |
|---|---|---|---|---|---|
| BP-1 | N1F N2F N4 N5F N7F N4 N9 N10 N11F N13 N14 ... N9F N15T N16 N15F N17 | baseline | 1 instalment (penalty 10.00, fees 0.00, interest 50.00, principal 200.00); payment 260.00 | each tier paid in full; unapplied 0.00 | `conservesThePayment`, `principalLast` |
| BP-2 | N1T N3 | P1 | payment `null`, 1 instalment | `InvalidAllocationException` | `Validation.rejectsMissingInput` |
| BP-3 | N1F N2T N3 | P2 | payment 100.00, list `null` | `InvalidAllocationException` | `rejectsMissingInput` |
| BP-4 | N1F N2F N4 N5T N6 | P4 | list `[null]` | `InvalidAllocationException` ("gap") | `rejectsMissingInput` |
| BP-5 | ... N7F N4 N5F N7T N8 | P5 | two instalments, both sequence 1 | `InvalidAllocationException` ("appears twice") | `rejectsDuplicateSequence` |
| BP-6 | N1F N2F N4(no) N9 N10F ... N9F N15F N17 | P3, P7 and P9 at zero iterations | payment 100.00, empty list | nothing allocated; unapplied 100.00 | `nothingOwed` |
| BP-7 | ... N11T N12 N14 ... | P8 | 1 instalment as BP-1; payment 30.00 | penalty 10.00, interest 20.00, principal 0.00; unapplied 0.00 | `partOfEachTier` |
| BP-8 | baseline with N10T and N15T taken twice per pass | P7, P9 (second iteration) | 2 instalments, due 10 Oct and 10 Nov (each penalty 5.00, interest 20.00, principal 100.00); payment 30.00 | both penalties 5.00 + 5.00 first, then 20.00 interest on the oldest; newest interest 0.00 | `twoOverdueTierByTier` |
| BP-9 | as BP-8, but the list is passed newest first | sort order feeding P7 | 3 instalments given out of due-date order | paid by due date, not by list position (BR-19) | `threeOverdueOutOfOrder` |
| BP-10 | N9F on first evaluation | P6 | none | **Infeasible.** `TIERS` is a constant list of 4 tiers, so the tier loop always runs 4 times | n/a (by inspection) |

BVA cases for the same method (0.00, 0.01, due - 0.01, exactly due, due + 0.01) are in `qa/test-design/black-box.md`, TC-LED-01 and TC-LED-02. They are automated in the parameterised `boundaries` test and in `oneRupeeShortOnNewestPrincipal`.

---

## 3. `LoanAccount.restore`

Source: `src/main/java/pk/edu/giki/microlend/ledger/LoanAccount.java`.

| Node | Statement | Decision |
|---|---|---|
| N1 | `if (id == null)` | P1 |
| N2 | `if (status == null)` | P2 |
| N3 | `if (outstandingPrincipal == null)` | P3 |
| N4 | `if (status == ACTIVE` | P4 |
| N5 | `&& closedOn != null)` | P5 |
| N6 | `if (status != ACTIVE` | P6 |
| N7 | `&& closedOn == null)` | P7 |
| N8 to N12 | five `throw new InvalidLoanAccountException(...)` | |
| N13 | `return new LoanAccount(...)` | |
| N14 | exit | |

There are 7 decision nodes, 7 other nodes and the exit. N4F and N5F both lead to N6, and N6F and N7F both lead to N13. Each throw node and N13 lead to the exit. E = 22, N = 16, **V(G) = 8**.

| Path | Decisions | Input | Expected | Automated in `LoanAccountTest` |
|---|---|---|---|---|
| BP-1 | P1..P3 F, P4 T, P5 F, P6 F | ACTIVE, closedOn null | account created, `isClosed()` false | `activeAccountState`, `activeLoanAcceptsPayment` |
| BP-2 | P1 T | id null | `InvalidLoanAccountException` | `rejectsNullFields` |
| BP-3 | P2 T | status null | `InvalidLoanAccountException` | `rejectsNullFields` |
| BP-4 | P3 T | principal null | `InvalidLoanAccountException` | `rejectsNullFields` |
| BP-5 | P4 T, P5 T | ACTIVE with closedOn 2026-11-01 | `InvalidLoanAccountException` | `activeAccountHasNoClosureDate` |
| BP-6 | P4 F, P6 T, P7 T | CLOSED, closedOn null | `InvalidLoanAccountException` | `closedAccountNeedsClosureDate` |
| BP-7 | P4 F, P6 T, P7 F | CLOSED, closedOn 2026-11-01 | account created, `isClosed()` true | `restoredClosedAccountState`, `closedLoanRejectsPayment` |
| BP-8 | P4 F, P6 T, P7 F, status WRITTEN_OFF | WRITTEN_OFF, closedOn 2026-11-01 | account created; payment then rejected (BR-26) | `writtenOffLoanRejectsPayment` |

JaCoCo reports 14 of 14 branches covered.

---

## 4. `LedgerEntry.requireValid`

Source: `src/main/java/pk/edu/giki/microlend/ledger/LedgerEntry.java`. There are five sequential guards (loan id, type, amount, posting time, zero amount), each leading to a throw. E = 16, N = 12, **V(G) = 6**.

| Path | Flipped guard | Input | Expected | Automated in `LedgerEntryTest` |
|---|---|---|---|---|
| BP-1 | none | valid debit of 100.00 | entry created, credit column 0.00 | `debitEntry`, `creditEntry` |
| BP-2 | loanId null | | `InvalidLedgerEntryException` | `rejectsNullFields` |
| BP-3 | type null | | `InvalidLedgerEntryException` | `rejectsNullFields` |
| BP-4 | amount null | | `InvalidLedgerEntryException` | `rejectsNullFields` |
| BP-5 | postedAt null | | `InvalidLedgerEntryException` | `rejectsNullFields` |
| BP-6 | amount 0.00 | | `InvalidLedgerEntryException` ("must move money") | `rejectsZeroAmount` |

JaCoCo reports 10 of 10 branches covered. The class as a whole is at 37 of 44 branches. The uncovered branches are in `equals` and `reversalOf`, which section 6 tracks.

---

## 5. `CoreRepaymentPoster.post`

Source: `src/main/java/pk/edu/giki/microlend/ledger/CoreRepaymentPoster.java`. This is the concrete component that the Decorator chain wraps (M1 section 5.3).

### 5.1 Why exception edges are drawn as decisions

JaCoCo gives `post` a complexity of only **2**, because it counts bytecode branches, and `post` has just one `if` (payment above the payoff). Its other outcomes leave the method as exceptions thrown by collaborators:
- `orElseThrow` for an unknown loan;
- `ensureAcceptsPayment` for a closed loan;
- `appendAll` failing;
- `save` failing, which leads to the compensating reversal.

These exits are the requirements this method exists to meet (BR-26, QR-03). So the CFG below draws each of them as a decision node, and the basis paths are derived from that graph.

| Node | Statement | Decision |
|---|---|---|
| N1 | `loans.findById(loanId)` found? | P1 |
| N2 | `throw LoanNotFoundException` | |
| N3 | `account.ensureAcceptsPayment()` passes? (BR-26) | P2 |
| N4 | `throw LoanClosedException` | |
| N5 | allocate, settle; `!outcome.excess().isZero()`? | P3 |
| N6 | `throw InvalidRepaymentException` (excess over payoff) | |
| N7 | build lines; `requireBalanced`; `ledger.appendAll(lines)` succeeds? | P4 |
| N8 | exception propagates; nothing written, loan not saved | |
| N9 | `loans.save(outcome.account())` succeeds? | P5 |
| N10 | `reverse(lines, saveFailed)`; rethrow | |
| N11 | `return new PostingResult(...)` | |
| N12 | exit | |

Each of the 5 decision nodes has two out-edges (10 edges). N2, N4, N6, N8, N10 and N11 each have one edge to the exit (6 edges). E = 16, N = 12, **V(G) = 16 - 12 + 2 = 6** = P + 1.

### 5.2 Basis paths

| Path | Decisions | Input | Expected | Automated test |
|---|---|---|---|---|
| BP-1 | P1 found, P2 pass, P3 no excess, P4 ok, P5 ok | active loan, partial payment | 1 `PAYMENT_RECEIVED` debit balanced by the tier credits; loan saved with reduced principal | `CoreRepaymentPosterTest.partialPaymentLines`, `principalReducesAccount`, `ledgerBalancesAfterEveryPosting` |
| BP-2 | P1 not found | unknown loan id | `LoanNotFoundException`; nothing written | `unknownLoan` |
| BP-3 | P2 closed | CLOSED or WRITTEN_OFF loan | `LoanClosedException`; nothing written | `closedLoanRefused` |
| BP-4 | P3 excess | payoff amount + 0.01 | `InvalidRepaymentException`; nothing written | `excessOverPayoffIsRefused` |
| BP-5 | P4 append fails | ledger writer that throws | exception propagates; loan not saved | `failedWriteDoesNotSaveLoan` |
| BP-6 | P5 save fails | loan repository that throws on save | every posted line reversed (BR-24), then the failure is rethrown; the ledger nets to zero | `PostingFaultInjectionTest.failureLeavesLedgerBalancedWithNoOrphans` (loan-save failure points) |

Variants of BP-1 cover BR-21 (`surplusBecomesAdvanceCredit`) and the BR-22 payoff (`exactPayoffClosesLoan`). These follow the same graph path with different data, so they are boundary cases, not extra basis paths.

### 5.3 `requireBalanced` (BR-23)

This method has one loop and one `if`, so V(G) = 3. JaCoCo agrees: complexity 3, with 4 of 4 branches covered. Its paths are: empty list (loop skipped, balanced), balanced lines, and unbalanced lines. `post` always builds balanced lines, so the unbalanced path cannot be reached through `post`. That is why `requireBalanced` is package-private and tested directly by `imbalanceDetected`. This is a guard against a future change to `linesFor`, not dead code.

---

## 6. Coverage baseline and gaps (M2)

Measured with `.\gradlew.bat build jacocoTestReport` on `701c161`: 182 tests, 0 failures.

| Counter | Covered / total | % |
|---|---|---|
| Lines | 773 / 818 | 94.5 |
| Branches | 244 / 282 | 86.5 |
| Methods | 184 / 188 | 97.9 |

This covers only the code that exists: `common`, `ledger`, `persistence` and `web`. The 60% target applies to core business logic at M3, once `origination`, `products` and `delinquency` are built.

Open gaps feed `qa/defect-log.md` only if a test reveals a fault:
- `LedgerEntry`: 7 branches in `equals` and in the null and blank checks of `reversalOf` are not executed.
- Ibrahim's `DelinquencyClassifier.classify` (RTM estimate CC about 6) and Hassan's schedule assembler get their white-box design when their code exists.
