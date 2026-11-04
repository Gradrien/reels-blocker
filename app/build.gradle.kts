import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Release signing comes from environment variables (CI) or an untracked keystore.properties file (local).
// Without either, release builds fall back to the debug key, which is fine for a personal sideload.
val keystoreProps = Properties().apply {
    val f = rootProject.file("keystore.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun signingValue(key: String, env: String): String? = System.getenv(env) ?: keystoreProps.getProperty(key)

android {
    namespace = "fr.adrien.reelsblocker"
    compileSdk = 35

    defaultConfig {
        applicationId = "fr.adrien.reelsblocker"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "1.0.0"
    }

    signingConfigs {
        val storeFile = signingValue("storeFile", "RB_KEYSTORE_FILE")
        if (storeFile != null) {
            create("release") {
                this.storeFile = file(storeFile)
                storePassword = signingValue("storePassword", "RB_KEYSTORE_PASSWORD")
                keyAlias = signingValue("keyAlias", "RB_KEY_ALIAS")
                keyPassword = signingValue("keyPassword", "RB_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
            signingConfig = signingConfigs.findByName("release") ?: signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
