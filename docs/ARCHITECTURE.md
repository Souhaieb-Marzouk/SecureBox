# SecureBox Architecture

## Goals

1. **One codebase, two very different devices.** A phone held in one hand and a TV box three metres away need different layouts, focus models and text sizes — not a single stretched phone UI.
2. **Clean boundaries that survive six milestones.** Scanning, VPN and threat feeds are large features; they must slot into the existing layers without rewrites.
3. **Everything testable without a device.** Business rules live in a pure JVM module.
4. **No hidden network or telemetry.** Data flows are explicit and auditable.

## Layers

```
┌──────────────────────────────────────────────────────────────┐
│ Presentation (Compose)                                       │
│  app: MainActivity, PhoneShell, TvShell, NavHost            │
│  feature-*: screens + ViewModels (StateFlow)                 │
├──────────────────────────────────────────────────────────────┤
│ Domain (pure Kotlin, core-domain)                            │
│  models, SemVer, Sha256, UpdateEvaluator, SecurityScore      │
│  repository interfaces, platform interfaces                  │
├──────────────────────────────────────────────────────────────┤
│ Data (core-data, core-network)                               │
│  DataStore settings, device detection, permissions,          │
│  GitHub client, APK download/verify, installer, signature    │
└──────────────────────────────────────────────────────────────┘
```

Dependency rule: **presentation → domain ← data**. Features depend on `core-domain` and `core-ui`;
they never import `core-network` or `core-data` implementations. Hilt binds the interfaces at the
application graph.

## Modules

| Module | Type | Depends on | Responsibility |
|---|---|---|---|
| `core-domain` | Kotlin/JVM | coroutines only | Models, business rules, interfaces |
| `core-data` | Android library | domain | DataStore, permissions, signature check |
| `core-network` | Android library | domain | GitHub API, cache, download, verify, install |
| `core-ui` | Android library | domain | Theme, type, icons, shared components |
| `core-tv-ui` | Android library | core-ui | TV focus components, sidebar scaffold |
| `feature-onboarding` | Android library | domain, core-ui | 3-step wizard |
| `feature-dashboard` | Android library | domain, core-ui | Score + quick actions |
| `feature-network/apps/threats` | Android library | core-ui | Roadmap screens (v0.2–v0.3) |
| `feature-settings` | Android library | domain, core-ui, update, threatintel | Settings + privacy |
| `feature-update` | Android library | domain, core-ui | Update UI + ViewModel |
| `feature-threatintel` | Android library | domain, core-ui | Feature flag + trial badge (v0.5) |
| `app` | Application | everything | Wiring, shells, navigation |

Intentional feature-to-feature edges: `feature-settings → feature-update` and
`feature-settings → feature-threatintel`. These are composition points (the settings screen embeds
the update card and the trial badge), not shared business logic.

## Runtime flow

### Startup

```
MainActivity
  └─ MainViewModel
       ├─ SettingsRepository.settings (DataStore) ──▶ MainUiState
       └─ PackageSignatureVerifier.verify() on Dispatchers.IO
  └─ SecureBoxRoot
       ├─ !onboardingCompleted  ──▶ OnboardingRoute (shared M3 theme)
       ├─ deviceMode == TV      ──▶ TvSecureBoxTheme + TvShell
       └─ else                  ──▶ SecureBoxTheme + PhoneShell
```

`MainActivity` applies the persisted language with `AppCompatDelegate.setApplicationLocales`
inside a `LaunchedEffect`; the activity recreates but the ViewModel (and the onboarding step)
survives because it is retained across configuration changes.

### Update check and install

```
UpdateSection ─▶ UpdateViewModel
                   ├─ UpdateRepository.fetchLatestRelease(force, current)
                   │     ├─ cache hit (< 12h)  ─▶ CheckResult.Fetched(fromCache = true)
                   │     └─ GithubApiClient    ─▶ 200 / 403 / 404 / error
                   ├─ UpdateEvaluator.evaluate(current, release)
                   │     └─ SemVer compare + versionCode tie-break
                   └─ downloadAndInstall()
                         ├─ choose asset by Build.SUPPORTED_ABIS[0] → universal fallback
                         ├─ require SHA256SUMS (asset or release body)
                         ├─ stream to cacheDir/updates (200 MB cap, progress callback)
                         ├─ Sha256.of(file) vs published → mismatch deletes the file
                         └─ ApkInstaller → FileProvider URI → system installer
```

