plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val githubOwner: String = providers.gradleProperty("securebox.githubOwner").getOrElse("Souhaieb-Marzouk")
val githubRepo: String = providers.gradleProperty("securebox.githubRepo").getOrElse("SecureBox")

android {
    namespace = "io.securebox.core.network"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        buildConfigField("String", "GITHUB_OWNER", "\"$githubOwner\"")
        buildConfigField("String", "GITHUB_REPO", "\"$githubRepo\"")
        buildConfigField("String", "GITHUB_API_BASE", "\"https://api.github.com\"")
        buildConfigField("String", "FILE_PROVIDER_AUTHORITY", "\"io.securebox.fileprovider\"")
    }

    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":core-domain"))

    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.core.ktx)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
