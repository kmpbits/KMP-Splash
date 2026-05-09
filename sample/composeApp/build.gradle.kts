plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("io.kmpbits.splash") version "0.1.0"
}

splashScreen {
    backgroundColor = "#1A1A2E"
    logoFile = "drawable/splash_logo.png"   // optional — omit if no logo
    iosProjectPath = "iosApp/iosApp"
    // backgroundColorNight = "#0F0F1A"     // optional dark-mode Android override
}

kotlin {
    androidTarget()
    listOf(iosX64(), iosArm64(), iosSimulatorArm64())

    sourceSets {
        commonMain.dependencies {
            implementation(project(":runtime"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
        }
    }
}

android {
    namespace = "io.kmpbits.splash.sample"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
        // Wire the generated splash theme in your AndroidManifest.xml:
        // android:theme="@style/Theme.App.SplashScreen"
    }
}
