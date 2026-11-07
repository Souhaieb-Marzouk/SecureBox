# Runs Gradle with build outputs redirected outside the repository.
#
# Use this in sandboxes where aapt2 or other native tools cannot write inside the checkout.
# The repository is not modified; outputs land in a fresh temporary directory.

param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]]$GradleArgs = @('assembleDebug')
)

$ErrorActionPreference = 'Stop'

$repo = Split-Path $PSScriptRoot -Parent
$buildRoot = Join-Path ([IO.Path]::GetTempPath()) ('securebox-build-' + [guid]::NewGuid().ToString('N').Substring(0, 8))
$env:SECUREBOX_BUILD_ROOT = $buildRoot

Write-Host "Build root: $buildRoot"
Push-Location $repo
try {
    & (Join-Path $repo 'gradlew.bat') -I (Join-Path $PSScriptRoot 'sandbox-build-root.gradle') @GradleArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
}
