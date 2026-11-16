# MicroLend - Black-box Test Design (EP and BVA)

**Milestone:** M2 (SE431) - Black-box test design
**Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Version:** 0.2, 2026-11-16 (refreshed against the final ledger build `87c5caf`, 182 tests)
**Sources:** `docs/business-rules.md` v1.1 (BR-01 to BR-43), `qa/rtm.md` (FR, QR and TC IDs),
`docs/M1-SE431-Quality-Requirements-RTM-SQAP.md`, and the test code under `src/test/java`.

## 1. Purpose and method

This document designs black-box test cases from the business rules alone, without reference
to the code structure. White-box design (CFG, cyclomatic complexity, basis paths) is in
`qa/test-design/white-box.md`.

Two techniques are used:

- **Equivalence partitioning (EP).** Each input domain is split into classes the system must
  treat the same way. Every class gets at least one test case. Valid classes are `EC-xx`
  with a `V` tag, invalid ones with an `I` tag.
- **Boundary value analysis (BVA).** Cases are placed at, just below and just above each
  edge a rule states. Integer inputs (ages, days, counts) use min-1 / min / min+1 /
  max-1 / max / max+1. Money uses the smallest unit, PKR 0.01, as the step.
  Percentages use 0.01 percentage points.

### Conventions

- **Test case IDs** keep the RTM prefix: `TC-LED-02.3` is the third case of RTM test
  `TC-LED-02`. Money and LoanId cases, which have no RTM row of their own, use `TC-CMN-xx`
  and trace to BR-40, BR-43 and C-01.
- **Money** is written with 2 decimal places, as `Money` holds it (`BigDecimal`, scale 2,
  HALF_EVEN). No case uses `double` or `float` (C-01).
- **Status** is either *automated* with the JUnit class and method, or *designed* (not yet
  automated). Several cases are designed for modules owned by other members. They are the
  test basis for those owners and are not implemented here.
- **"Rule silent"** marks an input whose boundary the business rules do not state. The
  team must decide it; this document does not invent one.

### Decisions this design depends on

| Decision | Effect on test design | Status |
|---|---|---|
| **BR-18/BR-19, tier by tier.** All penalties are settled oldest instalment first, then all fees oldest first, then all interest oldest first, then all principal oldest first. | Expected results of TC-LED-01 and TC-LED-02 | Adopted by the team on 2026-11-16 (recorded by Tughral Hussain) as decision D-01, and noted under BR-19 in `docs/business-rules.md`. The expected allocations in section 4.1 follow this reading. |
| Fees tier is always zero at M1 (BR-18) | A non-zero fee is an untestable class until a fee type is introduced by a recorded deviation | Frozen rule |
| Product amount and tenure bands are per product (BR-06, BR-07) | Boundaries are written relative to `min` and `max`; concrete values come from the product table | Product values not yet fixed in code |

## 2. Common inputs (`common` package)

### 2.1 Money - BR-40, BR-43, C-01

**Input:** a decimal amount passed to `Money.of(BigDecimal)`.

| EC | Class | Type |
|---|---|---|
| EC-01 | amount >= 0.00 with at most 2 decimal places | V |
| EC-02 | amount with fewer than 2 decimal places (e.g. `5`, `2.5`); normalised to scale 2 | V |
| EC-03 | negative amount | I |
| EC-04 | more than 2 decimal places | I |
| EC-05 | null | I |

**Boundaries:** -0.01 / 0.00 / 0.01; scale 2 (allowed) / scale 3 (rejected).

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-CMN-01.1 | -0.01 | `InvalidMoneyException` | EC-03, lower bound - 0.01 | automated `MoneyTest#rejectsNegativeAmount` |
| TC-CMN-01.2 | 0.00 | accepted, `isZero()` true | EC-01, lower bound | automated `MoneyTest#permitsZero` |
| TC-CMN-01.3 | 0.01 | accepted | EC-01, lower bound + 0.01 | automated `RepaymentCommandTest#acceptsOnePaisa` (builds and accepts `Money` 0.01) |
| TC-CMN-01.4 | 10.005 | `InvalidMoneyException` | EC-04, scale 3 | automated `MoneyTest#rejectsExcessiveScale` |
| TC-CMN-01.5 | 5 | held as 5.00 | EC-02 | automated `MoneyTest#normalisesScale` |
| TC-CMN-01.6 | null | `InvalidMoneyException` | EC-05 | automated `MoneyTest#rejectsNull` |
| TC-CMN-01.7 | 2.5 vs 2.50 | equal, `compareTo == 0` | EC-02, C-02 | automated `MoneyTest#comparesByValueNotScale` |

**Arithmetic** (`plus`, `minus`, `percentage`):

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-CMN-02.1 | 0.10 + 0.20 | 0.30 exactly | BR-40 | automated `MoneyTest#additionIsExact` |
| TC-CMN-02.2 | 10.00 - 10.00 | 0.00 | minus at boundary 0 | automated `MoneyTest#subtractionToZeroIsAllowed` |
| TC-CMN-02.3 | 10.00 - 20.00 | `InvalidMoneyException` | minus below 0 (BR-43) | automated `MoneyTest#subtractionCannotGoNegative` |
| TC-CMN-02.4 | 10.00 - 10.01 | `InvalidMoneyException` | minus at -0.01 | designed |
| TC-CMN-02.5 | 1000.00 x 0.05% | 0.50 | `percentage` | automated `MoneyTest#percentageRoundsHalfEven` |
| TC-CMN-02.6 | 1.01 vs 1.00 | 1.01 greater | ordering | automated `MoneyTest#ordersCorrectly` |

### 2.2 LoanId

**Input:** the identifier string passed to `LoanId.of(String)`.

| EC | Class | Type |
|---|---|---|
| EC-06 | non-blank string | V |
| EC-07 | non-blank string with surrounding whitespace (trimmed) | V |
| EC-08 | blank or empty string | I |
| EC-09 | null | I |

The rules do not state a maximum length or character set (rule silent), so there is no upper boundary.

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-CMN-03.1 | null | `InvalidLoanIdException` | EC-09 | automated `LoanIdTest#rejectsNull` |
| TC-CMN-03.2 | `""`, `"   "` | `InvalidLoanIdException` | EC-08 | automated `LoanIdTest#rejectsBlank` |
| TC-CMN-03.3 | `"  L-001  "` | equals `LoanId.of("L-001")` | EC-07 | automated `LoanIdTest#trimsWhitespace` |
| TC-CMN-03.4 | `"L-001"` twice | equal by value | EC-06 | automated `LoanIdTest#comparesByValue` |

