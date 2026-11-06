package io.securebox.core.domain.update

import io.securebox.core.domain.model.AppVersion
import io.securebox.core.domain.model.ReleaseInfo
import io.securebox.core.domain.model.UpdateDecision
import io.securebox.core.domain.version.SemVerParser

/**
 * Decides whether a published release is newer than the build installed on the device.
 *
 * Rules, in order:
 * 1. If the tag is not semantic versioning, refuse to compare (never auto-offer a mystery build).
 * 2. Compare installed versionName against the release tag.
 * 3. When equal, compare the Android versionCode carried by the `+N` tag suffix.
 */
public object UpdateEvaluator {
    public fun evaluate(
        current: AppVersion,
        release: ReleaseInfo,
    ): UpdateDecision {
        val latest = release.version
            ?: return UpdateDecision.Incomparable(
                reason = "Release tag '${release.tagName}' is not a semantic version.",
            )
        val installed = SemVerParser.parse(current.versionName)
            ?: return UpdateDecision.Incomparable(
                reason = "Installed version '${current.versionName}' is not a semantic version.",
            )

        val nameIsNewer = latest.withoutBuild() > installed.withoutBuild()
        val nameIsEqual = latest.withoutBuild() == installed.withoutBuild()
        val codeIsNewer = (release.versionCode ?: 0) > current.versionCode

        return when {
            nameIsNewer -> UpdateDecision.UpdateAvailable(release, current)
            nameIsEqual && codeIsNewer -> UpdateDecision.UpdateAvailable(release, current)
            else -> UpdateDecision.UpToDate
        }
    }

    private fun io.securebox.core.domain.version.SemVer.withoutBuild() = copy(build = 0)
}
