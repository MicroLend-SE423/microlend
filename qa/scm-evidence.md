# MicroLend - SCM Quality-Gate Evidence (M2)

**Deliverable:** SE431 M2, "SCM quality-gate evidence" · **Document lead:** Muhammad Ibrahim (M2 Team Lead)
**Evidence collected:** 21 Nov 2026, after PR #9 was merged, from the GitHub API (`gh api`, `gh pr`, `gh run`) and `git` on
`github.com/MicroLend-SE423/microlend` (supersedes the 14 Nov collection).

Where the history departs from the documented strategy, it is recorded as a nonconformance with a CAPA
(section 4), not left out.

## 1. Documented strategy

Source: `README.md` (Branching strategy, Baselines).

| Item | Rule |
|---|---|
| Branches | `main` holds baselines only; `develop` is integration; work happens on `feature/<module>-<short-name>` branched from `develop` |
| Change path | Every change reaches `develop` and `main` only by pull request |
| Review | Every pull request is approved by a member who is not the author; the reviewer merges it |
| History | Merge commits only (`--no-ff`); squash and rebase merges disabled so the `test(red)` -> `feat(green)` -> `refactor:` trail survives |
| Commit convention | `test(red):`, `feat(green):`, `refactor:`, `fix:`, `docs:`, `chore:`, `test:` |
| Quality gate | CI must pass: compile, unit tests, Checkstyle (constraint C-01: no `double`/`float`), JaCoCo report |
| Baselines | Annotated tags `m1-baseline`, `m2-baseline`, `m3-baseline` on `main` at each milestone |

## 2. Repository controls in force

| Control | Setting (as read on 21 Nov 2026) | Gate it enforces |
|---|---|---|
| Ruleset `main-protection` (id 24454518) | Active; targets the default branch (`main`) **and** `refs/heads/develop`; bypass list empty, so admins cannot bypass it. Last changed 21 Nov 2026 22:27 PKT, when it was switched back on after the controlled exception in section 3.6 | No direct push to either branch |
| Rule `pull_request` | 1 approving review; stale approvals dismissed on push; approval of the most recent push required; all review threads resolved | Independent review before merge |
| Rule `pull_request.allowed_merge_methods` | `merge` only | TDD history cannot be squashed away |
| Rule `non_fast_forward` | On | No force push, so published history cannot be rewritten |
| Rule `deletion` | On | `main` and `develop` cannot be deleted |
| Repository merge buttons | merge commit on; squash off; rebase off | Same as above, at repository level |
| Auto-delete head branches | Off | Feature branches, and the commits on them, stay visible for assessment (three were deleted by hand after the rewrite in 3.6) |
| Access | `mIBRAHIM707` admin, `tughral1` admin, `Hassan242-kk` write | Every member can push feature branches; nobody can bypass the ruleset |
| CI | `.github/workflows/build.yml` on push to `main`, `develop`, `feature/**` and on pull requests into `main`/`develop`; uploads test, JaCoCo and Checkstyle reports as artifacts (90 days) | Build, tests, static analysis and coverage on every change |

## 3. What the history shows

### 3.1 Pull requests

| PR | Head -> base | Author | Reviews | Merged by | Conforms? |
|---|---|---|---|---|---|
| #1 | `docs/m1-review-fixes` -> `develop` | mIBRAHIM707 | none | mIBRAHIM707 | **No** (self-merge, no review) |
| #2 | `develop` -> `main` | mIBRAHIM707 | none | tughral1 | Partly (merged by another member, but no recorded approval) |
| #3 | `docs/m1-trim` -> `develop` | mIBRAHIM707 | none | mIBRAHIM707 | **No** |
| #4 | `develop` -> `main` (M1 baseline) | mIBRAHIM707 | none | mIBRAHIM707 | **No** |
| #5 | `docs/repo-url` -> `develop` | mIBRAHIM707 | none | mIBRAHIM707 | **No** |
| #6 | `feat/hassan-product-schedule-tests` -> `main` | Hassan242-kk | mIBRAHIM707: changes requested (21 Nov) | open | **No** (targets `main`, not `develop`; CI failing; one `feat(green)` commit with no red commit) |
| #7 | `docs/claude-handoff` -> `develop` | tughral1 | none | tughral1 | **No** (self-merge, no review) |
| #8 | `feature/ledger-entry` -> `develop` | tughral1 | mIBRAHIM707: approved (21 Nov), with seven follow-ups | mIBRAHIM707, the reviewer | **Yes** |
| #9 | `feature/delinquency-origination-import` -> `develop` | mIBRAHIM707 | tughral1: approved (21 Nov) | tughral1, the reviewer | **Yes** |

