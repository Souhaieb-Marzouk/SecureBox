# SecureBox

**SecureBox** is a privacy-first security app for **Android phones, tablets and Android TV boxes**. It gives you plain-language, on-device visibility into what apps are doing on your network, what they are allowed to do, and whether anything looks dangerous — with **zero telemetry** and no cloud account.

> One purchase covers one phone **and** one TV box. The app is free and open source (Apache 2.0); the Threat Intelligence add-on is free during the v0.5 trial.

| | |
|---|---|
| **Current release** | `v0.8.0` — DNS Shield & Protection Stats |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Languages** | English, Français |
| **Devices** | Phone / tablet (`LAUNCHER`) and Android TV (`LEANBACK_LAUNCHER`) |
| **Network** | Only the GitHub update check and the optional signed threat feed |

---

## Why another security app?

Most "phone cleaners" and "security boosters" are advertising vehicles: they upload app lists, ship analytics SDKs, and show scary numbers that mean nothing. SecureBox takes the opposite approach:

- **Everything is computed on the device.** Scanning, traffic analysis and threat matching never leave the phone or TV box.
- **The only outbound calls are pull-only.** A GitHub release check, and (from v0.5) the signed Threat Intelligence feed.
- **Every number is explained.** Each screen has a one-sentence "what this does", and every technical term has an inline "What is this?" chip.
- **One codebase, two shells.** A touch-first bottom-navigation shell for phones and a D-pad-first 10-foot shell for TV boxes, selected automatically and overridable at any time.

## What works today (v0.4.0)

- **First-run onboarding**: language selection (EN/FR), automatic TV/phone detection with manual override, and plain-language permission explainers.
- **Two UI shells**: `PhoneShell` (bottom bar on phones, navigation rail on tablets) and `TvShell` (sidebar + large focus rings), sharing one navigation graph.
- **Device integrity (RASP)**: debugger, emulator, root and signature checks, with updates locked on a tampered build.
- **Device hardening**: one screen for developer options, USB debugging, accessibility, admins, user certificates and install-unknown-apps, each with a Fix shortcut.
- **App security scan**: signature rules, tracker SDKs, dangerous permission combos, exported components, accessibility services, device admins and hidden launchers — all on-device.
- **Per-app risk score**: 0–100 with plain-language reasons, All / Risky / Sideloaded filters, and a bottom-sheet detail view.
- **Sideload awareness**: Play Store / System / Sideloaded badges with installer details.
- **Automatic scans**: weekly and post-boot scans, controlled by a user preference, plus a notification when risky apps are found.
- **Signed threat feed**: optional Ed25519-verified rules and tracker updates, manual or daily, with a status screen and offline-safe failure.
- **Per-app bandwidth**: live rates plus 24 h / 7 d / 30 d history, sorted by consumption, with a block switch on every row.
- **Upload anomaly detector**: 7-day hourly baseline, 3× / 100 MB per hour thresholds, hourly checks and actionable notifications.
- **Local-only firewall**: block any app's internet with a `VpnService`, Allow all / Block unknown apps presets, persistent local-only notification and conflicting-VPN detection.
- **Open port monitor**: listening TCP/UDP sockets attributed to apps, with internet-facing flags and an honest "unavailable" state when Android restricts `/proc/net`.
- **Self-update system**: GitHub Releases API, 12-hour cache, semantic version comparison, SHA-256 verification against the published `SHA256SUMS`, FileProvider hand-off to the system installer, and graceful handling of GitHub rate limits.
- **Security score**: a transparent 0–100 score with weighted, documented factors, now aware of available updates.
- **APK self-integrity check**: the app verifies its own signing certificate at startup.
- **Settings**: language, device mode (with re-detection), theme, Threat Intelligence (signed feed with manual and daily updates), update check, privacy policy, about.
- **Docs & policies**: `PRIVACY.md`, `SECURITY.md`, `BUILDING.md`, `TESTING.md`.
- **CI/CD**: GitHub Actions builds, tests, lints, signs and hashes APKs on tag push and uploads them as workflow artifacts; the GitHub Release itself is published manually.

## Roadmap

