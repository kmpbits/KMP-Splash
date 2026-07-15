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

group = "io.github.kmpbits"
version = kmpSplashVersion

kotlin {
    jvmToolchain(17)
}

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
//
// This can't be a *conditional* override of the "real" coordinates() below: whether `:test` is
// actually going to run can't be read off `gradle.startParameter.taskNames` (that only reflects
// literally-invoked task names like "build"/"check", not the resolved task graph — `:test` can
// run as a transitive dependency without ever being named on the command line), and deciding it
// later from `gradle.taskGraph.whenReady` doesn't work either: by the time the task graph is
// populated, com.vanniktech.maven.publish has already finalized the properties `coordinates()`
// sets, so a second call in `whenReady` fails with "property ... is final and cannot be changed
// any further". So instead this is a second, independent, always-"-LOCAL" Maven publication,
// entirely separate from the real release publication below — its version never depends on which
// task was invoked, so there's no task-graph question to get wrong.
val localTestVersion = "$kmpSplashVersion-LOCAL"

publishing {
    publications {
        // The implementation artifact itself, under the always-"-LOCAL" version.
        create<MavenPublication>("localFunctionalTest") {
            groupId = "io.github.kmpbits"
            artifactId = "splash-plugin"
            version = localTestVersion
            from(components["java"])
        }
        // `java-gradle-plugin` only generates a plugin marker (the POM that lets
        // `id("io.github.kmpbits.splash") version "..."` resolve at all) for its own
        // automatically-created "pluginMaven" publication — not for arbitrary additional
        // publications — so the marker for this "-LOCAL" version has to be hand-rolled the same
        // way: a POM-only artifact at `<plugin id>.gradle.plugin` depending on the implementation
        // artifact above.
        create<MavenPublication>("localFunctionalTestMarker") {
            groupId = "io.github.kmpbits.splash"
            artifactId = "io.github.kmpbits.splash.gradle.plugin"
            version = localTestVersion
            pom {
                withXml {
                    asNode().appendNode("dependencies").appendNode("dependency").apply {
                        appendNode("groupId", "io.github.kmpbits")
                        appendNode("artifactId", "splash-plugin")
                        appendNode("version", localTestVersion)
                    }
                }
            }
        }
    }
}

tasks.named("test") {
    dependsOn(
        "publishLocalFunctionalTestPublicationToMavenLocal",
        "publishLocalFunctionalTestMarkerPublicationToMavenLocal",
    )
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

// com.vanniktech.maven.publish reconciles *every* MavenPublication in the project — not just the
// ones it created — to share its `coordinates()` groupId/artifactId/version, in its own
// `afterEvaluate` (registered by the `mavenPublishing {}` block above). That silently resets both
// of the "-LOCAL" publications declared earlier in this file back to the real release identity
// (group "io.github.kmpbits", artifact "splash-plugin", version "$kmpSplashVersion") — losing the
// marker's distinct "io.github.kmpbits.splash" / "io.github.kmpbits.splash.gradle.plugin" identity
// entirely, not just its version. Registering this `afterEvaluate` down here, after that block,
// guarantees it runs after vanniktech's (Gradle calls them in registration order), so this one
// re-applies the correct identity for both and wins.
afterEvaluate {
    publishing.publications.getByName<MavenPublication>("localFunctionalTest") {
        groupId = "io.github.kmpbits"
        artifactId = "splash-plugin"
        version = localTestVersion
    }
    publishing.publications.getByName<MavenPublication>("localFunctionalTestMarker") {
        groupId = "io.github.kmpbits.splash"
        artifactId = "io.github.kmpbits.splash.gradle.plugin"
        version = localTestVersion
    }
}