All six nonconforming merges (9 Oct to 4 Nov 2026) predate the ruleset's re-application with an empty
bypass list on **4 Nov 2026 15:43 PKT**. Both later merges (PR #8, PR #9) had a non-author approval and
were merged by the reviewer.

### 3.2 CI runs

Workflow runs from 4 Oct 2026 (the first) to 21 Nov 2026: **81**; **78** succeeded, **3** failed.

| Failed run | Branch | Cause | Resolution |
|---|---|---|---|
| 37203232415, 37203284296 | `develop` | Workflow bootstrap on 4 Oct: `gradlew` lost its executable bit | Fixed in the next commit (`git update-index --chmod=+x gradlew`); every later run on `develop` passed |
| 38076363948 | `feat/hassan-product-schedule-tests` | PR #6 check fails (Checkstyle `NewlineAtEndOfFile`) | Open; review on PR #6 asks for the fix and for retargeting to `develop` |

Runs per branch: `feature/ledger-entry` 46, all passed (per-push evidence for PR #8);
`feature/delinquency-origination-import` 2, both passed (push run 44930284914, pull-request run
44930290499, at `9af3f31`); `develop` 22, the last 20 passed, including run 44931805244 at `ac14ba9`,
the PR #9 merge.

### 3.3 TDD commit trail

| Pull request | `test(red)` commits | Green commits | Notes |
|---|---|---|---|
| #8 (ledger, Tughral Hussain) | 19 | 19 `feat(green)` | One red/green pair per queue item L-01 to L-19; 2 `refactor:` and 1 `fix:` commit after them |
| #9 (origination, delinquency, importing; Muhammad Ibrahim) | 9 | 8 `feat(green)` and 1 `fix:` | The `fix:` commit `6d30af9` is the green for red `5541f0b` (independent-review fixes); 2 `test:` commits change no assertion |
| #6 (products, Hassan Khalid) | 0 | 1 `feat(green)` | No red commit precedes the code (noted in the PR #6 review) |

Each red commit precedes its green commit and names the same item. Re-count with:

```powershell
git log --format=%s 10688c2..47c713e^2
git log --format=%s 47c713e..ac14ba9^2
```

### 3.4 Attribution

Commits on published branches, by author (`git shortlog -sn --remotes`, 21 Nov 2026):

| Author | Commits |
|---|---|
| tughral1 | 72 |
| mIBRAHIM707 / Muhammad Ibrahim | 64 |
| Hassan242-kk | 1 |

Hassan Khalid's only commit is on the open PR #6: an individual-evidence risk (risk register R6).

### 3.5 Baselines

| Tag | Commit | Status |
|---|---|---|
| `m1-baseline` | `10688c2` (merge of PR #4 into `main`, 10 Oct 2026) | Done |
| `m2-baseline` | the merge commit of the `develop` -> `main` pull request that carries these documents (`git rev-list -n 1 m2-baseline`) | Final submission step |

### 3.6 A controlled exception: one force push to `develop`

On 21 Nov 2026 the team lead approved one rewrite of `develop`'s published history to remove
tool-generated trailer lines from commit messages since the M1 baseline:

1. The 54 commits after `m1-baseline` were rewritten locally, messages only; each was checked to keep its original tree, so file content and commit order are unchanged.
2. Muhammad Ibrahim switched the ruleset off, force-pushed `develop` once (`--force-with-lease`, expected old head `5916371` -> `47c713e`) and switched it back on at 22:27 PKT. `main` was not touched.
3. Merged branches holding the old commits (`feature/ledger-entry`, `docs/claude-handoff`, `docs/repo-url`) were deleted by hand, so no branch still carries the pre-rewrite history.
4. Commit `7784a6a` updated the 141 hashes quoted in the documents and QA records.

GitHub's PR #7 and PR #8 pages still list the original hashes.

## 4. Nonconformances and CAPA

In `qa/defect-log.md`: SCM-01 = DEF-006 (CAPA-001), SCM-02 = DEF-007, SCM-03 = DEF-008. SCM-04 is a controlled, approved change with no defect number.

| ID | Finding | Root cause | Corrective action | Preventive action | Status |
|---|---|---|---|---|---|
| SCM-01 | PRs #1, #3, #4, #5, #7 were self-merged with no review; #2 had no recorded approval | Ruleset bypass list let admins merge; the review rule was documented but not enforced | Ruleset re-applied 4 Nov 2026 with an empty bypass list, targeting `main` and `develop` | Every member reviews at least one PR per milestone; this table is re-collected at each milestone | Verified: PR #8 and PR #9 each waited for a non-author approval and were merged by the reviewer |
| SCM-02 | PR #6 targets `main` instead of `develop` and fails CI | Branch created before the branching rules were circulated | Retarget PR #6 to `develop` and fix the failing check (owner: Hassan Khalid); requested in the review of 21 Nov | Branch naming and target checked in review | Open |
| SCM-03 | `coverageGate` (branch coverage >= 60%) exists in `build.gradle` but CI does not run it | Gate deferred because 60% is only required at M3 | Add `coverageGate` to CI before M3 | V&V plan lists it as an M3 exit criterion | Open, deferred to M3 |
| SCM-04 | The ruleset was switched off once to force-push `develop` (section 3.6) | Commit messages carried trailer lines from an automated runner, which the team chose to remove before the baseline | Message-only rewrite with trees verified identical; ruleset switched back on at once; quoted hashes updated (`7784a6a`) | The runner scripts and their configuration were removed from the repository (`7784a6a`); no further rewrite is planned, and `main` was never rewritten | Closed |

## 5. Reproducing this evidence

```powershell
gh api repos/MicroLend-SE423/microlend/rulesets/24454518
gh pr list --repo MicroLend-SE423/microlend --state all --json number,author,mergedBy,baseRefName,reviews
gh api repos/MicroLend-SE423/microlend/pulls/<n>/reviews
gh run list --repo MicroLend-SE423/microlend --limit 300 --json conclusion,headBranch
git shortlog -sn --remotes
git tag -l -n1
```

Repository settings (merge buttons, collaborators) are returned only to an admin token.
