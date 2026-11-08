# Daily coding

Windows task `MicroLend-Daily-Code` starts at **12:00 Pakistan time** every day and
runs Claude Code in headless mode. The runner enforces a **14:00 cutoff**, including
child processes. A normal run attempts one queue item from
[`m2-code-todo.md`](m2-code-todo.md) with a real red/green commit pair, updates its
QA evidence, and pushes to Tughral's open ledger PR into `develop` (or creates one).
It exits early when finished. No merge is automated.

## Install or refresh

Run after changing the runner, the prompt, or the Claude Code install:

```powershell
.\scripts\install-daily-code-task.ps1
```

The installer copies `daily-code.ps1` and `daily-code-prompt.txt` to
`%LOCALAPPDATA%\MicroLendScheduler\runner`, so the task works whatever branch the
interactive checkout is on. It also clones a separate scheduler checkout to
`%LOCALAPPDATA%\MicroLendScheduler\checkout` and runs a preflight.

## Requirements

- The laptop is on, online and logged in at noon. A run missed while it was off
  starts on wake, but still only works inside 12:00-14:00.
- GitHub CLI signed in as `tughral1`; JDK 21 in `JAVA_HOME`.
- A long-lived Claude Code token, because the interactive sign-in expires:
  run `claude setup-token` once and store the token as the user environment
  variable `CLAUDE_CODE_OAUTH_TOKEN`.

## What the unattended agent may do

It runs with `--permission-mode dontAsk`: tools not on the allowlist are refused,
not prompted. Allowed: file read/edit/write and search, `git`, `gh pr`, `gh run`
and `gradlew.bat`. Explicitly refused: force push, `git reset --hard`,
`git clean`, `gh pr merge` and `gh pr close`. The repository ruleset is a second
backstop: force pushes are blocked and every merge needs a non-author approval.

Sessions are stored only on this laptop; they do not appear on claude.ai. Usage
counts against the Claude account's limits.

## Logs

Each run writes `<stamp>.log` (transcript), `<stamp>-result.json` (agent's final
report) and `<stamp>-agent.log` in `%LOCALAPPDATA%\MicroLendScheduler`. A failed run
exits non-zero, so Task Scheduler's last result shows it. Uncommitted work in the
scheduler checkout blocks the next run until inspected.

## Expedite or disable

One-day sprint, working through queue items until finished, blocked or midnight:

```powershell
& "$env:LOCALAPPDATA\MicroLendScheduler\runner\daily-code.ps1" -Expedite
```

Disable the daily run:

```powershell
Disable-ScheduledTask -TaskName MicroLend-Daily-Code
```
