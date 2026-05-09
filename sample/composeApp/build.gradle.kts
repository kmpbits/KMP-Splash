plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("io.kmpbits.splash")
}

splashScreen {
    backgroundColor = "#1A1A2E"
    iosProjectPath = "sample/iosApp/iosApp"
    logoFile = "src/commonMain/composeResources/drawable/logo.png"
}

kotlin {
    jvmToolchain(17)

    androidTarget()
    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64(),
    ).forEach {
        it.binaries.framework {
            baseName = "composeApp"
            isStatic = true
        }
    }

    sourceSets {
        commonMain.dependencies {
            implementation(project(":runtime"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.splashscreen)
            implementation(libs.androidx.appcompat)
        }
    }
}

android {
    namespace = "io.kmpbits.splash.sample"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.kmpbits.splash.sample"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
