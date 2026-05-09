plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    `maven-publish`
}

group = "io.kmpbits"
version = "0.1.0"

kotlin {
    jvmToolchain(17)

    androidTarget {
        publishLibraryVariants("release")
    }
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach { it.binaries.framework { baseName = "runtime" } }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(libs.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.core)
            implementation(libs.androidx.splashscreen)
        }
    }
}

android {
    namespace = "io.kmpbits.splash.runtime"
    compileSdk = 35
    defaultConfig { minSdk = 24 }
}

publishing {
    repositories { mavenLocal() }
}
