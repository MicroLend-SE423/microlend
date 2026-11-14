# MicroLend - SCM Quality-Gate Evidence (M2)

**Deliverable:** SE431 M2, "SCM quality-gate evidence" · **Document lead:** to be assigned (M2 leads not yet chosen)
**Evidence collected:** 14 Nov 2026 from the GitHub API (`gh api`, `gh pr`, `gh run`) and `git` on the
repository `github.com/MicroLend-SE423/microlend`. Figures are re-collected immediately before submission.

This document shows the configuration-management strategy the team documented, the controls that
enforce it on GitHub, and what the repository history shows about whether it was followed. Where
history shows the strategy was **not** followed, that is recorded as a nonconformance with a CAPA
rather than left out.

## 1. Documented strategy

Source: `README.md` (Branching strategy, Baselines) and `CLAUDE.md` (Repository rules).

| Item | Rule |
|---|---|
| Branches | `main` holds baselines only; `develop` is integration; work happens on `feature/<module>-<short-name>` branched from `develop` |
| Change path | Every change reaches `develop` and `main` only by pull request |
| Review | Every pull request is approved by a member who is not the author; the author does not merge their own work |
| History | Merge commits only (`--no-ff`); squash and rebase merges disabled so the `test(red)` -> `feat(green)` -> `refactor:` trail survives |
| Commit convention | `test(red):`, `feat(green):`, `refactor:`, `fix:`, `docs:`, `chore:`, `test:` |
| Quality gate | CI must pass: compile, unit tests, Checkstyle (constraint C-01: no `double`/`float`), JaCoCo report |
| Baselines | Annotated tags `m1-baseline`, `m2-baseline`, `m3-baseline` on `main` at each milestone |

## 2. Repository controls in force

| Control | Setting (as read on 14 Nov 2026) | Gate it enforces |
|---|---|---|
| Ruleset `main-protection` (id 24454518) | Active; targets the default branch (`main`) **and** `refs/heads/develop`; bypass list empty, so admins cannot bypass it | No direct push to either branch |
| Rule `pull_request` | 1 approving review; stale approvals dismissed on push; approval of the most recent push required; all review threads resolved; extra approval for unattributed changes | Independent review before merge |
| Rule `pull_request.allowed_merge_methods` | `merge` only | TDD history cannot be squashed away |
| Rule `non_fast_forward` | On | No force push, so published history cannot be rewritten |
| Rule `deletion` | On | `main` and `develop` cannot be deleted |
| Repository merge buttons | merge commit on; squash off; rebase off | Same as above, at repository level |
| Auto-delete head branches | Off | Feature branches, and the commits on them, stay visible for assessment |
| Access | `mIBRAHIM707` admin, `tughral1` admin, `Hassan242-kk` write | Every member can push feature branches; nobody can bypass the ruleset |
| CI | `.github/workflows/build.yml` on push to `main`, `develop`, `feature/**` and on pull requests into `main`/`develop`; uploads test, JaCoCo and Checkstyle reports as artifacts (90 days) | Build, tests, static analysis and coverage on every change |

## 3. What the history shows

### 3.1 Pull requests

| PR | Head -> base | Author | Merged by | Reviews | Conforms? |
|---|---|---|---|---|---|
| #1 | `docs/m1-review-fixes` -> `develop` | mIBRAHIM707 | mIBRAHIM707 | none | **No** (self-merge, no review) |
| #2 | `develop` -> `main` | mIBRAHIM707 | tughral1 | none | Partly (merged by another member, but no recorded approval) |
| #3 | `docs/m1-trim` -> `develop` | mIBRAHIM707 | mIBRAHIM707 | none | **No** |
| #4 | `develop` -> `main` (M1 baseline) | mIBRAHIM707 | mIBRAHIM707 | none | **No** |
| #5 | `docs/repo-url` -> `develop` | mIBRAHIM707 | mIBRAHIM707 | none | **No** |
| #6 | `feat/hassan-product-schedule-tests` -> `main` | Hassan242-kk | open | none | **No** (targets `main`, not `develop`; CI failing) |
| #7 | `docs/claude-handoff` -> `develop` | tughral1 | tughral1 | none | **No** (self-merge, no review) |
| #8 | `feature/ledger-entry` -> `develop` | tughral1 | open | none yet; GitHub shows `REVIEW_REQUIRED` | Yes so far: merge is blocked until a non-author approves |

