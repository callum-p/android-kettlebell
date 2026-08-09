plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

val appVersionName = "1.7"

/**
 * Extracts the release notes for [version] from the repo's CHANGELOG.md so the app can show a
 * "What's new" modal after an update. Computed at build time — the notes are baked into
 * BuildConfig, never hand-copied into Kotlin. Falls back to an empty string if the section
 * is missing (e.g. a version that hasn't been documented yet).
 */
fun changelogFor(version: String): String {
    val file = rootProject.file("CHANGELOG.md")
    if (!file.exists()) return ""
    val out = StringBuilder()
    var capturing = false
    for (line in file.readLines()) {
        if (line.startsWith("## ")) {
            if (capturing) break
            capturing = line.removePrefix("## ").trim() == version
            continue
        }
        if (capturing) out.appendLine(line)
    }
    return out.toString().trim()
}

/** The full CHANGELOG.md (all versions), dropping the leading "# Changelog" title/preamble. */
fun fullChangelog(): String {
    val file = rootProject.file("CHANGELOG.md")
    if (!file.exists()) return ""
    return file.readLines()
        .dropWhile { !it.startsWith("## ") }
        .joinToString("\n")
        .trim()
}

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

android {
    namespace = "com.kettlebell.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.kettlebell.app"
        minSdk = 26
        targetSdk = 35
        versionCode = 10700
        versionName = appVersionName

        buildConfigField("String", "CHANGELOG", "\"${changelogFor(appVersionName).escapeForBuildConfig()}\"")
        buildConfigField("String", "CHANGELOG_FULL", "\"${fullChangelog().escapeForBuildConfig()}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        // The release signing key is injected from CI secrets (base64 keystore decoded to a file,
        // path + credentials passed via env). Every published build is signed with this one key so
        // updates install in place. When the env isn't set (local dev), fall back to the Android
        // default debug keystore. The keystore is NOT committed to source control.
        getByName("debug") {
            val keystorePath = System.getenv("SIGNING_KEYSTORE_FILE")
            if (!keystorePath.isNullOrBlank() && file(keystorePath).exists()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("SIGNING_KEY_ALIAS")
                keyPassword = System.getenv("SIGNING_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.play.services.auth)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation("org.xerial:sqlite-jdbc:3.44.1.0")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))

    debugImplementation(libs.androidx.ui.tooling)
}
