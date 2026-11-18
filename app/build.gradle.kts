plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.secrets)
}

android {
    namespace = "com.loypos.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.loypos.kxzmpq"
        minSdk = 26
        targetSdk = 36
        val propVersionCode = providers.gradleProperty("versionCode").orNull?.toIntOrNull()
        val propVersionName = providers.gradleProperty("versionName").orNull?.takeIf { it.isNotBlank() }
        versionCode = propVersionCode ?: 1
        versionName = propVersionName ?: "1.0.0"
        buildConfigField("String", "PLESK_API_BASE_URL", "\"https://novelsapps.com.et/api/v1\"")
    }

    signingConfigs {
        create("debugConfig") {
            val keystore = file("${rootDir}/debug.keystore")
            if (keystore.exists()) {
                storeFile = keystore
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
        create("release") {
            val releasePath = providers.gradleProperty("releaseStoreFile").orNull?.takeIf { it.isNotBlank() }
                ?: System.getenv("ANDROID_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
                ?: "${rootDir}/release.keystore"
            val releaseKeystore = file(releasePath)
            if (releaseKeystore.exists()) {
                storeFile = releaseKeystore
                storePassword = providers.gradleProperty("releaseStorePassword").orNull?.takeIf { it.isNotBlank() }
                    ?: System.getenv("ANDROID_KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: "loypos-release"
                keyAlias = providers.gradleProperty("releaseKeyAlias").orNull?.takeIf { it.isNotBlank() }
                    ?: System.getenv("ANDROID_KEY_ALIAS")?.takeIf { it.isNotBlank() }
                    ?: "loypos-release"
                keyPassword = providers.gradleProperty("releaseKeyPassword").orNull?.takeIf { it.isNotBlank() }
                    ?: System.getenv("ANDROID_KEY_PASSWORD")?.takeIf { it.isNotBlank() }
                    ?: "loypos-release"
                enableV1Signing = true
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }

    buildTypes {
        debug {
            val keystore = file("${rootDir}/debug.keystore")
            if (keystore.exists()) {
                signingConfig = signingConfigs.getByName("debugConfig")
            }
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            val releasePath = providers.gradleProperty("releaseStoreFile").orNull?.takeIf { it.isNotBlank() }
                ?: System.getenv("ANDROID_KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
                ?: "${rootDir}/release.keystore"
            val releaseKeystore = file(releasePath)
            val debugKeystore = file("${rootDir}/debug.keystore")
            if (releaseKeystore.exists()) {
                signingConfig = signingConfigs.getByName("release")
            } else if (debugKeystore.exists()) {
                signingConfig = signingConfigs.getByName("debugConfig")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.vico.compose.m3)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.security.crypto)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
}
