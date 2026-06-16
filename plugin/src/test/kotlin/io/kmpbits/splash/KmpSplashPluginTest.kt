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

    @Test
    fun `ExitAnimation None toKotlinExpression returns null`() {
        kotlin.test.assertNull(ExitAnimation.None.toKotlinExpression())
    }

    @Test
    fun `ExitAnimation FadeOut toKotlinExpression returns correct expression`() {
        kotlin.test.assertEquals("ExitAnimation.FadeOut(300)", ExitAnimation.FadeOut(300).toKotlinExpression())
        kotlin.test.assertEquals("ExitAnimation.FadeOut(500)", ExitAnimation.FadeOut(500).toKotlinExpression())
    }

    @Test
    fun `ExitAnimation SlideUp toKotlinExpression returns correct expression`() {
        kotlin.test.assertEquals("ExitAnimation.SlideUp(400)", ExitAnimation.SlideUp(400).toKotlinExpression())
    }

    @Test
    fun `ExitAnimation SlideDown toKotlinExpression returns correct expression`() {
        kotlin.test.assertEquals("ExitAnimation.SlideDown(400)", ExitAnimation.SlideDown(400).toKotlinExpression())
    }
}