## 3. Origination (owner: Muhammad Ibrahim)

### 3.1 FR-01 - age at disbursement and at maturity (BR-01, BR-02) - TC-ORG-01

**Inputs:** age in completed years at disbursement; age in completed years at maturity. The
rules use whole years; whether "65 at maturity" means 65 years and 0 days or any day before
the 66th birthday is rule silent. Completed years is assumed here.

| EC | Class | Type |
|---|---|---|
| EC-10 | age at disbursement >= 18 | V |
| EC-11 | age at disbursement < 18 | I |
| EC-12 | age at maturity <= 65 | V |
| EC-13 | age at maturity > 65 | I |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-01.1 | disbursement age 17 | rejected, age reason | EC-11, 18 - 1 | designed |
| TC-ORG-01.2 | disbursement age 18 | passes BR-01 | EC-10, min | designed |
| TC-ORG-01.3 | disbursement age 19 | passes BR-01 | EC-10, min + 1 | designed |
| TC-ORG-01.4 | maturity age 64 | passes BR-02 | EC-12, max - 1 | designed |
| TC-ORG-01.5 | maturity age 65 | passes BR-02 | EC-12, max | designed |
| TC-ORG-01.6 | maturity age 66 | rejected, maturity-age reason | EC-13, max + 1 | designed |
| TC-ORG-01.7 | age 40 at application, 66 at maturity | rejected (age is tested at maturity, not application) | EC-13 | designed |

### 3.2 FR-02 - active-loan limit (BR-03) - TC-ORG-02

**Input:** number of the applicant's existing active loans, not counting the one applied for.
A loan in arrears counts as active. A written-off loan does not.

| EC | Class | Type |
|---|---|---|
| EC-14 | 0 or 1 existing active loans | V |
| EC-15 | 2 or more existing active loans | I |
| EC-16 | existing loans are written off only | V (not counted) |
| EC-17 | existing loan in arrears | counted as active |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-02.1 | 0 existing active | passes | EC-14, min | designed |
| TC-ORG-02.2 | 1 existing active | passes | EC-14, max | designed |
| TC-ORG-02.3 | 2 existing active | rejected | EC-15, max + 1 | designed |
| TC-ORG-02.4 | 3 existing active | rejected | EC-15 | designed |
| TC-ORG-02.5 | 1 active + 1 written off | passes BR-03 (rejected later by BR-05) | EC-16 | designed |
| TC-ORG-02.6 | 1 active in arrears + 1 active | rejected | EC-17 | designed |

### 3.3 FR-03 - instalment burden (BR-04) - TC-ORG-03

**Input:** total monthly instalments including the new loan, at the product base rate on the
requested amount, divided by declared monthly income. Income 100,000.00 is used throughout.

| EC | Class | Type |
|---|---|---|
| EC-18 | burden <= 40.00% | V |
| EC-19 | burden > 40.00% | I |
| EC-20 | declared income 0.00 | rule silent (division by zero); must be decided |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-03.1 | instalments 39,990.00 (39.99%) | passes | EC-18, max - 0.01 pp | designed |
| TC-ORG-03.2 | instalments 40,000.00 (40.00%) | passes | EC-18, max | designed |
| TC-ORG-03.3 | instalments 40,010.00 (40.01%) | rejected, burden reason | EC-19, max + 0.01 pp | designed |
| TC-ORG-03.4 | Grade C applicant, burden 40.00% at base rate | passes; the spread does not count (BR-04 uses the base rate) | EC-18 | designed |

### 3.4 FR-04 - prior write-off (BR-05) - TC-ORG-04

| EC | Class | Type |
|---|---|---|
| EC-21 | no prior loans written off | V |
| EC-22 | at least one prior write-off | I |
| EC-23 | prior loan settled late but never written off | V |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-04.1 | no history | passes | EC-21 | designed |
| TC-ORG-04.2 | one written-off loan, all else valid | rejected, write-off reason | EC-22 | designed |
| TC-ORG-04.3 | one loan settled 45 days late | passes BR-05 | EC-23 | designed |

### 3.5 FR-05 - typed rejection reason in rule order (BR-08) - TC-ORG-05

**Input:** an application failing one or more of BR-01 to BR-07.

| EC | Class | Type |
|---|---|---|
| EC-24 | passes all rules | V (approved) |
| EC-25 | fails exactly one rule (one class per rule BR-01 to BR-07) | I |
| EC-26 | fails several rules | I (reason names the first in order) |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-05.1 to .7 | one case per rule BR-01 to BR-07 failing alone | reason names that rule | EC-25 | designed |
| TC-ORG-05.8 | age 17 and a prior write-off | reason names BR-01 (evaluated first) | EC-26 | designed |
| TC-ORG-05.9 | 2 active loans and burden 45% | reason names BR-03 | EC-26 | designed |

### 3.6 FR-06 - risk grade (BR-09, BR-10) - TC-ORG-06

**Inputs:** burden ratio; worst days past due on any earlier loan; whether there is an earlier loan.

| EC | Class | Type |
|---|---|---|
| EC-27 | Grade A: burden <= 25.00%, at least one earlier loan, worst DPD <= 30 | V |
| EC-28 | Grade B: burden <= 35.00%, worst DPD <= 60, including first-time borrowers | V |
| EC-29 | Grade C: every other applicant passing BR-01 to BR-07 | V |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-06.1 | burden 25.00%, 1 earlier loan, worst DPD 30 | A, +0.00%, up to 100% | EC-27, both max | designed |
| TC-ORG-06.2 | burden 25.01%, worst DPD 30 | B | EC-28, burden A max + 0.01 | designed |
| TC-ORG-06.3 | burden 20.00%, worst DPD 31 | B | EC-28, DPD A max + 1 | designed |
| TC-ORG-06.4 | burden 20.00%, no earlier loan | B (first-time borrower) | EC-28 | designed |
| TC-ORG-06.5 | burden 35.00%, worst DPD 60 | B, +1.50%, up to 80% | EC-28, both max | designed |
| TC-ORG-06.6 | burden 35.01%, worst DPD 0 | C | EC-29, burden B max + 0.01 | designed |
| TC-ORG-06.7 | burden 20.00%, worst DPD 61 | C, +3.00%, up to 60% | EC-29, DPD B max + 1 | designed |

