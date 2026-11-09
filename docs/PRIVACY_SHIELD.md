# Privacy Shield

SecureBox 0.9 adds a private browser, a combined forwarding firewall/DNS engine and protection
statistics. The goal is honest, useful protection on a normal Android phone or TV box — not a
promise that every ad in every app can be removed.

## What the private browser does

Open **Privacy shield → Open private browser**.

- Blocks requests to known advertising and analytics hostnames before they leave the device.
- Hides common cookie notices, social widgets, app-download banners and ad slots with a local CSS
  rule.
- Offers two modes:
  - **Standard** — ad/tracker host blocking, third-party cookies off.
  - **High** — also removes tracking parameters (`utm_*`, `fbclid`, `gclid`, `msclkid`, …) from
    navigated URLs and clears cookies when the browser closes.
- Checks navigations and subresources against a bundled **SHA-256 prefix** list. Only prefixes are
  stored; the visited URL is never sent to a server.
- Upgrades typed `http://` addresses to `https://` and disables file/content access, mixed content
  and unrequested media autoplay.

The browser is a normal WebView. It does not inject ads into other apps and does not proxy other
apps' traffic.

## System-wide DNS protection and the per-app firewall

Android gives a normal app only one VPN. SecureBox 0.9 therefore uses **one combined forwarding
engine** instead of two mutually exclusive services:

- **All apps are inside the tunnel.** This is required so every app’s DNS can be filtered.
- Allowed apps’ **TCP and UDP flows are forwarded on-device** through protected sockets.
- Apps selected in **Local firewall** are identified by UID; their TCP connections are reset and
  their UDP packets are dropped.
- DNS is answered locally: the query is checked against the ad/tracker list, then forwarded over
  **DNS-over-HTTPS** to AdGuard, Cloudflare for Families or Quad9.
- **Local firewall and DNS protection can be enabled at the same time.** They are two controls on
  the same VPN, not two VPNs.

### Privacy meaning of the forwarding engine

This is a real change from 0.8:

- Before, SecureBox only captured packets and discarded them. It **never forwarded** app traffic.
- Now, allowed app traffic is relayed by SecureBox’s local sockets. It goes directly from the device
  to the destination; it is **not sent to a SecureBox server**, not stored, and TLS is not
  intercepted.
- SecureBox is therefore in the data path for allowed apps, like any local VPN firewall. The
  notification and the DNS screen say this explicitly.
- The engine handles IPv4 TCP/UDP. The v0.9.5 tunnel is IPv4-only: IPv6 is not routed into the VPN,
  so IPv6 traffic is not filtered. This is a documented limitation, not a silent bypass claim.
- Per-app blocking needs UID attribution. On Android 10+ this uses
  `ConnectivityManager.getConnectionOwnerUid`; on older boxes it falls back to `/proc/net`. When the
  UID cannot be determined, SecureBox allows the flow rather than breaking an unknown app. This is a
  real limitation, not a hidden bypass guarantee.

### Private DNS (still optional)

The DNS screen also shows the current Private DNS hostname and offers three presets. Private DNS
works outside the VPN; if it is available it applies to every app in addition to the tunnel. Android
uses **DNS-over-TLS (DoT)** for Private DNS; DoH is used by the engine and the connection test.

## Protection statistics

The shield hub has **Protection statistics**:

- Ads blocked
- Trackers blocked
- Pages cleaned of cookie/popup clutter
- Dangerous links blocked
- DNS queries blocked locally

Browser counts are flushed when the browser closes; DNS counts are flushed in small batches. The
DNS number only includes queries SecureBox itself blocked; the upstream resolver may block more and
does not report its counts to SecureBox. Counters are stored only in the app's DataStore and can be
reset from the page.

## TV remote focus and scrolling

Android TV moves the viewport to the focused node. Text-only sections have no natural focus target,
which made the top/bottom of long pages unreachable. SecureBox now:

- Gives every glass card a focusable card body and a focusable tail after its last text line.
- Adds focusable top/bottom anchors to every scroll container.
- Makes screen headings, status banners and settings sections focus stops.

A remote can therefore land on text sections and scroll them into view, including the app-detail
sheet.

## Safe Browsing hash prefixes

`feature-settings/src/main/assets/safe_browsing_prefixes.txt` contains short SHA-256 prefixes
generated from:

- the [OpenPhish public feed](https://openphish.com/feed.txt), and
- Google's public Safe Browsing test pages.

No full URLs are stored. The browser computes the prefix of the current URL and checks it locally.
Regenerate the snapshot with:

```powershell
python tools/update-safe-browsing-prefixes.py
```

A snapshot ages. DNS threat filtering (Quad9/Cloudflare Families) is the live layer; the local list
is defense-in-depth and works offline. Google's full Safe Browsing API is not used because it needs
an API key and sends URL prefixes to Google.

## Limitations you should know

- **YouTube in-stream ads cannot be removed reliably** by DNS or an embedded browser. YouTube serves
  video ads from the same video infrastructure; blocking it would break playback. SecureBox does not
  pretend otherwise.
- **System-wide ad blocking in every app** is DNS-level: the local blocklist plus AdGuard DNS. It
  blocks ad/tracker domains, not every ad served from a first-party domain.
- **Filtering HTTPS inside other apps** requires root, a modified client or a MITM proxy. SecureBox
  does not MITM user traffic.
- The forwarding engine is IPv4-only in v0.9.5. IPv6 is not routed into the VPN, so it is not
  filtered; this is tracked as deferred work.
- Per-app blocking requires a readable UID. If the device refuses UID attribution, the flow is
  allowed; the firewall cannot guarantee blocking on that firmware.
- Cookie notices and popups vary by site. Cosmetic rules cover common patterns and are deliberately
  conservative so they do not hide login or checkout elements.
- The private browser does not store or upload browsing history. The DoH test sends a single
  `example.com` query to the selected provider.

## Code map

- `ShieldScreen.kt` — hub with modes/toggles/DNS status/stats entry.
- `ShieldStatsScreen.kt` — cumulative protection counters and reset.
- `BrowserScreen.kt` — WebView, request blocking, hash checks, cosmetic filtering.
- `DnsProtectionScreen.kt` — DNS switch, presets, DoH test, Private DNS guidance.
- `BrowserFilters.kt` — tracking parameters and cosmetic CSS; hosts come from `DnsBlocklist`.
- `DohClient.kt` — wire-format DoH probe.
- `core-data/network/SecureBoxVpnService.kt` — one all-app VPN, UID blocking, DNS and DoH.
- `core-data/network/CombinedTunnelEngine.kt` — IPv4 TCP/UDP forwarding data plane.
- `core-data/network/ProtectedDoh.kt` — protected DoH sockets that bypass the tunnel.
- `SafeBrowsingStore.kt` + `core-domain/security/SafeBrowsingHash.kt` — prefix checks.
- `ShieldViewModel.kt` — persisted shield settings, DNS test and stats state.
