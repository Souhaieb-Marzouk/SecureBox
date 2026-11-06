pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "SecureBox"

include(
    ":app",
    ":core-domain",
    ":core-data",
    ":core-network",
    ":core-ui",
    ":core-tv-ui",
    ":feature-onboarding",
    ":feature-dashboard",
    ":feature-network",
    ":feature-apps",
    ":feature-threats",
    ":feature-settings",
    ":feature-threatintel",
    ":feature-update",
)