**Capped amount (BR-10).** Product minimum 10,000.00 is assumed for the example. The real
value comes from the product table.

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-06.8 | Grade B, requested 50,000.00 | approved 40,000.00 (80%) | requested above cap | designed |
| TC-ORG-06.9 | Grade A, requested 50,000.00 | approved 50,000.00 | within cap | designed |
| TC-ORG-06.10 | Grade C, requested 16,666.00, min 10,000.00 | cap 9,999.60, rejected `BELOW_PRODUCT_MINIMUM` | cap just below minimum | designed |
| TC-ORG-06.11 | Grade C, requested 16,666.67, min 10,000.00 | cap 10,000.00, approved | cap at minimum | designed |

### 3.7 FR-07 - product amount and tenure bands (BR-06, BR-07) - TC-ORG-07

**Inputs:** requested amount; requested tenure in months. The bands are per product and
inclusive. The concrete minimum and maximum are product data, not rules.

| EC | Class | Type |
|---|---|---|
| EC-30 | min <= amount <= max | V |
| EC-31 | amount < min | I |
| EC-32 | amount > max | I |
| EC-33 | tenure within band | V |
| EC-34 | tenure below band | I |
| EC-35 | tenure above band | I |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-07.1 | amount min - 0.01 | rejected, amount reason | EC-31 | designed |
| TC-ORG-07.2 | amount min | passes | EC-30, min | designed |
| TC-ORG-07.3 | amount max | passes | EC-30, max | designed |
| TC-ORG-07.4 | amount max + 0.01 | rejected, amount reason | EC-32 | designed |
| TC-ORG-07.5 | tenure band min - 1 | rejected, tenure reason | EC-34 | designed |
| TC-ORG-07.6 | tenure band min, band max | passes | EC-33 | designed |
| TC-ORG-07.7 | tenure band max + 1 | rejected, tenure reason | EC-35 | designed |

The schema half of BR-06/BR-07 (a product row whose minimum exceeds its maximum is refused) is
automated in `SchemaMigratorTest#productBands`.

## 4. Ledger (owner: Tughral Hussain)

### 4.1 FR-13 - allocation waterfall, oldest first (BR-18, BR-19) - TC-LED-01

**Inputs:** payment amount; the list of unpaid instalments, each with outstanding penalty,
fees, interest and principal.

**Reading used:** tier by tier, adopted by the team on 2026-11-16, decision D-01 (section 1).
The fees tier is always 0.00 at M1 in real data; the allocator still orders it, so one case
uses a non-zero fee to show the order. The input type is the ledger-owned `InstalmentDue`
record (sequence, due date, penalty, fees, interest, principal).

The cases below use the inputs of the automated tests in `RepaymentAllocatorTest`, so each
expected result is the one the test asserts. Dates: I1 due 2026-01-15, I2 2026-02-15,
I3 2026-03-15.

| EC | Class | Type |
|---|---|---|
| EC-40 | payment covers part of the penalty tier only | V |
| EC-41 | payment covers penalties and part of interest | V |
| EC-42 | payment covers penalties and interest and part of principal | V |
| EC-43 | one overdue instalment | V |
| EC-44 | two overdue instalments | V |
| EC-45 | three overdue instalments, given out of due-date order | V |
| EC-46 | no unpaid instalments | V: the whole payment is returned unapplied, for settlement (BR-21) |
| EC-47 | null payment, null list or null element | I |
| EC-82 | two instalments with the same sequence number | I |
| EC-83 | instalment with sequence below 1 or a missing field | I |

| TC | Input (penalty / fees / interest / principal) | Payment | Expected allocation | Covers | Status |
|---|---|---|---|---|---|
| TC-LED-01.1 | I1 10.00 / 0.00 / 100.00 / 1,000.00 | 60.00 | penalty 10.00, interest 50.00; principal 0.00 | EC-41, EC-43 | automated `RepaymentAllocatorTest#partOfEachTier` |
| TC-LED-01.2 | I1 10.00 / 20.00 / 100.00 / 1,000.00 | 25.00 | penalty 10.00, fees 15.00; interest 0.00 | BR-18 fees between penalty and interest | automated `RepaymentAllocatorTest#feesBeforeInterest` |
| TC-LED-01.3 | I1 10.00 / 0.00 / 100.00 / 1,000.00 | 110.01 | penalty 10.00, interest 100.00, principal 0.01 | EC-42, first paisa into principal (BVA) | automated `RepaymentAllocatorTest#principalLast` |
| TC-LED-01.4 | I1 10.00 / 0.00 / 100.00 / 1,000.00; I2 5.00 / 0.00 / 100.00 / 1,000.00 | 120.00 | I1 penalty 10.00, interest 100.00; I2 penalty 5.00, interest 5.00 | EC-44, both penalties before any interest | automated `RepaymentAllocatorTest#twoOverdueTierByTier` |
| TC-LED-01.5 | I3 0.00 / 0.00 / 100.00 / 1,000.00; I1 20.00 / 0.00 / 100.00 / 1,000.00; I2 10.00 / 0.00 / 100.00 / 1,000.00 (listed in that order) | 2,350.00 | I1 and I2 paid in full; I3 interest 100.00, principal 20.00 | EC-45, order by due date, not list position | automated `RepaymentAllocatorTest#threeOverdueOutOfOrder` |
| TC-LED-01.6 | I1 10.00 / 0.00 / 100.00 / 1,000.00; I2 0.00 / 0.00 / 100.00 / 1,000.00 | 10.00 | I1 penalty 10.00; I2 reported with a zero allocation | EC-40, penalty tier exactly (BVA) | automated `RepaymentAllocatorTest#untouchedInstalmentIsZero` |
| TC-LED-01.7 | no instalments | 50.00 | nothing allocated; 50.00 unapplied | EC-46 | automated `RepaymentAllocatorTest#nothingOwed` |
| TC-LED-01.8 | null payment; null list; list with a null element | 1.00 | `InvalidAllocationException` | EC-47 | automated `RepaymentAllocatorTest#rejectsMissingInput` |
| TC-LED-01.9 | two instalments, both sequence 1 | 1.00 | `InvalidAllocationException` | EC-82 | automated `RepaymentAllocatorTest#rejectsDuplicateSequence` |

