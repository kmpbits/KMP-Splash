package io.kmpbits.splash.tasks

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PatchSplashManifestTaskTest {

    @Test
    fun `patch adds theme and provider, overwriting an existing theme attribute`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("patchTest", PatchSplashManifestTask::class.java)

        val input = File(project.projectDir, "input/AndroidManifest.xml").also {
            it.parentFile.mkdirs()
            it.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
                |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
                |    <application android:theme="@style/AppTheme">
                |    </application>
                |</manifest>
                """.trimMargin()
            )
        }
        val output = File(project.projectDir, "output/AndroidManifest.xml")

        task.mergedManifest.set(input)
        task.updatedManifest.set(output)
        task.patch()

        val result = output.readText()
        assertTrue(result.contains("Theme.App.SplashScreen"), "expected the splash theme to be applied")
        assertTrue(
            result.contains("io.kmpbits.splash.KmpSplashInitProvider"),
            "expected the provider entry to be injected"
        )
        assertTrue(!result.contains("@style/AppTheme"), "expected the app's own theme to be overwritten, not merged")
    }

    @Test
    fun `patch does not add icon attributes when iconEnabled is false`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("patchTest", PatchSplashManifestTask::class.java)

        val input = File(project.projectDir, "input/AndroidManifest.xml").also {
            it.parentFile.mkdirs()
            it.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
                |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
                |    <application>
                |    </application>
                |</manifest>
                """.trimMargin()
            )
        }
        val output = File(project.projectDir, "output/AndroidManifest.xml")

        task.mergedManifest.set(input)
        task.updatedManifest.set(output)
        task.iconEnabled.set(false)
        task.patch()

        assertTrue(!output.readText().contains("android:icon"), "expected no icon attribute when iconEnabled is false")
    }

    @Test
    fun `patch adds icon attributes, overwriting an existing icon attribute, when iconEnabled is true`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("patchTest", PatchSplashManifestTask::class.java)

        val input = File(project.projectDir, "input/AndroidManifest.xml").also {
            it.parentFile.mkdirs()
            it.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
                |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
                |    <application android:icon="@mipmap/ic_launcher">
                |    </application>
                |</manifest>
                """.trimMargin()
            )
        }
        val output = File(project.projectDir, "output/AndroidManifest.xml")

        task.mergedManifest.set(input)
        task.updatedManifest.set(output)
        task.iconEnabled.set(true)
        task.patch()

        val result = output.readText()
        assertTrue(
            result.contains("""android:icon="@mipmap/ic_kmp_app_icon""""),
            "expected the generated icon to be set, got:\n$result"
        )
        assertTrue(
            result.contains("""android:roundIcon="@mipmap/ic_kmp_app_icon_round""""),
            "expected the generated round icon to be set, got:\n$result"
        )
        assertTrue(!result.contains("@mipmap/ic_launcher"), "expected the app's own icon to be overwritten, not merged")
    }
}
