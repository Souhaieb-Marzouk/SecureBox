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

### Not tested

- IPv6 (deferred by request; the current tunnel is IPv4-only).
- Android TV box with both controls.
- Long-duration/large downloads and mobile-data handover.
