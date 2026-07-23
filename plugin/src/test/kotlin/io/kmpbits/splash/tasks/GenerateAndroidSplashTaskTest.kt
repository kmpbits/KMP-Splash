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
}
