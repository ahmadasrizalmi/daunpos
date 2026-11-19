plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.daunpos.app"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.daunpos.app"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.4.0"
    }

    signingConfigs {
        // Keystore tidak di-commit ke repo (lihat .gitignore).
        // Untuk rilis, sediakan keystore/daunpos-release.jks secara lokal atau via CI secret.
        val keystoreFile = rootProject.file("keystore/daunpos-release.jks")
        if (keystoreFile.exists()) {
            create("release") {
                storeFile = keystoreFile
                storePassword = System.getenv("DAUNPOS_STORE_PASSWORD") ?: "daunpos123"
                keyAlias = System.getenv("DAUNPOS_KEY_ALIAS") ?: "daunpos"
                keyPassword = System.getenv("DAUNPOS_KEY_PASSWORD") ?: "daunpos123"
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
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))

    // Room persistence
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.8.5")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.compose.ui:ui-text-google-fonts")
    testImplementation("junit:junit:4.13.2")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
