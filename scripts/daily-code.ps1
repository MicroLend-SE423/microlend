param(
    [switch]$Expedite,
    [switch]$Check,
    # Catch-up for missed days: complete up to this many queue items, one red/green
    # pair each, within -DurationMinutes from now, regardless of the noon window.
    [int]$CatchUpItems = 0,
    [int]$DurationMinutes = 120,
    [string]$ClaudePath = '',
    [string]$Model = 'claude-opus-5-5',
    [string]$StateRoot = (Join-Path $env:LOCALAPPDATA 'MicroLendScheduler')
)
# Daily MicroLend coding run with Claude Code in headless mode.
# Installed copy lives in $StateRoot\runner (see install-daily-code-task.ps1), so the
# scheduled task does not depend on which branch the interactive checkout is on.
$ErrorActionPreference = 'Stop'
$zone = [TimeZoneInfo]::FindSystemTimeZoneById('Pakistan Standard Time')
$now = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone)
$catchUp = $CatchUpItems -gt 0
$deadline = if ($catchUp) { $now.AddMinutes($DurationMinutes) }
    elseif ($Expedite) { $now.Date.AddDays(1) } else { $now.Date.AddHours(14) }
if (!$Check -and !$Expedite -and !$catchUp -and ($now.Hour -lt 12 -or $now.Hour -ge 14)) {
    Write-Output 'Outside 12:00-14:00 Asia/Karachi; skipping.'
    exit 0
}
if (!$ClaudePath) { $ClaudePath = (Get-Command claude.exe -ErrorAction Stop).Source }
if (!(Test-Path -LiteralPath $ClaudePath)) { throw 'Claude Code executable missing; reinstall the task.' }
if (!$env:JAVA_HOME) { $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User') }
if (!(Test-Path "$env:JAVA_HOME/bin/java.exe")) { throw 'Set JAVA_HOME to JDK 21.' }
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
if (!$env:CLAUDE_CODE_OAUTH_TOKEN) {
    $token = [Environment]::GetEnvironmentVariable('CLAUDE_CODE_OAUTH_TOKEN', 'User')
    if ($token) { $env:CLAUDE_CODE_OAUTH_TOKEN = $token }
}
foreach ($command in @('git', 'gh')) { $null = Get-Command $command -ErrorAction Stop }
$checkout = Join-Path $StateRoot 'checkout'
if (!(Test-Path -LiteralPath (Join-Path $checkout '.git'))) { throw "Scheduler checkout missing at $checkout; run the installer." }
$promptFile = Join-Path $PSScriptRoot 'daily-code-prompt.txt'

# Allowlist for --permission-mode dontAsk: anything not listed is refused, not prompted.
$allowed = @(
    'Read', 'Edit', 'Write', 'Glob', 'Grep',
    'PowerShell(git *)', 'PowerShell(gh pr *)', 'PowerShell(gh run *)',
    'PowerShell(.\gradlew.bat *)', 'PowerShell(./gradlew.bat *)'
)
$denied = @(
    'PowerShell(git push --force*)', 'PowerShell(git push -f*)', 'PowerShell(git reset --hard*)',
    'PowerShell(git clean*)', 'PowerShell(gh pr merge*)', 'PowerShell(gh pr close*)'
)
function Format-Args([string[]]$items) { ($items | ForEach-Object { '"' + $_ + '"' }) -join ' ' }

if ($Check) {
    # Native tools write warnings to stderr; judge them by exit code, not by stderr output.
    $ErrorActionPreference = 'Continue'
    gh auth status 2>&1 | Out-Null
    if ($LASTEXITCODE) { throw 'GitHub login required.' }
    $probe = 'Reply with the single word READY.' | & $ClaudePath -p --model $Model --permission-mode dontAsk 2>$null
    $probeExit = $LASTEXITCODE
    $ErrorActionPreference = 'Stop'
    if ($probeExit -or ($probe -join ' ') -notmatch 'READY') { throw "Claude Code did not answer: $probe" }
    if (!$env:CLAUDE_CODE_OAUTH_TOKEN) {
        Write-Warning 'No CLAUDE_CODE_OAUTH_TOKEN: runs depend on the interactive sign-in, which expires. Run "claude setup-token".'
    }
    Write-Output "Preflight passed. Model: $Model. Window: 12:00-14:00 Asia/Karachi. State: $StateRoot"
    exit 0
}

$lock = $null
$process = $null
$failed = $null
try {
    # Exclusive file handle also prevents overlap with manual expedited runs.
    $lock = [IO.File]::Open((Join-Path $StateRoot 'run.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    $stamp = $now.ToString('yyyyMMdd-HHmmss')
    Start-Transcript -Path (Join-Path $StateRoot "$stamp.log") | Out-Null
    $dirty = git -C $checkout status --porcelain
    if ($LASTEXITCODE -or $dirty) { throw 'Scheduler checkout has uncommitted work; inspect it before the next run.' }
    $mode = if ($catchUp) { "Catch-up for $CatchUpItems missed daily runs: complete up to $CatchUpItems pending queue items in order, each with its own test(red) then feat(green) pair, pushing after each green build. Stop early if blocked; never start an item you cannot finish before the deadline." }
        elseif ($Expedite) { 'Expedited: complete as many ordered items as safely possible today, using a red/green pair for each.' }
        else { 'Normal: complete ONE pending queue item with TWO commits: test(red), then feat(green). No filler commits.' }
    $prompt = Get-Content $promptFile -Raw
    $prompt += "`n$mode`nHard deadline: $($deadline.ToString('yyyy-MM-dd HH:mm:ss')) Asia/Karachi. Reserve the final 15 minutes for validation and publication."
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $ClaudePath
    $start.WorkingDirectory = $checkout
    $start.Arguments = "-p --model $Model --output-format json --permission-mode dontAsk " +
        "--allowedTools $(Format-Args $allowed) --disallowedTools $(Format-Args $denied)"
    $start.UseShellExecute = $false
    $start.CreateNoWindow = $true
    $start.RedirectStandardInput = $true
    $start.RedirectStandardOutput = $true
    $start.RedirectStandardError = $true
    $process = New-Object Diagnostics.Process
    $process.StartInfo = $start
    $null = $process.Start()
    $stdout = $process.StandardOutput.ReadToEndAsync()
    $stderr = $process.StandardError.ReadToEndAsync()
    $process.StandardInput.Write($prompt)
    $process.StandardInput.Close()
    $utcDeadline = [TimeZoneInfo]::ConvertTimeToUtc($deadline, $zone)
    while (!$process.WaitForExit(1000)) {
        if ([DateTime]::UtcNow -ge $utcDeadline) {
            taskkill.exe /PID $process.Id /T /F | Out-Null
            throw 'Deadline reached; process tree stopped. Inspect the checkout before the next run.'
        }
    }
    [IO.File]::WriteAllText((Join-Path $StateRoot "$stamp-result.json"), $stdout.Result)
    [IO.File]::WriteAllText((Join-Path $StateRoot "$stamp-agent.log"), $stderr.Result)
    if ($process.ExitCode -ne 0) { throw "Claude Code exited with code $($process.ExitCode); see $stamp-agent.log." }
    $result = $stdout.Result | ConvertFrom-Json
    if ($result.is_error) { throw "Claude Code reported an error: $($result.result)" }
    Write-Output $result.result
} catch {
    $failed = $_
} finally {
    if ($process -and !$process.HasExited) { taskkill.exe /PID $process.Id /T /F | Out-Null }
    if ($failed) { Write-Output "FAILED: $failed" }
    if ($lock) { Stop-Transcript -ErrorAction SilentlyContinue | Out-Null; $lock.Dispose() }
}
# A non-zero exit makes Task Scheduler record the failure instead of a false success.
if ($failed) { exit 1 }
exit 0
