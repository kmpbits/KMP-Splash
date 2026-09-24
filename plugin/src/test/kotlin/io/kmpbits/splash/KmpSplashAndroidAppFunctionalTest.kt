package io.kmpbits.splash

import org.gradle.testkit.runner.GradleRunner
import org.junit.Assume.assumeNotNull
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Regression test for `androidAppPath` mode with AGP 9's built-in Kotlin: the app module applies
 * neither `org.jetbrains.kotlin.android` nor `org.jetbrains.kotlin.multiplatform`, so the
 * generated `SplashInit.kt` (which holds `KmpSplashInitProvider`) used to be silently left out of
 * compilation while the manifest still registered the provider.
 *
 * Needs an Android SDK; skipped when none is found.
 */
class KmpSplashAndroidAppFunctionalTest {
    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var projectDir: File

    private fun findAndroidSdk(): File? =
        listOfNotNull(
            System.getenv("ANDROID_HOME"),
            System.getenv("ANDROID_SDK_ROOT"),
            "${System.getProperty("user.home")}/Library/Android/sdk",
            "${System.getProperty("user.home")}/Android/Sdk",
        ).map(::File).firstOrNull { it.resolve("platforms").isDirectory }

    @Before
    fun setup() {
        val sdk = findAndroidSdk()
        assumeNotNull("Android SDK not found; skipping AGP functional test", sdk)
        projectDir = tempFolder.newFolder("project")
        File(projectDir, "local.properties").writeText("sdk.dir=${sdk!!.absolutePath}\n")
    }

    @Test
    fun `androidAppPath compiles generated SplashInit into an app module with built-in Kotlin`() {
        File(projectDir, "settings.gradle.kts").writeText("""
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
                }
            }
            dependencyResolutionManagement {
                repositories {
                    google()
                    mavenCentral()
                }
            }
            rootProject.name = "test-project"
            include(":shared", ":androidApp")
        """.trimIndent())

        File(projectDir, "build.gradle.kts").writeText("""
            plugins {
                id("com.android.application") apply false
                kotlin("multiplatform") apply false
                id("io.github.kmpbits.splash") apply false
            }
        """.trimIndent())

        File(projectDir, "shared").mkdirs()
        File(projectDir, "shared/build.gradle.kts").writeText("""
            plugins {
                kotlin("multiplatform")
                id("io.github.kmpbits.splash")
            }

            kotlin {
                jvm()
            }

            splashScreen {
                backgroundColor = io.kmpbits.splash.SplashColor.hex("#FFFFFF")
                androidAppPath = "androidApp"
            }
        """.trimIndent())

        // Deliberately no Kotlin plugin: AGP 9 compiles Kotlin itself (built-in Kotlin). Also
        // sorts before :shared alphabetically, hence evaluationDependsOn.
        File(projectDir, "androidApp/src/main/kotlin").mkdirs()
        File(projectDir, "androidApp/src/main/kotlin/App.kt").writeText("package app\n\nclass App\n")
        File(projectDir, "androidApp/src/main/AndroidManifest.xml").writeText(
            "<manifest xmlns:android=\"http://schemas.android.com/apk/res/android\"><application/></manifest>"
        )
        File(projectDir, "androidApp/build.gradle.kts").writeText("""
            plugins {
                id("com.android.application")
            }

            evaluationDependsOn(":shared")

            android {
                namespace = "app.test"
                compileSdk = 35
                defaultConfig {
                    applicationId = "app.test"
                    minSdk = 24
                }
            }
        """.trimIndent())

        // The generated SplashInit.kt imports androidx.compose.* / the runtime, which this
        // minimal app deliberately doesn't depend on, so compilation is expected to fail — but
        // only *because* SplashInit.kt was picked up. Before the fix it was never compiled at all.
        val result = GradleRunner.create()
            .withProjectDir(projectDir)
            .withGradleVersion("9.2.1")
            .withArguments(":androidApp:compileDebugKotlin", "--stacktrace")
            .withPluginClasspath(
                System.getProperty("kmpsplash.androidTestPluginClasspath")
                    .split(File.pathSeparator).map(::File)
            )
            .buildAndFail()

        println(result.output)
        assertTrue(
            result.output.contains(":shared:generateAndroidSplash"),
            "compileDebugKotlin must depend on generateAndroidSplash",
        )
        assertTrue(
            result.output.contains("SplashInit.kt"),
            "generated SplashInit.kt must be part of the app module's Kotlin compilation",
        )
    }
}
