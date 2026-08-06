plugins {
    id("com.android.application")
}

android {
    namespace = "com.android.xhs"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.android.xhs"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    compileOnly("io.github.libxposed:api:102.0.1")
}
