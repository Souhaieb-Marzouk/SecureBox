#!/usr/bin/env node
// Verifies a detached Ed25519 signature over a release manifest.
//
// Usage:
//   node tools/verify-release.mjs --file <SHA256SUMS> --sig <SHA256SUMS.sig> [--key <public-key.pem>]
//
// The public key defaults to docs/release-signing-public.pem.

import { createPublicKey, verify } from 'node:crypto';
import { readFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import { dirname, join, resolve } from 'node:path';

const here = dirname(fileURLToPath(import.meta.url));
const args = process.argv.slice(2);
const value = (flag) => {
  const index = args.indexOf(flag);
  return index >= 0 ? args[index + 1] : undefined;
};

const file = value('--file');
const sigPath = value('--sig');
const keyPath = value('--key') ?? join(here, '..', 'docs', 'release-signing-public.pem');

if (!file || !sigPath) {
  console.error('Usage: node tools/verify-release.mjs --file <SHA256SUMS> --sig <SHA256SUMS.sig> [--key <public-key.pem>]');
  process.exit(2);
}

const data = readFileSync(resolve(file));
const signature = Buffer.from(readFileSync(resolve(sigPath), 'utf8').trim(), 'base64');
const publicKey = createPublicKey(readFileSync(resolve(keyPath)));

if (verify(null, data, publicKey, signature)) {
  console.log(`OK: ${file} signature is valid.`);
  process.exit(0);
}
console.error(`FAILED: ${file} signature does not match.`);
process.exit(1);
