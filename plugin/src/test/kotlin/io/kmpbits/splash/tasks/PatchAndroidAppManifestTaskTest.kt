package io.kmpbits.splash.tasks

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PatchAndroidAppManifestTaskTest {

    @Test
    fun `patch adds theme and provider, overwriting an existing theme attribute`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("patchTest", PatchAndroidAppManifestTask::class.java)

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
}
