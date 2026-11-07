# Testing SecureBox

This document covers automated tests, how to run them, and the manual checklist that must pass on real hardware before a release is announced.

## Automated tests

### Unit tests (JVM)

```bash
./gradlew testDebugUnitTest test
```

| Area | Test class | What it proves |
|---|---|---|
| SemVer parsing | `core-domain/.../SemVerParserTest` | Tags like `v2.1.0+21`, `1.2`, pre-release suffixes, rejection of garbage |
| Version comparison | `core-domain/.../UpdateEvaluatorTest` | Only genuinely newer releases are offered; versionCode tie-breaks; non-semver tags are refused |
| SHA-256 | `core-domain/.../Sha256Test` | NIST vector, streaming vs in-memory equality, case-insensitive constant-time compare, `SHA256SUMS` parsing |
| Security score | `core-domain/.../SecurityScoreTest` | Weighted factors, bounds, grades |

### UI tests (instrumented, emulator/device)

```bash
./gradlew connectedDebugAndroidTest
```

The Compose test in `feature-onboarding` renders the real `OnboardingScreen` and asserts:

- both language cards are present and selectable;
- the detected device text is shown;
- the permission cards appear with their "Allow" action.

### Lint and the hardcoded-string guard

```bash
./gradlew lintDebug verifyNoHardcodedStrings
```

- `HardcodedText` is configured as a **lint error**.
- `verifyNoHardcodedStrings` scans Compose sources for raw `text = "â€¦"` / `title = "â€¦"` literals and fails the build.

### CI matrix

| Job | Device | Command |
|---|---|---|
| `verify` | GitHub runner | `verifyNoHardcodedStrings`, `testDebugUnitTest`, `test`, `lintDebug`, `assembleDebug` |
| `signed-build` | GitHub runner (tag push or manual) | `lintRelease`, `testDebugUnitTest`, `test`, `assembleRelease`, `apksigner verify`, SHA-256, artifact upload (no release creation) |

Instrumented tests run on a dedicated emulator matrix once Milestone 2 introduces VPN code (a phone AVD and a Leanback AVD, API 33+).

## Manual test checklist

Record the result for both a **physical phone** and a **physical Android TV box**. A release is not announced until every row is âœ… on both devices.

### Installation

| # | Step | Expected |
|---|---|---|
| 1 | Install `securebox-universal-release.apk` via `adb install` | Installs, launcher icon appears |
| 2 | Open the app on a TV box | App appears in the Leanback launcher rows |
| 3 | Check `apksigner verify --print-certs` output | Certificate matches the documented release certificate |

### Onboarding

| # | Step | Expected |
|---|---|---|
| 4 | Fresh install, open the app | Language step appears with English and FranÃ§ais |
| 5 | Choose FranÃ§ais | UI switches immediately, no restart |
| 6 | Continue | Device step shows the detected form factor |
| 7 | Answer "No" to flip the mode | Layout preference is stored; you can verify it in Settings |
| 8 | Grant usage access | Returning to the app shows "Allowed" without a manual refresh |
| 9 | Grant VPN consent | System VPN dialog appears; the card turns to "Allowed" on return |
| 10 | Deny notifications | "You're protected" screen still appears; score reflects the missing factor |
| 11 | Tap "Open SecureBox" | Dashboard opens in the chosen shell |
| 12 | Force-stop and relaunch | Onboarding does **not** reappear; language and device mode persist |

### TV shell (D-pad only, no touch)

| # | Step | Expected |
|---|---|---|
| 13 | Navigate with the D-pad | Focus ring is always visible (4dp, high contrast) |
| 14 | Open every sidebar entry | All five screens render without clipping at 1080p and 720p |
| 15 | Open the privacy policy | Body text is at least 18sp and scrollable with the D-pad |
| 16 | Trigger an update dialog | Primary action receives focus first; Back dismisses |

### Phone shell

| # | Step | Expected |
|---|---|---|
| 17 | Swipe/scroll every tab | No content hidden behind the bottom navigation |
| 18 | Rotate the device | State survives; layout adapts |
| 19 | Change theme to Light, then System | Applies immediately and persists after restart |

### Network / firewall (physical device)

The firewall only exists on real hardware: emulators do not exercise the real `VpnService`, the
system VPN dialog, or per-app routing. Run these after the phone/TV shell checks.

| # | Step | Expected |
|---|---|---|
| N1 | Grant VPN consent, block a browser or test app, then open a website in it | Page fails to load; unblock, reload, page loads |
| N2 | Check the status bar while the firewall runs | VPN key icon is present; SecureBox shows "Protecting N apps" |
| N3 | Repeat N1 on Wi-Fi and on mobile data | The blocked app fails on both networks |
| N4 | With the firewall active, connect another VPN app | Network shows the conflict banner; SecureBox does not fight it or crash |
| N5 | Revoke usage access in system settings, return to Network | Permission card appears; granting it repopulates the list on return |
| N6 | Block one app and leave the firewall on for 10 minutes | No crash or ANR, reasonable battery use, tunnel survives screen-off |

### Recording results

Copy `docs/device-qa/v0.4.1-results.md`, fill in every row for a phone and a TV box, and paste the
`tools/device-qa.ps1` output for each. Link the file from SB-059 in `docs/backlog.html`. SB-059 stays
blocked until both devices pass.

### Update system

| # | Step | Expected |
|---|---|---|
| 20 | Settings â†’ Check for updates (no newer release) | "You are on the latest version." |
| 21 | Publish a newer test release, check again | Dialog shows version, size and Markdown notes |
| 22 | Download the update | Progress runs; file lands in `cacheDir/updates` |
| 23 | Corrupt the checksum (test build), retry | Alert: checksum mismatch, file deleted, nothing installed |
| 24 | Install a valid update | System installer opens; app upgrades; version changes |
| 25 | Deny "install unknown apps", retry | Dialog offers "Open settings"; returning retries the install |
| 26 | Disable network, check for updates | Friendly network error, no crash |
| 27 | Exhaust the GitHub anonymous quota (or mock 403) | "GitHub rate limit reached. Try again later." |

### Integrity and security

| # | Step | Expected |
|---|---|---|
| 28 | Re-sign the APK with a different key, install on a clean device | Warning banner on every shell; app still opens |
| 29 | Inspect with `adb shell dumpsys package io.securebox` | No accessibility service, no device admin, no SMS/call/contact permissions |
| 30 | Watch traffic with a proxy while using the app | Only `api.github.com` requests during update checks |

### Localization

| # | Step | Expected |
|---|---|---|
| 31 | Switch to French and visit every screen | No English leftovers, no truncated labels |
| 32 | Set the system language to French on a fresh install | Onboarding still defaults to English (explicit choice), switch works |
| 33 | Enable TalkBack and walk the dashboard | Score, cards and buttons are announced with meaningful labels |

