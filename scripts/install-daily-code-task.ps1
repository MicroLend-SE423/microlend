param([string]$TaskName = 'MicroLend-Daily-Code')
$ErrorActionPreference = 'Stop'
$codex = (Get-Command codex.exe -ErrorAction Stop).Source
& "$PSScriptRoot/daily-code.ps1" -Check -CodexPath $codex
if ($LASTEXITCODE) { throw 'Preflight failed.' }
$runner = Join-Path $PSScriptRoot 'daily-code.ps1'
$action = New-ScheduledTaskAction -Execute "$env:SystemRoot/System32/WindowsPowerShell/v1.0/powershell.exe" `
    -Argument "-NoProfile -NonInteractive -WindowStyle Hidden -ExecutionPolicy Bypass -File `"$runner`" -CodexPath `"$codex`""
$trigger = New-ScheduledTaskTrigger -Daily -At '12:00'
# Explicit offset keeps noon Pakistan time even if the machine timezone changes.
$zone = [TimeZoneInfo]::FindSystemTimeZoneById('Pakistan Standard Time')
$now = [TimeZoneInfo]::ConvertTimeFromUtc([DateTime]::UtcNow, $zone)
$firstRun = $now.Date.AddHours(12)
if ($firstRun -le $now) { $firstRun = $firstRun.AddDays(1) }
$trigger.StartBoundary = $firstRun.ToString('yyyy-MM-ddTHH:mm:ss') + '+05:00'
$settings = New-ScheduledTaskSettingsSet -MultipleInstances IgnoreNew `
    -ExecutionTimeLimit (New-TimeSpan -Hours 2 -Minutes 5) `
    -AllowStartIfOnBatteries -DontStopIfGoingOnBatteries
$principal = New-ScheduledTaskPrincipal -UserId ([Security.Principal.WindowsIdentity]::GetCurrent().Name) `
    -LogonType Interactive -RunLevel Limited
Register-ScheduledTask -TaskName $TaskName -Action $action -Trigger $trigger `
    -Settings $settings -Principal $principal -Force | Select-Object TaskName,State
Get-ScheduledTaskInfo -TaskName $TaskName | Select-Object NextRunTime,LastTaskResult
