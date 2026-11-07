# Translation status

SecureBox ships user-facing copy in **English** (`values/strings.xml`) and **French**
(`values-fr/strings.xml`) for every module. English values are the source of truth; French is
maintained alongside them.

## Outstanding review items

**None.** The v0.4.0 French review closed every row that was previously listed here:

| Decision | Result |
|---|---|
| "VPN local" | Changed to **"VPN local uniquement"** for clarity. |
| GitHub rate limit | Changed to **"Quota GitHub atteint. Réessayez plus tard."** |
| "checksum" | Kept **"empreinte"** consistently across the updater. |
| Firewall roadmap copy | Removed the stale "Arrive en v0.2.0" phrase. |
| "boîtier TV" | Kept; preferred over "box Android TV". |
| Usage access | Kept Android's own label **"accès à l'usage"**. |
| Completion title | Changed to gender-neutral **"Votre appareil est protégé"**. |

## Process for new strings

1. Add the English string to the module's `values/strings.xml`.
2. Add the French string to the matching `values-fr/strings.xml` in the same commit.
3. Run `./gradlew verifyNoHardcodedStrings lintDebug`; missing translations surface as lint warnings.
4. If a term needs a decision, record it in this file and resolve it before the release.

## Known gaps

- No RTL language is shipped. `supportsRtl` is enabled and layouts use start/end, but there is no
  RTL translation yet.
- Plurals are handled with quantity strings where counts are shown; new count strings must add a
  `plurals.xml` entry rather than concatenating numbers.
- Screenshots and store listings are English-only until the v1.0.0 store pass.
