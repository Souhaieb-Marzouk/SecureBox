# Changelog

All notable changes to SecureBox are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned

- v0.2.0: per-app bandwidth, anomaly detector, local VPN firewall, open-port monitor.
- v0.3.0: on-device malware scanner, permission and tracker auditor, sideload checker.
- v0.4.0: RASP, dangerous-settings checker, scheduled and boot-time scans.
- v0.5.0: Threat Intelligence add-on with Ed25519-signed feeds.
- v1.0.0: localization review, accessibility pass, real-device sign-off.

## [0.3.0] - 2026-11-07

### Added

- On-device app security scan: signature rules, tracker SDKs, dangerous permission combinations, exported components, accessibility services, device admins and hidden launchers.
- Per-app risk score (0–100) with plain-language findings, All / Risky / Sideloaded filters and a detail dialog.
- Sideload detection with installer information.
- Weekly automatic scan and post-boot scan, both controlled by a user preference.
- Daily background update check with a notification when a new version is available.
- `docs/scanner-rules.json` (readable source) and an AES-256-GCM encrypted `scanner_rules.enc` asset.
- 15 unit tests for rule matching, permission heuristics and risk scoring (41 total).
- `docs/FIREWALL_EDGE_CASES.md`.

### Changed

- Permission state is refreshed every time the app returns to the foreground.
- Apps and Threats tabs now show real data instead of roadmap placeholders.

### Fixed

- Stale permission state after changing system settings.
- Update availability now reaches users without opening Settings.

## [0.2.1] - 2026-11-07

### Added

- Checksum-verification dialog and a real `UpdatePhase.Verifying` state in the updater.
- `tools/sandbox-build.ps1` and `tools/sandbox-build-root.gradle` for restricted build environments.
- `tools/device-qa.ps1` on-device smoke test helper.
- `docs/PLAY_COMPLIANCE.md` with the package-visibility declaration, VPN disclosure and data-safety answers.
- A real Blocked state in the backlog.

### Changed

- Removed unused dependencies and catalog entries (`mockk`, `kotlinx-coroutines-test`, `material-icons-extended`, `lifecycle-process`, `espresso-core`, unused app test dependencies).
- Backlog: six issues closed; the two v0.1 device checks folded into SB-059 (Blocked pending hardware).

### Fixed

- Dead `UpdatePhase.Verifying` replaced by a working verification step.
- `v0.1.0` and `v0.2.0` release titles use the specified em dash.
- Stale `SecureBox-dist` copy removed from the old workspace.
- CI now runs the `core-domain` JVM tests that `testDebugUnitTest` skipped.

## [0.2.0] - 2026-11-07

### Added

- Per-app bandwidth monitor: live rates from TrafficStats plus 24 h / 7 d / 30 d history from NetworkStatsManager, sorted by consumption with a per-row block switch.
- Upload anomaly detector: 7-day hourly baseline, 3× multiplier or 100 MB/h hard threshold, hourly WorkManager check, and notifications with Block and Uninstall actions.
- Local-only VPN firewall: per-app blocking, Allow all / Block unknown apps presets, persistent local-only notification, and detection of a conflicting VPN.
- Open port and inbound listener monitor with UID attribution and internet-facing flags.
- Complete Network screen for both phone and TV shells, with app icons, range filters and firewall controls.
- Six unit tests for the anomaly rules.

### Changed

- Security Score now reflects the real update status instead of assuming the app is current.
- Threat Intelligence is clearly marked as arriving in v0.5.0; its toggle is disabled until the signed feed exists.

### Security

- The VPN never forwards packets; blocked apps' traffic is discarded on-device.
- The keystore password is now DPAPI-encrypted and `signing.properties` was removed; `tools/build-release.ps1` injects it only into the build process.
- `QUERY_ALL_PACKAGES` added for per-app attribution; Play declaration tracked in the backlog.

## [0.1.0] - 2026-11-06

### Added

- Multi-module Kotlin/Compose project with Hilt, Coroutines/Flow, DataStore and a Gradle version catalog.
- Three-step onboarding wizard: language (English/Français), automatic device detection with manual override, and plain-language permission explainers with a final "You're protected" score screen.
- Two runtime UI shells sharing one navigation graph: `PhoneShell` (bottom navigation) and `TvShell` (D-pad-first sidebar, 4dp focus ring, 10-foot typography).
- Shared design system: dark-first navy/teal palette, 16dp rounded cards, custom line-icon set, inline "What is this?" explainers.
- Complete self-update system: GitHub Releases API client, 12-hour cache, semantic-version comparison with `+versionCode` support, SHA-256 verification against `SHA256SUMS`, FileProvider-scoped installer hand-off, rate-limit handling, Markdown release-notes rendering, download progress.
- Security score (0–100) with documented weighted factors and a circular gauge.
- APK self-integrity check against the build-time signing certificate.
- Settings: language, device mode with re-detection, theme, Threat Intelligence toggle ("Free Trial"), update check, privacy policy, about, GitHub/license links.
- Isolated `feature-threatintel` module with a local feature flag and trial badge, ready for the v0.5.0 signed feed.
- Documentation: `README.md`, `PRIVACY.md`, `SECURITY.md`, `BUILDING.md`, `TESTING.md`, `docs/ARCHITECTURE.md`.
- GitHub Actions: CI (tests, lint, hardcoded-string guard, debug APK) and a signed-build workflow (signed universal + ABI APKs, `SHA256SUMS`, uploaded as workflow artifacts). Releases are published manually.
- Unit tests for SemVer parsing, update evaluation, SHA-256 handling and the security score.

### Security

- No telemetry, no analytics, no crash-reporting servers.
- `allowBackup=false`, `usesCleartextTraffic=false`, FileProvider limited to `cacheDir/updates`.
- No Accessibility Service, no Device Admin, no SMS/call-log/contacts permissions.
- Signing keys are supplied only through encrypted GitHub Actions secrets.
