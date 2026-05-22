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

    @Test
    fun `can run generateLaunchScreen task`() {
        val iosAppDir = File(projectDir, "iosApp")
        iosAppDir.mkdirs()
        val xcodeProjDir = File(projectDir, "iosApp.xcodeproj")
        xcodeProjDir.mkdirs()
        File(xcodeProjDir, "project.pbxproj").writeText("") // Dummy pbxproj

        File(iosAppDir, "Assets.xcassets").mkdirs()
        File(iosAppDir, "Info.plist").writeText("<?xml version=\"1.0\" encoding=\"UTF-8\"?><plist version=\"1.0\"><dict></dict></plist>")

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
}
