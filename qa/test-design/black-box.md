# MicroLend - Black-box Test Design (EP and BVA)

**Milestone:** M2 (SE431) - Black-box test design
**Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Version:** 0.4, 2026-11-22, refreshed against the integrated build (492 tests); 0.3 (2026-11-21) used `4067d2f`, 0.2 (2026-11-16) used `701c161`
**Sources:** `docs/business-rules.md` v1.1 (BR-01 to BR-43), `qa/rtm.md` (FR, QR and TC IDs),
`docs/M1-SE431-Quality-Requirements-RTM-SQAP.md`, and the tests under `src/test/java`.

## 1. Purpose and method

Cases come from the business rules alone; white-box design is in `qa/test-design/white-box.md`.
Equivalence partitioning (EP) gives each real input valid (`V`) and invalid (`I`) classes
`EC-xx`, each covered by at least one case. Boundary value analysis (BVA) tests each stated edge
at min-1 / min / min+1 / max-1 / max / max+1 for integers (ages, days, counts), in steps of
PKR 0.01 for money and 0.01 percentage points for percentages.

### Conventions

- **Test case IDs** keep the RTM prefix (`TC-LED-02.3` is the third case of `TC-LED-02`), so
  each case traces to its FR or QR and BR. Money and LoanId cases use `TC-CMN-xx`, tracing to
  BR-40, BR-43 and C-01.
- **Money** has scale 2, HALF_EVEN, as `Money` holds it; no case uses `double` or `float` (C-01).
- **Status** is *automated* (JUnit class and method) or *designed* (not yet automated);
  designed cases in other members' modules are their owners' test basis.
- **"Rule silent"** marks a boundary the rules do not state; the team decides it, and this
  design does not invent one.
- **Submission PDF.** The SE431 PDF prints a representative design for each module owner: Money,
  FR-01, FR-03, FR-06, FR-09, FR-10, FR-13, FR-14, FR-18, FR-21, FR-23 and FR-28, so its section
  numbers have gaps. This file, `qa/test-design/black-box.md`, holds every design and every case;
  section 9 counts all of them.

### Decisions this design depends on

| Decision | Effect on test design | Status |
|---|---|---|
| **BR-18/BR-19, tier by tier.** All penalties are settled oldest instalment first, then all fees oldest first, then all interest oldest first, then all principal oldest first. | Expected results of TC-LED-01 and TC-LED-02 | Adopted by the team on 2026-11-16 as decision D-01 (recorded by Tughral Hussain), noted under BR-19 in `docs/business-rules.md`; section 4.1 follows it. |
| Fees tier is always zero at M1 (BR-18) | A non-zero fee is an untestable class until a fee type is introduced by a recorded deviation | Frozen rule |
| Product amount and tenure bands are per product (BR-06, BR-07) | Boundaries are written relative to `min` and `max`; concrete values come from the product table | Product values not yet fixed in code |

## 2. Common inputs (`common` package)

### 2.1 Money - BR-40, BR-43, C-01

**Input:** the amount passed to `Money.of(BigDecimal)`.

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

<!-- pdf:skip -->
### 2.2 LoanId

**Input:** the string passed to `LoanId.of(String)`.

| EC | Class | Type |
|---|---|---|
| EC-06 | non-blank string | V |
| EC-07 | non-blank string with surrounding whitespace (trimmed) | V |
| EC-08 | blank or empty string | I |
| EC-09 | null | I |

Length and character set are rule silent: no upper boundary.

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-CMN-03.1 | null | `InvalidLoanIdException` | EC-09 | automated `LoanIdTest#rejectsNull` |
| TC-CMN-03.2 | `""`, `"   "` | `InvalidLoanIdException` | EC-08 | automated `LoanIdTest#rejectsBlank` |
| TC-CMN-03.3 | `"  L-001  "` | equals `LoanId.of("L-001")` | EC-07 | automated `LoanIdTest#trimsWhitespace` |
| TC-CMN-03.4 | `"L-001"` twice | equal by value | EC-06 | automated `LoanIdTest#comparesByValue` |

<!-- /pdf:skip -->

## 3. Origination (owner: Muhammad Ibrahim)

### 3.1 FR-01 - age at disbursement and at maturity (BR-01, BR-02) - TC-ORG-01

Ages are completed years (`Applicant.ageOn`, `LoanApplicationTest#ageInCompletedYears`).
Whether "65 at maturity" means 65 years and 0 days or any day before the 66th birthday is rule
silent.

| EC | Class | Type |
|---|---|---|
| EC-10 | age at disbursement >= 18 | V |
| EC-11 | age at disbursement < 18 | I |
| EC-12 | age at maturity <= 65 | V |
| EC-13 | age at maturity > 65 | I |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-01.1 | disbursement age 17 | rejected, age reason | EC-11, 18 - 1 | automated `AgeRuleTest#ageAtDisbursement` (row TC-ORG-01.1); one day short of 18 in `#dayBeforeEighteenthBirthday` |
| TC-ORG-01.2 | disbursement age 18 | passes BR-01 | EC-10, min | automated `AgeRuleTest#ageAtDisbursement` (row TC-ORG-01.2) |
| TC-ORG-01.3 | disbursement age 19 | passes BR-01 | EC-10, min + 1 | automated `AgeRuleTest#ageAtDisbursement` (row TC-ORG-01.3) |
| TC-ORG-01.4 | maturity age 64 | passes BR-02 | EC-12, max - 1 | automated `AgeRuleTest#ageAtMaturity` (row TC-ORG-01.4) |
| TC-ORG-01.5 | maturity age 65 | passes BR-02 | EC-12, max | automated `AgeRuleTest#ageAtMaturity` (row TC-ORG-01.5); one day short of 66 in `#dayBeforeSixtySixthBirthday` |
| TC-ORG-01.6 | maturity age 66 | rejected, maturity-age reason | EC-13, max + 1 | automated `AgeRuleTest#ageAtMaturity` (row TC-ORG-01.6) |
| TC-ORG-01.7 | age 40 at application, 66 at maturity | rejected (age is tested at maturity, not application) | EC-13 | automated `AgeRuleTest#ageIsTestedAtMaturityNotApplication` |

<!-- pdf:skip -->
### 3.2 FR-02 - active-loan limit (BR-03) - TC-ORG-02

**Input:** existing active loans, excluding the one applied for.

