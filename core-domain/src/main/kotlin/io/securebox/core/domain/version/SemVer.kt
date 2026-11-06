package io.securebox.core.domain.version

/**
 * A tolerant semantic-version value object.
 *
 * SecureBox releases use the tag convention `vMAJOR.MINOR.PATCH+BUILD`, for example
 * `v2.1.0+21`. Build metadata is treated as the Android versionCode and only participates in
 * comparison when the semantic parts are equal, which mirrors how Android upgrades work.
 */
public data class SemVer(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val build: Int = 0,
) : Comparable<SemVer> {
    override fun compareTo(other: SemVer): Int {
        major.compareTo(other.major).takeIf { it != 0 }?.let { return it }
        minor.compareTo(other.minor).takeIf { it != 0 }?.let { return it }
        patch.compareTo(other.patch).takeIf { it != 0 }?.let { return it }
        return build.compareTo(other.build)
    }

    public fun isNewerThan(other: SemVer): Boolean = this > other

    override fun toString(): String =
        buildString {
            append("$major.$minor.$patch")
            if (build > 0) append("+$build")
        }

    public companion object {
        public val ZERO: SemVer = SemVer(0, 0, 0, 0)
    }
}

/** Parses release tags such as `v0.1.0`, `0.1.0`, `v2.1.0+21` or `v1.4.0-rc.1`. */
public object SemVerParser {
    private val pattern =
        Regex("""^[vV]?(\d{1,9})(?:\.(\d{1,9}))?(?:\.(\d{1,9}))?(?:-([0-9A-Za-z.\-]+))?(?:\+(\d{1,9}))?$""")

    /** Returns null when [raw] is not a version we understand; callers must not guess. */
    public fun parse(raw: String?): SemVer? {
        val value = raw?.trim().orEmpty()
        if (value.isEmpty()) return null
        val match = pattern.matchEntire(value) ?: return null
        val parts = match.destructured
        val major = parts.component1().toIntOrNull() ?: return null
        val minor = parts.component2().ifEmpty { "0" }.toIntOrNull() ?: return null
        val patch = parts.component3().ifEmpty { "0" }.toIntOrNull() ?: return null
        val build = parts.component5().ifEmpty { "0" }.toIntOrNull() ?: 0
        return SemVer(major = major, minor = minor, patch = patch, build = build)
    }

    /** Release tags of the form `v1.2.3+45` carry the Android versionCode in the build metadata. */
    public fun versionCodeFromTag(tag: String?): Int? {
        val value = tag?.trim().orEmpty()
        val plus = value.substringAfter('+', missingDelimiterValue = "")
        return plus.toIntOrNull()?.takeIf { it > 0 }
    }
}
