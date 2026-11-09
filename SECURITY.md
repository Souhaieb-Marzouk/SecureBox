# Security Policy

## Supported versions

| Version | Supported |
|---|---|
| 0.9.x | ✅ |
| 0.8.x | ✅ (fixes only) |
| < 0.8 | ❌ |

Only the latest published release receives security fixes. Update through the in-app updater or from the Releases page. Release manifests are signed with Ed25519 (`SHA256SUMS.sig`); the threat feed uses a separate Ed25519 key documented in `docs/THREAT_FEED.md`.

## Reporting a vulnerability

**Please do not open a public issue for security problems.**

Use GitHub's private vulnerability reporting: **Security → Report a vulnerability** on
`https://github.com/Souhaieb-Marzouk/SecureBox/security/advisories/new`.

Include:

- The affected version (Settings → About, or the release tag).
- Device and Android version (phone or TV box model).
- Steps to reproduce and the impact you believe it has.
- Any logs or proof-of-concept, with personal data removed.

You can expect:

| Stage | Target |
|---|---|
| Acknowledgement | 72 hours |
| Triage and severity assessment | 7 days |
| Fix or mitigation plan | 30 days for high/critical issues |
| Public disclosure | After a fixed release is available, coordinated with you |

We will credit reporters in the release notes unless you ask to stay anonymous.

## Security design

### Update integrity

1. Release metadata comes from the public GitHub Releases API over HTTPS.
2. A release is only offered if its tag parses as semantic versioning **and** is newer than the installed `versionName`/`versionCode`.
3. The APK is downloaded only if the release publishes a checksum (`SHA256SUMS` asset, or a checksum line in the release body).
4. The downloaded file's SHA-256 is compared in constant time with the published value. On mismatch the file is **deleted** and installation is refused.
5. Installation is handed to the system installer via a `FileProvider` scoped to `cacheDir/updates`. There is no silent install.

> The updater trusts the GitHub repository and its release publisher. That trust is explicit: anyone who can publish a release can ship an update. Signing keys are held only in GitHub Actions encrypted secrets, and the resulting APK must be signed by the same certificate the installed app expects (see below).

### Self-integrity

At startup, SecureBox computes the SHA-256 of its own signing certificate and compares it with the value baked in at build time from the release keystore. A re-signed build shows a warning banner and the user is directed to the official release.

### Local data

- Settings, scan results and protection statistics live in app-private storage.
- `android:allowBackup="false"` — no cloud backup of app data.
- `android:usesCleartextTraffic="false"` — no HTTP fallback.
- The FileProvider exposes one directory (`cacheDir/updates`), read-only, per-URI.

### Combined VPN data path

- The Local firewall and DNS protection share one on-device `VpnService`.
- Allowed traffic is relayed through `VpnService.protect()` sockets or bound to a non-VPN network if
  the OEM returns `false`; it is not sent to a SecureBox server.
- TLS is not intercepted. No root or MITM proxy is used.
- Blocked app flows are matched by UID; unknown UIDs are allowed rather than breaking an app.
- The DNS resolver is DoH; a plaintext fallback is used only if DoH fails.
- IPv6 is not filtered in the current release; this is documented in the UI and release notes.

### Secrets

- No keystore, password, token or API key is stored in the repository.
- CI reads them from encrypted GitHub Actions secrets.
- The repository is public, so every commit is effectively audited by the world.

### Scope decisions for v1

- No Accessibility Service.
- No Device Admin.
- No SMS, call-log or contacts permissions.
- No third-party analytics, ad SDKs or remote crash reporting.

## Dependency policy

Only free, open-source dependencies are allowed. Every dependency is declared in `gradle/libs.versions.toml` and reviewed before upgrades. Dependabot PRs are welcome but must pass CI and the release-signing checks.

## Hardening checklist for users

1. Install only from this repository's Releases page (or a verified build you made yourself).
2. Keep the app updated; the in-app updater verifies checksums for you.
3. On TV boxes, disable "unknown sources" when you are not actively installing something.
4. Check the SHA-256 of any APK you download manually against `SHA256SUMS`.
