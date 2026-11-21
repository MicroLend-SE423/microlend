# INS-01 - Inspection plan: repayment and ledger money path

Process: [`../README.md`](../README.md) | Checklist: [`checklist.md`](checklist.md) |
Forms: [`forms.md`](forms.md) | Defects: [`../../defect-log.md`](../../defect-log.md)

## Why this work product

Repayment allocation and ledger posting are where an error turns directly into wrong
money: every penalty, delinquency bucket, provision and report downstream reads the
ledger. Risk R1 in the risk register (exposure 20, Critical) names this path. The M1
design marks `RepaymentAllocator.allocate()` as the main inspection and basis-path
target (RTM FR-13). M1 estimated its cyclomatic complexity at about 11; the built method
measures 10 (`qa/test-design/white-box.md`, section 2).

**Status on 21 Nov 2026:** planned, not held. No inspector from another team has been
assigned yet (risk R3). The roles, schedule, entry-criteria check and frozen version
below are filled in when the inspection is scheduled.

## Scope

| # | File | Approx. NCLOC | Requirement / rule | Status |
|---|---|---|---|---|
| 1 | `ledger/RepaymentAllocator.java` and its input type `ledger/InstalmentDue.java` | ~55 + ~20 | FR-13, FR-14; BR-18, BR-19, BR-20 | Exists (L-05 landed) |
| 2 | `ledger/Allocation.java` | ~69 | FR-13; BR-18 | Exists |
| 3 | `ledger/LoanAccount.java` (`applyAllocation`, `ensureAcceptsPayment`) | ~84 (was ~73) | FR-19; BR-26 | Exists |
| 4 | `ledger/LedgerEntry.java` | ~137 | FR-17; BR-23, BR-24 | Exists |
| 5 | `ledger/CompensatingEntryService.java` | ~16 | FR-17; BR-24 | Exists |
| 6 | `persistence/SqliteLedgerRepository.java` | ~153 (was ~140) | FR-16, FR-17; QR-05 | Exists |
| 7 | `persistence/SchemaMigrator.java` (`ledger_entry`, `payment`, `loan` DDL) | ~146 (was ~113; schema version 2 added) | BR-23 to BR-26; C-04 | Exists |
| 8 | `ledger/CoreRepaymentPoster.java` | ~99 | FR-16; BR-23 | Exists (L-08 landed) |

Paths are under `src/main/java/pk/edu/giki/microlend/`. NCLOC means non-blank, non-comment
lines. It was first counted on 2026-11-14 from the PR #8 branch. The figures above were
recounted on 2026-11-21 at `7784a6a`, the branch that contains the merged PR #8 code.

Supporting context, read but not inspected line by line: `common/Money.java` (~81),
`ledger/EntryType.java` (~15), `ledger/LedgerWriter.java`, `ledger/LedgerReader.java`,
`persistence/SqliteDatabase.java` (~63), and the matching tests under `src/test/java`.
Added to the posting path since this plan was written, and candidates for meeting B:
`ledger/LockingPoster.java` (~22) and `common/LockRegistry.java` (~11), the per-account
lock shared with the end-of-day engine.

**Size control.** The full scope is well above the 250 NCLOC limit per meeting. Planned split:

- **Meeting A (primary, the M2 evidence):** items 1, 2 and 3, about 228 NCLOC
  (55 + 20 + 69 + 84). These cover the allocation waterfall and the account state change.
- **Meeting B (M3):** items 4 to 8, about 551 NCLOC (137 + 16 + 153 + 146 + 99), which is
  more than two meetings' worth. They will be split when scheduled. These cover the ledger
  immutability, persistence and posting path.

**Frozen version:** commit hash ______ on `develop` (PR #8 was merged on 21 Nov 2026), CI run ______ (green).

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
accrued penalty is an input amount. `PenaltyAccruer` (Muhammad Ibrahim, `delinquency`)
now exists, but it is outside this inspection.

The code review of PR #8 on 21 Nov found defects in items 6 and 8 (DEF-009 to DEF-013 in
`qa/defect-log.md`). They are review defects, not inspection results. Inspectors should
check the state of the code at the frozen version, including the fixes for DEF-009 and
DEF-010.

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
