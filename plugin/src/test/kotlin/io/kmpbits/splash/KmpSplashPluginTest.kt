package io.kmpbits.splash

import org.gradle.testfixtures.ProjectBuilder
import kotlin.test.Test
import kotlin.test.assertNotNull

class KmpSplashPluginTest {
    @Test
    fun `plugin registers splashScreen extension`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("io.github.kmpbits.splash")

        assertNotNull(project.extensions.findByName("splashScreen"))
    }

    @Test
    fun `plugin registers generateLaunchScreen task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("io.github.kmpbits.splash")

        assertNotNull(project.tasks.findByName("generateLaunchScreen"))
    }
}
