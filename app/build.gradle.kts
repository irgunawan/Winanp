plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Signing: pakai keystore dari env (CI) atau file lokal winanp-release.jks.
// Kalau tidak ada sama sekali, fallback ke debug signing supaya APK tetap bisa dipasang.
val ksFile = file(System.getenv("WINANP_KEYSTORE") ?: "winanp-release.jks")
val ksPassword = System.getenv("WINANP_KEYSTORE_PASSWORD") ?: "winanp2026"
val ksAlias = System.getenv("WINANP_KEY_ALIAS") ?: "winanp"
val ksKeyPassword = System.getenv("WINANP_KEY_PASSWORD") ?: ksPassword
val hasReleaseKeystore = ksFile.exists()

android {
    namespace = "id.irgunawan.winanp"
    compileSdk = 34

    defaultConfig {
        applicationId = "id.irgunawan.winanp"
        // Redmi Note 11 runs Android 11 (API 30) up to Android 13/14 (API 33/34)
        minSdk = 26
        targetSdk = 34
        versionCode = 291
        versionName = "2.91.0"

        buildConfigField("String", "GITHUB_OWNER", "\"irgunawan\"")
        buildConfigField("String", "GITHUB_REPO", "\"Winanp\"")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("winanp") {
                storeFile = ksFile
                storePassword = ksPassword
                keyAlias = ksAlias
                keyPassword = ksKeyPassword
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
            // Signature konsisten antar rilis supaya update OTA bisa menimpa APK lama.
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("winanp")
            } else {
                signingConfigs.getByName("debug")
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
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.webkit:webkit:1.10.0")

    // Media3 ExoPlayer with Native FLAC & MP3 extraction for Redmi Note 11 Snapdragon 680
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-session:1.3.1")
    implementation("androidx.media3:media3-common:1.3.1")

    // Coroutines for GitHub OTA check & MediaStore scanning
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")
}
