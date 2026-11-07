package io.securebox.core.domain.security

/**
 * Lightweight, YARA-style rule matcher.
 *
 * The corpus is the printable-string dump of an APK's DEX files. Rules are either "any pattern"
 * or "all patterns"; matching is substring based. This is intentionally not full YARA: it has no
 * regex/hex grammar, which keeps it deterministic and cheap enough to run on a TV box.
 */
public class RuleEngine(
    private val ruleSet: RuleSet,
) {
    public fun scan(corpus: CharSequence): List<Finding> {
        if (corpus.isEmpty()) return emptyList()
        val findings = mutableListOf<Finding>()

        for (rule in ruleSet.rules) {
            val matched =
                if (rule.allRequired) {
                    rule.patterns.all { corpus.contains(it) }
                } else {
                    rule.patterns.any { corpus.contains(it) }
                }
            if (matched) {
                findings += Finding(FindingCode.SIGNATURE_RULE, rule.severity, rule.name)
            }
        }

        for (prefix in ruleSet.trackerPrefixes) {
            if (corpus.contains(prefix)) {
                findings += Finding(FindingCode.TRACKER_SDK, RiskLevel.LOW, prefix)
            }
        }

        return findings
    }

    public fun rulesVersion(): Int = ruleSet.version
}

