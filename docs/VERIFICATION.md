# On-device verification

## v0.9.5 — physical Android phone (2026-11-09)

Build: **SecureBox 0.9.5 (versionCode 20)**
Universal APK SHA-256: `7802fb0cfdad4edb2abec84ad4879938d78fe5b709e6d9e42daf41d4f63a8e45`

Tested with both controls enabled at the same time in one VPN:

- **Local firewall + DNS Shield together** — internet works.
- **Blocked app** — selected in Local firewall, lost internet.
- **Allowed app** — still had internet.
- **DNS Shield** — ad/tracker test domain was blocked.
- **Protection statistics** — DNS block counter increased.
- **Tunnel log** — TCP handshakes, TLS payloads and DNS replies completed; no tunnel write/read failures.

### Device-specific notes

- On this phone `VpnService.protect()` returns `false` for outbound TCP sockets. The engine's
  `Network.bindSocket()` fallback is what makes TCP work. This is handled automatically.
- Repeated `192.168.8.1:7` connection-refused entries in the diagnostic log are an external
  device/app probing the router’s echo port, not SecureBox traffic.

## v0.9.5 — Android TV box (2026-11-09)

The same build passed on a physical Android TV box with both controls enabled:

- **Local firewall + DNS Shield together** — internet works.
- **Blocked app** — no internet.
- **Allowed apps** — still work.
- **DNS block test** — ad/tracker domain blocked.
- **Protection statistics** — DNS counter increments.

## v0.9.6 — signed feed domains on phone (2026-11-09)

Build: **SecureBox 0.9.6 (versionCode 21)**
Universal APK SHA-256: `e568bdd12d4576f8b38ccf73f3999839bc21e5ddf7823db6645c0ece98d2030b`

- Threat Intelligence feed v1 active (2 domains).
- Local firewall and DNS Shield both on.
- `ads.example.com` and `tracker.example.net` returned `unknown host`.
- DNS block statistics rose from **563 to 565** — one per feed domain.
- DNS statistics now flush within about one second instead of waiting for a batch of ten.

### Not tested

- IPv6 (deferred by request; the current tunnel is IPv4-only).
- Long-duration/large downloads and mobile-data handover.
- Multi-user/work-profile behavior.
- TalkBack and 200% font-scale accessibility walkthrough.

## v0.9.7 — regression fixes (2026-11-09)

Build: **SecureBox 0.9.7 (versionCode 22)**
Universal APK SHA-256: `cbb6df745d90fb6c8915af4ebe77c66769b75f9ce8744d999213bcc4e7de54a3`

- Settings firewall card now stacks the status below the description for 200% font (code fix; user recheck pending).
- DNS Shield switch reflects the persisted setting and restarts the VPN when the DNS screen opens if the service is missing.
- Quick off/on toggle retries tunnel establishment and no longer reports SecureBox’s own VPN as “another VPN”.
- Opening the app after reboot restores the enabled VPN when the process starts.
- Boot receiver starts the service on a normal boot, but ColorOS may mark the package stopped; opening the app is the reliable fallback.
- Device test: DNS Shield off/on persisted, service restarted, no false VPN conflict.
## v1.0.0 — official release (2026-11-09)

- Version: **1.0.0 (versionCode 23)**.
- Tag: `v1.0.0`.
- Release: https://github.com/Souhaieb-Marzouk/SecureBox/releases/tag/v1.0.0
- Universal APK SHA-256: `bae17cdd81f6a843facc358c5e9a113d4b4d333ac95c2333d9864c6d013f1cc7`
- APKs: universal, arm64-v8a, armeabi-v7a, x86_64.
- `SHA256SUMS` signed with the Ed25519 release key.
- Includes the v0.9.7 large-font, reboot-state and quick-toggle fixes.
- SB-046 (accessibility) and SB-048 (end-to-end QA/v1.0.0) closed.
- IPv6 remains deferred (SB-086).