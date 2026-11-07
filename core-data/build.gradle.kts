plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.securebox.core.data"
    compileSdk = 34

    defaultConfig {
        minSdk = 26
        // AES-256 key for the bundled scanner ruleset. This is obfuscation, not a secret: anyone
        // holding the APK can extract it. Real protection comes from signing the feed in v0.5.0.
        buildConfigField("String", "SCANNER_RULES_KEY", "\"7717e8f658f94b2b0df202757a2510b3aefd187d22ab19b4724c4fb677468b7c\"")
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
}
