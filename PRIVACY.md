# SecureBox Privacy Policy

**Last updated: 2026-11-09**

SecureBox is designed so that the honest answer to "what do you collect?" is **nothing**.

## Summary

- SecureBox has **no telemetry**, no analytics, no advertising SDKs and no crash-reporting servers.
- Your app list, traffic statistics, scan results, DNS counters and settings **never leave your device**.
- Network requests are limited to the GitHub update check, the optional signed Threat Intelligence feed, the DNS-over-HTTPS provider you select, and sites you open in the private browser.
- All requests are **pull-only**. SecureBox never uploads data about you.

## The combined VPN

The Local firewall and DNS protection share one on-device `VpnService`:

- Allowed app traffic is **relayed on the device** through protected sockets.
- It goes directly from the device to the destination; it is **not sent to a SecureBox server**.
- It is **not stored**, and **TLS is not intercepted**.
- Selected apps are dropped/reset; DNS queries are filtered and sent to the chosen DoH provider.
- The plaintext DNS fallback (`1.1.1.1:53`) is used only if DoH fails.
- There is no SecureBox VPN server.

This means SecureBox is in the data path for allowed apps while the VPN is enabled. That is a local
privacy trade-off, not data collection.

## What SecureBox stores on the device

| Data | Where it lives | Why |
|---|---|---|
| Language, theme, device mode, firewall/DNS preferences | App-private DataStore | Remember settings |
| Protection statistics (ads, trackers, pages, malicious, DNS blocks) | App-private DataStore | Show the statistics page |
| Cached latest-release metadata and last-check time | App-private DataStore | Avoid GitHub rate limits |
| Downloaded update packages | App cache (`cacheDir/updates`) | Handed to the system installer, then deleted |
| Traffic statistics and scan results | Device memory / local database | Show bandwidth and findings |
| Browser settings and Safe Browsing prefix list | App-private storage | Filter locally; no history is stored |

Uninstalling SecureBox removes this data. Nothing above is uploaded.

## Network requests

1. **Update check** — HTTPS `GET` to `https://api.github.com/repos/Souhaieb-Marzouk/SecureBox/releases/latest`. GitHub sees your IP address and User-Agent, as with any HTTPS request. No account or identifier is sent. Results are cached for 12 hours.
2. **Threat Intelligence feed (opt-in)** — HTTPS `GET` for a signed JSON file; the Ed25519 signature is verified locally. No query parameters identify you.
3. **DNS-over-HTTPS** — when DNS protection is on, DNS queries are sent to the provider you select: AdGuard, Cloudflare for Families or Quad9. The provider sees the query and your IP, as with any DNS resolver. The local blocklist is applied before forwarding.
4. **Private browser** — only sites you choose to open, plus their normal subresources.

No other network connections are made by SecureBox itself.

## Permissions

| Permission | Why | If you deny it |
|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Update check, feed, DoH, browser | Those features are unavailable |
| VPN consent | Combined firewall + DNS | Protection unavailable; the rest works |
| `PACKAGE_USAGE_STATS` (special access) | Per-app bandwidth | Bandwidth features show an explanation |
| `POST_NOTIFICATIONS` | Security and VPN status alerts | Alerts appear in-app only |
| `REQUEST_INSTALL_PACKAGES` | Install a verified update | Install releases manually |
| `QUERY_ALL_PACKAGES` | Attribute traffic/sockets and block per app | App names and per-app firewall unavailable |

SecureBox **never** requests SMS, call-log or contacts permissions. It can audit apps that hold them,
but it does not read your messages or calls.

### Package visibility (`QUERY_ALL_PACKAGES`)

SecureBox lists installed applications so it can attribute network traffic and listening sockets to
the correct app and offer per-app internet blocking. The list is processed only on the device, is
never uploaded, and is not used for advertising, analytics or profiling.

## Children

SecureBox does not knowingly collect data from anyone, including children.

## Changes

Material changes ship with a new release and are listed in `CHANGELOG.md`. Because the app has no
telemetry, a policy change cannot retroactively affect data that was never collected.

## Contact

Open an issue at <https://github.com/Souhaieb-Marzouk/SecureBox/issues>, or use GitHub's private
vulnerability reporting for security issues. Do not include sensitive device data in public issues.
