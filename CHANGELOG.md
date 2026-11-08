# Changelog

All notable changes to SecureBox are documented here.
The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned

- v1.0.0: TalkBack and 200% font pass, physical TV verification, and a decision on forwarding DNS for domain blocking.

## [0.7.0] - 2026-11-08

### Added

- Privacy Shield hub with Standard/High tracking protection.
- Private browser with ad/tracker host blocking, cosmetic filtering, tracking-parameter stripping and cookie clearing.
- DNS protection presets for AdGuard, Cloudflare for Families and Quad9, with Private DNS status, hostname copy and DoH test.
- Local SHA-256 hash-prefix Safe Browsing for navigations and subresources.
- Dashboard and Settings entries for Privacy Shield.

### Changed

- AppSettings persists tracking mode, ad/tracker blocking, annoyance hiding, Safe Browsing and DNS preset.

### Limited

- YouTube in-stream ads and HTTPS filtering inside third-party apps are not possible without root/MITM; documented in-app and in the release notes.

## [0.6.2] - 2026-11-08

### Changed

- Apps tab now lists only currently granted permissions, including effective AppOps/accessibility/notification/admin capabilities.
- App score and Criticality are based only on granted permissions; risk signals are context only.
- Each app has a recommendation: leave alone, reduce permissions, consider uninstall, or no action.
- Apps are classified as built-in, Play Store, other store or manual APK, with badges and filters.
- Returning to Apps auto-refreshes; Refresh also appears in the detail sheet.

## [0.6.1] - 2026-11-08

### Fixed

- Dark-theme text outside cards no longer falls back to black; the theme provides `LocalContentColor`/`LocalTextStyle`.
- French and English navigation, filters, ranges, settings rows and update button fit narrow phones; chips scroll horizontally.
- Android TV Usage access action now shows the manual path and opens a working generic Settings screen.

## [0.6.0] - 2026-11-07

### Added

- Aurora design system: deep-ink gradient backdrop, glass cards, gradient accents, refreshed type scale and gradient score ring.
- New 2.5D shield mark, adaptive launcher icon, Android TV banner and 512 px store icon.
- Floating glass bottom navigation on phones and a cinematic sidebar on Android TV.
- Redesigned dashboard with a score hero and four plain-language actions.
- “How to use SecureBox” guide with four steps and a plain-language glossary, linked from the dashboard and Settings.
- TV-aware dashboard sizing for 10-foot readability.

### Changed

- Phone and TV shells now share the Aurora backdrop and brand mark.
- Onboarding uses the new mark and backdrop.
- Screen headings gained a gradient accent bar.

### Fixed

- Carried forward the v0.5.2 Apps, Network Live and TV integrity fixes.

## [0.5.2] - 2026-11-07

### Added

- Apps tab explainer, risk legend, attention summary, two-line risk badges, plain-language rating meanings.
- Sensitive-first permission list with human-readable labels and system-settings actions (app settings, uninstall, accessibility, overlay, usage, device admin).
- Network explainers for Open ports and Upload anomalies; ports Rescan action.

### Fixed

- Live bandwidth showed only SecureBox because `TrafficStats` hides other UIDs; it now uses `NetworkStatsManager` for every app, including Ethernet.
- Apps scores like “0 Critical” and permission counts are now explained and actionable.
- Rooted/test-key TV boxes no longer show a tampered-build warning; only signature mismatch locks updates.
- Installed version now comes from `PackageManager` and shows the build number.

## [0.5.1] - 2026-11-07

### Added

- Signed threat feed (Ed25519), starter `threat-intel/latest.json`, signing CI workflow and verification tools.
- Threat Intelligence screen with feed status, Update now and daily auto-update; verified rules and trackers merge into the scanner.
- Detached Ed25519 signature for release checksums (`SHA256SUMS.sig`, `tools/verify-release.mjs`).
- x86_64 ABI split.
- Clickable links and fenced code blocks in release notes.
- Local-only paid-tier placeholder behind `FeatureFlags.PAID_TIER_AVAILABLE`.
- `docs/THREAT_FEED.md`, `docs/PERFORMANCE.md`; 3 Ed25519 tests (44 total).

### Fixed

- Stale APKs are deleted from `cacheDir/updates` before a new download.
- French review closed: VPN wording, GitHub quota, gender-neutral completion title, stale roadmap copy.
- Firewall now captures IPv6 as well as IPv4, closing a bypass for blocked apps.
- Update installer uses the runtime package name for its FileProvider authority, fixing debug builds.
- Feed downloads are capped at 2 MB and empty/zero-version feeds are rejected; tunnel rebuilds are serialized.
- Daily update and feed workers require network connectivity.
- Explicit `dataExtractionRules`/`fullBackupContent` no-backup rules.
- Monochrome launcher icon, French `many` plurals, plural notifications, banner densities, unused colours removed.
- Dashboard scores the firewall only when the tunnel is actually running.
- CI release workflow ships x86_64, verifies the signed feed and optionally signs checksums; `.gitattributes` keeps feed bytes LF.
- BUILDING, SECURITY and TESTING docs updated for NDK/CMake, supported versions and version-independent QA.

### Known limits

- Feed `domains` are not enforced by the firewall (no forwarding DNS); tracked as blocked in SB-041.
- TalkBack/200% font pass and TV QA remain open.

## [0.4.1] - 2026-11-07

### Fixed

- SB-061: choosing "Android TV box" on a phone produced a 280dp sidebar that hid the content and blocked access to the setting needed to switch back. The TV sidebar now collapses to a 96dp icon rail on narrow windows, and a "Use phone layout" escape button appears when the TV shell runs on non-TV hardware.

### Changed

- Version 0.4.1 (code 6). Do not publish 0.4.0.

## [0.4.0] - 2026-11-07

### Added

- RASP checks: debugger, emulator, root indicators and APK signature mismatch, with a Device integrity card in Settings.
- Update installation is locked when the build looks tampered (debugger/root/signature mismatch).
- Device hardening screen: developer options, USB debugging, accessibility, device admins, user CA certificates and install-unknown-apps, each with a Fix shortcut.
- Adaptive phone shell: navigation rail on tablets and foldables via WindowSizeClass.
- TV sidebar now requests initial D-pad focus.
- Pull-to-refresh on the app list and a bottom sheet for app details.
- Instrumented Compose tests for the Apps and Threats screens.
- CI emulator matrix: phone (API 34) and Android TV (API 33) instrumented test jobs.
- Native NDK packet pump for the firewall, with the Kotlin reader as a fallback.

### Changed

- Version 0.4.0 (code 5); CI installs NDK 27.0.12077973.

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
