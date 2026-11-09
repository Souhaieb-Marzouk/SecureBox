# SecureBox

**SecureBox** is a privacy-first security app for **Android phones, tablets and Android TV boxes**. It gives you plain-language, on-device visibility into what apps are doing on your network, what they are allowed to do, and whether anything looks dangerous — with **zero telemetry** and no cloud account.

| | |
|---|---|
| **Current release** | `v1.0.1` — Complete firewall + DNS protection |
| **Verified on** | Physical Android phone and Android TV box |
| **Min SDK** | 26 (Android 8.0) |
| **Target SDK** | 34 (Android 14) |
| **Languages** | English, Français |
| **Devices** | Phone/tablet (`LAUNCHER`) and Android TV (`LEANBACK_LAUNCHER`) |
| **License** | Apache 2.0 |

## What it does

### One VPN, two protections

Older versions had a local firewall **or** DNS protection because Android allows one VPN. v0.9.5 combines them in one on-device forwarding engine:

- **DNS protection** — queries are checked against the local ad/tracker list and the active signed threat-feed domains, then forwarded over DNS-over-HTTPS to AdGuard, Cloudflare for Families or Quad9.
- **Local firewall** — selected apps are matched by UID; their TCP connections are reset and their UDP packets are dropped.
- **Both can run at the same time.** The service stays alive while either control is enabled.

Allowed traffic is relayed on-device through `VpnService.protect()` sockets (with an underlying-network binding fallback for OEM builds where `protect()` returns `false`). It is never sent to a SecureBox server, is not stored, and TLS is not intercepted.

### Protection statistics

**Privacy shield → Protection statistics** shows:

- Ads blocked
- Trackers blocked
- Pages cleaned of cookie/popup clutter
- Dangerous links blocked
- DNS queries blocked locally

Counters are stored in the app's DataStore and can be reset.

### Android TV

Every scroll container has focusable top/bottom anchors, and cards/banners/headings are focus stops, so a D-pad can reach text-only sections. Verified on a physical TV box.

### Private browser

Blocks ad/tracker hosts, hides common annoyances, removes tracking parameters in High mode, and checks navigations/subresources against a local SHA-256 prefix list. No browsing history is stored or uploaded.

### Device security

- RASP checks (debugger, emulator, root, signature)
- App security scan and per-app risk score
- Sideload awareness
- Dangerous-settings hardening
- Per-app bandwidth and upload anomaly detection
- Open-port monitor (honest “unavailable” state when `/proc/net` is restricted)
- Signed, optional Threat Intelligence feed
- Signed GitHub self-update with SHA-256 verification before installing

## Network privacy

SecureBox has **no analytics, no advertising SDKs and no crash-reporting servers**.

Outbound traffic is limited to:

1. The GitHub release check.
2. The optional signed Threat Intelligence feed.
3. The DNS-over-HTTPS provider you select, when DNS protection is enabled.
4. Pages you open in the private browser.

The VPN is local. There is no SecureBox VPN server.

## Install

1. Open the [Releases page](https://github.com/Souhaieb-Marzouk/SecureBox/releases).
2. Download `securebox-universal-release.apk`, or the smaller ABI-specific APK.
3. Verify it if you want:

```bash
sha256sum -c SHA256SUMS --ignore-missing
```

4. Accept the Android VPN prompt when enabling the firewall/DNS protection.

TV boxes: sideload with `adb install securebox-universal-release.apk`, or use a USB stick and a file manager.

## Build from source

Requirements: **JDK 17 or 21**, Android SDK Platform 34 and Build-Tools 34.0.0, Gradle 8.9 (wrapper included). The unused native packet pump has been removed from the source tree, so the NDK and CMake are no longer required.

```bash
git clone https://github.com/Souhaieb-Marzouk/SecureBox.git
cd SecureBox
./gradlew assembleDebug
```

Signed-release instructions are in **[BUILDING.md](BUILDING.md)**.

## Documentation

| Document | Contents |
|---|---|
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Modules, layers and the combined VPN data plane |
| [docs/PRIVACY_SHIELD.md](docs/PRIVACY_SHIELD.md) | Browser, DNS provider, firewall and privacy model |
| [docs/FIREWALL_EDGE_CASES.md](docs/FIREWALL_EDGE_CASES.md) | VPN conflicts, Private DNS and per-app edge cases |
| [docs/VERIFICATION.md](docs/VERIFICATION.md) | Physical phone and TV verification record |
| [docs/backlog.html](docs/backlog.html) | Prioritized work list |
| [PRIVACY.md](PRIVACY.md) | Privacy policy |
| [SECURITY.md](SECURITY.md) | Security policy and supported versions |
| [TESTING.md](TESTING.md) | Automated and manual test checklist |
| [CHANGELOG.md](CHANGELOG.md) | Release history |

## Known limits

- **IPv4-only tunnel.** IPv6 forwarding is deferred; IPv6 traffic is not filtered in this build.
- Per-app blocking depends on UID attribution. If the UID cannot be resolved, the flow is allowed rather than breaking an unknown app.
- Apps using their own DoH/DoT resolver can bypass DNS filtering.
- HTTPS inside other apps is not intercepted; no root or MITM.
- YouTube in-stream ads cannot be removed by DNS or the embedded browser.
- Some OEM builds return `false` from `VpnService.protect()`; SecureBox binds those sockets to a non-VPN network and continues.

## Roadmap

| Milestone | Status |
|---|---|
| Combined firewall + DNS forwarding | **Done — v0.9.5, verified phone + TV** |
| Android TV focus/scroll | **Done** |
| Protection statistics | **Done** |
| IPv6 forwarding | Deferred by request |
| Long-duration/mobile-data handover QA | Planned |
| Localization/accessibility pass | Planned |

## License

Apache License 2.0. See [LICENSE](LICENSE).
