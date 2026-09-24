plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.vanniktech.maven.publish") version "0.30.0"
    id("org.jetbrains.dokka") version "2.2.0"
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.1.0")
    compileOnly("org.jetbrains.compose:compose-gradle-plugin:1.7.3")
    compileOnly("com.android.tools.build:gradle-api:8.0.0")
    testImplementation(kotlin("test"))
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.compose:compose-gradle-plugin:1.7.3")
}

// Classpath for KmpSplashAndroidAppFunctionalTest only: the plugin's own classes plus AGP 9 and a
// matching KGP. Kept separate from the shared plugin-under-test classpath (which carries this
// plugin's KGP 2.1.0) because AGP 9's built-in Kotlin needs a newer KGP, and AGP applied via the
// test project's `plugins {}` would otherwise sit in a classloader the plugin can't see.
val androidFunctionalTestClasspath by configurations.creating
dependencies {
    androidFunctionalTestClasspath("com.android.tools.build:gradle:9.0.0")
    androidFunctionalTestClasspath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
}
tasks.test {
    dependsOn(tasks.named("classes"))
    systemProperty(
        "kmpsplash.androidTestPluginClasspath",
        (sourceSets.main.get().output.files + androidFunctionalTestClasspath).joinToString(File.pathSeparator),
    )
}

val kmpSplashVersion: String by project

group = "io.github.kmpbits"
version = kmpSplashVersion

kotlin {
    jvmToolchain(17)
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
    coordinates("io.github.kmpbits", "splash-plugin", kmpSplashVersion)

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

