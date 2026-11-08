"""Builds the local Safe Browsing hash-prefix asset from the public OpenPhish feed.

Only SHA-256 prefixes are written; full URLs are never bundled. Run from the repo root:

    python tools/update-safe-browsing-prefixes.py

OpenPhish public feed: https://openphish.com/feed.txt
"""

from __future__ import annotations

import hashlib
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
ASSET = ROOT / "feature-settings" / "src" / "main" / "assets" / "safe_browsing_prefixes.txt"
FEED = "https://openphish.com/feed.txt"
MAX_URLS = 5000
PREFIX_HEX = 8

# Google's public Safe Browsing test pages. These are harmless test URLs used to verify the
# hash-prefix path without shipping real malicious destinations.
TEST_URLS = [
    "http://testsafebrowsing.appspot.com/s/malware.html",
    "https://testsafebrowsing.appspot.com/s/malware.html",
    "http://testsafebrowsing.appspot.com/s/phishing.html",
    "https://testsafebrowsing.appspot.com/s/phishing.html",
    "http://testsafebrowsing.appspot.com/s/unwanted.html",
    "https://testsafebrowsing.appspot.com/s/unwanted.html",
]


def main() -> None:
    urls: list[str] = list(TEST_URLS)
    try:
        request = urllib.request.Request(FEED, headers={"User-Agent": "SecureBox/0.7"})
        with urllib.request.urlopen(request, timeout=45) as response:
            body = response.read(4_000_000).decode("utf-8", "ignore")
        for line in body.splitlines():
            line = line.strip()
            if line and not line.startswith("#") and line.startswith(("http://", "https://")):
                urls.append(line)
            if len(urls) >= MAX_URLS:
                break
    except Exception as error:  # Keep the build usable offline; the test URLs still work.
        print(f"warning: could not refresh OpenPhish feed: {error}")

    prefixes = sorted({hashlib.sha256(url.encode("utf-8")).hexdigest()[:PREFIX_HEX] for url in urls})
    ASSET.parent.mkdir(parents=True, exist_ok=True)
    header = [
        "# SecureBox local Safe Browsing hash prefixes.",
        "# SHA-256 prefix (first 4 bytes) of known phishing URLs; full URLs are not stored.",
        f"# Refreshed: {datetime.now(timezone.utc).strftime('%Y-%m-%dT%H:%M:%SZ')}",
        "# Source: OpenPhish public feed (https://openphish.com/feed.txt) + Google test pages.",
        f"# Entries: {len(prefixes)}",
        "",
    ]
    ASSET.write_text("\n".join(header + prefixes) + "\n", encoding="utf-8")
    print(f"wrote {len(prefixes)} prefixes to {ASSET}")


if __name__ == "__main__":
    main()
