plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

fun getGitVersion(): String {
    return try {
        val process = Runtime.getRuntime().exec("git describe --tags --always --dirty")
        val version = process.inputStream.bufferedReader().readText().trim()
        if (version.startsWith("v")) version.substring(1) else version
    } catch (e: Exception) {
        "1.0.0-beta" // Fallback version
    }
}

fun getVersionCode(): Int {
    return try {
        val process = Runtime.getRuntime().exec("git rev-list --count HEAD")
        process.inputStream.bufferedReader().readText().trim().toInt()
    } catch (e: Exception) {
        1 // Fallback version code
    }
}

android {
    namespace = "com.sasakulab.yure_android_client"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.sasakulab.yure_android_client"
        minSdk = 26
        targetSdk = 36
        versionCode = getVersionCode()
        versionName = getGitVersion()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.okhttp)
    implementation(libs.gson)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
