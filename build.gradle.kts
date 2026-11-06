// Top-level build file. Configuration that applies to every module lives here;
// module-specific Android configuration stays in each module's build script.
plugins {
    base
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
}

/**
 * Guard rail required by the project specification: user-facing copy must live in
 * res/values/strings.xml (and its translations). This task fails the build when a
 * Compose text parameter is given a raw string literal instead of a resource id.
 */
val verifyNoHardcodedStrings by tasks.registering {
    group = "verification"
    description = "Fails when user-visible strings are hardcoded in Compose sources."

    val sourceRoots = listOf(
        "app/src/main",
        "core-ui/src/main",
        "core-tv-ui/src/main",
        "feature-onboarding/src/main",
        "feature-dashboard/src/main",
        "feature-network/src/main",
        "feature-apps/src/main",
        "feature-threats/src/main",
        "feature-settings/src/main",
        "feature-threatintel/src/main",
        "feature-update/src/main",
    )
    inputs.files(sourceRoots.map { rootProject.fileTree(it) { include("**/*.kt") } })
    doLast {
        val suspicious = Regex(
            """(?:^|[^A-Za-z0-9_.])(?:text|title|label|subtitle|contentDescription|supportingText)\s*=\s*"[^"]*[A-Za-z][^"]*"""",
        )
        val offenders = mutableListOf<String>()
        sourceRoots.forEach { root ->
            val dir = rootProject.file(root)
            if (dir.exists()) {
                dir.walkTopDown()
                    .filter { it.isFile && it.extension == "kt" }
                    .forEach { file ->
                        file.readLines().forEachIndexed { index, line ->
                            if (suspicious.containsMatchIn(line)) {
                                offenders += "${file.relativeTo(rootProject.projectDir)}:${index + 1}: ${line.trim()}"
                            }
                        }
                    }
            }
        }
        if (offenders.isNotEmpty()) {
            throw GradleException(
                buildString {
                    appendLine("Hardcoded user-facing strings detected. Move them to strings.xml:")
                    offenders.take(50).forEach { appendLine("  - $it") }
                },
            )
        }
        logger.lifecycle("No hardcoded user-facing strings detected.")
    }
}

tasks.named("check") {
    dependsOn(verifyNoHardcodedStrings)
}
