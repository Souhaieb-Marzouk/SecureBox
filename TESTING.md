# Testing SecureBox

This document covers automated tests, manual verification and the checks required before a release.

## Automated tests

### Unit tests (JVM)

```bash
./gradlew testDebugUnitTest test
```

Current suite: **55 tests, 0 failures** (52 domain + 3 core-data).

| Area | Examples |
|---|---|
| SemVer parsing/comparison | `SemVerParserTest`, `UpdateEvaluatorTest` |
| SHA-256 / release manifest | `Sha256Test` |
| Security score | `SecurityScoreTest` |
| Safe Browsing hash prefixes | `SafeBrowsingHashTest` |
| Settings/domain rules | remaining `core-domain` tests |

### Lint and hardcoded strings

```bash
./gradlew verifyNoHardcodedStrings lintDebug
```

- `HardcodedText` is a lint error.
- `verifyNoHardcodedStrings` scans Compose sources for raw UI strings.

### Debug build

```bash
./gradlew assembleDebug
```

### CI

| Job | Runner | Commands |
|---|---|---|
| `verify` | GitHub Actions | `verifyNoHardcodedStrings`, `test`, `testDebugUnitTest`, `lintDebug`, `assembleDebug` |
| `signed-build` | GitHub Actions on tag/manual | `lintRelease`, tests, `assembleRelease`, `apksigner verify`, SHA-256, artifact upload |

Instrumented Compose tests exist for onboarding. VPN behavior is verified manually on real hardware.

## Release gates

A release is not published until:

1. `verifyNoHardcodedStrings`, `test`, `testDebugUnitTest`, `lintDebug`, `assembleRelease` pass.
2. `apksigner verify` passes and the APK is signed by the release certificate.
3. `SHA256SUMS` is generated and its Ed25519 signature verifies.
4. The physical phone checklist passes.
5. The physical Android TV checklist passes.

## Manual checklist

### Phone

| # | Step | Expected |
|---|---|---|
| 1 | Install the universal APK over the previous version | Installs with the same signing key |
| 2 | Accept the VPN prompt | Persistent “Network protection” notification |
| 3 | Enable **Local firewall** and **DNS Shield** together | Both stay enabled; internet works |
| 4 | Block one app, then open it | No internet for that app |
| 5 | Open a non-blocked app | Internet works |
| 6 | Open a known ad/tracker test domain | DNS Shield blocks it |
| 7 | Open **Protection statistics** | DNS block counter increases |
| 8 | Load an HTTPS page, download a file, upload a file | No stalls; TLS works |
| 9 | Reboot and re-open | VPN returns with the saved switches |
| 10 | Turn off both switches | VPN stops; internet returns |

### Android TV

| # | Step | Expected |
|---|---|---|
| 1 | Install with `adb install` | App appears in the Leanback launcher |
| 2 | D-pad through Network | Text above the first card and below the last card is reachable |
| 3 | Apps → app detail | Refresh and the bottom actions are reachable |
| 4 | Enable both protections | Internet works |
| 5 | Block one app | That app has no internet; others work |
| 6 | DNS test domain | Blocked; stats increment |
| 7 | Reboot | Both switches and the VPN return |

### Release artifacts

| Check | Command |
|---|---|
| Signature | `apksigner verify --verbose securebox-universal-release.apk` |
| Hashes | `sha256sum -c SHA256SUMS --ignore-missing` |
| Manifest signature | `verify-release.mjs` |
| Version | `aapt2 dump badging ... | Select-String versionName` |

## Verified hardware

See [docs/VERIFICATION.md](VERIFICATION.md) for the v0.9.5 phone and TV record.

For the remaining accessibility and v1.0.0 work, follow the step-by-step
[docs/QA_v1.0.0.md](docs/QA_v1.0.0.md) guide.

## Known gaps

- IPv6 forwarding is not implemented; the tunnel is IPv4-only.
- Long-duration/mobile-data handover has not been stress-tested.
- No automated VPN instrumentation test yet.
