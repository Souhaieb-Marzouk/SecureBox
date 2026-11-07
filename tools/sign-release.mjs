#!/usr/bin/env node
// Signs a release manifest (SHA256SUMS) with the project's Ed25519 private key.
//
// Usage:
//   node tools/sign-release.mjs --file <SHA256SUMS> --key <private-key.pem> [--out <file.sig>]
//
// The private key lives outside the repository (see docs/RELEASE_SIGNING.md).

import { createPrivateKey, sign } from 'node:crypto';
import { readFileSync, writeFileSync } from 'node:fs';
import { resolve } from 'node:path';

const args = process.argv.slice(2);
const value = (flag) => {
  const index = args.indexOf(flag);
  return index >= 0 ? args[index + 1] : undefined;
};

const file = value('--file');
const keyPath = value('--key');
const out = value('--out') ?? (file ? `${file}.sig` : undefined);

if (!file || !keyPath) {
  console.error('Usage: node tools/sign-release.mjs --file <SHA256SUMS> --key <private-key.pem> [--out <file.sig>]');
  process.exit(2);
}

const data = readFileSync(resolve(file));
const privateKey = createPrivateKey(readFileSync(resolve(keyPath)));
const signature = sign(null, data, privateKey).toString('base64');
writeFileSync(resolve(out), `${signature}\n`, 'utf8');
console.log(`Signed ${file} -> ${out}`);
console.log(`Signature: ${signature}`);
