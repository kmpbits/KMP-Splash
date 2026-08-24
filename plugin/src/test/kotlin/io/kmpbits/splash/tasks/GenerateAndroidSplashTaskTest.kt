package io.kmpbits.splash.tasks

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class GenerateAndroidSplashTaskTest {

    @Test
    fun `generated provider is internal so consumers with explicitApi compile`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateTest", GenerateAndroidSplashTask::class.java)

        val configFile = File(project.projectDir, "generated/SplashInit.kt")
        task.backgroundColor.set("#FFFFFF")
        task.splashConfigFile.set(configFile)
        task.resOutputDir.set(File(project.projectDir, "generated/res"))
        task.generate()

        val result = configFile.readText()
        assertTrue(
            result.contains("internal class KmpSplashInitProvider"),
            "expected the generated provider to be internal (explicitApi mode rejects " +
                "declarations without an explicit visibility modifier), but was:\n$result"
        )
    }

    @Test
    fun `postSplashTheme unset generates the default fallback Theme App`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateTest", GenerateAndroidSplashTask::class.java)

        val resDir = File(project.projectDir, "generated/res")
        task.backgroundColor.set("#FFFFFF")
        task.resOutputDir.set(resDir)
        task.generate()

        val theme = resDir.resolve("values/theme.xml").readText()
        assertTrue(theme.contains("""<item name="postSplashScreenTheme">@style/Theme.App</item>"""))
        assertTrue(theme.contains("""<style name="Theme.App" parent="android:Theme.Material.NoActionBar"/>"""))
    }

    @Test
    fun `postSplashTheme set points postSplashScreenTheme at the consumer's own theme`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateTest", GenerateAndroidSplashTask::class.java)

        val resDir = File(project.projectDir, "generated/res")
        task.backgroundColor.set("#FFFFFF")
        task.postSplashTheme.set("@style/Theme.MyApp")
        task.resOutputDir.set(resDir)
        task.generate()

        val theme = resDir.resolve("values/theme.xml").readText()
        assertTrue(theme.contains("""<item name="postSplashScreenTheme">@style/Theme.MyApp</item>"""))
        assertTrue(
            !theme.contains("Theme.App\""),
            "expected the unused fallback Theme.App style to be omitted when postSplashTheme is set, but was:\n$theme"
        )
    }
}