**Supporting value objects.** `Allocation` holds the four tiers; `InstalmentDue` is the allocator's input.

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-LED-01.10 | four tier amounts | each tier exposed; total is their sum | automated `AllocationTest#exposesEachTier`, `#totalIsSumOfTiers` |
| TC-LED-01.11 | all tiers 0.00 | valid, total 0.00 | automated `AllocationTest#allZeroAllocation` |
| TC-LED-01.12 | any tier null | `InvalidAllocationException` | automated `AllocationTest#rejectsNullTiers` |
| TC-LED-01.13 | principal tier vs outstanding principal 1,000.00 | 1,000.00 leaves 0.00; 1,000.01 rejected | automated `LoanAccountTest#exactPrincipalLeavesZero`, `#principalAboveOutstandingRejected` |
| TC-LED-01.14 | `InstalmentDue` with sequence 0 (min - 1), or a null date or amount | `InvalidAllocationException`; a valid one totals its four amounts | automated `RepaymentAllocatorTest#instalmentDueInvariants` (EC-83) |

### 4.2 FR-14 - partial payment (BR-20) - TC-LED-02

**Input:** payment amount against two overdue instalments due 2,230.00 in total
(I1 10.00 / 0.00 / 100.00 / 1,000.00; I2 20.00 / 0.00 / 100.00 / 1,000.00), as in
`RepaymentAllocatorTest.PartialPayment`.

| EC | Class | Type |
|---|---|---|
| EC-48 | 0.01 <= payment < amount due (partial) | V |
| EC-49 | payment = amount due | V |
| EC-50 | payment = 0.00 | I at the command and the web form; at the allocator it allocates nothing |
| EC-84 | payment > amount due | V: everything due is paid and the remainder is returned unapplied (hand-off to BR-21) |

| TC | Payment | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-02.1 | 0.00 | `InvalidRepaymentException` at the command; HTTP 400 at the teller form; 0.00 allocated if it reached the allocator | EC-50, min - 0.01 | automated `RepaymentCommandTest#rejectsZeroAmount`, `TellerWebServerTest#malformedInputIs400`, `RepaymentAllocatorTest#boundaries` |
| TC-LED-02.2 | 0.01 | penalty 0.01; nothing else | EC-48, min | automated `RepaymentAllocatorTest#boundaries`, `RepaymentCommandTest#acceptsOnePaisa` |
| TC-LED-02.3 | 2,229.99 | penalty 30.00, interest 200.00, principal 1,999.99; 0.01 of I2's principal outstanding | EC-48, due - 0.01 | automated `RepaymentAllocatorTest#boundaries`, `#oneRupeeShortOnNewestPrincipal` |
| TC-LED-02.4 | 2,230.00 | everything paid; 0.00 unapplied | EC-49, exactly due | automated `RepaymentAllocatorTest#boundaries` |
| TC-LED-02.5 | 2,230.01 | everything paid; 0.01 unapplied | EC-84, due + 0.01 | automated `RepaymentAllocatorTest#boundaries` |
| TC-LED-02.6 | 0.00, 0.01, 17.35, 2,230.00, 9,999.99 | allocated + unapplied = payment in every case | EC-48, EC-49, EC-84, conservation | automated `RepaymentAllocatorTest#conservesThePayment` |

### 4.3 FR-15 - overpayment and early settlement (BR-21, BR-22) - TC-LED-03

**Inputs:** payment amount; total amount currently due; total payoff amount; product type.
Due now is 460.00 and the payoff is 2,000.00.

| EC | Class | Type |
|---|---|---|
| EC-51 | due < payment < payoff (overpayment) | V, advance credit |
| EC-52 | payment = payoff | V, early settlement |
| EC-53 | payment > payoff | handling of the excess is rule silent: the settlement policy reports it and the poster refuses the posting until a rule exists |
| EC-54 | product flat-rate | no rebate |
| EC-55 | product reducing-balance | unearned interest refunded |
| EC-56 | product bullet | unaccrued monthly interest refunded |

| TC | Payment | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-03.1 | 460.01 | instalment paid; 0.01 advance credit; loan stays active | EC-51, due + 0.01 | automated `SettlementPolicyTest#overpaymentBecomesAdvanceCredit` (surplus 0.01 with principal owing); posted as an `ADVANCE_CREDIT` line in `CoreRepaymentPosterTest#surplusBecomesAdvanceCredit` |
| TC-LED-03.2 | 1,999.99 | advance credit; loan stays active | EC-51, payoff - 0.01 | designed (needs the `products` implementation of `InstalmentsDueSource`, decision D-02: only instalments due by the business date) |
| TC-LED-03.3 | 2,000.00, flat-rate | settled (loan CLOSED on the business date); rebate 0.00 | EC-52, EC-54 | automated with a stub rebate: `SettlementPolicyTest#exactPayoffClosesTheLoan`, `#zeroRebate`, `CoreRepaymentPosterTest#exactPayoffClosesLoan`; the flat-rate product itself is designed (`products`) |
| TC-LED-03.4 | 2,000.00, reducing-balance | settled; unearned interest refunded | EC-52, EC-55 | designed (rebate amount from `products`) |
| TC-LED-03.5 | 2,000.00, bullet | settled; unaccrued monthly interest refunded | EC-52, EC-56 | designed (rebate amount from `products`) |
| TC-LED-03.6 | 2,000.01 | policy reports 0.01 excess (not advance credit); the poster refuses the payment with `InvalidRepaymentException` and writes nothing | EC-53, payoff + 0.01 | automated `SettlementPolicyTest#payoffPlusOneIsExcess`, `CoreRepaymentPosterTest#excessOverPayoffIsRefused` |

The per-product rebate amounts are product knowledge owned by Hassan Khalid. The ledger asks
for them through `EarlySettlementRebate`, so TC-LED-03.4 and .5 need his calculators to
provide expected values.

### 4.4 FR-16 - balanced posting (BR-23) - TC-LED-04

This is an invariant over every posting path, not an input domain. EP is applied to the
posting paths instead.

