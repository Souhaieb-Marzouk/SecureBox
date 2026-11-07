# Builds a signed SecureBox release using the local keystore.
#
# The password is stored encrypted with Windows DPAPI in ..\_securebox_keys\credentials.enc.
# It is decrypted only in memory, exported to the Gradle process, and cleared afterwards.
# No plaintext password ever touches the repository or a properties file.

param(
    [string]$Task = "assembleRelease",
    [string[]]$ExtraArgs = @()
)

$ErrorActionPreference = "Stop"

$repo = Split-Path $PSScriptRoot -Parent
$keys = Join-Path (Split-Path $repo -Parent) "_securebox_keys"
$keystore = Join-Path $keys "securebox-release.jks"
$encrypted = Join-Path $keys "credentials.enc"

if (-not (Test-Path $keystore)) { throw "Keystore not found: $keystore" }
if (-not (Test-Path $encrypted)) { throw "Encrypted credentials not found: $encrypted" }

$secure = Get-Content $encrypted | ConvertTo-SecureString
$ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
try {
    $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
} finally {
    [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
}

$env:SECUREBOX_KEYSTORE_PATH = $keystore
$env:SECUREBOX_KEYSTORE_PASSWORD = $password
$env:SECUREBOX_KEY_ALIAS = "securebox"
$env:SECUREBOX_KEY_PASSWORD = $password

Push-Location $repo
try {
    $gradlew = Join-Path $repo "gradlew.bat"
    & $gradlew $Task @ExtraArgs
    exit $LASTEXITCODE
} finally {
    Pop-Location
    Remove-Item Env:SECUREBOX_KEYSTORE_PASSWORD -ErrorAction SilentlyContinue
    Remove-Item Env:SECUREBOX_KEY_PASSWORD -ErrorAction SilentlyContinue
}
