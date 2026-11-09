import java.security.KeyStore
import java.security.MessageDigest
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Signing is configured from environment variables (CI) or a local, git-ignored
 * `signing.properties` file. Secrets never live in the repository.
 */
val signingProperties = Properties()
val signingPropertiesFile = rootProject.file("signing.properties")
if (signingPropertiesFile.exists()) {
    signingPropertiesFile.inputStream().use(signingProperties::load)
}

fun signingValue(key: String, env: String): String? =
    System.getenv(env)?.takeIf { it.isNotBlank() } ?: signingProperties.getProperty(key)?.takeIf { it.isNotBlank() }

val keystorePathValue = signingValue("storeFile", "SECUREBOX_KEYSTORE_PATH")
val keystorePasswordValue = signingValue("storePassword", "SECUREBOX_KEYSTORE_PASSWORD")
val keyAliasValue = signingValue("keyAlias", "SECUREBOX_KEY_ALIAS") ?: "securebox"
val keyPasswordValue = signingValue("keyPassword", "SECUREBOX_KEY_PASSWORD") ?: keystorePasswordValue
val releaseKeystore = keystorePathValue?.let { rootProject.file(it) }?.takeIf { it.exists() }

/** Computes the SHA-256 of the release signing certificate so the app can verify itself at runtime. */
fun signingCertificateSha256(): String =
    runCatching {
        val store = releaseKeystore ?: return@runCatching ""
        val password = keystorePasswordValue ?: return@runCatching ""
        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        store.inputStream().use { keyStore.load(it, password.toCharArray()) }
        val certificate = keyStore.getCertificate(keyAliasValue) ?: return@runCatching ""
        MessageDigest
            .getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString(separator = "") { byte -> (byte.toInt() and 0xFF).toString(16).padStart(2, '0') }
    }.getOrDefault("")

val expectedSigningCertSha256 = signingCertificateSha256()

android {
    namespace = "io.securebox"
    compileSdk = 34

    defaultConfig {
        applicationId = "io.securebox"
        minSdk = 26
        targetSdk = 34
        versionCode = 23
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Only release builds know the signing certificate they expect; debug builds are signed by
        // the local debug key, so an empty value makes the integrity check report "not configured".
        buildConfigField("String", "EXPECTED_SIGNING_CERT_SHA256", "\"\"")
    }

    signingConfigs {
        if (releaseKeystore != null && keystorePasswordValue != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = keystorePasswordValue
                keyAlias = keyAliasValue
                keyPassword = keyPasswordValue
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
            buildConfigField("String", "EXPECTED_SIGNING_CERT_SHA256", "\"$expectedSigningCertSha256\"")
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64")
            isUniversalApk = true
        }
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkReleaseBuilds = true
        error += "HardcodedText"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.material3.window.size)

    implementation(project(":core-domain"))
    implementation(project(":core-data"))
    implementation(project(":core-network"))
    implementation(project(":core-ui"))
    implementation(project(":core-tv-ui"))
    implementation(project(":feature-onboarding"))
    implementation(project(":feature-dashboard"))
    implementation(project(":feature-network"))
    implementation(project(":feature-apps"))
    implementation(project(":feature-threats"))
    implementation(project(":feature-settings"))
    implementation(project(":feature-threatintel"))
    implementation(project(":feature-update"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.kotlinx.coroutines.android)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    debugImplementation(libs.compose.ui.tooling)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
}