| EC | Posting path | Type |
|---|---|---|
| EC-57 | teller cash payment | V |
| EC-58 | bank payment from an import | V |
| EC-59 | reversal of a posted entry | V |
| EC-60 | posting that fails part-way (fault injection, QR-03) | I, must leave the ledger balanced |
| EC-85 | a set of posting lines whose debits and credits differ | I, refused before anything is written |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-04.1 | teller cash postings in sequence | one `PAYMENT_RECEIVED` debit balanced by the tier credits; sum of debits = sum of credits after every posting | EC-57 | automated `CoreRepaymentPosterTest#partialPaymentLines`, `#ledgerBalancesAfterEveryPosting`; on SQLite through the teller chain in `CompositionRootTest#tellerChainPostsAuditsAndTimes` |
| TC-LED-04.2 | bank payment HBL / TX-1, 50.00, through the import chain | posted once by the core poster | EC-58 | partly automated: `CompositionRootTest#importChainGuardsOutermost` posts it on SQLite but does not assert the balance; statement parsing (FR-20) is designed |
| TC-LED-04.3 | reversal of a posted entry | each column nets to zero; original kept | EC-59 | automated `LedgerImmutabilityTest#correctionKeepsOriginalAndNetsToZero` |
| TC-LED-04.4 | failure injected at each of 14 points: every line of postings of 60.00, 1,300.00 and 2,210.00 (11 line points), and the loan save after each (3 points) | ledger still balanced; loan unchanged; 0 unreversed lines of the failed posting | EC-60, QR-03 | automated `PostingFaultInjectionTest#failureLeavesLedgerBalancedWithNoOrphans` (real SQLite file) |
| TC-LED-04.5 | debit 10.00 against credit 9.99 | `LedgerImbalanceException` | EC-85, imbalance of 0.01 (BVA) | automated `CoreRepaymentPosterTest#imbalanceDetected` |
| TC-LED-04.6 | ledger write fails; last line of a batch is invalid | loan not saved; no line of the batch written | EC-60 | automated `CoreRepaymentPosterTest#failedWriteDoesNotSaveLoan`, `SqliteLedgerRepositoryTest#appendAllIsAtomic` |

### 4.5 FR-17 - immutable entries and compensating reversals (BR-24, BR-43) - TC-LED-05

**Inputs:** the entry to correct and the reason; the amount of a new ledger line.

| EC | Class | Type |
|---|---|---|
| EC-61 | reversal of a credit entry | V |
| EC-62 | reversal of a debit entry | V |
| EC-63 | reversal with blank or null reason | I |
| EC-64 | reversal of an entry not in the ledger | I |
| EC-65 | ledger line amount 0.00 | I (BR-43, exactly one non-zero column) |
| EC-66 | ledger line amount >= 0.01 | V |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-05.1 | reverse a credit | debit REVERSAL of the same amount, referencing the original | EC-61 | automated `CompensatingEntryServiceTest#reversesACredit` |
| TC-LED-05.2 | reverse a debit | credit REVERSAL | EC-62 | automated `CompensatingEntryServiceTest#reversesADebit` |
| TC-LED-05.3 | reason blank / null | rejected | EC-63 | automated `CompensatingEntryServiceTest#requiresReason` |
| TC-LED-05.4 | original not stored | refused | EC-64 | automated `SqliteLedgerRepositoryTest#reversalOfUnknownEntryIsRefused` |
| TC-LED-05.5 | line amount 0.00 | `InvalidLedgerEntryException` | EC-65, min - 0.01 | automated `LedgerEntryTest#rejectsZeroAmount` |
| TC-LED-05.6 | line amount 0.01 stored and read back | 1 paisa stored, 0.01 read | EC-66, min | automated `SqliteLedgerRepositoryTest#moneyStoredAsPaisa` |
| TC-LED-05.7 | no mutator on `LedgerEntry`; write role offers only `append` | true | QR-05 | automated `LedgerEntryTest#noMutators`, `LedgerImmutabilityTest#writerCanOnlyAppend` |
| TC-LED-05.8 | correction leaves the original unchanged | original still present, net zero | BR-24 | automated `LedgerImmutabilityTest#correctionKeepsOriginalAndNetsToZero` |

### 4.6 FR-18 - duplicate (bank, reference) rejected (BR-25) - TC-LED-06

**Inputs:** payment source (cash or bank); bank; reference.

| EC | Class | Type |
|---|---|---|
| EC-67 | cash payment, no reference | V |
| EC-68 | bank payment, new (bank, reference) pair | V |
| EC-69 | bank payment, pair already in the ledger | I, duplicate |
| EC-70 | same reference, different bank | V |
| EC-71 | bank payment with blank bank or reference | I |
| EC-86 | retry of a pair whose earlier posting failed | V, accepted (the failed attempt releases the pair) |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-06.1 | cash, no reference | accepted; never treated as a duplicate | EC-67 | automated `RepaymentCommandTest#cashPaymentHasNoReference`, `IdempotencyGuardPosterTest#cashIsNeverADuplicate` |
| TC-LED-06.2 | HBL / REF-1 first time | accepted and forwarded | EC-68 | automated `RepaymentCommandTest#bankPaymentCarriesPair`, `IdempotencyGuardPosterTest#samePairTwice` (first posting) |
| TC-LED-06.3 | HBL / REF-1 again | `DuplicatePaymentException`; not forwarded | EC-69 | automated `IdempotencyGuardPosterTest#samePairTwice`, `CompositionRootTest#importChainGuardsOutermost`; schema key `SchemaMigratorTest#paymentIdempotencyKey`. Pairs are held in memory only (FR-18 partial) |
| TC-LED-06.4 | HBL / REF-1 then MCB / REF-1 | both accepted | EC-70 | automated `RepaymentCommandTest#sameReferenceFromTwoBanksDiffers`, `IdempotencyGuardPosterTest#sameReferenceTwoBanks` |
| TC-LED-06.5 | bank "" or reference "  " | `InvalidRepaymentException` | EC-71 | automated `RepaymentCommandTest#rejectsBlankBankOrReference` |
| TC-LED-06.6 | HBL / REF-1 posting fails, then is retried | retry accepted | EC-86 | automated `IdempotencyGuardPosterTest#failedPostingReleasesReference` |
| TC-LED-06.7 | 8 threads post HBL / REF-1 at the same moment | forwarded exactly once; 7 duplicates refused | EC-69 under concurrency | automated `IdempotencyGuardPosterTest#concurrentDuplicatesForwardOnce` |

### 4.7 FR-19 - payment against closed or written-off loan (BR-26) - TC-LED-07

