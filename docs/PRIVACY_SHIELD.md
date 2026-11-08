# Privacy Shield

SecureBox 0.7 adds a private browser, local request filtering and encrypted-DNS guidance. The goal is
honest, useful protection on a normal Android phone or TV box — not a promise that every ad in every
app can be removed.

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

## System-wide DNS protection

Android does not let a normal app change Private DNS silently. There are two paths:

### DNS Shield (recommended, no Private DNS setup)

Turn on **Privacy shield → Set up DNS → DNS Shield**. It uses Android's VPN permission once and
creates a **DNS-only VPN**:

- The VPN exposes a private DNS server address and routes only that address into the tunnel.
- All other app traffic bypasses SecureBox and uses the normal network.
- DNS queries are filtered against the local ad/tracker list, then forwarded to the selected
  provider over DNS-over-HTTPS.
- No Private DNS hostname is needed. This is the path for TV boxes that do not expose Private DNS.
- Android allows one VPN at a time: turn off the Local firewall before enabling DNS Shield.
- Apps that use their own DoH/DoT resolver can bypass any DNS filter, including this one.

### Private DNS (optional)

The DNS screen also shows the current Private DNS hostname and offers three presets:

- **AdGuard DNS** — ads and trackers system-wide.
- **Cloudflare for Families** — malware and adult content (not most ads).
- **Quad9** — known malware/phishing domains.

It can copy the DoT hostname and open Android's Private DNS settings. Android's system-wide
encrypted DNS is **DNS-over-TLS (DoT)**; DoH is used for the connection test.

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
- **System-wide ad blocking in every app** is only possible at DNS level — either the one-switch
  DNS Shield VPN or Private DNS with the AdGuard preset. It blocks ad/tracker domains, not every ad
  served from a first-party domain.
- **Filtering HTTPS inside other apps** requires root, a modified client or a MITM proxy. SecureBox
  does not MITM user traffic.
- Cookie notices and popups vary by site. Cosmetic rules cover common patterns and are deliberately
  conservative so they do not hide login or checkout elements.
- The private browser does not store or upload browsing history. The DoH test sends a single
  `example.com` query to the selected provider.

## Code map

- `ShieldScreen.kt` — hub with modes/toggles/DNS status/stats entry.
- `ShieldStatsScreen.kt` — cumulative protection counters and reset.
- `BrowserScreen.kt` — WebView, request blocking, hash checks, cosmetic filtering.
- `DnsProtectionScreen.kt` — DNS Shield switch, presets, DoH test, Private DNS instructions.
- `BrowserFilters.kt` — tracking parameters and cosmetic CSS; hosts come from `DnsBlocklist`.
- `DohClient.kt` — wire-format DoH probe.
- `core-data/network/DnsShieldVpnService.kt` — DNS-only VPN, local blocklist and DoH forwarding.
- `SafeBrowsingStore.kt` + `core-domain/security/SafeBrowsingHash.kt` — prefix checks.
- `ShieldViewModel.kt` — persisted shield settings, DNS test and stats state.
