param(
    [switch]$Expedite,
    [switch]$Check,
    [string]$CodexPath = '',
    [string]$StateRoot = (Join-Path $env:LOCALAPPDATA 'MicroLendScheduler')
)
$ErrorActionPreference = 'Stop'
$source = Split-Path $PSScriptRoot -Parent
$zone = [TimeZoneInfo]::FindSystemTimeZoneById('Pakistan Standard Time')
$now = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone)
$deadline = if ($Expedite) { $now.Date.AddDays(1) } else { $now.Date.AddHours(14) }
if (!$Check -and !$Expedite -and ($now.Hour -lt 12 -or $now.Hour -ge 14)) {
    Write-Output 'Outside 12:00-14:00 Asia/Karachi; skipping.'
    exit 0
}
if (!$CodexPath) { $CodexPath = (Get-Command codex.exe -ErrorAction Stop).Source }
if (!(Test-Path -LiteralPath $CodexPath)) { throw 'Codex executable missing; reinstall the task.' }
if (!$env:JAVA_HOME) { $env:JAVA_HOME = [Environment]::GetEnvironmentVariable('JAVA_HOME', 'User') }
if (!(Test-Path "$env:JAVA_HOME/bin/java.exe")) { throw 'Set JAVA_HOME to JDK 21.' }
foreach ($command in @('git', 'gh')) { $null = Get-Command $command -ErrorAction Stop }
if ($Check) {
    & $CodexPath login status
    if ($LASTEXITCODE) { throw 'Codex login required.' }
    gh auth status
    if ($LASTEXITCODE) { throw 'GitHub login required.' }
    Write-Output "Preflight passed. Window: 12:00-14:00 Asia/Karachi. State: $StateRoot"
    exit 0
}
New-Item -ItemType Directory -Force -Path $StateRoot | Out-Null
$lock = $null
$process = $null
try {
    # Exclusive file handle also prevents overlap with manual expedited runs.
    $lock = [IO.File]::Open((Join-Path $StateRoot 'run.lock'), 'OpenOrCreate', 'ReadWrite', 'None')
    $stamp = $now.ToString('yyyyMMdd-HHmmss')
    Start-Transcript -Path (Join-Path $StateRoot "$stamp.log") | Out-Null
    $checkout = Join-Path $StateRoot 'checkout'
    if (!(Test-Path -LiteralPath $checkout)) {
        git clone https://github.com/MicroLend-SE423/microlend.git $checkout
        if ($LASTEXITCODE) { throw 'Clone failed.' }
        foreach ($key in @('user.name', 'user.email')) {
            $value = git -C $source config --get $key
            if (!$value) { throw "Configure $key in the source repository." }
            git -C $checkout config $key $value
            if ($LASTEXITCODE) { throw "Could not configure $key." }
        }
    }
    $dirty = git -C $checkout status --porcelain
    if ($LASTEXITCODE -or $dirty) { throw 'Scheduler checkout needs inspection; preserving unfinished work.' }
    $mode = if ($Expedite) { 'Expedited: complete as many ordered items as safely possible today, using a red/green pair for each.' }
        else { 'Normal: complete ONE pending queue item with TWO commits: test(red), then feat(green). No filler commits.' }
    $prompt = Get-Content (Join-Path $PSScriptRoot 'daily-code-prompt.txt') -Raw
    $prompt += "`n$mode`nHard deadline: $($deadline.ToString('yyyy-MM-dd HH:mm:ss')) Asia/Karachi. Reserve the final 15 minutes for validation and publication."
    $start = New-Object Diagnostics.ProcessStartInfo
    $start.FileName = $CodexPath
    $start.WorkingDirectory = $checkout
    $start.Arguments = 'exec --sandbox danger-full-access -c approval_policy=never --color never -'
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
            throw 'Deadline reached; process tree stopped. Inspect checkout before next run.'
        }
    }
    [IO.File]::WriteAllText((Join-Path $StateRoot "$stamp-result.txt"), $stdout.Result)
    [IO.File]::WriteAllText((Join-Path $StateRoot "$stamp-agent.log"), $stderr.Result)
    if ($process.ExitCode -ne 0) { throw "Codex failed with exit code $($process.ExitCode)." }
} finally {
    if ($process -and !$process.HasExited) { taskkill.exe /PID $process.Id /T /F | Out-Null }
    if ($lock) { $lock.Dispose(); Stop-Transcript -ErrorAction SilentlyContinue | Out-Null }
}
