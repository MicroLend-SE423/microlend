# Daily coding

Windows task `MicroLend-Daily-Code` starts at **12:00 Pakistan time** every day.
The runner enforces a **14:00 cutoff**, including child processes. A normal run
attempts one queue item with a real red/green commit pair, updates its QA evidence,
and pushes to Tughral's open ledger PR into `develop` (or creates one).
It exits early when finished. No merge is automated.

Install or refresh after moving the repository or updating the Codex executable:

```powershell
.\scripts\install-daily-code-task.ps1
.\scripts\daily-code.ps1 -Check
```

The computer must be awake, connected and logged in, with working Codex ChatGPT
and GitHub CLI sessions and JDK 21 in JAVA_HOME. No new API key or hosting is used;
Codex consumes the existing account's usage allowance. Missed runs are skipped.
The task does not wake the computer or run after logout.

Work happens in `%LOCALAPPDATA%\MicroLendScheduler\checkout`; logs are alongside it.
Your interactive checkout is untouched. A lock prevents overlapping manual and
scheduled runs. Dirty work is preserved and blocks the next run for inspection.
The unattended agent has filesystem/network access to build and publish; its
prompt limits work to this repository and the authorized queue.

For an explicitly requested one-day sprint, run:

```powershell
.\scripts\daily-code.ps1 -Expedite
```

That invocation works through successive queue items until finished, blocked, or
midnight Pakistan time, without changing the daily schedule. It cannot guarantee
completion of dependencies owned by teammates. To disable daily runs:

```powershell
Disable-ScheduledTask -TaskName MicroLend-Daily-Code
```

The executable path is resolved at installation. Reinstall if an IDE upgrade moves
Codex. Local preflight and scheduler checks do not prove a future unattended model
run will succeed; inspect the dated result and agent log after each run.
