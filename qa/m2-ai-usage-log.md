# MicroLend — AI Usage Log (Milestone 2)

Required by the joint-project guidelines §9. Same columns as the M1 log
(`qa/m1-ai-usage-log.md`). Entries are added on the day the work is done; the
repository's commit history corroborates the dates.

| # | Date | Tool | Purpose | Prompt / activity summary | Material adopted | Modifications made | Verification |
|-|----|----|----|-----|-----|-----|-----|
| 1 | 2026-11-04 | Claude Fable 5.1 (Claude Code) | Repository and environment check before M2 work | Asked for an overview of the repository, the M1 documents, the GitHub organisation, open PRs and branch rules | Findings: the `main-protection` ruleset required 0 approvals and did not cover `develop`; no JDK on the development machine; PR #6 fails one Checkstyle rule (missing final newline) | Ruleset corrected to 1 non-author approval with last-push approval, extended to `develop`; Temurin JDK 21 installed locally | Ruleset read back through the GitHub API; `./gradlew build` green locally (11 tests, Checkstyle, JaCoCo) |
| 2 | 2026-11-04 | Claude Fable 5.1 (Claude Code) | Start `ledger` module: `LoanId`, `EntryType`, `LedgerEntry` (queue item L-01) | Asked for the first red-green cycle of the ledger module from the M1 View 2 design, plus a work queue file for the daily scheduled session | `LoanIdTest`, `LedgerEntryTest` (red commit); `LoanId`, `InvalidLoanIdException`, `EntryType`, `LedgerEntry`, `InvalidLedgerEntryException` (green commit); `docs/planning/m2-code-todo.md` | Entry invariant chosen by the owner: exactly one of debit/credit is non-zero (BR-43 direction-by-column) | Red commit fails to compile as expected; green commit passes `./gradlew build` locally and in CI on the PR |

| 3 | 2026-11-06 | OpenAI Codex | L-02 and daily coding scheduler | Owner requested repository review, daily noon-2 PM Pakistan work, two commits and a PR | LedgerReader/LedgerWriter contracts, tested in-memory fake, Windows task runner and instructions | Kept fake test-only; immutable snapshots; isolated scheduler checkout; explicit deadline and owner scope; partial RTM evidence | Expected red compilation failure confirmed before a33c187; full Gradle build passes (tests, Checkstyle, JaCoCo); PowerShell parse/preflight and outside-window skip pass; task registered and read back; future unattended execution not yet observed |

## Declaration for Milestone 2

> Each member named below has read, understood and verified the material credited to them, can explain and defend the design decisions and code it contains, and reviewed and modified any AI-assisted content where needed.

- **Muhammad Ibrahim — 2023446**
- **Hassan Khalid — 2023242**
- **Tughral Hussain — 2023532**

## Team rules for AI use

Unchanged from M1: generated code enters the repository only through the module owner; no generated test is accepted without reading what it asserts; business rules and thresholds are the team's decisions; entries are added when the work happens; anything a member cannot explain is rewritten or withdrawn before submission.
