# Building SecureBox

## Requirements

| Tool | Version | Notes |
|---|---|---|
| JDK | 17 or 21 | AGP 8.5 requires 17+; CI uses Temurin 17 |
| Android SDK | Platform 34, Build-Tools 34.0.0 | `ANDROID_HOME` or `local.properties` |
| Gradle | 8.9 (wrapper included) | Do not use an older Gradle |
| Kotlin | 2.0.21 | Provided by the version catalog |

No NDK is required for v0.1.0. The native packet engine used by the VPN arrives in v0.2.0.

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
sdkmanager "platforms;android-34" "build-tools;34.0.0" "platform-tools"
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

2. Create `signing.properties` in the repository root (git-ignored):

```properties
storeFile=/absolute/path/to/securebox-release.jks
storePassword=…
keyAlias=securebox
keyPassword=…
```

Alternatively set the environment variables `SECUREBOX_KEYSTORE_PATH`,
`SECUREBOX_KEYSTORE_PASSWORD`, `SECUREBOX_KEY_ALIAS`, `SECUREBOX_KEY_PASSWORD`.

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