| EC | Class | Type |
|---|---|---|
| EC-14 | 0 or 1 existing active loans | V |
| EC-15 | 2 or more existing active loans | I |
| EC-16 | existing loans are written off only | V (not counted) |
| EC-17 | existing loan in arrears | counted as active |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-02.1 | 0 existing active | passes | EC-14, min | automated `ActiveLoanLimitRuleTest#existingActiveLoans` (row TC-ORG-02.1) |
| TC-ORG-02.2 | 1 existing active | passes | EC-14, max | automated `ActiveLoanLimitRuleTest#existingActiveLoans` (row TC-ORG-02.2) |
| TC-ORG-02.3 | 2 existing active | rejected | EC-15, max + 1 | automated `ActiveLoanLimitRuleTest#existingActiveLoans` (row TC-ORG-02.3) |
| TC-ORG-02.4 | 3 existing active | rejected | EC-15 | automated `ActiveLoanLimitRuleTest#existingActiveLoans` (row TC-ORG-02.4) |
| TC-ORG-02.5 | 1 active + 1 written off | passes BR-03 (rejected later by BR-05) | EC-16 | automated `ActiveLoanLimitRuleTest#writtenOffLoanIsNotActive`; the BR-05 rejection in `EligibilityEvaluatorTest#writtenOffLoanRejectedByWriteOffRule` |
| TC-ORG-02.6 | 1 active in arrears + 1 active | rejected | EC-17 | automated `ActiveLoanLimitRuleTest#loanInArrearsIsActive` |

<!-- /pdf:skip -->

### 3.3 FR-03 - instalment burden (BR-04) - TC-ORG-03

**Input:** monthly instalments including the new loan (base rate, requested amount) over
declared monthly income, 100,000.00 throughout.

| EC | Class | Type |
|---|---|---|
| EC-18 | burden <= 40.00% | V |
| EC-19 | burden > 40.00% | I |
| EC-20 | declared income 0.00 | rule silent (division by zero). The code refuses it at applicant creation (`LoanApplicationTest#rejectsZeroIncome`); a recorded team decision is still owed (rule gap 1) |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-03.1 | instalments 39,990.00 (39.99%) | passes | EC-18, max - 0.01 pp | automated `BurdenRatioRuleTest#burdenAtTheLimit` (row TC-ORG-03.1) |
| TC-ORG-03.2 | instalments 40,000.00 (40.00%) | passes | EC-18, max | automated `BurdenRatioRuleTest#burdenAtTheLimit` (row TC-ORG-03.2: existing 30,000.00 plus new 10,000.00) |
| TC-ORG-03.3 | instalments 40,010.00 (40.01%) | rejected, burden reason | EC-19, max + 0.01 pp | automated `BurdenRatioRuleTest#burdenAtTheLimit` (row TC-ORG-03.3); one paisa over 40% in `#onePaisaOverTheLimit`, rounded up in `LoanApplicationTest#burdenRoundsUp` |
| TC-ORG-03.4 | Grade C applicant, burden 40.00% at base rate | passes; the spread does not count (BR-04 uses the base rate) | EC-18 | automated `EligibilityEvaluatorTest#gradeCAtTheBurdenLimit` |

The new loan's instalment reaches the rule as a function (schedules belong to `products`); the tests supply a fixed amount.

<!-- pdf:skip -->
### 3.4 FR-04 - prior write-off (BR-05) - TC-ORG-04

| EC | Class | Type |
|---|---|---|
| EC-21 | no prior loans written off | V |
| EC-22 | at least one prior write-off | I |
| EC-23 | prior loan settled late but never written off | V |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-04.1 | no history | passes | EC-21 | automated `WriteOffHistoryRuleTest#noHistory` |
| TC-ORG-04.2 | one written-off loan, all else valid | rejected, write-off reason | EC-22 | automated `WriteOffHistoryRuleTest#priorWriteOff` |
| TC-ORG-04.3 | one loan settled 45 days late | passes BR-05 | EC-23 | automated `WriteOffHistoryRuleTest#settledLateIsNotAWriteOff` |

### 3.5 FR-05 - typed rejection reason in rule order (BR-08) - TC-ORG-05

| EC | Class | Type |
|---|---|---|
| EC-24 | passes all rules | V (approved) |
| EC-25 | fails exactly one rule (one class per rule BR-01 to BR-07) | I |
| EC-26 | fails several rules | I (reason names the first in order) |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-05.1 to .7 | one case per rule BR-01 to BR-07 failing alone | reason names that rule | EC-25 | automated `EligibilityEvaluatorTest#ageAtDisbursementAlone`, `#ageAtMaturityAlone`, `#activeLoanLimitAlone`, `#burdenRatioAlone`, `#priorWriteOffAlone`, `#amountBandAlone`, `#tenureBandAlone` |
| TC-ORG-05.8 | age 17 and a prior write-off | reason names BR-01 (evaluated first) | EC-26 | automated `EligibilityEvaluatorTest#ageBeforeWriteOff` |
| TC-ORG-05.9 | 2 active loans and burden 45% | reason names BR-03 | EC-26 | automated `EligibilityEvaluatorTest#activeLoansBeforeBurden` |

EC-24 is automated in `EligibilityEvaluatorTest#approvesEligibleApplication`; `#stopsAtFirstFailure` shows later rules are skipped.

<!-- /pdf:skip -->

### 3.6 FR-06 - risk grade (BR-09, BR-10) - TC-ORG-06

