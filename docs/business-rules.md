# MicroLend — Business Rules Register

**Status:** frozen at Milestone 1. Changes after M1 require a recorded deviation (SQAP §6.3) because the RTM, the test design and both milestone documents trace to these rules.

**Version:** 1.0 (M1) · **Owner:** the module owner named against each section

Every rule has an ID. The RTM (`qa/rtm.md`) traces requirements to these IDs, and test cases cite them, so a rule that changes can be followed through to every artifact it affects.

---

## 1. Scope decisions

**In scope:** individual loans, three product types, fixed-rate lending in a single currency, repayment by cash at the branch or by bank statement import, penalty accrual, delinquency classification, provisioning, write-off, statements and portfolio reporting.

**Deferred — explicitly out of scope for all three milestones:** group and joint-liability lending; guarantors and collateral registers; loan restructuring and rescheduling; multi-currency; savings products; interest paid on savings; mobile-money integration; any user interface beyond the CLI; multi-branch consolidation; and customer-facing self-service.

This list exists so that scope creep is a visible decision rather than a drift.

---

## 2. Origination and eligibility — *owner: Muhammad Ibrahim*

| ID | Rule | Boundary values for test design |
|---|---|---|
| **BR-01** | An applicant must be at least **18 years old** at disbursement | 17 / 18 / 19 years |
| **BR-02** | An applicant must be no more than **65 years old at maturity** (not at application) | maturity age 64 / 65 / 66 |
| **BR-03** | An applicant may hold at most **2 active loans**; a written-off loan is not active, a loan in arrears is | 0 / 1 / 2 / 3 active |
| **BR-04** | Total monthly instalment burden, including the new loan, must not exceed **40% of declared monthly income** | 39.99% / 40.00% / 40.01% |
| **BR-05** | An applicant with **any prior write-off** is rejected regardless of other factors | none / one write-off / settled-late but never written off |
| **BR-06** | The requested amount must lie within the product's **minimum and maximum**, inclusive | min − 1 / min / max / max + 1 |
| **BR-07** | The requested tenure must lie within the product's **tenure band**, inclusive | band − 1 / band edges / band + 1 |
| **BR-08** | A rejected application returns a **typed rejection reason** naming the first rule that failed, evaluated in the order BR-01 … BR-07 | one case per reason |
| **BR-09** | Risk grade adjusts the rate by a fixed spread and caps the approved amount: **A** +0.00% and up to 100% of requested; **B** +1.50% and up to 80%; **C** +3.00% and up to 60% | one case per grade |
| **BR-10** | Approval is for the **capped** amount, not the requested amount; the applicant may accept or decline | requested within cap / above cap |

**Invariant:** no loan may exist in the system that these rules would not have approved.

---

## 3. Products and schedule generation — *owner: Hassan Khalid*

| ID | Rule | Boundary values for test design |
|---|---|---|
| **BR-11** | Three products exist: **flat-rate** (interest on original principal throughout), **reducing-balance** (interest on outstanding balance, equal instalments), **bullet** (interest monthly, principal at maturity) | one schedule per product |
| **BR-12** | Instalments fall on the **same day-of-month as disbursement**, clamped to the last day for short months | disbursement on 28 / 29 / 30 / 31; February in leap and non-leap years |
| **BR-13** | An optional **grace period** of 0–3 months defers the first instalment, but interest still accrues during it and is added to the first instalment | grace 0 / 1 / 3 / 4 (4 rejected) |
| **BR-14** | All **rounding residue is absorbed into the final instalment**, so instalments sum exactly to principal plus total interest | amounts and tenures that do not divide evenly |
| **BR-15** | Money is rounded to **2 decimal places using HALF_EVEN** at every step | values ending .005 |
| **BR-16** | A schedule always contains **exactly `tenureMonths` instalments**, regardless of product or grace period | tenure 1 / 6 / 36 |
| **BR-17** | The interest rate is **fixed for the life of the loan**; rate changes apply only to new loans | — |

**Invariant:** the instalments of a schedule sum exactly to principal plus total interest.

---

## 4. Repayment posting and the ledger — *owner: Tughral Hussain*

