# Translation TODO

SecureBox ships user-facing copy in **English** (`app/src/main/res/values/`, plus each module's
`values/strings.xml`) and **French** (`values-fr/strings.xml` in the same modules).

Every string below was translated carefully but should be reviewed by a fluent French speaker before
v1.0.0. Strings marked ⚠️ are the ones where a native reviewer is most likely to prefer different
wording.

## Terms kept in English on purpose

| Term | Reason |
|---|---|
| "Threat Intelligence" | Product/module name and a common industry term; also used in the GitHub repository. |
| "SecureBox" | Brand name, never translated. |
| "GitHub" | Proper noun. |
| "Android TV" | Platform name. |

## Strings needing review

| Key | English | French (current) | Note |
|---|---|---|---|
| `onboarding_permission_vpn_why` | "A local-only VPN lets SecureBox stop one app's internet access. No traffic ever leaves your device." | "Un VPN local permet à SecureBox de couper l'accès internet d'une application. Aucun trafic ne quitte votre appareil." | ⚠️ "VPN local" vs "VPN local uniquement" — the latter is more explicit but longer. |
| `update_error_rate_limited` | "GitHub rate limit reached. Try again later." | "Limite de requêtes GitHub atteinte. Réessayez plus tard." | ⚠️ Consider "Quota GitHub atteint" for brevity. |
| `update_error_checksum_why` | "A mismatch can mean a corrupted download or a tampered file." | "Une différence peut signifier un téléchargement corrompu ou un fichier modifié." | ⚠️ "empreinte" vs "somme de contrôle" for "checksum"; the app uses "empreinte" consistently. |
| `settings_firewall_body` | "Blocks an app's internet access on this device. Arrives in v0.2.0." | "Bloque l'accès internet d'une application sur cet appareil. Arrive en v0.2.0." | ⚠️ Confirm future-tense style for roadmap strings. |
| `privacy_on_device_body` | "Your app list never leaves the phone or TV box." | "Votre liste d'applications ne quitte jamais le téléphone ou le boîtier TV." | ⚠️ "boîtier TV" vs "box Android TV" — the former is preferred by French reviewers in our test. |
| `dashboard_factor_usage` | "Allow usage access to see per-app data." | "Autorisez l'accès à l'usage pour voir les données par application." | ⚠️ "accès à l'usage" is Android's own French label; confirm. |
| `onboarding_done_title` | "You're protected" | "Vous êtes protégé" | ⚠️ Gender-neutral alternative: "Votre appareil est protégé". |

## Process

1. Review the table above.
2. If a change is needed, edit BOTH `values/strings.xml` and `values-fr/strings.xml`.
3. Run `./gradlew verifyNoHardcodedStrings lintDebug` (lint reports missing translations as warnings).
4. Remove the row from this file in the same commit.

## Known gaps

- `feature-network`, `feature-apps` and `feature-threats` screens are roadmap placeholders; their copy will be rewritten in v0.2.0/v0.3.0 and must be re-reviewed then.
- No RTL language is supported yet; `supportsRtl` is enabled and layouts use start/end, but no RTL translation exists.
- Plural forms: no `plurals.xml` resources are needed in v0.1.0; they will be introduced with the bandwidth module (counts of apps/bytes).
