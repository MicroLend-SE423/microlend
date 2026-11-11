# M2 code to-do — Tughral Hussain (`ledger`, persistence, `web`)

Work queue for the daily scheduled coding session. Items are taken **top to
bottom**, one red-green cycle per run unless told to expedite. Tick an item only
when its tests pass in CI on the pull request. Items owned by Ibrahim and Hassan
are listed at the end for context only and must not be picked up here.

Every item traces to the M1 design (`docs/M1-SE423-Proposal-OOD-Pattern-Plan.md`
§3.2 View 2, §3.7, §5.3), the Business Rules Register (`docs/business-rules.md`)
and the RTM (`qa/rtm.md`).

Schedule: **12:00-14:00 Asia/Karachi daily**, one red/green pair per normal run.
The Windows runner, setup and one-day expedite command are documented in
[`daily-coding.md`](daily-coding.md). Commits, pushes and ledger PR creation are
authorized by the owner; PR merges still require teammate review.

2026-11-06: L-01 and L-02 are implemented on `feature/ledger-entry`; the local
build passes. Checkboxes await verification of PR CI and reconciliation on the
next run. L-03 is the next implementation item once those checks pass.

2026-11-08: L-01 and L-02 ticked; PR #8 CI passed for them on 2026-11-06. L-03 is
implemented and passes locally; tick it once PR CI passes. Next item: L-04.
The scheduled runs on 11-07 and 11-08 produced no work (Codex model rejected;
machine off at noon). The Codex-based runner cannot work until it is replaced.

2026-11-09 (catch-up run): L-03 ticked; PR #8 CI run 42471721763 passed for
ff8d352. L-04 is implemented and passes the full local build; tick it once PR CI
passes. **L-05 is blocked** and was not started:
- `Instalment` (Hassan, `products`) is not on `develop` or any branch; PR #6 only
  adds `DueDateCalculator`. The M1 design gives it principal and interest only,
  with no paid-to-date or accrued-penalty fields the allocator needs.
- BR-18 and BR-19 can be read two ways: settle each instalment fully (penalty →
  fees → interest → principal) before the next, or apply each tier across all
  instalments oldest first before the next tier. The team must choose; the run
  does not invent it.
- Per-instalment penalty comes from Ibrahim's `PenaltyAccruer`, not yet built.
Unblock by agreeing the `Instalment` shape with Hassan and the BR-18/BR-19 reading
with the team. Items are taken in order, so L-06 onwards wait for L-05.

2026-11-11 (catch-up run, out-of-order override authorised by Tughral on
2026-11-11): L-04 ticked; PR #8 CI run 42599213093 passed for 525d7cc.
L-05 re-checked and **still blocked**: no `Instalment` type exists on `develop`,
PR #6 or any other branch, and the BR-18/BR-19 reading is undecided. Skipped as
blocked, because each needs `RepaymentAllocator` or a working `RepaymentPoster`
chain: L-06 (settlement acts on an allocation), L-08 (`CoreRepaymentPoster`
reads → allocates → writes), L-09 to L-11 (decorators need a core poster to
wrap), L-16 (fault injection into the posting path) and L-17 (wires the chains).
Taken **out of order** because they depend only on code already on this branch:
L-07 (`RepaymentCommand`), then L-12, L-13 and L-14 as time allows. Decision to
confirm with the team: `RepaymentCommand` rejects a zero amount because a
posting that moves no money cannot write a ledger line (BR-23); the BR-20 "0"
boundary is still tested at the allocator's own API when L-05 is unblocked.

## Queue

- [x] **L-01** `LoanId` value object (`common`) and `LedgerEntry` immutable value with
      `EntryType`: exactly one of debit/credit is non-zero, no mutators. BR-23, BR-24,
      BR-43; FR-17, QR-05.
- [x] **L-02** `LedgerWriter` and `LedgerReader` role interfaces (`ledger`), plus an
      in-memory implementation for tests. FR-16, FR-17; ISP/DIP (§4.4, §4.5).
- [x] **L-03** `LoanAccount` with `LoanStatus` (ACTIVE / CLOSED / WRITTEN_OFF),
      outstanding principal, `isClosed()`, and `LoanClosedException` for postings
      against a closed or written-off loan. BR-26; FR-19.
