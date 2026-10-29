import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val keystorePropertiesFile = rootProject.file("keystore/keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun envOrProp(name: String): String? =
    System.getenv(name) ?: keystoreProperties.getProperty(name)

/**
 * Versions are dates taken from git tags: `v2026.10.29` is a release, `v2026.10.29.2`
 * a second release that day, and `v2026.10.29-beta.1` a beta. CI passes the tag in
 * CATCH_VERSION_NAME. A local build is the next same-day number after the newest
 * release tag plus "-dev" (2026.10.29.2-dev after v2026.10.29), so it installs over
 * that release. `update/Version.kt` decodes the same scheme.
 */
val catchVersionName: String = System.getenv("CATCH_VERSION_NAME")?.removePrefix("v")
    ?: run {
        val tag = providers.exec {
            commandLine("git", "describe", "--tags", "--match", "v2[0-9][0-9][0-9].*", "--exclude", "*-*", "--abbrev=0")
            isIgnoreExitValue = true
        }.standardOutput.asText.get().trim().removePrefix("v").ifEmpty { "2000.01.01" }
        val parts = tag.split('.')
        "${parts[0]}.${parts[1]}.${parts[2]}.${(parts.getOrNull(3)?.toInt() ?: 1) + 1}-dev"
    }

val catchVersionCode: Int = run {
    val match = Regex("""^(\d{4})\.(\d{1,2})\.(\d{1,2})(?:\.(\d))?(?:-(dev|(?:alpha|beta|rc)\.(\d{1,2})))?$""")
        .matchEntire(catchVersionName)
        ?: error("Version '$catchVersionName' is not a Catch version (2026.10.29, 2026.10.29.2, or 2026.10.29-beta.1)")
    val (year, month, day, sameDay, suffix, pre) = match.destructured
    val preRelease = when {
        suffix == "dev" -> 0
        suffix.isEmpty() -> 99
        else -> pre.toInt().coerceIn(1, 98)
    }
    val date = (year.toInt() - 2000) * 10_000 + month.toInt() * 100 + day.toInt()
    date * 1_000 + (sameDay.toIntOrNull() ?: 1) * 100 + preRelease
}

android {
    namespace = "app.catchrower"
    compileSdk = 35

    defaultConfig {
        applicationId = "app.catchrower.catch"
        minSdk = 29
        // The Hydrow runs Android 10 (API 29).
        targetSdk = 34
        versionCode = catchVersionCode
        versionName = catchVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "RELEASES_API",
            "\"https://api.github.com/repos/thegregstengel/catch/releases\"",
        )
    }

    signingConfigs {
        val storeFilePath = envOrProp("CATCH_KEYSTORE_FILE")
        val storePassword = envOrProp("CATCH_KEYSTORE_PASSWORD")
        val keyAlias = envOrProp("CATCH_KEY_ALIAS")
        val keyPassword = envOrProp("CATCH_KEY_PASSWORD")

        if (storeFilePath != null && storePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                // Paths in keystore.properties are relative to the android/ project root.
                storeFile = rootProject.file(storeFilePath)
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfigs.findByName("release")?.let { signingConfig = it }
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
        aidl = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("androidx.media:media:1.7.0")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.foundation:foundation")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")

    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