The repository is the trust boundary: the ViewModel never sees unchecked bytes, and
`DownloadResult.MissingChecksum` is a hard stop.

### Settings

`SettingsRepository` exposes a single `Flow<AppSettings>` from DataStore. Every screen observes the
same snapshot, so language, theme and device mode change atomically everywhere. Permission state is
recorded opportunistically (on onboarding completion and on resume), because Android's usage-access
and install-packages screens cannot return a result.

## Design decisions

### SemVer with Android versionCode

Release tags use `vMAJOR.MINOR.PATCH+BUILD`, e.g. `v0.1.0+1`. The build metadata becomes the
Android `versionCode`. Comparison is semantic first; when the version names are equal, the higher
`versionCode` wins. Tags that do not parse are never offered — the updater refuses to guess.

### Checksums are mandatory

A release without `SHA256SUMS` is not installable through the app. This is deliberate: a
"convenient" fallback to "trust the TLS connection" would silently weaken the only integrity control
the updater has.

### Localised strings, no hardcoded copy

All copy lives in `strings.xml` (EN) and `values-fr/strings.xml` (FR). A custom Gradle task,
`verifyNoHardcodedStrings`, fails the build when Compose receives a raw literal, and lint treats
`HardcodedText` as an error. CI runs both.

### Two shells, one graph

`SecureBoxNavHost` is shared. `PhoneShell` wraps it in a `Scaffold` + `NavigationBar`; `TvShell`
wraps it in `TvScaffold` (sidebar + focus-managed content). Screens do not know which shell hosts
them, which keeps every feature to a single implementation.

### TV focus is explicit

`TvActionCard` and `TvNavItem` read `collectIsFocusedAsState()` from their own `MutableInteractionSource`
and render a 4dp high-contrast border plus a tinted surface. Focus is never communicated by colour
alone.

### Hilt boundaries

`core-data` provides `SettingsRepository`, `DeviceModeDetector`, `PlatformPermissions` and
`PackageSignatureVerifier`. `core-network` provides `UpdateRepository` and `ApkInstaller`. The
application module provides `CalculateSecurityScoreUseCase`, `AppVersion` and the expected signing
certificate hash (computed at build time from the keystore). Domain classes are annotation-free so
the JVM module stays free of Dagger code generation.

## Threading

- DataStore and OkHttp work runs on `Dispatchers.IO`.
- ViewModels expose immutable `StateFlow<UiState>`; Compose collects with
  `collectAsStateWithLifecycle`.
- The download progress callback updates the `MutableStateFlow` directly from the IO thread —
  `MutableStateFlow` is safe for that and avoids a channel per 64 KB chunk.

## Error handling

Failures are modelled as data, not exceptions:

- `CheckResult` — `Fetched`, `RateLimited`, `NoRelease`, `Failed(reason)`.
- `DownloadResult` — `Verified`, `ChecksumMismatch`, `MissingChecksum`, `Failed(reason)`.
- `UpdatePhase.Failed(kind, detail)` in the ViewModel; the UI maps `kind` to a localised string.

Nothing user-visible is produced in the data layer; it has no access to resources by design.

## Extending for later milestones

- **v0.2 firewall**: add `core-vpn` (VpnService + JNI), a `FirewallRepository` interface in
  `core-domain`, and a `feature-network` ViewModel. The shells do not change.
- **v0.3 scanner**: add `core-scan` with the rule engine; findings become domain models rendered by
  `feature-apps`/`feature-threats`.
- **v0.5 threat intel**: `feature-threatintel` already exists and is isolated; add the feed client
  to `core-network` behind a `ThreatFeedRepository` interface and an Ed25519 verifier in
  `core-domain`.
- **Remote feature flags**: `ThreatIntelFlags` is the single switch; replace the constant with a
  remote-config-backed value when a backend exists.

## Testing strategy

- Domain rules are pure and unit-tested on the JVM (SemVer, evaluation, SHA-256, score).
- Compose screens accept plain state + callbacks, so they can be rendered in instrumented tests
  without Hilt.
- Repository interfaces make the updater testable with fakes; the HTTP client is the only untested
  boundary and is covered by the manual checklist.
