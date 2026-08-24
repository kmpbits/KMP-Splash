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
    // Regression guard: generated code (e.g. KmpSplashInitProvider) must compile
    // in consumer modules that enable explicit API mode.
    explicitApi()
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
    generateAppIcon = true
    // androidApp's MainActivity extends AppCompatActivity (see its installKmpSplash() demo),
    // which requires postSplashScreenTheme to resolve to an AppCompat-descended theme.
    androidPostSplashTheme = "@style/Theme.App.AppCompat"
}

android {
    namespace = "io.kmpbits.splash.sample.shared"
    compileSdk = 35
    defaultConfig {
        minSdk = 24
    }
}