| EC | Class | Type |
|---|---|---|
| EC-27 | Grade A: burden <= 25.00%, at least one earlier loan, worst DPD <= 30 | V |
| EC-28 | Grade B: burden <= 35.00%, worst DPD <= 60, including first-time borrowers | V |
| EC-29 | Grade C: every other applicant passing BR-01 to BR-07 | V |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-06.1 | burden 25.00%, 1 earlier loan, worst DPD 30 | A, +0.00%, up to 100% | EC-27, both max | automated `RiskGradeTest#assignsGrade` (row TC-ORG-06.1); spread and cap in `#spreadAndCap` |
| TC-ORG-06.2 | burden 25.01%, worst DPD 30 | B | EC-28, burden A max + 0.01 | automated `RiskGradeTest#assignsGrade` (row TC-ORG-06.2); through the evaluator in `EligibilityEvaluatorTest#gradeUsesTheBurdenRatio` |
| TC-ORG-06.3 | burden 20.00%, worst DPD 31 | B | EC-28, DPD A max + 1 | automated `RiskGradeTest#assignsGrade` (row TC-ORG-06.3) |
| TC-ORG-06.4 | burden 20.00%, no earlier loan | B (first-time borrower) | EC-28 | automated `RiskGradeTest#assignsGrade` (row TC-ORG-06.4) |
| TC-ORG-06.5 | burden 35.00%, worst DPD 60 | B, +1.50%, up to 80% | EC-28, both max | automated `RiskGradeTest#assignsGrade` (row TC-ORG-06.5); spread and cap in `#spreadAndCap` |
| TC-ORG-06.6 | burden 35.01%, worst DPD 0 | C | EC-29, burden B max + 0.01 | automated `RiskGradeTest#assignsGrade` (row TC-ORG-06.6) |
| TC-ORG-06.7 | burden 20.00%, worst DPD 61 | C, +3.00%, up to 60% | EC-29, DPD B max + 1 | automated `RiskGradeTest#assignsGrade` (row TC-ORG-06.7); spread and cap in `#spreadAndCap` |

**Capped amount (BR-10)**, test product band 10,000.00 to 100,000.00:

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-ORG-06.8 | Grade B, requested 50,000.00 | approved 40,000.00 (80%) | requested above cap | automated `EligibilityEvaluatorTest#gradeBIsCappedAtEightyPercent` |
| TC-ORG-06.9 | Grade A, requested 50,000.00 | approved 50,000.00 | within cap | automated `EligibilityEvaluatorTest#gradeAIsNotCapped` |
| TC-ORG-06.10 | Grade C, requested 16,666.00, min 10,000.00 | cap 9,999.60, rejected `BELOW_PRODUCT_MINIMUM` | cap just below minimum | automated `EligibilityEvaluatorTest#capBelowProductMinimum` |
| TC-ORG-06.11 | Grade C, requested 16,666.67, min 10,000.00 | cap 10,000.00, approved | cap at minimum | automated `EligibilityEvaluatorTest#capAtProductMinimum` |

Applying the grade's spread at loan creation (queue item I-03) is not built; `products` schedules at the base rate (DEF-025).

<!-- pdf:skip -->
### 3.7 FR-07 - product amount and tenure bands (BR-06, BR-07) - TC-ORG-07

Bands are per product and inclusive; the test product's are 10,000.00 to 100,000.00 and 6 to
24 months.

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
| TC-ORG-07.1 | amount min - 0.01 | rejected, amount reason | EC-31 | automated `ProductBandRuleTest#amountBand` (row TC-ORG-07.1: 9,999.99) |
| TC-ORG-07.2 | amount min | passes | EC-30, min | automated `ProductBandRuleTest#amountBand` (row TC-ORG-07.2: 10,000.00) |
| TC-ORG-07.3 | amount max | passes | EC-30, max | automated `ProductBandRuleTest#amountBand` (row TC-ORG-07.3: 100,000.00) |
| TC-ORG-07.4 | amount max + 0.01 | rejected, amount reason | EC-32 | automated `ProductBandRuleTest#amountBand` (row TC-ORG-07.4: 100,000.01) |
| TC-ORG-07.5 | tenure band min - 1 | rejected, tenure reason | EC-34 | automated `ProductBandRuleTest#tenureBand` (row TC-ORG-07.5: 5 months) |
| TC-ORG-07.6 | tenure band min, band max | passes | EC-33 | automated `ProductBandRuleTest#tenureBand` (rows TC-ORG-07.6: 6 and 24 months) |
| TC-ORG-07.7 | tenure band max + 1 | rejected, tenure reason | EC-35 | automated `ProductBandRuleTest#tenureBand` (row TC-ORG-07.7: 25 months) |

A band whose minimum exceeds its maximum is refused by `ProductBand`
(`LoanApplicationTest#rejectsInvalidAmountBand`, `#rejectsInvalidTenureBand`) and by the schema
(`SchemaMigratorTest#productBands`).

<!-- /pdf:skip -->

## 4. Ledger (owner: Tughral Hussain)

### 4.1 FR-13 - allocation waterfall, oldest first (BR-18, BR-19) - TC-LED-01

**Inputs:** payment and the unpaid instalments as `InstalmentDue` records (sequence, due date,
penalty, fees, interest, principal), read tier by tier (D-01). Fees are always 0.00 at M1 but
still ordered, so TC-LED-01.2 uses a non-zero fee. Values are those `RepaymentAllocatorTest`
asserts; I1 is due 2026-01-15, I2 2026-02-15, I3 2026-03-15.

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

**Supporting value objects** (`Allocation` holds the four tiers):

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-LED-01.10 | four tier amounts | each tier exposed; total is their sum | automated `AllocationTest#exposesEachTier`, `#totalIsSumOfTiers` |
| TC-LED-01.11 | all tiers 0.00 | valid, total 0.00 | automated `AllocationTest#allZeroAllocation` |
| TC-LED-01.12 | any tier null | `InvalidAllocationException` | automated `AllocationTest#rejectsNullTiers` |
| TC-LED-01.13 | principal tier vs outstanding principal 1,000.00 | 1,000.00 leaves 0.00; 1,000.01 rejected | automated `LoanAccountTest#exactPrincipalLeavesZero`, `#principalAboveOutstandingRejected` |
| TC-LED-01.14 | `InstalmentDue` with sequence 0 (min - 1), or a null date or amount | `InvalidAllocationException`; a valid one totals its four amounts | automated `RepaymentAllocatorTest#instalmentDueInvariants` (EC-83) |

### 4.2 FR-14 - partial payment (BR-20) - TC-LED-02

**Input:** a payment against two overdue instalments due 2,230.00 in total
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

<!-- pdf:skip -->
### 4.3 FR-15 - overpayment and early settlement (BR-21, BR-22) - TC-LED-03