All six merges happened between 9 Oct and 4 Nov 2026. The ruleset was last updated on
**4 Nov 2026 15:43 PKT**, after PR #7 (merged 12:46 the same day). Before that update the
ruleset's bypass list allowed admins through, which is how those merges went in. Since then no
pull request has been merged without approval. PR #8 is the first to be held by the gate.

### 3.2 CI runs

All workflow runs from 4 Oct 2026 (first run) to 14 Nov 2026: **50**. **45** succeeded, **3** failed and **2** were in progress at collection time.

| Failed run | Branch | Cause | Resolution |
|---|---|---|---|
| 37203232415, 37203284296 | `develop` | Workflow bootstrap on 4 Oct: `gradlew` lost its executable bit | Fixed in the next commit (`git update-index --chmod=+x gradlew`); every later run on `develop` passed |
| 38076363948 | `feat/hassan-product-schedule-tests` | PR #6 check fails | Open; PR #6 also needs retargeting to `develop` |

The 20 runs on `feature/ledger-entry` are the per-push evidence that each green commit built cleanly in CI.

### 3.3 TDD commit trail (PR #8)

On `feature/ledger-entry` (commits not yet on `develop`) the history holds **8** `test(red):` and
**8** `feat(green):` commits. Each red commit precedes its green commit and names the same queue
item (L-01 ... L-14). Re-count with:

```powershell
git log --format=%s origin/develop..origin/feature/ledger-entry
```

### 3.4 Attribution

Commits across all branches, by author:

| Author | Commits |
|---|---|
| tughral1 | 39 |
| mIBRAHIM707 / Muhammad Ibrahim | 16 |
| Hassan242-kk | 1 |

Hassan's M2 code exists only in open PR #6. This is an individual-evidence risk (risk register R6).

### 3.5 Baselines

| Tag | Commit | Status |
|---|---|---|
| `m1-baseline` | `10688c2` (merge of PR #4 into `main`, 10 Oct 2026) | Done |
| `m2-baseline` | - | To be tagged on `main` after `develop` -> `main` is reviewed and merged |

## 4. Nonconformances and CAPA

Recorded in `qa/defect-log.md`: SCM-01 = DEF-006 (CAPA-001), SCM-02 = DEF-007, SCM-03 = DEF-008.

| ID | Finding | Root cause | Corrective action | Preventive action | Status |
|---|---|---|---|---|---|
| SCM-01 | PRs #1, #3, #4, #5, #7 were self-merged with no review; #2 had no recorded approval | Ruleset bypass list let admins merge; the review rule was documented but not enforced | Ruleset re-applied 4 Nov 2026 with an empty bypass list, targeting `main` and `develop` | Every member reviews at least one PR per milestone; this table is re-collected at each milestone | Corrective action verified: PR #8 is blocked with `REVIEW_REQUIRED` |
| SCM-02 | PR #6 targets `main` instead of `develop` and fails CI | Branch created before the branching rules were circulated | Retarget PR #6 to `develop` and fix the failing check (owner: Hassan) | Branch naming and target checked in review | Open |
| SCM-03 | `coverageGate` (branch coverage >= 60%) exists in `build.gradle` but CI does not run it | Gate deferred because 60% is only required at M3 | Add `coverageGate` to CI before M3 | V&V plan lists it as an M3 exit criterion | Open, deferred to M3 |

## 5. Reproducing this evidence

```powershell
gh api repos/MicroLend-SE423/microlend/rulesets/24454518
gh pr list --repo MicroLend-SE423/microlend --state all --json number,author,mergedBy,baseRefName,reviewDecision
gh api repos/MicroLend-SE423/microlend/pulls/<n>/reviews
gh run list --repo MicroLend-SE423/microlend --limit 100 --json conclusion,headBranch
git shortlog -sne --all
git tag -l -n1
```

Repository settings (merge buttons, collaborators) are only returned to an admin token, so run
those calls as an admin account (`gh auth switch --user tughral1`).
