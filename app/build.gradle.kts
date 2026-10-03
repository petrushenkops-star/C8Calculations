plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.pavel.c8calculations"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pavel.c8calculations"
        minSdk = 26
        targetSdk = 37
        versionCode = 106
        versionName = "1.1.4"
    }

    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("C8_KEYSTORE_PATH")
            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("C8_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("C8_KEY_ALIAS")
                keyPassword = System.getenv("C8_KEYSTORE_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    buildFeatures { compose = true }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.09.00")
    implementation(composeBom)
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.navigation:navigation-compose:2.10.2")
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")
    testImplementation("junit:junit:4.13.2")
}