- [x] **L-04** `Allocation` value object (toPenalty, toFees, toInterest, toPrincipal)
      and `LoanAccount.applyAllocation()`. BR-18.
- [ ] **L-05** `RepaymentAllocator.allocate(Money, List<Instalment>)`: penalty →
      fees → interest → principal, oldest unpaid instalment first; partial payment
      applied as far as it reaches. One readable method (it is the Fagan inspection
      target). BR-18, BR-19, BR-20; FR-13, FR-14. Needs `Instalment` from `products`;
      if not yet merged, define the minimal interface the allocator needs and
      coordinate with Hassan.
- [ ] **L-06** `SettlementPolicy`: overpayment becomes advance credit; exact payoff
      triggers early settlement with per-product rebate rules. BR-21, BR-22; FR-15.
- [ ] **L-07** `RepaymentCommand` (validated factory) and `InvalidRepaymentException`.
      BR-25 data (bank, reference) carried for the idempotency guard.
- [ ] **L-08** `RepaymentPoster` interface and `CoreRepaymentPoster`: read → allocate →
      write balanced debit and credit lines; invariant test Σ debits = Σ credits after
      every posting; `LedgerImbalanceException`. BR-23; FR-16, QR-03.
- [ ] **L-09** Decorator pattern: abstract `RepaymentPosterDecorator` (IS-A and HAS-A
      `RepaymentPoster`), then `TimingPoster`. §5.3.
- [ ] **L-10** `AuditLoggingPoster` (operator named on every teller posting). FR-31.
- [ ] **L-11** `IdempotencyGuardPoster` (rejects a repeated (bank, reference) pair).
      BR-25; FR-18.
- [ ] **L-12** `CompensatingEntryService.reverse(LedgerEntry, reason)` and
      `LedgerImmutabilityTest` (no mutator on `LedgerEntry`, corrections only by
      reversal). BR-24; FR-17, QR-05.
- [ ] **L-13** SQLite schema DDL for the seven tables (§3.7): CHECK and UNIQUE
      constraints, FKs, `PRAGMA foreign_keys = ON`, money as INTEGER paisa (C-04).
      Schema applied by a `SchemaMigrator` on first connection.
- [ ] **L-14** `SqliteLedgerRepository` implementing `LedgerWriter` and `LedgerReader`;
      no UPDATE or DELETE against `ledger_entry` anywhere. QR-05.
- [ ] **L-15** `SqliteLoanRepository` implementing `LoanRepository`, with
      `BEGIN IMMEDIATE` transactions and a busy timeout (§3.6).
- [ ] **L-16** Fault-injection test: a `LedgerWriter` stub that throws at each write
      point leaves the ledger balanced (`PostingFaultInjectionTest`). QR-03.
- [ ] **L-17** Composition root wiring the two production decorator chains from §5.3
      (import chain: Idempotency → Timing → Core; teller chain: Audit → Timing → Core).
- [ ] **L-18** Minimal local web interface (`web`): teller posting form and a page that
      triggers end-of-day. Served from a local process only; never deployed.
- [ ] **L-19** ArchUnit test for constraint C-03 (no cycles between module packages),
      wired into `./gradlew build`.

## Rules for every run

1. Branch from `develop` as `feature/ledger-<short-name>`; one open PR at a time.
2. Commit the failing test first (`test(red): …`), then the implementation
   (`feat(green): …`), then any `refactor: …`. Never squash.
3. `./gradlew build` must pass before the green commit is pushed (Checkstyle:
   no `double`/`float`, Javadoc on public API, files end with a newline).
4. Add a row to `qa/m2-ai-usage-log.md` for the run.
5. Update `qa/rtm.md` code/test columns when an FR moves from *planned* to
   *implemented*.

## Not in this queue (other owners)

- Ibrahim: `origination` rules and evaluator; `delinquency` (`PenaltyAccruer`,
  `DaysPastDueCalculator`, `DelinquencyClassifier`, `ProvisioningPolicy`,
  `WriteOffPolicy`, `EndOfDayEngine` + `LockRegistry`); `ledger.importing` adapters.
- Hassan: `products` (`LoanProduct` Factory Method, calculators, `GracePeriodPolicy`,
  `ScheduleAssembler`, golden schedules); `reporting`.
