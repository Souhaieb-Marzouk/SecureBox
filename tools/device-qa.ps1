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

# adb's server writes adb.log into %TEMP%. Some locked-down environments deny that write and the
# server fails with "cannot open ...\adb.log: Permission denied". Point TEMP at a writable folder
# inside the repo so the helper works from any shell.
$adbTemp = Join-Path (Split-Path $PSScriptRoot -Parent) 'build\adb-temp'
New-Item -ItemType Directory -Force -Path $adbTemp | Out-Null
$env:TEMP = $adbTemp
$env:TMP = $adbTemp

if (-not (Get-Command $Adb -ErrorAction SilentlyContinue)) {
    if ($env:ANDROID_HOME) {
        $candidate = Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe'
        if (Test-Path $candidate) { $Adb = $candidate }
    }
}
if (-not (Get-Command $Adb -ErrorAction SilentlyContinue)) {
    throw "adb not found. Install platform-tools or pass -Adb."
}

$expectedVersion = ""
if (-not $Apk) {
    $repo = Split-Path $PSScriptRoot -Parent
    $dist = Join-Path (Split-Path $repo -Parent) 'SecureBox-dist'
    $latest = Get-ChildItem $dist -Directory -ErrorAction Stop |
        Where-Object { $_.Name -match '^v\d+\.\d+\.\d+$' } |
        Sort-Object { [version]($_.Name.TrimStart('v')) } -Descending |
        Select-Object -First 1
    if (-not $latest) { throw "No release folder found under $dist" }
    $Apk = Join-Path $latest.FullName 'securebox-universal-release.apk'
    $expectedVersion = $latest.Name.TrimStart('v')
}
if (-not (Test-Path $Apk)) { throw "APK not found: $Apk" }
Write-Host "APK: $Apk" -ForegroundColor Green

$devices = & $Adb devices | Select-Object -Skip 1 | Where-Object { $_ -match '\tdevice$' }
if (-not $devices) {
    throw "No device connected. Enable USB debugging (or adb over Wi-Fi) and retry."
}
Write-Host "Device: $($devices -join ', ')" -ForegroundColor Green

Write-Host "Installing $Apk ..."
& $Adb install -r $Apk | ForEach-Object { Write-Host $_ }
if ($LASTEXITCODE -ne 0) { throw "adb install failed" }

Write-Host "`nPackage facts:"
$facts = & $Adb shell dumpsys package $Package | Select-String 'versionCode=|versionName='
$facts | ForEach-Object { $_.Line.Trim() }
if ($expectedVersion) {
    $installed = ($facts | Where-Object { $_.Line -match 'versionName=' } | Select-Object -First 1).Line
    if ($installed -notmatch [regex]::Escape($expectedVersion)) {
        Write-Host "WARNING: expected v$expectedVersion, found: $installed" -ForegroundColor Red
    } else {
        Write-Host "Version check: v$expectedVersion OK" -ForegroundColor Green
    }
}

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

Write-Host "`nNext: complete the v0.4.0 checks in TESTING.md - onboarding, phone/TV shell, network N1-N6, scan and RASP." -ForegroundColor Yellow
