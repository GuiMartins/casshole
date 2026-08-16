plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

// The published version is decided by the tag the release workflow computes,
// which passes it back in as -PversionName (see .github/workflows/release.yml)
// - that keeps the number Settings > Sobre shows in sync with the GitHub
// release the APK came from. A local build has no tag and says so ("-dev"),
// instead of pretending to be some released version.
fun versionCodeFrom(versionName: String): Int {
    val parts = versionName.substringBefore("-").split(".").mapNotNull { it.toIntOrNull() }
    if (parts.size < 3) return 1
    // Android rejects 0 - the dev fallback ("0.0.0-dev") would land there.
    return (parts[0] * 10_000 + parts[1] * 100 + parts[2]).coerceAtLeast(1)
}

val appVersionName = (findProperty("versionName") as String?)?.removePrefix("v") ?: "0.0.0-dev"

android {
    namespace = "com.casshole"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.casshole"
        minSdk = 26
        targetSdk = 34
        versionCode = versionCodeFrom(appVersionName)
        versionName = appVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Only configured when RELEASE_STORE_FILE is set (CI, via GitHub Secrets) -
    // local/dev.sh builds never touch this and keep using assembleDebug, so
    // there's nothing to configure for local development. See CLAUDE.md for
    // the full release-signing setup.
    val releaseStoreFile = System.getenv("RELEASE_STORE_FILE")
    if (releaseStoreFile != null) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = System.getenv("RELEASE_STORE_PASSWORD")
                keyAlias = System.getenv("RELEASE_KEY_ALIAS")
                keyPassword = System.getenv("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (releaseStoreFile != null) {
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
        // Off by default since AGP 8 - needed for BuildConfig.VERSION_NAME,
        // shown in Settings > Sobre.
        buildConfig = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    packaging {
        // android-mail and android-activation (Jakarta Mail) both ship the
        // same META-INF license/notice files - known, documented conflict.
        resources {
            excludes += "/META-INF/NOTICE.md"
            excludes += "/META-INF/LICENSE.md"
            excludes += "/META-INF/LICENSE.txt"
            excludes += "/META-INF/NOTICE.txt"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    // AppCompatDelegate.setApplicationLocales - the official per-app language
    // API (works even in a Compose-only app with no AppCompat theme).
    implementation("androidx.appcompat:appcompat:1.7.0")

    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // IMAP client for the email auto-fetch feature - reintroduces network
    // access on purpose (deliberate, user-approved reversal of the earlier
    // "no network" decision, see CLAUDE.md).
    implementation("com.sun.mail:android-mail:1.6.7")
    implementation("com.sun.mail:android-activation:1.6.7")

    // EncryptedSharedPreferences - the official way to store a real account
    // credential (the email app password) on-device, unlike the low-stakes
    // PDF-open passwords which are fine in plain Room.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
}
