import io.kmpbits.splash.ExitAnimation
import io.kmpbits.splash.SplashColor
import io.kmpbits.splash.SplashLogo

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    id("io.github.kmpbits.splash")
}

kotlin {
    jvmToolchain(17)
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":runtime"))
            implementation(compose.runtime)
            implementation(compose.ui)
        }
    }
}

splashScreen {
    backgroundColor = SplashColor.white
    backgroundColorNight = SplashColor.hex("#1A1A2E")
    logo = SplashLogo.resource("logo.png")
    exitAnimation = ExitAnimation.SlideUp()
    androidAppPath = "sample/androidApp"
}

android {
    namespace = "io.kmpbits.splash.sample.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
}
