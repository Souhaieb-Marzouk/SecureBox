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

Android does not let a normal app change Private DNS silently. The DNS screen therefore:

- Shows the current Private DNS hostname (`LinkProperties.privateDnsServerName`).
- Offers three presets with their DoH endpoints and DoT hostnames:
  - **AdGuard DNS** — ads and trackers system-wide.
  - **Cloudflare for Families** — malware and adult content (not most ads).
  - **Quad9** — known malware/phishing domains.
- Copies the hostname to the clipboard and opens Android's Private DNS settings.
- Tests the provider's **DNS-over-HTTPS** endpoint from the device.

Android's system-wide encrypted DNS is **DNS-over-TLS (DoT)**. DoH is used for the connection test;
the screen says this explicitly. Once the user saves the DoT hostname, the resolver applies to every
app on the device, including apps that do not use SecureBox.

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
- **System-wide ad blocking in every app** is only possible at DNS level (AdGuard DNS preset), after
  the user applies Private DNS. It blocks ad/tracker domains, not every ad served from a first-party
  domain.
- **Filtering HTTPS inside other apps** requires root, a modified client or a MITM proxy. SecureBox
  does not MITM user traffic.
- Cookie notices and popups vary by site. Cosmetic rules cover common patterns and are deliberately
  conservative so they do not hide login or checkout elements.
- The private browser does not store or upload browsing history. The DoH test sends a single
  `example.com` query to the selected provider.

## Code map

- `ShieldScreen.kt` — hub with modes/toggles/DNS status.
- `BrowserScreen.kt` — WebView, request blocking, hash checks, cosmetic filtering.
- `DnsProtectionScreen.kt` — presets, DoH test, Private DNS instructions.
- `BrowserFilters.kt` — host list, tracking parameters, cosmetic CSS.
- `DohClient.kt` — wire-format DoH probe.
- `SafeBrowsingStore.kt` + `core-domain/security/SafeBrowsingHash.kt` — prefix checks.
- `ShieldViewModel.kt` — persisted shield settings and DNS test state.
