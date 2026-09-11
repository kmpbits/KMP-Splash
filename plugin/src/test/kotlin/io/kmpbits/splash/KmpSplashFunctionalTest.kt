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

    private fun setupClassicIosProject(): File {
        val iosAppDir = File(projectDir, "iosApp")
        iosAppDir.mkdirs()
        val xcodeProjDir = File(projectDir, "iosApp.xcodeproj")
        xcodeProjDir.mkdirs()
        File(xcodeProjDir, "project.pbxproj").writeText(
            """
            {
            /* Begin PBXBuildFile section */
            /* End PBXBuildFile section */
            /* Begin PBXFileReference section */
            /* End PBXFileReference section */
            /* Begin PBXSourcesBuildPhase section */
            		AAAA1111 /* Sources */ = {
            			isa = PBXSourcesBuildPhase;
            			files = (
            			);
            		};
            /* End PBXSourcesBuildPhase section */
            }
            """.trimIndent()
        )
        File(iosAppDir, "Assets.xcassets").mkdirs()
        File(iosAppDir, "Info.plist").writeText(
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><plist version=\"1.0\"><dict></dict></plist>"
        )
        return iosAppDir
    }

    @Test
    fun `generateLaunchScreen in SwiftUI mode writes the Swift view and patches pbxproj`() {
        val iosAppDir = setupClassicIosProject()

        buildFile.writeText("""
            plugins {
                kotlin("multiplatform") version "2.1.0"
                id("io.github.kmpbits.splash")
            }

            kotlin { jvm() }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                iosProjectPath = "iosApp"
                iosUi = io.kmpbits.splash.IosUi.SwiftUI
                exitAnimation = io.kmpbits.splash.ExitAnimation.FadeOut(300)
            }
        """.trimIndent())

        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withArguments("generateLaunchScreen", "--stacktrace")
            .withPluginClasspath()
            .build()

        assertTrue(result.output.contains("SUCCESS"))

        val swift = File(iosAppDir, "KmpSplashView.swift")
        assertTrue(swift.exists())
        assertTrue(swift.readText().contains("public struct KmpSplashView"))

        val splashInit = File(projectDir, "build/generated/kmpSplash/iosMain/kotlin/io/kmpbits/splash/SplashInit.kt")
        assertTrue(!splashInit.exists())

        assertTrue(
            File(projectDir, "iosApp.xcodeproj/project.pbxproj").readText().contains("KmpSplashView.swift in Sources")
        )
    }
}