| EC | Class | Type |
|---|---|---|
| EC-72 | loan ACTIVE | V |
| EC-73 | loan CLOSED | I |
| EC-74 | loan WRITTEN_OFF | I |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-07.1 | ACTIVE | accepted | EC-72 | automated `LoanAccountTest#activeLoanAcceptsPayment` |
| TC-LED-07.2 | CLOSED | `LoanClosedException` | EC-73 | automated `LoanAccountTest#closedLoanRejectsPayment` |
| TC-LED-07.3 | WRITTEN_OFF | `LoanClosedException` | EC-74 | automated `LoanAccountTest#writtenOffLoanRejectsPayment` |
| TC-LED-07.4 | allocation applied to CLOSED loan | rejected | EC-73 | automated `LoanAccountTest#closedLoanRefusesAllocation` |
| TC-LED-07.5 | repayment posted against a CLOSED loan | `LoanClosedException` before any line is written | EC-73 | automated `CoreRepaymentPosterTest#closedLoanRefused` |
| TC-LED-07.6 | settlement of a loan already CLOSED | `LoanClosedException` | EC-73 | automated `SettlementPolicyTest#closedLoanRefused` |

### 4.8 FR-20 - bank statement import - TC-LED-08

No business rule fixes the statement formats, so the boundaries are rule silent. EP only.

| EC | Class | Type |
|---|---|---|
| EC-75 | well-formed HBL statement | V |
| EC-76 | well-formed MCB statement | V |
| EC-77 | unsupported bank format | I |
| EC-78 | supported format with a malformed row (missing amount, bad date) | I |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-08.1, .2 | one valid file per bank | one `RepaymentCommand` per row | EC-75, EC-76 | designed (owner: Muhammad Ibrahim) |
| TC-LED-08.3 | unknown format | typed rejection, nothing posted | EC-77 | designed |
| TC-LED-08.4 | malformed row | row rejected with its line number | EC-78 | designed |

### 4.9 FR-31 - audit line (no BR) - TC-LED-09

| EC | Class | Type |
|---|---|---|
| EC-79 | teller posting with named operator | V, one audit line naming the operator |
| EC-80 | teller posting with blank operator | I |
| EC-81 | import of one file | V, one audit line per file |
| EC-87 | teller posting refused by the domain | V, audited with the reason, then the refusal is rethrown |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-09.1 | teller "teller.amina" posts 10.00; operator given as "  teller.amina " | one audit line naming "teller.amina" (trimmed) | EC-79 | automated `AuditLoggingPosterTest#auditsSuccess`, `#trimsOperator`; on SQLite in `CompositionRootTest#tellerChainPostsAuditsAndTimes` |
| TC-LED-09.2 | operator null, "" or "   " | refused; nothing forwarded or audited | EC-80 | automated `AuditLoggingPosterTest#refusesAnonymousPosting` |
| TC-LED-09.3 | file with 3 rows imported | exactly 1 audit line | EC-81 | designed (the import job is not built; the import chain has no per-posting audit, `CompositionRootTest#importChainGuardsOutermost`) |
| TC-LED-09.4 | posting refused by the wrapped poster | audit line with the reason; same exception rethrown | EC-87 | automated `AuditLoggingPosterTest#auditsRefusal` |
| TC-LED-09.5 | operator named on the teller web form | the posting is made as that operator, and the name does not carry over to the next request | EC-79 | automated `TellerWebServerTest#postsCashPayment` |

## 5. Products and schedules (owner: Hassan Khalid)

### 5.1 FR-08 - three products (BR-11, BR-17) - TC-PRD-01

EP over product type: flat-rate, reducing-balance and bullet (three valid classes). Any other
type is invalid. The expected schedules are golden files (QR-02), so BVA does not apply.
Rate is fixed for the life of the loan (BR-17), which is an invariant with no boundary.

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-PRD-01.1 to .3 | 100,000.00, 12 months, one per product | matches golden schedule | designed |

### 5.2 FR-09 - due dates (BR-12) - TC-PRD-02

**Input:** disbursement date.

| EC | Class | Type |
|---|---|---|
| EC-90 | disbursement day 1 to 28 | V, same day every month |
| EC-91 | disbursement day 29 to 31 | V, clamped in short months |
| EC-92 | February, leap year | V |
| EC-93 | February, non-leap year | V |

| TC | Disbursement | Expected first dues | Covers | Status |
|---|---|---|---|---|
| TC-PRD-02.1 | 2027-01-28 | 2027-02-28, 2027-03-28 | EC-90, max | designed |
| TC-PRD-02.2 | 2027-01-29 | 2027-02-28, 2027-03-29 | EC-91, EC-93 | designed |
| TC-PRD-02.3 | 2027-01-30 | 2027-02-28, 2027-03-30 | EC-91 | designed |
| TC-PRD-02.4 | 2027-01-31 | 2027-02-28, 2027-03-31, 2027-04-30 | EC-91, max | designed |
| TC-PRD-02.5 | 2028-01-31 | 2028-02-29 | EC-92 | designed |
| TC-PRD-02.6 | 2028-01-29 | 2028-02-29 | EC-92 | designed |

Hassan's open PR #6 adds `DueDateCalculator` with a test. Its cases should be mapped to these
IDs once it merges into `develop`.

### 5.3 FR-10 - grace period (BR-13) - TC-PRD-03

| EC | Class | Type |
|---|---|---|
| EC-94 | grace 0 to 3 months | V |
| EC-95 | grace > 3 | I |
| EC-96 | grace < 0 | I (implied by the 0 to 3 range) |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-PRD-03.1 | grace -1 | rejected | EC-96, min - 1 | designed |
| TC-PRD-03.2 | grace 0 | first instalment one month after disbursement | EC-94, min | designed |
| TC-PRD-03.3 | grace 1 | first instalment deferred one month; accrued interest added to it | EC-94, min + 1 | designed |
| TC-PRD-03.4 | grace 3 | deferred three months | EC-94, max | designed |
| TC-PRD-03.5 | grace 4 | rejected | EC-95, max + 1 | designed |

### 5.4 FR-11 - sum invariant and rounding (BR-14, BR-15) - TC-PRD-04

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-PRD-04.1 | 10,000.00 over 3 months, flat-rate | instalments sum exactly to principal + interest; residue in the last | BR-14, amount not divisible by tenure | designed |
| TC-PRD-04.2 | a value ending .005, e.g. 0.125 | 0.12 (HALF_EVEN) | BR-15 | designed |
| TC-PRD-04.3 | 0.135 | 0.14 | BR-15 | designed |
| TC-PRD-04.4 | a value at scale 3 passed to `Money.of` | rejected; callers must round first | BR-15, BR-40 | automated `MoneyTest#rejectsExcessiveScale` |

