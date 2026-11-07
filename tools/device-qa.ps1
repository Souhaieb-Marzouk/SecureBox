# On-device smoke test for SecureBox.
#
# Installs the release APK on the connected phone or TV box, launches it, and prints the facts that
# are easy to get wrong (version, resumed activity, crashes). Manual steps from TESTING.md still
# apply for gestures and VPN consent.
#
# Usage:
#   .\tools\device-qa.ps1
#   .\tools\device-qa.ps1 -Apk "C:\path\to\securebox-universal-release.apk" -Adb "C:\...\adb.exe"

param(
    [string]$Apk = "",
    [string]$Adb = "adb",
    [string]$Package = "io.securebox"
)

$ErrorActionPreference = 'Stop'

if (-not (Get-Command $Adb -ErrorAction SilentlyContinue)) {
    if ($env:ANDROID_HOME) {
        $candidate = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
        if (Test-Path $candidate) { $Adb = $candidate }
    }
}
if (-not (Get-Command $Adb -ErrorAction SilentlyContinue)) {
    throw "adb not found. Install platform-tools or pass -Adb."
}

if (-not $Apk) {
    $repo = Split-Path $PSScriptRoot -Parent
    $Apk = Join-Path (Split-Path $repo -Parent) 'SecureBox-dist\v0.2.0\securebox-universal-release.apk'
}
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk" }

$devices = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
if (-not $devices) {
    throw "No device connected. Enable USB debugging (or adb over Wi-Fi) and retry."
}
Write-Host "Device: $($devices -join ', ')" -ForegroundColor Green

Write-Host "Installing $Apk ..."
& $Adb install -r $Apk | ForEach-Object { Write-Host $_ }
if ($LASTEXITCODE -ne 0) { throw "adb install failed" }

Write-Host "`nPackage facts:"
& $Adb shell dumpsys package $Package | Select-String 'versionCode=|versionName=' | ForEach-Object { $_.Line.Trim() }

Write-Host "`nLaunching ..."
& $Adb shell am start -n "$Package/.MainActivity" | Out-Null
Start-Sleep -Seconds 4

$resumed = & $Adb shell dumpsys activity activities | Select-String 'mResumedActivity' | Select-Object -First 1
Write-Host "Resumed: $($resumed.Line.Trim())"

Write-Host "`nRecent crashes (AndroidRuntime):"
$crashes = & $Adb logcat -d -t 400 -s AndroidRuntime:E
if ($crashes) { $crashes | Select-Object -Last 40 } else { Write-Host "  none" -ForegroundColor Green }

Write-Host "`nScreenshot ..."
try {
    $shot = Join-Path (Get-Location) 'securebox-device.png'
    & cmd /c "`"$Adb`" exec-out screencap -p > `"$shot`""
    if (Test-Path $shot) { Write-Host "  $shot" }
} catch {
    Write-Host "  screenshot skipped"
}

Write-Host "`nNext: complete the v0.2.0 checks in TESTING.md (block a test app, unblock it, trigger an anomaly notification, open Network with usage access denied)." -ForegroundColor Yellow
