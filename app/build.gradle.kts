plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// Release builds get their version from the git tag: CI passes -PappVersion=MAJOR.MINOR.PATCH (tag vMAJOR.MINOR.PATCH).
// versionCode = MAJOR*10000 + MINOR*100 + PATCH, so every later tag installs over the previous one.
val appVersion: String? = providers.gradleProperty("appVersion").orNull
val appVersionCode: Int? = appVersion?.let { version ->
    val parts = Regex("""(\d+)\.(\d+)\.(\d+)""").matchEntire(version)?.groupValues?.drop(1)?.map(String::toInt)
    require(parts != null && parts[1] < 100 && parts[2] < 100) { "appVersion must be MAJOR.MINOR.PATCH (minor, patch < 100), got $version" }
    parts[0] * 10_000 + parts[1] * 100 + parts[2]
}

// Release signing comes from the environment (CI secrets); without it assembleRelease yields an unsigned APK.
val releaseKeystore: String? = System.getenv("IRBRUTE_KEYSTORE_PATH")

android {
    namespace = "com.pandorika.irbruteforcer"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.pandorika.irbruteforcer"
        minSdk = 27
        targetSdk = 36
        versionCode = appVersionCode ?: 1
        versionName = appVersion ?: "1.0.0-dev"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = file(releaseKeystore)
                storePassword = System.getenv("IRBRUTE_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("IRBRUTE_KEY_ALIAS")
                keyPassword = System.getenv("IRBRUTE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            if (releaseKeystore != null) signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}