# Release signing

SecureBox publishes an Ed25519 signature for the `SHA256SUMS` manifest of every release. This lets
anyone verify the checksums independently of GitHub's transport and of the APK signing key.

## Files

| File | Purpose |
|---|---|
| `docs/release-signing-public.pem` | Ed25519 public key embedded in the repository |
| `SHA256SUMS` | SHA-256 of every APK in the release |
| `SHA256SUMS.sig` | Base64 Ed25519 signature over the exact `SHA256SUMS` bytes |
| `tools/sign-release.mjs` | Signs a manifest (maintainer only) |
| `tools/verify-release.mjs` | Verifies a manifest (anyone) |

The private key **never enters the repository or CI logs**. It is kept offline (for this project:
`_securebox_keys/release-sums-ed25519.pem`, outside the checkout) and referenced through the
`RELEASE_SUMS_KEY` secret when a signing workflow is added.

## Verify a release

```powershell
cd C:\Users\Mercenary\Documents\GitHub\SecureBox
node tools/verify-release.mjs --file "C:\path\to\SHA256SUMS" --sig "C:\path\to\SHA256SUMS.sig"
```

Expected: `OK: ... signature is valid.` Then compare the APK hashes:

```powershell
Get-FileHash .\securebox-universal-release.apk -Algorithm SHA256
```

## Sign a new release

```powershell
node tools/sign-release.mjs --file .\SecureBox-dist\vX.Y.Z\SHA256SUMS --key C:\Users\Mercenary\Documents\GitHub\_securebox_keys\release-sums-ed25519.pem
```

Attach `SHA256SUMS.sig` to the GitHub release next to `SHA256SUMS` and the APKs.

## Why Ed25519

- Small keys and signatures (32/64 bytes), fast verification on any platform.
- Node's `crypto` verifies it without extra dependencies, so the check works offline.
- It is independent of the Android APK signing key: a compromised build machine cannot silently
  change the published checksums without the offline key.
