import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Release signing (keystore + credentials are gitignored, see local.properties). Falls back to
// unsigned if not configured, so a fresh checkout without local.properties still builds release.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}
val releaseStoreFile: String = localProperties.getProperty("RELEASE_STORE_FILE", "")
val releaseStorePassword: String = localProperties.getProperty("RELEASE_STORE_PASSWORD", "")
val releaseKeyAlias: String = localProperties.getProperty("RELEASE_KEY_ALIAS", "")
val releaseKeyPassword: String = localProperties.getProperty("RELEASE_KEY_PASSWORD", "")
val hasReleaseSigning = releaseStoreFile.isNotBlank() && rootProject.file(releaseStoreFile).exists()

android {
    namespace = "dev.quenguyen.ytgrab"
    compileSdk = 35

    defaultConfig {
        applicationId = "dev.quenguyen.ytgrab"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        // youtubedl-android bundles a full Python + yt-dlp + ffmpeg per ABI, which is most of
        // the APK's size. Almost every real device sold since ~2017 is arm64-v8a, so building
        // only that ABI cuts the APK to roughly a quarter of the all-ABI size.
        ndk {
            abiFilters += "arm64-v8a"
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        // youtubedl-android ships the Python runtime and yt-dlp as .so-named zip/data blobs it
        // reads with plain file I/O, so they must be extracted to disk instead of mapped from the APK.
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.service)
    implementation(libs.activity.compose)

    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.material3)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.navigation.compose)

    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    implementation(libs.datastore.preferences)

    implementation(libs.youtubedl.library)
    implementation(libs.youtubedl.ffmpeg)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.json)
}
