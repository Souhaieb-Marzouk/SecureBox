plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

android {
    namespace = "io.securebox.core.data"
    compileSdk = 34
    ndkVersion = "27.2.12479018"

    defaultConfig {
        minSdk = 26
        // AES-256 key for the bundled scanner ruleset. This is obfuscation, not a secret: anyone
        // holding the APK can extract it. Real protection comes from signing the feed in v0.5.0.
        buildConfigField("String", "SCANNER_RULES_KEY", "\"7717e8f658f94b2b0df202757a2510b3aefd187d22ab19b4724c4fb677468b7c\"")
        buildConfigField(
            "String",
            "THREAT_FEED_URL",
            "\"https://raw.githubusercontent.com/Souhaieb-Marzouk/SecureBox/main/threat-intel/latest.json\"",
        )
        buildConfigField(
            "String",
            "THREAT_FEED_PUBLIC_KEY",
            "\"46e1da16ba8e6584b217ba69cdf5be39f2585a34bcb2dc2e6f62c5d17900b055\"",
        )
    }

    buildFeatures {
        buildConfig = true
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
            // Keep native staging inside the (possibly redirected) build directory so restricted
            // environments do not need write access to the checkout root.
            buildStagingDirectory = layout.buildDirectory.dir("cxx").get().asFile
        }
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
    implementation(libs.okhttp)
    implementation(libs.eddsa)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
}

