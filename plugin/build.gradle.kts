plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    compileOnly("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
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