| ID | Rule | Boundary values for test design |
|---|---|---|
| **BR-18** | Payments are applied in strict order: **penalty → fees → interest → principal** | payment covering part of each tier |
| **BR-19** | Within that order, the **oldest unpaid instalment is settled first** | two and three instalments overdue |
| **BR-20** | A **partial payment** is applied as far as it reaches; the remainder stays outstanding | 0 / 0.01 / due − 0.01 / exactly due |
| **BR-21** | An **overpayment** becomes an advance credit held against future instalments | due + 0.01 |
| **BR-22** | If a payment clears the loan entirely, **early settlement** applies: unearned interest is refunded for reducing-balance loans; **no rebate** for flat-rate loans; bullet loans refund unaccrued monthly interest only | exact payoff / payoff + 0.01, per product |
| **BR-23** | Every posting writes **balanced debit and credit ledger lines** | every posting path |
| **BR-24** | A posted ledger entry is **immutable**. A mistake is corrected only by a **compensating reversal entry** that references the original | correction of each entry type |
| **BR-25** | A payment carrying a **bank reference already present** in the ledger is rejected as a duplicate | same reference twice |
| **BR-26** | Payments against a **closed or written-off loan** are rejected with a typed exception | closed / written-off / active |

**Invariant:** the ledger always balances (Σ debits = Σ credits) and nothing in it is ever altered.

---

## 5. Delinquency, penalty and provisioning — *owner: Muhammad Ibrahim*

| ID | Rule | Boundary values for test design |
|---|---|---|
| **BR-27** | Penalty accrues at **0.05% per day** on overdue **principal only**, not on overdue interest | 1 / 30 / 90 days overdue |
| **BR-28** | Accrued penalty is **capped at 10% of the overdue amount** | just below / at / just above the cap |
| **BR-29** | **Days past due** is counted from the day after the instalment due date | due date / due + 1 |
| **BR-30** | Buckets and classifications: **0 days → Standard**; **1–30 → Watch**; **31–60 → Substandard**; **61–90 → Doubtful**; **90+ → Loss** | 0 / 1 / 30 / 31 / 60 / 61 / 90 / 91 |
| **BR-31** | Provision percentages by classification: **Standard 1%**, **Watch 5%**, **Substandard 25%**, **Doubtful 50%**, **Loss 100%** of outstanding principal | one case per classification |
| **BR-32** | A loan more than **180 days past due is written off automatically**, moving outstanding principal to the write-off account | 179 / 180 / 181 days |
| **BR-33** | The end-of-day process is **idempotent per business date**: running it twice for the same date has no additional effect | same date run twice |
| **BR-34** | A classification change **raises an alert event**; an improvement raises one too | downgrade / upgrade / no change |

**Invariant:** no penalty is accrued twice for the same loan and business date.

---

## 6. Reporting — *owner: Hassan Khalid*

| ID | Rule |
|---|---|
| **BR-35** | A customer statement lists every ledger entry for a loan within a date range, with a running balance |
| **BR-36** | **PAR30** (portfolio at risk over 30 days) = outstanding principal of loans 31+ days past due ÷ total outstanding principal |
| **BR-37** | **Provision coverage** = total provision held ÷ outstanding principal of non-Standard loans |
| **BR-38** | **Collection efficiency** = amount collected in a period ÷ amount due in that period |
| **BR-39** | Reports are produced **as at a stated business date** and are reproducible for any past date from the ledger |

---

## 7. Cross-cutting rules

| ID | Rule |
|---|---|
| **BR-40** | All money is held as `BigDecimal` at scale 2 with `HALF_EVEN` rounding, wrapped in the `Money` value object. `double` and `float` are prohibited (constraint C-01) |
| **BR-41** | A single currency (PKR) is assumed throughout; no conversion exists |
| **BR-42** | All dates are business dates in a single timezone; there is no intra-day time component on a loan event other than the ledger's posting timestamp |
| **BR-43** | A negative monetary amount cannot be constructed; direction is expressed by the debit or credit column, never by a sign |

---

## 8. Open decisions for the team to confirm

These are the numbers the team must own, because they are quoted in both milestone documents and traced through the RTM. Each is a reasonable default taken from common microfinance practice; change them now if the team disagrees, not after M2 work has been built on them.

| Decision | Current value | Why it matters |
|---|---|---|
| Instalment-to-income burden ceiling | 40% | Drives BR-04 and its boundary tests |
| Daily penalty rate and cap | 0.05%/day, capped at 10% | Drives BR-27, BR-28 and all penalty tests |
| Write-off threshold | 180 days past due | Drives BR-32 |
| Risk-grade spreads and caps | A +0.00%/100%, B +1.50%/80%, C +3.00%/60% | Drives BR-09 and BR-10 |
| Provision percentages | 1 / 5 / 25 / 50 / 100% | Drives BR-31 |
| Maximum grace period | 3 months | Drives BR-13 |

---

## Change log

| Date | Version | Change | Approved by |
|---|---|---|---|
| 2026-10-04 | 1.0 | Register created and frozen for M1: 43 rules across five modules | — |
