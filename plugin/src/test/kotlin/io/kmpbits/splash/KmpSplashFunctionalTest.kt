package io.kmpbits.splash

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class KmpSplashFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectDir: File
    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @Before
    fun setup() {
        projectDir = tempFolder.newFolder("project")
        buildFile = File(projectDir, "build.gradle.kts")
        settingsFile = File(projectDir, "settings.gradle.kts")

        settingsFile.writeText("rootProject.name = \"test-project\"")
    }

    /**
     * Tests that also apply `org.jetbrains.compose` cannot use `withPluginClasspath()`: TestKit
     * injects this plugin's Kotlin Gradle Plugin dependency into its own isolated classloader,
     * which is a different `KotlinMultiplatformExtension` Class instance than the one the
     * portal-resolved Compose Gradle plugin sees, causing a runtime "Could not find
     * KotlinMultiplatformExtension" failure. Resolving this plugin from mavenLocal by id+version
     * (like a real consumer project would) avoids the duplicate classloading entirely. The
     * `test` task in build.gradle.kts depends on `publishToMavenLocal` so the version below is
     * always up to date.
     */
    private fun useMavenLocalPluginResolution() {
        settingsFile.writeText("""
            pluginManagement {
                repositories {
                    mavenLocal()
                    gradlePluginPortal()
                    mavenCentral()
                }
            }
            rootProject.name = "test-project"
        """.trimIndent())
    }

    private val pluginVersion: String by lazy {
        val props = java.util.Properties()
        File("../gradle.properties").inputStream().use { props.load(it) }
        // Matches the "-LOCAL" suffix build.gradle.kts applies when publishing to mavenLocal
        // for a functional test run (see the comment on `isFunctionalTestRun` there).
        "${props.getProperty("kmpSplashVersion")}-LOCAL"
    }

    private fun setupIosProject(): File {
        val iosAppDir = File(projectDir, "iosApp")
        iosAppDir.mkdirs()
        val xcodeProjDir = File(projectDir, "iosApp.xcodeproj")
        xcodeProjDir.mkdirs()
        File(xcodeProjDir, "project.pbxproj").writeText("")
        File(iosAppDir, "Assets.xcassets").mkdirs()
        File(iosAppDir, "Info.plist").writeText(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><plist version=\"1.0\"><dict></dict></plist>"
        )
        return iosAppDir
    }

    @Test
    fun `can run generateLaunchScreen task`() {
        val iosAppDir = setupIosProject()

        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("io.github.kmpbits.splash")
            }

            kotlin {
                jvm()
            }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                iosProjectPath = "iosApp"
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateLaunchScreen", "--stacktrace")
            .withPluginClasspath()
            .build()

        println(result.output)
        assertTrue(result.output.contains("SUCCESS"))
        assertTrue(File(iosAppDir, "Assets.xcassets/SplashBackground.colorset").exists())
    }

    @Test
    fun `generateLaunchScreen includes exitAnimation in SplashInit`() {
        setupIosProject()

        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("io.github.kmpbits.splash")
            }

            kotlin {
                jvm()
            }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                iosProjectPath = "iosApp"
                exitAnimation = io.kmpbits.splash.ExitAnimation.FadeOut(300)
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateLaunchScreen", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("SUCCESS"))

        val splashInit = File(projectDir, "build/generated/kmpSplash/iosMain/kotlin/io/kmpbits/splash/SplashInit.kt")
        assertTrue(splashInit.exists())
        assertTrue(splashInit.readText().contains("ExitAnimation.FadeOut(300)"))
    }

    @Test
    fun `generateLaunchScreen with None exitAnimation omits animation line`() {
        setupIosProject()

        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("io.github.kmpbits.splash")
            }

            kotlin {
                jvm()
            }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                iosProjectPath = "iosApp"
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateLaunchScreen", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("SUCCESS"))

        val splashInit = File(projectDir, "build/generated/kmpSplash/iosMain/kotlin/io/kmpbits/splash/SplashInit.kt")
        assertTrue(splashInit.exists())
        assertTrue(!splashInit.readText().contains("exitAnimation"))
    }

    @Test
    fun `generateLaunchScreen uses the configured compose packageOfResClass`() {
        setupIosProject()
        useMavenLocalPluginResolution()

        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
                id("org.jetbrains.compose") version "1.7.3"
                id("io.github.kmpbits.splash") version "$pluginVersion"
            }

            kotlin {
                jvm()
            }

            compose {
                resources {
                    packageOfResClass = "com.example.custom.generated.resources"
                }
            }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                iosProjectPath = "iosApp"
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateLaunchScreen", "--stacktrace")
            .build()

        assertTrue(result.output.contains("SUCCESS"))

        val splashInit = File(projectDir, "build/generated/kmpSplash/iosMain/kotlin/io/kmpbits/splash/SplashInit.kt")
        assertTrue(splashInit.readText().contains("import com.example.custom.generated.resources.Res"))
    }

    @Test
    fun `splashScreen resourcePackage override wins over compose packageOfResClass`() {
        setupIosProject()
        useMavenLocalPluginResolution()

        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
                id("org.jetbrains.compose") version "1.7.3"
                id("io.github.kmpbits.splash") version "$pluginVersion"
            }

            kotlin {
                jvm()
            }

            compose {
                resources {
                    packageOfResClass = "com.example.fromcompose.generated.resources"
                }
            }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                iosProjectPath = "iosApp"
                resourcePackage = "com.example.fromoverride.generated.resources"
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateLaunchScreen", "--stacktrace")
            .build()

        assertTrue(result.output.contains("SUCCESS"))

        val splashInit = File(projectDir, "build/generated/kmpSplash/iosMain/kotlin/io/kmpbits/splash/SplashInit.kt")
        val content = splashInit.readText()
        assertTrue(content.contains("import com.example.fromoverride.generated.resources.Res"))
        assertTrue(!content.contains("fromcompose"))
    }

    @Test
    fun `generateLaunchScreen falls back to Compose's own default package formula`() {
        setupIosProject()
        useMavenLocalPluginResolution()

        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("org.jetbrains.kotlin.plugin.compose") version "2.1.0"
                id("org.jetbrains.compose") version "1.7.3"
                id("io.github.kmpbits.splash") version "$pluginVersion"
            }

            kotlin {
                jvm()
            }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                iosProjectPath = "iosApp"
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateLaunchScreen", "--stacktrace")
            .build()

        assertTrue(result.output.contains("SUCCESS"))

        // rootProject.name is "test-project" (set in setup()); with no group configured,
        // Compose's own default formula is "{moduleName}.generated.resources".
        val splashInit = File(projectDir, "build/generated/kmpSplash/iosMain/kotlin/io/kmpbits/splash/SplashInit.kt")
        assertTrue(splashInit.readText().contains("import test_project.generated.resources.Res"))
    }
}