| Milestone | Version | Scope |
|---|---|---|
| 1 | **v0.1.0** | Foundation, onboarding, two shells, complete self-update system |
| 2 | v0.2.0 | Per-app bandwidth, anomaly detection, local VPN firewall, open-port monitor |
| 3 | v0.3.0 | Malware scanner, permission & tracker auditor, sideload checker, component detector |
| 4 | v0.4.0 | RASP, dangerous-settings checker, hardening wizards, scheduled scans |
| 5 | **v0.5.1** | Threat Intelligence add-on: signed feed, Ed25519 verification, free trial |
| 6 | v1.0.0 | Localization review, accessibility/TalkBack pass, performance, real-device QA |

The live, prioritized work list is kept in **[docs/backlog.html](docs/backlog.html)** (P0–P3, features and open issues). It is updated in the same commit as every change.

## Install

1. Open the [Releases page](https://github.com/Souhaieb-Marzouk/SecureBox/releases).
2. Download `securebox-universal-release.apk` (works on every device) or the ABI-specific APK for a smaller download.
3. Verify the download if you want to be certain:

```bash
sha256sum -c SHA256SUMS --ignore-missing
```

4. Allow installation from the browser/file manager when Android asks. SecureBox never installs anything silently; every update opens the standard installer dialog.

> **TV boxes:** sideload with `adb install securebox-universal-release.apk`, or use a USB stick and a file manager. The app registers a `LEANBACK_LAUNCHER` entry so it appears on the TV home screen.

## Build from source

See **[BUILDING.md](BUILDING.md)**. Quick start:

```bash
git clone https://github.com/Souhaieb-Marzouk/SecureBox.git
cd SecureBox
./gradlew assembleDebug
```

A signed release build requires a keystore and four secrets — never committed to the repository:

| Variable | Purpose |
|---|---|
| `SECUREBOX_KEYSTORE_PATH` | Path to the `.jks` / `.p12` keystore |
| `SECUREBOX_KEYSTORE_PASSWORD` | Keystore password |
| `SECUREBOX_KEY_ALIAS` | Key alias (`securebox`) |
| `SECUREBOX_KEY_PASSWORD` | Key password |

## Architecture

SecureBox uses **MVVM + Clean Architecture** across a Gradle multi-module project:

```
app/                  Wires everything: Hilt, navigation, the two shells
core-domain/          Pure Kotlin: models, SemVer, SHA-256, security score, repository interfaces
core-data/            DataStore settings, device detection, runtime permissions, signature check
core-network/         GitHub API client, release parsing, cached update checks, APK downloader + installer
core-ui/              Design system: theme, typography, custom icons, shared components
core-tv-ui/           TV-only components: focus rings, sidebar scaffold, 10-foot type scale
feature-onboarding/   Three-step first-run wizard
feature-dashboard/    Security score and quick actions
feature-network/      Network: bandwidth, anomaly alerts, VPN firewall, port monitor
feature-apps/         App risk auditing, permissions, trackers and sideload detection
feature-threats/      On-device scanning, results and scheduled scans
feature-settings/     Settings and privacy policy
feature-threatintel/  Signed threat feed, status and daily updates
feature-update/       Update UI, ViewModel and Markdown release notes
```

Details and dependency rules: **[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md)**.

## Security & privacy

- No analytics, no ad SDKs, no crash-reporting servers.
- All scanning and traffic analysis is local.
- Downloads are installed only after their SHA-256 matches the published checksum; a mismatch deletes the file and raises an alert.
- The APK verifies its own signing certificate at startup; a re-signed build shows a warning banner.
- The FileProvider is scoped to `cacheDir/updates` and nothing else.
- No Accessibility Service or Device Admin usage in v1.

Read **[PRIVACY.md](PRIVACY.md)** and **[SECURITY.md](SECURITY.md)**.

## Contributing

Issues and pull requests are welcome. Every commit follows Conventional Commits, and every pull request must pass `./gradlew verifyNoHardcodedStrings testDebugUnitTest lintDebug`. User-facing strings must be added to both `values/strings.xml` and `values-fr/strings.xml`.

## License

Apache License 2.0 — see [LICENSE](LICENSE).

SecureBox is an independent project. It is not affiliated with Google, Android TV, or GitHub.
