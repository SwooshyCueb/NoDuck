plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "zone.swoosh.noduck"
    compileSdk = 34

    defaultConfig {
        applicationId = "zone.swoosh.noduck"
        minSdk = 26
        targetSdk = 34
        versionCode = 2
        versionName = "0.2"
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
    buildToolsVersion = "34.0.0"
}

dependencies {
    compileOnly(libs.xposed)
}