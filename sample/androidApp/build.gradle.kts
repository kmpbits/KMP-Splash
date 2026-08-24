plugins {
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.android.application)
}

// Gradle evaluates subprojects in path-alphabetical order by default, which would configure
// this module (":sample:androidApp") before ":sample:shared" — too early for the KmpSplash
// plugin (applied on :sample:shared, wiring itself into this module's variants via androidAppPath)
// to register its AGP Variant API callbacks before this module's variants are computed. Forcing
// :sample:shared to evaluate first keeps that wiring working.
evaluationDependsOn(":sample:shared")

kotlin {
    jvmToolchain(17)
}

android {
    namespace = "io.kmpbits.splash.sample.androidapp"
    compileSdk = 35
    defaultConfig {
        applicationId = "io.kmpbits.splash.sample.androidapp"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {
    implementation(project(":runtime"))
    implementation(project(":sample:shared"))
    implementation(libs.androidx.appcompat)
}
