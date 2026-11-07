# Play Store compliance notes

This document records the declarations and rationale SecureBox needs for a Google Play listing. It
is written to be pasted into the Play Console and keeps the answers consistent with what the app
actually does.

## Data safety

| Question | Answer | Why |
|---|---|---|
| Does the app collect or share any user data? | **No** | No analytics, ads, crash-reporting or telemetry SDKs. |
| Is data encrypted in transit? | **Yes** (HTTPS) | Only the GitHub update check and the optional signed feed. |
| Can users request data deletion? | **Yes** | No server holds data; uninstalling removes everything locally. |
| Is data used for advertising? | **No** | No ad SDKs. |
| Is data sold? | **No** | Nothing leaves the device. |

## Permissions and declarations

### `QUERY_ALL_PACKAGES` (package visibility)

**Declaration text:**

> SecureBox is an on-device security application. It uses package visibility to enumerate installed
> applications so it can attribute network traffic and listening sockets to the correct app and
> offer per-app internet blocking. The list of installed apps is processed only on the device, is
> never uploaded, and is not used for advertising or analytics. Users see it only inside the app's
> security screens.

**Eligibility:** core functionality of a security/utility app (per-app firewall and traffic
attribution), not advertising or profiling.

### VPN service (`BIND_VPN_SERVICE`)

**Disclosure shown in the app and the persistent notification:**

> SecureBox inspects traffic locally on your device. Nothing is sent to us.

The VPN is a local firewall: blocked apps are routed into the tunnel and their packets are
discarded. SecureBox does **not** forward traffic, does **not** operate a remote VPN server, and
does **not** collect browsing data. The service declares `foregroundServiceType="specialUse"` with
the subtype "Local on-device firewall VPN" and is started only after the system VPN consent dialog.

### `PACKAGE_USAGE_STATS` (special access)

Used to read per-app bandwidth from `NetworkStatsManager` and `TrafficStats`. Data stays on-device.
Users grant it through Settings → Usage access; the app degrades to an explanatory banner without
it.

### `POST_NOTIFICATIONS`

Used for security alerts (upload anomalies). Denying it only suppresses notifications; findings
remain visible in the app.

### `REQUEST_INSTALL_PACKAGES`

Used only for the user-initiated self-update. The installer is always the system dialog; there is
no silent install.

## What SecureBox does not use

- No Accessibility Service.
- No Device Admin.
- No SMS, call-log or contacts permissions (it only audits other apps that hold them).
- No background location, camera or microphone.
- No advertising ID.

## Listing checklist

- [x] Privacy policy in the repository (`PRIVACY.md`, also shown in-app).
- [x] VPN disclosure in-app and in the foreground notification.
- [x] Package-visibility declaration drafted above.
- [x] Data safety answers prepared.
- [ ] Play Console submission (maintainer action).
- [ ] Data safety form entered in Play Console (maintainer action).
- [ ] Screenshots for phone and TV (maintainer action).
