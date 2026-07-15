package io.kmpbits.splash

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import kotlin.test.Test
import kotlin.test.assertEquals
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

    /** A project with kotlin-multiplatform, org.jetbrains.compose and this plugin already applied. */
    private fun composeEnabledProject(): Pair<Project, ResourcesExtension> {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("org.jetbrains.compose")
        project.plugins.apply("io.github.kmpbits.splash")

        val resources = project.extensions.getByType(ComposeExtension::class.java)
            .extensions.getByType(ResourcesExtension::class.java)
        return project to resources
    }

    private fun Project.generateLaunchScreenTask() =
        tasks.getByName("generateLaunchScreen") as io.kmpbits.splash.tasks.GenerateLaunchScreenTask

    @Test
    fun `resourcePackage resolves the configured compose packageOfResClass`() {
        val (project, resources) = composeEnabledProject()
        resources.packageOfResClass = "com.example.custom.generated.resources"

        assertEquals("com.example.custom.generated.resources", project.generateLaunchScreenTask().resourcePackage.get())
    }

    @Test
    fun `resourcePackage DSL override wins over compose packageOfResClass`() {
        val (project, resources) = composeEnabledProject()
        resources.packageOfResClass = "com.example.fromcompose.generated.resources"

        val ext = project.extensions.getByType(KmpSplashExtension::class.java)
        ext.resourcePackage.set("com.example.fromoverride.generated.resources")

        assertEquals("com.example.fromoverride.generated.resources", project.generateLaunchScreenTask().resourcePackage.get())
    }

    @Test
    fun `resourcePackage falls back to Compose's own default formula`() {
        // Custom name/group needed before the plugins are applied, so this can't reuse
        // composeEnabledProject() as-is.
        val project = ProjectBuilder.builder().withName("my-module").build()
        project.group = "com.example"
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("org.jetbrains.compose")
        project.plugins.apply("io.github.kmpbits.splash")

        // Compose's own `asUnderscoredIdentifier()` (compose-gradle-plugin's
        // GenerateResourceAccessorsTask.kt) only replaces '-' with '_' (and prefixes a leading
        // digit with '_') — it doesn't touch '.', so a dotted group like "com.example" is passed
        // through as-is; only the hyphenated module name gets underscored.
        assertEquals("com.example.my_module.generated.resources", project.generateLaunchScreenTask().resourcePackage.get())
    }
}
