plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.dokka)
}

val kmpSplashVersion: String by project

group = "io.github.kmpbits"
version = kmpSplashVersion

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

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("signingInMemoryKey")) signAllPublications()
    coordinates("io.github.kmpbits", "splash-runtime", kmpSplashVersion)

    pom {
        name = "KMP Splash"
        description = "Splash screen plugin for Compose Multiplatform — no Xcode required"
        url = "https://github.com/kmpbits/KMP-Splash"
        inceptionYear = "2025"
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0"
            }
        }
        developers {
            developer {
                id = "kmpbits"
                name = "KMP Bits"
                url = "https://github.com/kmpbits/"
            }
        }
        scm {
            url = "https://github.com/kmpbits/KMP-Splash"
            connection = "scm:git:git://github.com/kmpbits/KMP-Splash.git"
            developerConnection = "scm:git:ssh://git@github.com/kmpbits/KMP-Splash.git"
        }
    }
}
