# INS-01 - Inspection plan: repayment and ledger money path

Process: [`../README.md`](../README.md) | Checklist: [`checklist.md`](checklist.md) |
Forms: [`forms.md`](forms.md) | Defects: [`../../defect-log.md`](../../defect-log.md)

## Why this work product

Repayment allocation and ledger posting are where an error turns directly into wrong
money: every penalty, delinquency bucket, provision and report downstream reads the
ledger. Risk R1 in the risk register (exposure 20, Critical) names this path. The M1
design marks `RepaymentAllocator.allocate()` as the main inspection and basis-path
target (RTM FR-13, estimated cyclomatic complexity about 11).

## Scope

| # | File | Approx. NCLOC | Requirement / rule | Status |
|---|---|---|---|---|
| 1 | `ledger/RepaymentAllocator.java` (and its input type, e.g. `InstalmentDue`) | to be counted | FR-13, FR-14; BR-18, BR-19, BR-20 | To be added when L-05 lands |
| 2 | `ledger/Allocation.java` | ~69 | FR-13; BR-18 | Exists |
| 3 | `ledger/LoanAccount.java` (`applyAllocation`, `ensureAcceptsPayment`) | ~73 | FR-19; BR-26 | Exists |
| 4 | `ledger/LedgerEntry.java` | ~137 | FR-17; BR-23, BR-24 | Exists |
| 5 | `ledger/CompensatingEntryService.java` | ~16 | FR-17; BR-24 | Exists |
| 6 | `persistence/SqliteLedgerRepository.java` | ~140 | FR-16, FR-17; QR-05 | Exists |
| 7 | `persistence/SchemaMigrator.java` (`ledger_entry`, `payment`, `loan` DDL) | ~113 | BR-23 to BR-26; C-04 | Exists |
| 8 | `ledger/CoreRepaymentPoster.java` | to be counted | FR-16; BR-23 | To be added when L-08 lands |

Paths are under `src/main/java/pk/edu/giki/microlend/`. NCLOC counted on 2026-11-14
from the PR #8 branch (non-blank, non-comment lines).

Supporting context, read but not inspected line by line: `common/Money.java` (~81),
`ledger/EntryType.java` (~13), `ledger/LedgerWriter.java`, `ledger/LedgerReader.java`,
`persistence/SqliteDatabase.java` (~61), and the matching tests under `src/test/java`.

**Size control.** The full scope is well above the 250 NCLOC limit per meeting. Planned split:

- **Meeting A (primary, the M2 evidence):** items 1, 2 and 3. These cover the allocation
  waterfall and the account state change.
- **Meeting B (if time allows before submission, otherwise M3):** items 4 to 8. These cover the
  ledger immutability, persistence and posting path.

**Frozen version:** commit hash ______ on `feature/ledger-entry` (PR #8), CI run ______ (green).

## Business-rule note for inspectors

BR-18 and BR-19 can be read two ways. The allocator implements the literal reading,
**tier by tier**: all penalties oldest instalment first, then all fees oldest first,
then all interest oldest first, then all principal oldest first. This reading was
adopted by Tughral Hussain on 2026-11-14 and confirmed as team decision **D-01** on 2026-11-16 (`docs/planning/team-decisions.md`).
Inspectors should check the code against this reading and may log the ambiguity itself
as a Documentation defect against `docs/business-rules.md` if they consider it
unresolved.

`Instalment` (owned by Hassan Khalid, `products`) does not exist yet. The allocator
takes a ledger-side input type that `products.Instalment` will be mapped to. The
accrued penalty is an input amount; `PenaltyAccruer` (Muhammad Ibrahim, `delinquency`)
is outside this inspection.

## Roles

| Role | Name | Team |
|---|---|---|
| Author | Tughral Hussain (2023532) | MicroLend |
| Moderator | Muhammad Ibrahim (Team Lead) | MicroLend |
| Reader | Hassan Khalid | MicroLend |
| Recorder | Muhammad Ibrahim (moderator also records in a three-person team) | MicroLend |
| External inspector | name, team and reg. no. to be filled after instructor pairing | ______ |

The author does not moderate, read or record.

## Schedule

| Stage | Date | Time | Duration | Location |
|---|---|---|---|---|
| Planning (entry criteria checked) | | | | |
| Overview | | | 15 min | |
| Preparation (individual) | | | about 2 h each | |
| Inspection meeting A | | | at most 2 h | |
| Rework | | | | |
| Follow-up | | | | |

## Entry criteria check

| Criterion | Met? | Evidence |
|---|---|---|
| Frozen commit builds; CI green | | |
| Checkstyle clean | | |
| Meeting package at most ~250 NCLOC | | |
| Package and checklist sent 48 h before | | |
| External inspector confirmed | | |

## Materials sent to inspectors

- This plan and [`checklist.md`](checklist.md)
- Link to the frozen commit on GitHub (PR #8)
- `docs/business-rules.md` sections BR-18 to BR-26
- M1 design section 3.6 (concurrency) and 3.7 (persistence)
- A blank preparation log from [`forms.md`](forms.md)
