# SecureBox Release Notes

## v0.1.0 — Foundation & Update System

The first public build of SecureBox: a privacy-first security app for Android phones and Android TV boxes, with a complete self-update pipeline and no telemetry.

### Highlights

- First-run onboarding in **English and French**, with automatic TV/phone detection and a manual override.
- Two real UI shells — a touch-first phone shell and a D-pad-first TV shell — sharing every screen.
- **Self-update from GitHub Releases**, with SHA-256 verification before the installer is ever opened.
- A plain-language **Security Score** with a documented breakdown.
- **Nothing leaves your device.** The only network calls are the update check and the optional signed threat feed.

### Notes

- This is a foundation release. Network monitoring, the VPN firewall and malware scanning arrive in v0.2.0 and v0.3.0; the in-app screens state this clearly.
- Existing installs will not receive this build through the updater (it is the first one). Future releases will update in place.

See `CHANGELOG.md` and the GitHub release description for the full list.
