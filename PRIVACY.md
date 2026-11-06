# SecureBox Privacy Policy

**Last updated: 2026-11-06**

SecureBox is designed so that the honest answer to "what do you collect?" is **nothing**.

## Summary

- SecureBox has **no telemetry**, no analytics, no advertising SDKs and no crash-reporting servers.
- Your app list, traffic statistics, scan results and settings **never leave your device**.
- The app makes network requests for exactly two purposes: checking GitHub for a new version, and — only if you enable it — downloading the signed Threat Intelligence feed.
- Both requests are **pull-only**. SecureBox never uploads anything about you.

## What SecureBox does on your device

| Data | Where it lives | Why |
|---|---|---|
| Language, theme, device mode, firewall/feed preferences | App-private DataStore on the device | Remember your settings |
| Cached latest-release metadata and last-check time | App-private DataStore | Avoid hitting GitHub's rate limit |
| Downloaded update packages | App cache (`cacheDir/updates`) | Handed to the system installer, then deleted |
| Per-app traffic statistics (v0.2.0) | Device memory only | Show bandwidth usage |
| Scan results (v0.3.0) | Device memory / local database | Show findings |

None of the above is transmitted anywhere. Uninstalling SecureBox removes it.

## Network requests

1. **Update check** — an HTTPS `GET` to `https://api.github.com/repos/Souhaieb-Marzouk/SecureBox/releases/latest`, with a `SecureBox-Android` User-Agent. GitHub sees your IP address and User-Agent, as it would for any HTTPS request. No token, no account and no identifier is sent. Results are cached for 12 hours by default.
2. **Threat Intelligence feed (v0.5.0, opt-in)** — an HTTPS `GET` for a signed, versioned JSON file. The signature is verified with an Ed25519 public key embedded in the app. No query parameters identify you or your device.

There are no other network connections. The VPN module (v0.2.0) is **local-only**: it routes packets through an on-device filter and never sends traffic to a SecureBox server. There is no SecureBox server.

## Permissions

| Permission | Why | If you deny it |
|---|---|---|
| `INTERNET`, `ACCESS_NETWORK_STATE` | Update check and optional feed | Updates and feed unavailable; the rest works |
| `PACKAGE_USAGE_STATS` (special access) | Per-app bandwidth (v0.2.0) | Bandwidth features show an explanation instead |
| `POST_NOTIFICATIONS` | Security alerts | Alerts appear in-app only |
| `REQUEST_INSTALL_PACKAGES` | Install a verified update | You can install releases manually from GitHub |
| VPN consent (v0.2.0) | Local firewall | Firewall unavailable; the rest works |

SecureBox **never** requests SMS, call-log or contacts permissions. It can *audit* other apps that hold them, but it does not read your messages or calls.

## Children

SecureBox does not knowingly collect data from anyone, including children.

## Changes

Material changes to this policy will ship with a new release and be listed in `CHANGELOG.md`. Because the app has no telemetry, a policy change cannot retroactively affect data that was never collected.

## Contact

Open an issue at https://github.com/Souhaieb-Marzouk/SecureBox/issues or email the maintainer listed on the GitHub profile. Do not include sensitive device data in public issues.
