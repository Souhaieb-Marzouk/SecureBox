# Building SecureBox

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 or 21 | AGP 8.5 requires 17+; CI uses Temurin 17 |
| Android SDK | Platform 34, Build-Tools 34.0.0 | `ANDROID_HOME` or `local.properties` |
| Android NDK | 27.2.12479018 (r27c) | Builds the native firewall pump; CI installs it |
| CMake | 3.22.1 | Ships with the SDK; used instead of ndk-build |
| Gradle | 8.9 (wrapper included) | Do not use an older Gradle |
| Kotlin | 2.0.21 | Provided by the version catalog |

The native `securebox_pump` library is built with CMake and the NDK. Without the NDK the build
fails; install both through `sdkmanager` (see below).

## Setup

```bash
git clone https://github.com/Souhaieb-Marzouk/SecureBox.git
cd SecureBox
```

Create `local.properties` (git-ignored):

```properties
sdk.dir=/absolute/path/to/Android/Sdk
```

Install the SDK packages:

```bash
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools" "ndk;27.2.12479018" "cmake;3.22.1"
```

## Debug build

```bash
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk` (package `io.securebox.debug`).

Install on a phone or TV box:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## Signed release build

1. Create a keystore **outside** the repository:

```bash
keytool -genkeypair -v \
  -keystore ~/securebox-release.jks \
  -alias securebox \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -dname "CN=SecureBox, OU=Release, O=SecureBox, C=TN"
```

2. Provide the signing credentials through **environment variables** — never a plaintext file:

```powershell
$env:SECUREBOX_KEYSTORE_PATH = "C:\path\to\securebox-release.jks"
$env:SECUREBOX_KEYSTORE_PASSWORD = "..."
$env:SECUREBOX_KEY_ALIAS = "securebox"
$env:SECUREBOX_KEY_PASSWORD = "..."
```

On Windows the password can instead be stored encrypted with DPAPI in
`..\_securebox_keys\credentials.enc` (outside the repository). The bundled helper decrypts it in
memory, exports it to Gradle, and clears it afterwards:

```powershell
.\tools\build-release.ps1
```

`signing.properties` is no longer supported: it stored the keystore password in clear text.

3. Build:

```bash
./gradlew assembleRelease
```

Outputs in `app/build/outputs/apk/release/`:

| File | Contents |
|---|---|
| `app-universal-release.apk` | Every ABI, install on anything |
| `app-arm64-v8a-release.apk` | 64-bit ARM phones and TV boxes |
| `app-armeabi-v7a-release.apk` | Older 32-bit ARM devices |

The build also bakes the SHA-256 of your signing certificate into `BuildConfig.EXPECTED_SIGNING_CERT_SHA256`, which the app uses for its self-integrity check. **A release built with a different keystore cannot update an existing install** — Android will reject it.

## Verification

```bash
# Signature
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs \
  app/build/outputs/apk/release/app-universal-release.apk

# Checksums
cd app/build/outputs/apk/release
sha256sum *.apk
```

## Quality gates

```bash
./gradlew verifyNoHardcodedStrings   # fails on Compose string literals
./gradlew testDebugUnitTest          # JVM unit tests
./gradlew lintDebug                  # HardcodedText is an error
./gradlew check                      # everything above
```

`verifyNoHardcodedStrings` is a custom Gradle task: every user-visible string must live in
`res/values/strings.xml` and `res/values-fr/strings.xml`.

## Sandboxed or restricted builds

Some sandboxes (CI containers, agent environments on Windows) let the JVM write inside the checkout
but block native tools such as `aapt2`. Redirect every module's build directory to a writable
location without touching the repository:

```powershell
.\tools\sandbox-build.ps1 assembleDebug
.\tools\sandbox-build.ps1 verifyNoHardcodedStrings test lintDebug
```

The script creates a fresh temporary build root and passes `tools/sandbox-build-root.gradle` through
`-I`. Set `SECUREBOX_BUILD_ROOT` yourself if you prefer a fixed path. On a normal developer machine
this is not needed.

## Reproducibility

SecureBox aims for reproducible *inputs*, not bit-identical outputs (Android's build pipeline is not
bit-reproducible by default):

- The Gradle wrapper pins Gradle 8.9.
- `gradle/libs.versions.toml` pins every dependency exactly.
- The JDK version is documented and pinned in CI.
- No dynamic version ranges are used.
- Release builds disable configuration cache but enable build caching for speed; use
  `./gradlew assembleRelease --no-build-cache --rerun-tasks` for a from-scratch build.

To compare two builds, use `apksigner verify --print-certs` (certificate must match) and compare the
`classes.dex` SHA-256 after `unzip`. DEX ordering may differ between machines.

## Architecture notes

- `core-domain` is a pure Kotlin JVM module; it has no Android dependency and can be unit-tested fast.
- Feature modules never depend on each other except `feature-settings → feature-update` and
  `feature-settings → feature-threatintel`, which are intentional UI composition points.
- Hilt modules live next to the layer they provide (`core-data`, `core-network`, `app`).
