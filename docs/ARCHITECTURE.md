# SecureBox Architecture

## Goals

1. **One codebase, two device families.** A touch-first phone shell and a D-pad-first TV shell share the same screens and state.
2. **On-device by default.** Scanning, traffic analysis, DNS filtering and firewall decisions happen locally.
3. **Clear boundaries.** Pure business rules stay in `core-domain`; Android/IO implementations live in `core-data`/`core-network`; screens depend on interfaces.
4. **No hidden network or telemetry.** Outbound calls are the GitHub update check, the optional signed feed, the selected DoH resolver, and the private browser.

## Layers

```
Presentation
  app: MainActivity, PhoneShell, TvShell, NavHost
  feature-*: Compose screens + ViewModels (StateFlow)

Domain (pure Kotlin)
  core-domain: models, business rules, repository interfaces

Data / platform
  core-data: DataStore, permissions, VPN service, forwarding engine, shield stats
  core-network: GitHub update client, download/verify/install, threat feed
```

Dependency rule: presentation depends on `core-domain` and `core-ui`; Android implementations are
bound by Hilt. Feature modules do not import VPN/native internals directly.

## Modules

| Module | Type | Responsibility |
|---|---|---|
| `core-domain` | Kotlin/JVM | Models, SemVer, SHA-256, security score, repository interfaces |
| `core-data` | Android library | Settings, device data, app registry, firewall repository, **combined VPN + forwarding engine**, shield stats |
| `core-network` | Android library | GitHub API, update download/verify/install, threat feed |
| `core-ui` | Android library | Theme, shared components, `FocusAnchor` |
| `core-tv-ui` | Android library | TV focus components and sidebar scaffold |
| `feature-onboarding` | Android library | Language/device/permissions wizard |
| `feature-dashboard` | Android library | Score and quick actions |
| `feature-network` | Android library | Bandwidth, firewall UI, ports |
| `feature-apps` | Android library | App scan/risk/permissions |
| `feature-threats` | Android library | Threats and hardening |
| `feature-threatintel` | Android library | Signed feed UI |
| `feature-settings` | Android library | Settings, Privacy Shield, DNS, statistics, help |
| `feature-update` | Android library | Update UI |
| `app` | Application | Wiring, shells, navigation |

## Combined VPN data plane

v0.9.5 replaced the two mutually exclusive VPN services with one all-app IPv4 VPN:

```
Apps
  │  IPv4 packets
  ▼
TUN (SecureBoxVpnService)
  │
  ├─ UDP:53 ──▶ DNS parser ──▶ local blocklist ──▶ DoH (protected)
  │                                  └─ plaintext fallback only if DoH fails
  ├─ UDP other ──▶ protected DatagramSocket ──▶ internet
  └─ TCP ──▶ TCP relay (SYN-ACK, sequence/ACK, retransmission handling)
                 └─ protected Socket (or Network.bindSocket fallback) ──▶ internet

Blocked app UID ──▶ TCP RST / UDP drop
```

Key points:

- **One VPN.** Android permits one; both protections share it.
- **UID blocking.** `ConnectivityManager.getConnectionOwnerUid` on Android 10+; `/proc/net` fallback on older builds.
- **Protected sockets.** OkHttp DoH and forwarded sockets use `VpnService.protect()`. When the OEM returns `false`, sockets are bound to a non-VPN network with `Network.bindSocket()`.
- **No TLS interception.** The relay forwards bytes; it does not MITM.
- **IPv4 only.** IPv6 forwarding is deferred; IPv6 is not routed into the tunnel in this release.
- **Stats.** DNS blocks are batched into DataStore; browser counts flush on close.

## Runtime flow (startup)

```
MainActivity
  └─ MainViewModel
       ├─ SettingsRepository.settings (DataStore) → state
       └─ PackageSignatureVerifier.verify()
  └─ SecureBoxRoot
       ├─ onboarding incomplete → OnboardingRoute
       ├─ deviceMode == TV → TvShell
       └─ else → PhoneShell
```

The firewall/DNS service starts from the Network or DNS screen, after `VpnService.prepare()`. It
observes both `FirewallRepository.state` and `SettingsRepository.settings`, establishes when either
is enabled, and stops when both are off.

## Privacy boundary

Allowed app traffic is in SecureBox’s data path while the VPN is on. It is relayed locally and never
sent to a SecureBox server, but this is a real change from the old “capture and discard only”
firewall. The UI, `PRIVACY.md`, `docs/PRIVACY_SHIELD.md` and the Play compliance notes state it.
