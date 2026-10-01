android {
    namespace = "com.aurcus.companion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.aurcus.companion"
        minSdk = 28
        targetSdk = 28
        versionCode = 1
        versionName = "1.0.0"
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