### 5.5 FR-12 - instalment count (BR-16) - TC-PRD-05

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-PRD-05.1 | tenure 1 | exactly 1 instalment | designed |
| TC-PRD-05.2 | tenure 6, grace 3 | exactly 6 instalments | designed |
| TC-PRD-05.3 | tenure 36 | exactly 36 instalments | designed |

The schema side, one row per (loan, sequence), is automated in `SchemaMigratorTest#instalmentSequenceUnique`.

## 6. Delinquency (owner: Muhammad Ibrahim)

### 6.1 FR-21 - penalty accrual and cap (BR-27, BR-28) - TC-DEL-01

**Inputs:** overdue principal; overdue interest; days overdue. Overdue principal is 10,000.00.
At 0.05% a day that is 5.00 a day. The cap is 10% of the overdue amount, 1,000.00, reached
on day 200. Simple, non-compounding accrual is assumed, because the rule accrues on
principal only.

| EC | Class | Type |
|---|---|---|
| EC-100 | overdue principal > 0, accrual below cap | V |
| EC-101 | accrual at or above the cap | V, capped |
| EC-102 | overdue interest only, principal 0.00 | V, no penalty |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-DEL-01.1 | 1 day | 5.00 | EC-100, min | designed |
| TC-DEL-01.2 | 30 days | 150.00 | EC-100 | designed |
| TC-DEL-01.3 | 90 days | 450.00 | EC-100 | designed |
| TC-DEL-01.4 | 199 days | 995.00 | EC-100, cap - 1 day | designed |
| TC-DEL-01.5 | 200 days | 1,000.00 | EC-101, at cap | designed |
| TC-DEL-01.6 | 201 days | 1,000.00 (capped) | EC-101, cap + 1 day | designed |
| TC-DEL-01.7 | interest 500.00 overdue, principal 0.00 | 0.00 | EC-102 | designed |

### 6.2 FR-22 - days past due (BR-29) - TC-DEL-02

| TC | Input (due 2027-03-10) | Expected DPD | Status |
|---|---|---|---|
| TC-DEL-02.1 | business date 2027-03-09 | 0 | designed |
| TC-DEL-02.2 | 2027-03-10 (due date) | 0 | designed |
| TC-DEL-02.3 | 2027-03-11 (due + 1) | 1 | designed |

### 6.3 FR-23 - buckets and classification (BR-30) - TC-DEL-03

Five valid classes, one per bucket. Negative days past due are invalid.

| TC | DPD | Expected | Status |
|---|---|---|---|
| TC-DEL-03.1 | -1 | rejected | designed |
| TC-DEL-03.2 | 0 | Standard | designed |
| TC-DEL-03.3 | 1 / 30 | Watch | designed |
| TC-DEL-03.4 | 31 / 60 | Substandard | designed |
| TC-DEL-03.5 | 61 / 90 | Doubtful | designed |
| TC-DEL-03.6 | 91 | Loss | designed |

### 6.4 FR-24 - provisioning (BR-31) - TC-DEL-04

| TC | Classification, outstanding 10,000.00 | Expected provision | Status |
|---|---|---|---|
| TC-DEL-04.1 | Standard | 100.00 (1%) | designed |
| TC-DEL-04.2 | Watch | 500.00 (5%) | designed |
| TC-DEL-04.3 | Substandard | 2,500.00 (25%) | designed |
| TC-DEL-04.4 | Doubtful | 5,000.00 (50%) | designed |
| TC-DEL-04.5 | Loss | 10,000.00 (100%) | designed |

### 6.5 FR-25 - automatic write-off (BR-32) - TC-DEL-05

| TC | DPD | Expected | Status |
|---|---|---|---|
| TC-DEL-05.1 | 179 | not written off | designed |
| TC-DEL-05.2 | 180 | not written off ("more than 180") | designed |
| TC-DEL-05.3 | 181 | written off; outstanding principal moved to write-off | designed |

### 6.6 FR-26 - idempotent end of day (BR-33) - TC-DEL-06

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-DEL-06.1 | run end of day for 2027-03-11 once | penalties and snapshot written | designed |
| TC-DEL-06.2 | run it again for 2027-03-11 | no additional effect | designed; the unique key is automated in `SchemaMigratorTest#snapshotPerLoanPerDate` |
| TC-DEL-06.3 | run for 2027-03-12 | new accrual for the new date | designed |

### 6.7 FR-27 - classification-change alert (BR-34) - TC-DEL-07

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-DEL-07.1 | Watch to Substandard | one alert | designed |
| TC-DEL-07.2 | Substandard to Watch | one alert | designed |
| TC-DEL-07.3 | Watch to Watch | no alert | designed |

## 7. Reporting (owner: Hassan Khalid)

### 7.1 FR-28 - statement (BR-35) - TC-REP-01

Whether the range is inclusive at both ends is rule silent; inclusive is assumed and must be
confirmed.

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-REP-01.1 | entries before, on the start, inside, on the end and after the range | only the start, inside and end entries, with running balance | designed |
| TC-REP-01.2 | range with no entries | empty statement, opening balance shown | designed |
| TC-REP-01.3 | start after end | rejected (rule silent; proposed) | designed |

### 7.2 FR-29 - portfolio ratios (BR-36 to BR-38) - TC-REP-02

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-REP-02.1 | total outstanding 100,000.00; a 30-DPD loan of 5,000.00; a 31-DPD loan of 7,500.00 | PAR30 = 7.50% (30 DPD excluded, 31 included) | designed |
| TC-REP-02.2 | provision 3,000.00; non-Standard outstanding 12,500.00 | coverage 24.00% | designed |
| TC-REP-02.3 | collected 9,000.00; due 10,000.00 | efficiency 90.00% | designed |
| TC-REP-02.4 | denominator 0.00 for any ratio | rule silent; must be decided | not designed |

### 7.3 FR-30 and FR-32 - as-at reports and CSV export (BR-39) - TC-REP-03, TC-REP-04

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-REP-03.1 | report as at a past date, then post later entries | same report reproduced for that date | designed |
| TC-REP-04.1 | report exported | CSV with header and one row per loan | designed |

## 8. Quality requirements

These are verified by measurement or invariant tests rather than input partitioning. They are
listed so every RTM row has a test basis.

