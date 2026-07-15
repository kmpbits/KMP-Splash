plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("org.jetbrains.dokka") version "2.2.0"
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.7.3")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
}

val kmpSplashVersion: String by project

// Functional tests that also apply org.jetbrains.compose can't resolve this plugin through
// GradleRunner's withPluginClasspath() classpath injection: TestKit's injected classpath loads
// this plugin's Kotlin Gradle Plugin dependency in an isolated classloader, which is a different
// Class instance than the one the Compose Gradle plugin (resolved through the normal plugin
// portal) sees — causing "Could not find KotlinMultiplatformExtension" at runtime. Those tests
// instead resolve this plugin from mavenLocal by id+version, like a real consumer project would.
//
// That resolution needs a version Gradle has never seen before: non-SNAPSHOT module versions are
// treated as immutable, so if "$kmpSplashVersion" was ever previously resolved on this machine
// (e.g. the real published release), Gradle reuses that cached — and possibly stale — artifact
// instead of the one `publishToMavenLocal` just wrote, even though the file on disk changed. A
// "-LOCAL" suffix (the convention already used elsewhere in this repo for local-only artifacts)
// keeps functional-test publishing on its own version line, decoupled from real releases.
val isFunctionalTestRun = gradle.startParameter.taskNames.any { it == "test" || it.endsWith(":test") }
val publishVersion = if (isFunctionalTestRun) "$kmpSplashVersion-LOCAL" else kmpSplashVersion

group = "io.github.kmpbits"
version = publishVersion

kotlin {
    jvmToolchain(17)
}

tasks.named("test") {
    dependsOn("publishToMavenLocal")
}

gradlePlugin {
    plugins {
        create("kmpSplash") {
            id = "io.github.kmpbits.splash"
            implementationClass = "io.kmpbits.splash.KmpSplashPlugin"
            displayName = "KMP Splash Screen Plugin"
            description = "Configure splash screens for Android and iOS from a single DSL block"
        }
    }
}

mavenPublishing {
    publishToMavenCentral(com.vanniktech.maven.publish.SonatypeHost.CENTRAL_PORTAL)
    if (project.hasProperty("signingInMemoryKey")) signAllPublications()
    coordinates("io.github.kmpbits", "splash-plugin", publishVersion)

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

