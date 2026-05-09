plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

group = "io.kmpbits"
version = "0.1.0"

kotlin {
    jvmToolchain(17)
}

gradlePlugin {
    plugins {
        create("kmpSplash") {
            id = "io.kmpbits.splash"
            implementationClass = "io.kmpbits.splash.KmpSplashPlugin"
            displayName = "KMP Splash Screen Plugin"
            description = "Configure splash screens for Android and iOS from a single DSL block"
        }
    }
}