| QR | Black-box check | Status |
|---|---|---|
| QR-01 | 10,000 loans, 4 threads, 5 runs: median <= 20 s, none > 25 s | designed (`EndOfDayPerformanceTest`) |
| QR-02 | 30 golden schedules, 0 discrepancies | designed (`GoldenScheduleTest`) |
| QR-03 | failure at >= 10 injected points leaves the ledger balanced | automated: 14 points in `PostingFaultInjectionTest#failureLeavesLedgerBalancedWithNoOrphans` (TC-LED-04.4), plus `SqliteLedgerRepositoryTest#appendAllIsAtomic`; the demonstration half of the verification method is still owed |
| QR-05 | 0 mutators; 0 `UPDATE`/`DELETE` on `ledger_entry` | automated in part: `LedgerEntryTest#noMutators`, `LedgerImmutabilityTest#writerCanOnlyAppend`, `SqliteLedgerRepositoryTest#noMutatingSqlAgainstLedger`; the Fagan checklist item (INS-01) is still owed |
| QR-06 | 50 concurrent runs: 0 lost updates, 0 double accruals | designed (`EndOfDayConcurrencyTest`; the end-of-day engine is not built). Supporting concurrency checks exist on the posting side: `IdempotencyGuardPosterTest#concurrentDuplicatesForwardOnce`, `SqliteLoanRepositoryTest#saveWaitsForConcurrentWriter` |

QR-04 (complexity limits) is an analysis requirement and belongs to the static-analysis report
(`qa/static-analysis-report.md`). Constraint C-03 (no cycles between module packages) is not an
input domain either; it is enforced on every build by `ArchitectureTest#noCyclesBetweenModules`.

## 9. Coverage summary

Counts are equivalence classes and BVA cases defined in this document. "Automated" counts
test cases already implemented in JUnit at commit `87c5caf` (182 tests, all passing),
including partial automation at the command or schema level. The "designed only" column
includes TC-REP-02.4, which is blocked on a rule decision.

| Requirement | BR | EP classes | BVA cases | Test cases | Automated | Designed only |
|---|---|---|---|---|---|---|
| Money (common) | BR-40, BR-43 | 5 | 4 | 13 | 12 | 1 |
| LoanId (common) | - | 4 | 0 | 4 | 4 | 0 |
| FR-01 | BR-01, BR-02 | 4 | 6 | 7 | 0 | 7 |
| FR-02 | BR-03 | 4 | 3 | 6 | 0 | 6 |
| FR-03 | BR-04 | 3 | 3 | 4 | 0 | 4 |
| FR-04 | BR-05 | 3 | 0 | 3 | 0 | 3 |
| FR-05 | BR-08 | 3 | 0 | 9 | 0 | 9 |
| FR-06 | BR-09, BR-10 | 3 | 8 | 11 | 0 | 11 |
| FR-07 | BR-06, BR-07 | 6 | 6 | 7 | 0 | 7 |
| FR-13 | BR-18, BR-19 | 10 | 4 | 14 | 14 | 0 |
| FR-14 | BR-20 | 4 | 5 | 6 | 6 | 0 |
| FR-15 | BR-21, BR-22 | 6 | 4 | 6 | 3 | 3 (rebates and instalment source from `products`) |
| FR-16 | BR-23 | 5 | 1 | 6 | 6 | 0 (TC-LED-04.2 partly automated) |
| FR-17 | BR-24, BR-43 | 6 | 2 | 8 | 8 | 0 |
| FR-18 | BR-25 | 6 | 0 | 7 | 7 | 0 (pairs held in memory only) |
| FR-19 | BR-26 | 3 | 0 | 6 | 6 | 0 |
| FR-20 | - | 4 | 0 | 4 | 0 | 4 |
| FR-31 | - | 4 | 0 | 5 | 4 | 1 (per-file import audit) |
| FR-08 | BR-11, BR-17 | 3 | 0 | 3 | 0 | 3 |
| FR-09 | BR-12 | 4 | 6 | 6 | 0 | 6 |
| FR-10 | BR-13 | 3 | 5 | 5 | 0 | 5 |
| FR-11 | BR-14, BR-15 | 2 | 3 | 4 | 1 | 3 |
| FR-12 | BR-16 | 1 | 3 | 3 | 0 | 3 |
| FR-21 | BR-27, BR-28 | 3 | 6 | 7 | 0 | 7 |
| FR-22 | BR-29 | 2 | 3 | 3 | 0 | 3 |
| FR-23 | BR-30 | 6 | 9 | 6 | 0 | 6 |
| FR-24 | BR-31 | 5 | 0 | 5 | 0 | 5 |
| FR-25 | BR-32 | 2 | 3 | 3 | 0 | 3 |
| FR-26 | BR-33 | 2 | 0 | 3 | 0 | 3 (unique key automated at schema level) |
| FR-27 | BR-34 | 3 | 0 | 3 | 0 | 3 |
| FR-28 | BR-35 | 3 | 2 | 3 | 0 | 3 |
| FR-29 | BR-36 to BR-38 | 3 | 2 | 4 | 0 | 3 (1 blocked on a rule decision) |
| FR-30, FR-32 | BR-39 | 2 | 0 | 2 | 0 | 2 |
| **Total** | | **127** | **88** | **186** | **71** | **115** |

The ledger module (sections 4.1 to 4.9) has 62 test cases, of which 54 are automated. The 8
designed only are TC-LED-03.2, .4 and .5 (product rebates and the `products` instalment source, decision D-02),
TC-LED-08.1 to .4 (bank statement import, owner Muhammad Ibrahim) and TC-LED-09.3 (per-file
import audit). Every case in `origination`, `products`, `delinquency` and `reporting` is
designed only, because none of those modules is built on `develop` or PR #8 yet (Hassan's
`DueDateCalculator` is in open PR #6); they are the test basis for their owners.

### Rule gaps found while designing

These are inputs the business rules do not cover. Each needs a team decision, recorded as a
deviation under SQAP section 6.3, before its test can have an expected result:

1. Declared monthly income of 0.00 in the burden ratio (BR-04).
2. Payment larger than the full payoff: the excess beyond settlement (BR-21, BR-22). Until a
   rule exists, the poster refuses such a payment and writes nothing (TC-LED-03.6).
3. Zero denominators in PAR30, provision coverage and collection efficiency (BR-36 to BR-38).
4. Whether a statement date range includes both ends (BR-35).
5. The BR-18/BR-19 reading. **Resolved:** tier by tier, adopted by the team on 2026-11-16
   (decision D-01, recorded by Tughral Hussain).
6. Maximum length and character set of a loan ID.
7. Resolved by decision D-02 (16 Nov): the instalment source passed to the allocator lists only instalments due by the
   business date, not every remaining instalment (TC-LED-03.2).
