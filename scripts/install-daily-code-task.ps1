param(
    [string]$TaskName = 'MicroLend-Daily-Code',
    [string]$StateRoot = (Join-Path $env:LOCALAPPDATA 'MicroLendScheduler')
)
# Installs (or refreshes) the daily Claude Code run. Copies the runner out of the
# repository so the task keeps working whatever branch the interactive checkout is on.
$ErrorActionPreference = 'Stop'
$source = Split-Path $PSScriptRoot -Parent
$claude = (Get-Command claude.exe -ErrorAction Stop).Source
$runnerDir = Join-Path $StateRoot 'runner'
New-Item -ItemType Directory -Force -Path $runnerDir | Out-Null
Copy-Item (Join-Path $PSScriptRoot 'daily-code.ps1'), (Join-Path $PSScriptRoot 'daily-code-prompt.txt') $runnerDir -Force

$checkout = Join-Path $StateRoot 'checkout'
if (!(Test-Path -LiteralPath (Join-Path $checkout '.git'))) {
    git clone https://github.com/MicroLend-SE423/microlend.git $checkout
    if ($LASTEXITCODE) { throw 'Clone failed.' }
}
foreach ($key in @('user.name', 'user.email')) {
    $value = git -C $source config --get $key
    if (!$value) { throw "Configure $key in the source repository." }
    git -C $checkout config $key $value
}

$runner = Join-Path $runnerDir 'daily-code.ps1'
& $runner -Check -ClaudePath $claude
if ($LASTEXITCODE) { throw 'Preflight failed.' }

$action = New-ScheduledTaskAction -Execute "$env:SystemRoot\System32\WindowsPowerShell\v1.0\powershell.exe" `
    -Argument "-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$runner`" -ClaudePath `"$claude`"" `
    -WorkingDirectory $runnerDir
$trigger = New-ScheduledTaskTrigger -Daily -At '12:00'
# Explicit offset keeps noon Pakistan time even if the machine timezone changes.
$zone = [TimeZoneInfo]::FindSystemTimeZoneById('Pakistan Standard Time')
$now = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone)
$firstRun = $now.Date.AddHours(12)
if ($firstRun -le $now) { $firstRun = $firstRun.AddDays(1) }
$trigger.StartBoundary = $firstRun.ToString('yyyy-MM-ddTHH:mm:ss') + '+05:00'
# StartWhenAvailable: a run missed while the laptop was off starts on wake;
# the runner itself still refuses to work outside 12:00-14:00.
$settings = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew -StartWhenAvailable `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2 -Minutes 5) `
    -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
$principal = New-ScheduledTaskPrincipal -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger `
    -Settings $settings -Principal $principal -Force | Select-Object TaskName, State
Get-ScheduledTaskInfo -TaskName $TaskName | Select-Object NextRunTime, LastTaskResult