**Inputs:** payment; amount due now (460.00); payoff (2,000.00); product type.

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
| TC-LED-03.2 | 1,999.99 | advance credit; loan stays active | EC-51, payoff - 0.01 | designed. `ScheduleInstalmentsDueSource` now lists only instalments due by the business date (D-02, `ScheduleInstalmentsDueSourceTest#dueTodayListed`), but no test pays payoff - 0.01; it also lists paid instalments again (DEF-024) |
| TC-LED-03.3 | 2,000.00, flat-rate | settled (loan CLOSED on the business date); rebate 0.00 | EC-52, EC-54 | automated with a stub rebate: `SettlementPolicyTest#exactPayoffClosesTheLoan`, `#zeroRebate`, `CoreRepaymentPosterTest#exactPayoffClosesLoan`; the flat-rate product's 0.00 in `EarlySettlementRebateTest#flatRateGivesNoRebate`, `ProductEarlySettlementRebateTest#delegatesToTheLoansProduct` |
| TC-LED-03.4 | 2,000.00, reducing-balance | settled; unearned interest refunded | EC-52, EC-55 | designed; the rebate amount alone in `EarlySettlementRebateTest#reducingFullRefund`, `#reducingMidPeriod`, `#reducingAroundADueDate`, `#reducingAtMaturity` |
| TC-LED-03.5 | 2,000.00, bullet | settled; unaccrued monthly interest refunded | EC-52, EC-56 | designed; the rebate amount alone in `EarlySettlementRebateTest#bulletRefundsUnaccruedMonths` |
| TC-LED-03.6 | 2,000.01 | policy reports 0.01 excess (not advance credit); the poster refuses the payment with `InvalidRepaymentException` and writes nothing | EC-53, payoff + 0.01 | automated `SettlementPolicyTest#payoffPlusOneIsExcess`, `CoreRepaymentPosterTest#excessOverPayoffIsRefused` |

TC-LED-03.4 and .5 stay designed: the per-product rebate (D-03) is tested alone and by loan
(`ProductEarlySettlementRebateTest#delegatesToTheLoansProduct`), but no test settles a real
product's loan at payoff or payoff + 0.01, and the refund is not written to the ledger
(DEF-013). With a grace period the rebate also refunds earned grace interest (DEF-026).

### 4.4 FR-16 - balanced posting (BR-23) - TC-LED-04

BR-23 is an invariant, not an input domain, so EP partitions the posting paths.

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
| TC-LED-04.2 | bank payment HBL / TX-1, 50.00, through the import chain | posted once by the core poster | EC-58 | partly automated: `CompositionRootTest#importChainGuardsOutermost` posts it on SQLite; `StatementImportWiringTest#importsThroughTheProductionChain` imports an HBL statement through the production chain and asserts the loan's new principal. Neither asserts the ledger balance after an import |
| TC-LED-04.3 | reversal of a posted entry | each column nets to zero; original kept | EC-59 | automated `LedgerImmutabilityTest#correctionKeepsOriginalAndNetsToZero` |
| TC-LED-04.4 | failure injected at each of 14 points: every line of postings of 60.00, 1,300.00 and 2,210.00 (11 line points), and the loan save after each (3 points) | ledger still balanced; loan unchanged; 0 unreversed lines of the failed posting | EC-60, QR-03 | automated `PostingFaultInjectionTest#failureLeavesLedgerBalancedWithNoOrphans` (real SQLite file) |
| TC-LED-04.5 | debit 10.00 against credit 9.99 | `LedgerImbalanceException` | EC-85, imbalance of 0.01 (BVA) | automated `CoreRepaymentPosterTest#imbalanceDetected` |
| TC-LED-04.6 | ledger write fails; last line of a batch is invalid | loan not saved; no line of the batch written | EC-60 | automated `CoreRepaymentPosterTest#failedWriteDoesNotSaveLoan`, `SqliteLedgerRepositoryTest#appendAllIsAtomic` |

### 4.5 FR-17 - immutable entries and compensating reversals (BR-24, BR-43) - TC-LED-05

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

<!-- /pdf:skip -->

### 4.6 FR-18 - duplicate (bank, reference) rejected (BR-25) - TC-LED-06

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
| TC-LED-06.3 | HBL / REF-1 again | `DuplicatePaymentException`; not forwarded | EC-69 | automated `IdempotencyGuardPosterTest#samePairTwice`, `CompositionRootTest#importChainGuardsOutermost`; from a statement file in `StatementImportJobTest#duplicateInSecondFileRefused` (the rest of the file is posted) and `StatementImportWiringTest#secondImportIsRefused` (production chain, SQLite); across a restart in `SqlitePaymentReferencesTest#claimSurvivesRestart` and `StatementImportWiringTest#secondImportAfterRestartIsRefused`; schema key `SchemaMigratorTest#paymentIdempotencyKey`. Since `8401031` the claimed pairs are kept in SQLite (`payment_reference`, DEF-012 fixed) |
| TC-LED-06.4 | HBL / REF-1 then MCB / REF-1 | both accepted | EC-70 | automated `RepaymentCommandTest#sameReferenceFromTwoBanksDiffers`, `IdempotencyGuardPosterTest#sameReferenceTwoBanks`, `StatementImportJobTest#sameReferenceFromTwoBanksAccepted`, `SqlitePaymentReferencesTest#sameReferenceTwoBanks` |
| TC-LED-06.5 | bank "" or reference "  " | `InvalidRepaymentException` | EC-71 | automated `RepaymentCommandTest#rejectsBlankBankOrReference` |
| TC-LED-06.6 | HBL / REF-1 posting fails, then is retried | retry accepted | EC-86 | automated `IdempotencyGuardPosterTest#failedPostingReleasesReference`; on SQLite `SqlitePaymentReferencesTest#failedPostingReleasesTheStoredPair`, `#releaseFreesThePair` |
| TC-LED-06.7 | 8 threads post HBL / REF-1 at the same moment | forwarded exactly once; 7 duplicates refused | EC-69 under concurrency | automated `IdempotencyGuardPosterTest#concurrentDuplicatesForwardOnce`; on SQLite `SqlitePaymentReferencesTest#concurrentClaimsHaveOneWinner` (8 threads, one winner) |

Known limitation (M3): a process stop between claim and posting leaves the pair claimed, so a
later import of that row is refused; writing the `payment` row in the posting transaction fixes
it.

<!-- pdf:skip -->
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

No business rule fixes the statement formats (rule silent), so EP only.

