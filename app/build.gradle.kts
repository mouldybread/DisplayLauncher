plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.yourcompany.headlesslauncher"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.yourcompany.headlesslauncher"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3"
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    // Jetpack Compose
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose:compose-ui")
    implementation("androidx.compose:compose-material3")
    implementation("androidx.activity:activity-compose:1.9.0")

    // NanoHTTPD for embedded web server
    implementation("org.nanohttpd:nanohttpd:2.3.1")

    // Gson for JSON handling
    implementation("com.google.code.gson:gson:2.10.1")

    // Core Android
    implementation("androidx.core:core-ktx:1.13.1")
}
