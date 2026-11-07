# Threat intelligence feed

SecureBox can refresh its scanner rules and tracker list from a signed JSON feed without an app
update. The feed is **optional**: the app ships with a bundled ruleset and works offline.

## Contract

- `threat-intel/latest.json` — payload, UTF-8, no BOM.
- `threat-intel/latest.json.sig` — base64 Ed25519 signature over the **exact bytes** of `latest.json`.
- The app fetches both from the URL in `core-data`'s `THREAT_FEED_URL` build field, verifies the
  signature with the embedded public key (`THREAT_FEED_PUBLIC_KEY`), and only then parses the payload.
- A missing, malformed or mismatched signature rejects the whole feed; nothing is stored.
- The payload is written in a single DataStore edit, so a partial download never becomes active.

## Payload

```json
{
  "version": 1,
  "generatedAt": "2026-11-07T00:00:00Z",
  "rules": [
    {
      "id": "feed-001",
      "name": "Banking overlay dropper",
      "severity": "HIGH",
      "patterns": ["dropper_payload", "overlay_inject"],
      "allRequired": true
    }
  ],
  "trackers": ["com.adjust.sdk"],
  "domains": ["ads.example.com"]
}
```

- `version` must increase for the app to accept an update.
- `severity` is `SAFE`, `LOW`, `MEDIUM`, `HIGH` or `CRITICAL`.
- `allRequired` mirrors the scanner's rule semantics.
- `trackers` are added to the existing tracker-prefix list.
- `domains` are stored and shown in the status card. **They are not used by the firewall yet**: the
  current VPN only routes blocked apps and discards packets, so there is no DNS layer to filter on.
  Domain blocking needs the forwarding DNS design tracked separately.

## Key management

| Item | Location |
|---|---|
| Feed private key | `_securebox_keys/threat-feed-ed25519.pem` (outside the repository) |
| Feed public key | `docs/threat-feed-public.pem` and the `THREAT_FEED_PUBLIC_KEY` build field |
| Release manifest key | `_securebox_keys/release-sums-ed25519.pem` (different key, see `RELEASE_SIGNING.md`) |

The private key must never be committed or pasted into CI logs. Add it as the
`THREAT_FEED_KEY` repository secret; `.github/workflows/threat-feed.yml` signs and verifies the feed
on every change to `latest.json`.

## Sign locally

```powershell
node tools/sign-release.mjs --file threat-intel/latest.json --key C:\Users\Mercenary\Documents\GitHub\_securebox_keys\threat-feed-ed25519.pem
node tools/verify-release.mjs --file threat-intel/latest.json --sig threat-intel/latest.json.sig --key docs/threat-feed-public.pem
```

## Rotating the key

1. Generate a new Ed25519 key pair.
2. Ship an app update with the new `THREAT_FEED_PUBLIC_KEY` **before** signing feeds with the new
   private key; old clients will reject the new feed otherwise.
3. Keep the old public key in the app for one release cycle if you need a transition window.
