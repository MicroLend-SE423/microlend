# INS-01 - Inspection plan: repayment and ledger money path

Process: [`../README.md`](../README.md) | Checklist: [`checklist.md`](checklist.md) |
Forms: [`forms.md`](forms.md) | Defects: [`../../defect-log.md`](../../defect-log.md)

## Why this work product

An error here is wrong money that every penalty, delinquency bucket, provision and report
then reads from the ledger. Risk R1 (exposure 20, Critical) names this path, and the
M1 design made `RepaymentAllocator.allocate()` the main inspection and basis-path target (RTM
FR-13; complexity estimated at about 11, measured 10 in `qa/test-design/white-box.md` section 2).

**Status on 21 Nov 2026:** planned, not held; no inspector from another team has been assigned
(risk R3). The blank fields below are filled in when it is scheduled.

## Scope

| # | File | Approx. NCLOC | Requirement / rule | Status |
|---|---|---|---|---|
| 1 | `ledger/RepaymentAllocator.java` and its input type `ledger/InstalmentDue.java` | ~55 + ~20 | FR-13, FR-14; BR-18, BR-19, BR-20 | Exists (L-05 landed) |
| 2 | `ledger/Allocation.java` | ~69 | FR-13; BR-18 | Exists |
| 3 | `ledger/LoanAccount.java` (`applyAllocation`, `ensureAcceptsPayment`) | ~84 (was ~73) | FR-19; BR-26 | Exists |
| 4 | `ledger/LedgerEntry.java` | ~137 | FR-17; BR-23, BR-24 | Exists |
| 5 | `ledger/CompensatingEntryService.java` | ~16 | FR-17; BR-24 | Exists |
| 6 | `persistence/SqliteLedgerRepository.java` | ~153 (was ~140) | FR-16, FR-17; QR-05 | Exists |
| 7 | `persistence/SchemaMigrator.java` (`ledger_entry`, `payment`, `loan` DDL) | ~150 (was ~113; schema version 2 added) | BR-23 to BR-26; C-04 | Exists |
| 8 | `ledger/CoreRepaymentPoster.java` | ~99 | FR-16; BR-23 | Exists (L-08 landed) |

Paths are under `src/main/java/pk/edu/giki/microlend/`. NCLOC (non-blank, non-comment lines)
was counted on 2026-11-14 on the PR #8 branch and recounted on 2026-11-21 at code head
`4067d2f`, which contains the merged PR #8 code.

Read for context only: `common/Money.java` (~81), `ledger/EntryType.java` (~15),
`ledger/LedgerWriter.java`, `ledger/LedgerReader.java`, `persistence/SqliteDatabase.java` (~63)
and their tests under `src/test/java`. Meeting B candidates added to the posting path since:
`ledger/LockingPoster.java` (~22) and `common/LockRegistry.java` (~11), the per-account lock
shared with the end-of-day engine.

**Size control.** The full scope exceeds the 250 NCLOC per-meeting limit:

- **Meeting A (primary, the M2 evidence):** items 1 to 3, about 228 NCLOC
  (55 + 20 + 69 + 84): the allocation waterfall and the account state change.
- **Meeting B (M3):** items 4 to 8, about 555 NCLOC (137 + 16 + 153 + 150 + 99): ledger
  immutability, persistence and posting; over two meetings' worth, so split again when scheduled.

**Frozen version:** commit hash ______ on `develop` (PR #8 was merged on 21 Nov 2026), CI run ______ (green).

## Business-rule note for inspectors

BR-18 and BR-19 can be read two ways. The allocator implements the literal tier-by-tier
reading (all penalties, then fees, interest and principal, each oldest instalment first),
adopted by Tughral Hussain on 2026-11-14 and confirmed as team decision D-01 on 2026-11-16
(`docs/planning/team-decisions.md`). Inspect against it; an ambiguity
still thought unresolved may be logged as a Documentation defect against `docs/business-rules.md`.

`products.Instalment` (Hassan Khalid) does not exist yet and will map to the ledger-side input
`InstalmentDue`. The accrued penalty is an input; `PenaltyAccruer` (Muhammad Ibrahim,
`delinquency`) exists but is out of scope.

Code reviews on 21 Nov found DEF-009 to DEF-013 in items 6 and 8 (review of PR #8), DEF-022 in
item 8 and DEF-023 in item 7 (independent code review); see `qa/defect-log.md`. These are not
inspection results. Inspect the frozen version, including the fixes for DEF-009, DEF-010 and DEF-012.

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

This plan and [`checklist.md`](checklist.md); a link to the frozen commit on GitHub (PR #8);
`docs/business-rules.md` BR-18 to BR-26; M1 design sections 3.6 (concurrency) and 3.7
(persistence); a blank preparation log from [`forms.md`](forms.md).