| EC | Class | Type |
|---|---|---|
| EC-75 | well-formed HBL statement | V |
| EC-76 | well-formed MCB statement | V |
| EC-77 | unsupported bank format | I |
| EC-78 | supported format with a malformed row (missing amount, bad date) | I |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-LED-08.1, .2 | one valid file per bank | one `RepaymentCommand` per row | EC-75, EC-76 | automated `HblStatementAdapterTest#mapsCreditRows` (TC-LED-08.1: CR rows only, paisa to rupees, `ddMMyyyy`), `McbStatementAdapterTest#mapsCreditRows` (TC-LED-08.2: positive rows only, `dd/MM/yyyy`); posted by `StatementImportJobTest#postsEveryCommand` and, on SQLite through the production chain, `StatementImportWiringTest#importsThroughTheProductionChain` |
| TC-LED-08.3 | unknown format | typed rejection, nothing posted | EC-77 | automated `HblStatementAdapterTest#otherBanksFormat`, `McbStatementAdapterTest#otherBanksFormat` (each refuses the other bank's file), `StatementImportWiringTest#unknownBank` (no adapter for the bank), `HblStatementAdapterTest#missingColumn` |
| TC-LED-08.4 | malformed row | row rejected with its line number | EC-78 | automated `HblStatementAdapterTest#malformedRow` (9 rows: bad or zero or negative amount, bad date, wrong date format, unknown direction, blank reference, blank loan, missing values), `McbStatementAdapterTest#malformedRow` (6 rows), `HblStatementAdapterTest#brokenQuoting`; the physical line is named even after blank lines (`#physicalLineNumbers` in both adapter tests, DEF-019); nothing from the file is posted in `StatementImportJobTest#malformedFilePostsNothing` |

Also EC-75/EC-76: a leading UTF-8 byte-order mark is accepted (`#byteOrderMark` in both adapter
tests, DEF-020).

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
| TC-LED-09.3 | file with 3 rows imported | exactly 1 audit line | EC-81 | designed. `StatementImportJob` exists (FR-20) but writes no audit line per file; the import chain has no per-posting audit (`CompositionRootTest#importChainGuardsOutermost`) |
| TC-LED-09.4 | posting refused by the wrapped poster | audit line with the reason; same exception rethrown | EC-87 | automated `AuditLoggingPosterTest#auditsRefusal` |
| TC-LED-09.5 | operator named on the teller web form | the posting is made as that operator, and the name does not carry over to the next request | EC-79 | automated `TellerWebServerTest#postsCashPayment` |

<!-- /pdf:skip -->

## 5. Products and schedules (owner: Hassan Khalid)

<!-- pdf:skip -->
### 5.1 FR-08 - three products (BR-11, BR-17) - TC-PRD-01

EP: flat-rate, reducing-balance and bullet are valid; any other type is invalid. No BVA:
schedules are golden files (QR-02) and the fixed rate (BR-17) has no boundary.

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-PRD-01.1 to .3 | 100,000.00, 12 months, one per product | matches golden schedule | automated `GoldenScheduleTest#flatRate`, `#reducingBalance`, `#bullet` |

<!-- /pdf:skip -->

### 5.2 FR-09 - due dates (BR-12) - TC-PRD-02

| EC | Class | Type |
|---|---|---|
| EC-90 | disbursement day 1 to 28 | V, same day every month |
| EC-91 | disbursement day 29 to 31 | V, clamped in short months |
| EC-92 | February, leap year | V |
| EC-93 | February, non-leap year | V |

| TC | Disbursement | Expected first dues | Covers | Status |
|---|---|---|---|---|
| TC-PRD-02.1 | 2027-01-28 | 2027-02-28, 2027-03-28 | EC-90, max | automated `DueDateCalculatorTest#disbursementOnThe28th` (same dates in 2025) |
| TC-PRD-02.2 | 2027-01-29 | 2027-02-28, 2027-03-29 | EC-91, EC-93 | partly automated: `DueDateCalculatorTest#disbursementOnThe29th` gives 2025-02-28 for 2025-01-29; 29 March is not asserted |
| TC-PRD-02.3 | 2027-01-30 | 2027-02-28, 2027-03-30 | EC-91 | automated `DueDateCalculatorTest#disbursementOnThe30th` (same dates in 2025) |
| TC-PRD-02.4 | 2027-01-31 | 2027-02-28, 2027-03-31, 2027-04-30 | EC-91, max | automated `GoldenScheduleTest#flatRate`, `#reducingBalance`, `#bullet` (disbursed 2027-01-31); `DueDateCalculatorTest#disbursementOnThe31st` (2025) |
| TC-PRD-02.5 | 2028-01-31 | 2028-02-29 | EC-92 | designed |
| TC-PRD-02.6 | 2028-01-29 | 2028-02-29 | EC-92 | automated `DueDateCalculatorTest#disbursementOnThe29th` |

### 5.3 FR-10 - grace period (BR-13) - TC-PRD-03

| EC | Class | Type |
|---|---|---|
| EC-94 | grace 0 to 3 months | V |
| EC-95 | grace > 3 | I |
| EC-96 | grace < 0 | I (implied by the 0 to 3 range) |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-PRD-03.1 | grace -1 | rejected | EC-96, min - 1 | automated `GracePeriodPolicyTest#outOfRangeRejected` |
| TC-PRD-03.2 | grace 0 | first instalment one month after disbursement | EC-94, min | automated `GracePeriodPolicyTest#graceZero` |
| TC-PRD-03.3 | grace 1 | first instalment deferred one month; accrued interest added to it | EC-94, min + 1 | automated `GracePeriodPolicyTest#graceOne` (date), `LoanProductTest#graceReducing` (interest 12.00 + 12.00 accrued) |
| TC-PRD-03.4 | grace 3 | deferred three months | EC-94, max | automated `GracePeriodPolicyTest#graceThree`, `LoanProductTest#graceDefersAndAccrues` |
| TC-PRD-03.5 | grace 4 | rejected | EC-95, max + 1 | automated `GracePeriodPolicyTest#outOfRangeRejected`, `LoanProductTest#graceFourRefused` |

<!-- pdf:skip -->
### 5.4 FR-11 - sum invariant and rounding (BR-14, BR-15) - TC-PRD-04

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-PRD-04.1 | 10,000.00 over 3 months, flat-rate | instalments sum exactly to principal + interest; residue in the last | BR-14, amount not divisible by tenure | automated `LoanProductTest#flatTotalInterest` (300.00; last principal 3,333.34), `#sumInvariant` |
| TC-PRD-04.2 | a value ending .005, e.g. 0.125 | 0.12 (HALF_EVEN) | BR-15 | automated `FlatRateCalculatorTest#halfEvenAtTheBoundary` (0.025 to 0.02, 0.005 to 0.00) |
| TC-PRD-04.3 | 0.135 | 0.14 | BR-15 | automated `FlatRateCalculatorTest#halfEvenAtTheBoundary` (0.015 to 0.02), `GracePeriodPolicyTest#accruedInterest` |
| TC-PRD-04.4 | a value at scale 3 passed to `Money.of` | rejected; callers must round first | BR-15, BR-40 | automated `MoneyTest#rejectsExcessiveScale` |

BR-14 fails at very low flat rates (DEF-027); `#sumInvariant` checks the schedule against its
own total, so it cannot see this.

### 5.5 FR-12 - instalment count (BR-16) - TC-PRD-05

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-PRD-05.1 | tenure 1 | exactly 1 instalment | automated `LoanProductTest#instalmentCount` (each product) |
| TC-PRD-05.2 | tenure 6, grace 3 | exactly 6 instalments | automated `LoanProductTest#graceKeepsInstalmentCount` (each product) |
| TC-PRD-05.3 | tenure 36 | exactly 36 instalments | automated `LoanProductTest#instalmentCount` (each product) |

The schema side, one row per (loan, sequence), is automated in `SchemaMigratorTest#instalmentSequenceUnique`.

<!-- /pdf:skip -->

## 6. Delinquency (owner: Muhammad Ibrahim)

### 6.1 FR-21 - penalty accrual and cap (BR-27, BR-28) - TC-DEL-01

Overdue principal 10,000.00 accrues 5.00 a day (0.05%) up to the cap of 10% of the overdue
amount, 1,000.00, reached on day 200. Accrual is assumed simple (non-compounding), as the rule
accrues on principal only.

| EC | Class | Type |
|---|---|---|
| EC-100 | overdue principal > 0, accrual below cap | V |
| EC-101 | accrual at or above the cap | V, capped |
| EC-102 | overdue interest only, principal 0.00 | V, no penalty |

| TC | Input | Expected | Covers | Status |
|---|---|---|---|---|
| TC-DEL-01.1 | 1 day | 5.00 | EC-100, min | automated `PenaltyAccruerTest#accruedUpToTheCap` (row 1 day) |
| TC-DEL-01.2 | 30 days | 150.00 | EC-100 | automated `PenaltyAccruerTest#accruedUpToTheCap` (row 30) |
| TC-DEL-01.3 | 90 days | 450.00 | EC-100 | automated `PenaltyAccruerTest#accruedUpToTheCap` (row 90) |
| TC-DEL-01.4 | 199 days | 995.00 | EC-100, cap - 1 day | automated `PenaltyAccruerTest#accruedUpToTheCap` (row 199) |
| TC-DEL-01.5 | 200 days | 1,000.00 | EC-101, at cap | automated `PenaltyAccruerTest#accruedUpToTheCap` (row 200) |
| TC-DEL-01.6 | 201 days | 1,000.00 (capped) | EC-101, cap + 1 day | automated `PenaltyAccruerTest#accruedUpToTheCap` (row 201); the daily amount on day 201 is 0.00 in `#noAccrualPastTheCap` |
| TC-DEL-01.7 | interest 500.00 overdue, principal 0.00 | 0.00 | EC-102 | automated `PenaltyAccruerTest#interestOnlyAccruesNothing` |

Also automated in `PenaltyAccruerTest`: 0 days gives 0.00 (row 0 of `#accruedUpToTheCap`);
negative days are rejected (`#negativeDaysRejected`); due today is not overdue
(`#dueTodayIsNotOverdue`); each overdue instalment adds its own day
(`#oneDayPerOverdueInstalment`); 210 daily amounts on 333.33 sum exactly to the cumulative
penalty (`#dailyAmountsSumToCumulative`); skipped days are caught up on a first run
(`#firstRunAccruesEveryOverdueDay`) and after a gap (`#catchesUpSkippedDates`; at engine level
`EndOfDayEngineTest#catchesUpSkippedDates`: 10,000.00 overdue, runs four days apart, 25.00 in
all per loan; DEF-018). End of day posts a balanced `PENALTY_ACCRUAL` / `PENALTY_INCOME` pair
(`EndOfDayEngineTest#processesEveryLoan`, `SqliteDailyResultStoreTest#recordsSnapshotAndPenalty`).

<!-- pdf:skip -->
### 6.2 FR-22 - days past due (BR-29) - TC-DEL-02

| TC | Input (due 2027-03-10) | Expected DPD | Status |
|---|---|---|---|
| TC-DEL-02.1 | business date 2027-03-09 | 0 | automated `DaysPastDueCalculatorTest#countedFromTheDayAfterTheDueDate` (row 2027-03-09) |
| TC-DEL-02.2 | 2027-03-10 (due date) | 0 | automated `DaysPastDueCalculatorTest#countedFromTheDayAfterTheDueDate` (row 2027-03-10) |
| TC-DEL-02.3 | 2027-03-11 (due + 1) | 1 | automated `DaysPastDueCalculatorTest#countedFromTheDayAfterTheDueDate` (row 2027-03-11; also 2027-04-09 gives 30) |

Also automated: the oldest instalment owing principal or interest sets the count
(`#oldestUnpaidInstalmentDrives`), penalty alone does not (`#penaltyOnlyIsNotPastDue`), and
nothing owing gives 0 (`#nothingOwing`).

<!-- /pdf:skip -->

### 6.3 FR-23 - buckets and classification (BR-30) - TC-DEL-03

EP: one valid class per bucket (five); negative DPD is invalid.

| TC | DPD | Expected | Status |
|---|---|---|---|
| TC-DEL-03.1 | -1 | rejected | automated `DelinquencyClassifierTest#negativeRejected` |
| TC-DEL-03.2 | 0 | Standard | automated `DelinquencyClassifierTest#bucketBoundaries` (row 0) |
| TC-DEL-03.3 | 1 / 30 | Watch | automated `DelinquencyClassifierTest#bucketBoundaries` (rows 1, 30) |
| TC-DEL-03.4 | 31 / 60 | Substandard | automated `DelinquencyClassifierTest#bucketBoundaries` (rows 31, 60) |
| TC-DEL-03.5 | 61 / 90 | Doubtful | automated `DelinquencyClassifierTest#bucketBoundaries` (rows 61, 90) |
| TC-DEL-03.6 | 91 | Loss | automated `DelinquencyClassifierTest#bucketBoundaries` (rows 91, 400) |

Basis paths of `classify` for these inputs: `qa/test-design/white-box.md`, section 6.

<!-- pdf:skip -->
### 6.4 FR-24 - provisioning (BR-31) - TC-DEL-04

| TC | Classification, outstanding 10,000.00 | Expected provision | Status |
|---|---|---|---|
| TC-DEL-04.1 | Standard | 100.00 (1%) | automated `ProvisioningAndWriteOffTest#provisionByClassification` (row STANDARD) |
| TC-DEL-04.2 | Watch | 500.00 (5%) | automated `ProvisioningAndWriteOffTest#provisionByClassification` (row WATCH) |
| TC-DEL-04.3 | Substandard | 2,500.00 (25%) | automated `ProvisioningAndWriteOffTest#provisionByClassification` (row SUBSTANDARD) |
| TC-DEL-04.4 | Doubtful | 5,000.00 (50%) | automated `ProvisioningAndWriteOffTest#provisionByClassification` (row DOUBTFUL) |
| TC-DEL-04.5 | Loss | 10,000.00 (100%) | automated `ProvisioningAndWriteOffTest#provisionByClassification` (row LOSS) |

`ProvisioningPolicy` has no caller yet; the first will be the FR-29 provision-coverage ratio.

### 6.5 FR-25 - automatic write-off (BR-32) - TC-DEL-05

| TC | DPD | Expected | Status |
|---|---|---|---|
| TC-DEL-05.1 | 179 | not written off | automated `ProvisioningAndWriteOffTest#notYetWrittenOff` |
| TC-DEL-05.2 | 180 | not written off ("more than 180") | automated `ProvisioningAndWriteOffTest#notYetWrittenOff` |
| TC-DEL-05.3 | 181 | written off; outstanding principal moved to write-off | partly automated: the decision in `ProvisioningAndWriteOffTest#writtenOffAfter180`, and the loan listed as due for write-off in the end-of-day report in `EndOfDayEngineTest#writeOffDueIsReported`. The write-off posting itself (loan status and the principal moved) is not built (team decision open item T-03) |

### 6.6 FR-26 - idempotent end of day (BR-33) - TC-DEL-06

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-DEL-06.1 | run end of day for 2027-03-11 once | penalties and snapshot written | automated `EndOfDayEngineTest#processesEveryLoan` (20 loans), `SqliteDailyResultStoreTest#recordsSnapshotAndPenalty` (snapshot and penalty pair in one transaction) |
| TC-DEL-06.2 | run it again for 2027-03-11 | no additional effect | automated `EndOfDayEngineTest#idempotentPerBusinessDate`; an earlier date run after a later one also records nothing (`#earlierDateAfterALaterRunIsSkipped`); `SqliteDailyResultStoreTest#secondRecordIsANoOp`, `#concurrentRecordsForTheSameDate` (two connections at once, exactly one wins); the unique key in `SchemaMigratorTest#snapshotPerLoanPerDate` |
| TC-DEL-06.3 | run for 2027-03-12 | new accrual for the new date | automated `EndOfDayEngineTest#idempotentPerBusinessDate` (the next date records all 5 loans again) |

In `SqliteDailyResultStoreTest#oneTransaction` a failing ledger line rolls the snapshot back too.

### 6.7 FR-27 - classification-change alert (BR-34) - TC-DEL-07

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-DEL-07.1 | Watch to Substandard | one alert | automated `EndOfDayEngineTest#classificationChangeEvents` (31 days after Watch) |
| TC-DEL-07.2 | Substandard to Watch | one alert | automated `EndOfDayEngineTest#classificationChangeEvents` (30 days after Substandard) |
| TC-DEL-07.3 | Watch to Watch | no alert | automated `EndOfDayEngineTest#classificationChangeEvents` (30 days after Watch) |

The earlier classification comes from the last snapshot (`SqliteDailyResultStoreTest#lastSnapshot`).
The alert, a `ClassificationChangeEvent` in the end-of-day report, is shown on the end-of-day
page (`TellerWebServerTest#endOfDayRuns`); a run that fails as a whole shows a 503 page naming
the cause (`TellerWebServerTest#endOfDayFailureIsReported`, DEF-021).

<!-- /pdf:skip -->

## 7. Reporting (owner: Hassan Khalid)

### 7.1 FR-28 - statement (BR-35) - TC-REP-01

Both ends inclusive is assumed until confirmed (rule silent).

| TC | Input | Expected | Status |
|---|---|---|---|
| TC-REP-01.1 | entries before, on the start, inside, on the end and after the range | only the start, inside and end entries, with running balance | automated `StatementRendererTest#rangeIsInclusiveAtBothEnds`, `#runningBalanceLineByLine`; the balance passes only on one-sided test entries and is always nil on real balanced postings (DEF-028) |
| TC-REP-01.2 | range with no entries | empty statement, opening balance shown | automated `StatementRendererTest#emptyRangeShowsOpeningBalance` (same DEF-028 limit) |
| TC-REP-01.3 | start after end | rejected (rule silent; proposed) | automated `StatementRendererTest#startAfterEndRefused` |

<!-- pdf:skip -->
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

Verified by measurement or invariant tests, not partitioning; listed so every RTM row has a
test basis.

| QR | Black-box check | Status |
|---|---|---|
| QR-01 | 10,000 loans, 4 threads, 5 runs: median <= 20 s, none > 25 s | designed (`EndOfDayPerformanceTest`) |
| QR-02 | 30 golden schedules, 0 discrepancies | partly automated: 3 (one per product, TC-PRD-01) in `GoldenScheduleTest`; the other 27 are an M3 target |
| QR-03 | failure at >= 10 injected points leaves the ledger balanced | automated: 14 points in `PostingFaultInjectionTest#failureLeavesLedgerBalancedWithNoOrphans` (TC-LED-04.4), plus `SqliteLedgerRepositoryTest#appendAllIsAtomic`; the demonstration half of the verification method is still owed |
| QR-05 | 0 mutators; 0 `UPDATE`/`DELETE` on `ledger_entry` | automated in part: `LedgerEntryTest#noMutators`, `LedgerImmutabilityTest#writerCanOnlyAppend`, `SqliteLedgerRepositoryTest#noMutatingSqlAgainstLedger`; the Fagan checklist item (INS-01) is still owed |
| QR-06 | 50 concurrent runs: 0 lost updates, 0 double accruals | automated, one run per build: `ConcurrentTellerAndEndOfDayTest#tellersAndEndOfDayTogether`. Four teller threads each post 10.00 five times to each of 5 loans while end of day runs on them, through the production wiring on one SQLite file. It asserts each loan's expected principal (no posting lost), total debits = total credits, 5 snapshots and exactly one penalty pair per loan (no double accrual). With `LockingPoster` removed it failed 3 of 3 runs with lost postings. The 50-run repetition is not yet set up. Supporting checks: `EndOfDayEngineTest#runsFourTasksInParallel`, `#workRunsUnderTheAccountLock`, `LockingPosterTest`, `LockRegistryTest#concurrentRequestsShareOneLock`, `SqliteDailyResultStoreTest#concurrentRecordsForTheSameDate`, `IdempotencyGuardPosterTest#concurrentDuplicatesForwardOnce`, `SqliteLoanRepositoryTest#saveWaitsForConcurrentWriter` |

QR-04 (complexity limits) is verified by analysis (`qa/static-analysis-report.md`); C-03 (no
package cycles) by `ArchitectureTest#noCyclesBetweenModules` on every build.

<!-- /pdf:skip -->

## 9. Coverage summary

"Automated" means in JUnit in the integrated build (492 tests, all passing), partial command-
or schema-level automation included; "designed only" includes TC-REP-02.4, blocked on a rule
decision. At `4067d2f` (384 tests): 152 automated, 34 designed only. At `701c161` (182 tests):
71 automated, 115 designed only.

| Requirement | BR | EP classes | BVA cases | Test cases | Automated | Designed only |
|---|---|---|---|---|---|---|
| Money (common) | BR-40, BR-43 | 5 | 4 | 13 | 12 | 1 |
| LoanId (common) | - | 4 | 0 | 4 | 4 | 0 |
| FR-01 | BR-01, BR-02 | 4 | 6 | 7 | 7 | 0 |
| FR-02 | BR-03 | 4 | 3 | 6 | 6 | 0 |
| FR-03 | BR-04 | 3 | 3 | 4 | 4 | 0 |
| FR-04 | BR-05 | 3 | 0 | 3 | 3 | 0 |
| FR-05 | BR-08 | 3 | 0 | 9 | 9 | 0 |
| FR-06 | BR-09, BR-10 | 3 | 8 | 11 | 11 | 0 |
| FR-07 | BR-06, BR-07 | 6 | 6 | 7 | 7 | 0 |
| FR-13 | BR-18, BR-19 | 10 | 4 | 14 | 14 | 0 |
| FR-14 | BR-20 | 4 | 5 | 6 | 6 | 0 |
| FR-15 | BR-21, BR-22 | 6 | 4 | 6 | 3 | 3 (payoff - 0.01; payoff on reducing-balance and bullet loans) |
| FR-16 | BR-23 | 5 | 1 | 6 | 6 | 0 (TC-LED-04.2 partly automated) |
| FR-17 | BR-24, BR-43 | 6 | 2 | 8 | 8 | 0 |
| FR-18 | BR-25 | 6 | 0 | 7 | 7 | 0 (pairs kept in SQLite since `8401031`) |
| FR-19 | BR-26 | 3 | 0 | 6 | 6 | 0 |
| FR-20 | - | 4 | 0 | 4 | 4 | 0 |
| FR-31 | - | 4 | 0 | 5 | 4 | 1 (per-file import audit) |
| FR-08 | BR-11, BR-17 | 3 | 0 | 3 | 3 | 0 |
| FR-09 | BR-12 | 4 | 6 | 6 | 5 | 1 (TC-PRD-02.5; TC-PRD-02.2 partly automated) |
| FR-10 | BR-13 | 3 | 5 | 5 | 5 | 0 |
| FR-11 | BR-14, BR-15 | 2 | 3 | 4 | 4 | 0 |
| FR-12 | BR-16 | 1 | 3 | 3 | 3 | 0 |
| FR-21 | BR-27, BR-28 | 3 | 6 | 7 | 7 | 0 |
| FR-22 | BR-29 | 2 | 3 | 3 | 3 | 0 |
| FR-23 | BR-30 | 6 | 9 | 6 | 6 | 0 |
| FR-24 | BR-31 | 5 | 0 | 5 | 5 | 0 |
| FR-25 | BR-32 | 2 | 3 | 3 | 3 | 0 (TC-DEL-05.3 partly automated: the write-off posting is not built) |
| FR-26 | BR-33 | 2 | 0 | 3 | 3 | 0 |
| FR-27 | BR-34 | 3 | 0 | 3 | 3 | 0 |
| FR-28 | BR-35 | 3 | 2 | 3 | 3 | 0 |
| FR-29 | BR-36 to BR-38 | 3 | 2 | 4 | 0 | 4 (1 blocked on a rule decision) |
| FR-30, FR-32 | BR-39 | 2 | 0 | 2 | 0 | 2 |
| **Total** | | **127** | **88** | **186** | **174** | **12** |

By module: ledger (4.1 to 4.9) 58 of 62 automated, the other 4 being TC-LED-03.2, .4, .5
(payoff - 0.01 and per-product payoff) and TC-LED-09.3, with TC-LED-08.1 to .4 automated on 21 Nov;
origination (3.1 to 3.7) 47 of 47; delinquency (6.1 to 6.7) 30 of 30; `products` (5.1 to 5.5)
20 of 21, all but TC-PRD-02.5; `reporting` (7.1 to 7.3) 3 of 9, the statement only;
`common` (2.1, 2.2) 16 of 17.

### Rule gaps found while designing

Each needs a team decision, recorded as a deviation (SQAP section 6.3), before its test has an
expected result:

1. Declared monthly income of 0.00 in the burden ratio (BR-04): the code refuses it
   (`LoanApplicationTest#rejectsZeroIncome`, 21 Nov); team decision and deviation record still
   owed.
2. Payment above the full payoff (BR-21, BR-22): refused, with nothing written, until a rule
   exists (TC-LED-03.6).
3. Zero denominators in PAR30, provision coverage and collection efficiency (BR-36 to BR-38).
4. Whether a statement date range includes both ends (BR-35).
5. The BR-18/BR-19 reading. **Resolved:** tier by tier (decision D-01, 2026-11-16).
6. Maximum length and character set of a loan ID.
7. Resolved by decision D-02 (16 Nov): the allocator's instalment source lists only
   instalments due by the business date (TC-LED-03.2).
