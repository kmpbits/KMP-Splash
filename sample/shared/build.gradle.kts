import io.kmpbits.splash.SplashColor
import io.kmpbits.splash.SplashLogo

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    id("io.github.kmpbits.splash")
}

kotlin {
    jvmToolchain(17)
    androidTarget()
}

splashScreen {
    backgroundColor = SplashColor.white
    backgroundColorNight = SplashColor.hex("#1A1A2E")
    logo = SplashLogo.resource("logo.png")
    androidAppPath = "sample/androidApp"
}

android {
    namespace = "io.kmpbits.splash.sample.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
}
