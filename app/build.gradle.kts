plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

import java.io.FileInputStream
import java.util.Properties

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

base {
    archivesName.set("passvaultgen")
}

android {
    namespace = "com.example.passwordvault"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.passwordvault"
        // 30 (Android 11): drops Android 8-10 devices but aligns with the
        // platform baseline where scoped storage, biometric Class 3
        // requirements, and package-visibility rules are fully enforced.
        minSdk = 30
        targetSdk = 35
        versionCode = 16
        versionName = "1.8.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Release signing. Secrets are NEVER committed to version control.
    // Order of precedence:
    //   1. CI environment variables (PV_STORE_FILE, PV_STORE_PASSWORD,
    //      PV_KEY_ALIAS, PV_KEY_PASSWORD) â€” the only supported path on
    //      shared/CI machines.
    //   2. A local, git-ignored keystore.properties (developer machines).
    // Without either, the release build stays unsigned (debug key) and
    // must not be published.
    val envStoreFile = System.getenv("PV_STORE_FILE")
    val envStorePassword = System.getenv("PV_STORE_PASSWORD")
    val envKeyAlias = System.getenv("PV_KEY_ALIAS")
    val envKeyPassword = System.getenv("PV_KEY_PASSWORD")
    val keystorePropsFile = rootProject.file("keystore.properties")
    val hasReleaseKey = (envStoreFile != null && envStorePassword != null) ||
        keystorePropsFile.exists()
    if (hasReleaseKey) {
        signingConfigs {
            create("release") {
                if (envStoreFile != null && envStorePassword != null) {
                    storeFile = file(envStoreFile)
                    storePassword = envStorePassword
                    keyAlias = envKeyAlias ?: "passvaultgen"
                    keyPassword = envKeyPassword ?: envStorePassword
                } else {
                    val keystoreProps = Properties()
                    FileInputStream(keystorePropsFile).use { keystoreProps.load(it) }
                    storeFile = file(keystoreProps.getProperty("storeFile"))
                    storePassword = keystoreProps.getProperty("storePassword")
                    keyAlias = keystoreProps.getProperty("keyAlias")
                    keyPassword = keystoreProps.getProperty("keyPassword")
                }
            }
        }
    }

    lint {
        // Two known-noisy checks stay disabled; everything else is enforced,
        // including on release builds (Gradle defaults, restored on purpose).
        disable += listOf("QueryPermissionsNeedAppAccess", "HardcodedDebugMode")
        checkReleaseBuilds = true
        abortOnError = true
    }

    testOptions {
        animationsDisabled = true
    }

    buildTypes {
        named("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            if (hasReleaseKey) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        named("debug") {
            isDebuggable = true
            applicationIdSuffix = ".debug"
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
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

tasks.withType<Test> {
    testLogging {
        events("passed", "failed", "skipped")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
    }
}

dependencies {
    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.bouncycastle)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)
    implementation(libs.androidx.fragment)
    implementation(libs.gson)

    testImplementation(libs.junit)
    testImplementation(libs.truth)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
