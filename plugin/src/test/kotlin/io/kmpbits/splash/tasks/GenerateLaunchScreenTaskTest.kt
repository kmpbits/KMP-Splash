package io.kmpbits.splash.tasks

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateLaunchScreenTaskTest {

    private fun writeTestLogo(file: File) {
        file.parentFile.mkdirs()
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color.RED
        g.fillRect(10, 10, 20, 20)
        g.dispose()
        ImageIO.write(image, "png", file)
    }

    private fun newTask(project: Project): GenerateLaunchScreenTask {
        val task = project.tasks.create("generateLaunchScreenTest", GenerateLaunchScreenTask::class.java)
        task.xcassetsDir.set(File(project.projectDir, "iosApp/Assets.xcassets"))
        task.splashConfigFile.set(File(project.projectDir, "generated/SplashInit.kt"))
        task.pbxprojFile.set(File(project.projectDir, "iosApp.xcodeproj/project.pbxproj"))
        return task
    }

    @Test
    fun `generates a 1024x1024 opaque icon and single-size Contents-json when enabled`() {
        val project = ProjectBuilder.builder().build()
        val task = newTask(project)
        val logoFile = File(project.projectDir, "logo.png").also { writeTestLogo(it) }

        task.backgroundColor.set("#0000FF")
        task.logoSourceFile.set(logoFile)
        task.generateAppIcon.set(true)
        task.generate()

        val appiconset = File(project.projectDir, "iosApp/Assets.xcassets/AppIcon.appiconset")
        val iconFile = File(appiconset, "ic_kmp_app_icon.png")
        assertTrue(iconFile.exists())

        val icon = ImageIO.read(iconFile)
        assertEquals(1024, icon.width)
        assertEquals(1024, icon.height)
        assertFalse(icon.colorModel.hasAlpha(), "expected the generated icon to have no alpha channel")

        val contentsJson = File(appiconset, "Contents.json").readText()
        assertTrue(contentsJson.contains(""""size":"1024x1024""""))
        assertTrue(contentsJson.contains(""""filename":"ic_kmp_app_icon.png""""))
        assertTrue(contentsJson.contains(""""idiom":"universal""""))
    }

    @Test
    fun `leaves the empty placeholder untouched when disabled`() {
        val project = ProjectBuilder.builder().build()
        val task = newTask(project)

        task.backgroundColor.set("#0000FF")
        task.generateAppIcon.set(false)
        task.generate()

        val contentsJson = File(project.projectDir, "iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json")
        assertTrue(contentsJson.exists())
        assertEquals("""{"images":[],"info":{"author":"xcode","version":1}}""", contentsJson.readText())
    }

    @Test
    fun `fails with an actionable message when enabled but no logo is set`() {
        val project = ProjectBuilder.builder().build()
        val task = newTask(project)

        task.backgroundColor.set("#0000FF")
        task.generateAppIcon.set(true)

        val error = assertFailsWith<GradleException> { task.generate() }
        assertTrue(error.message!!.contains("no 'logo' is set"), "expected an actionable message, got: ${error.message}")
    }

    @Test
    fun `fails with an actionable message for an unsupported logo format`() {
        val project = ProjectBuilder.builder().build()
        val task = newTask(project)

        val logoFile = File(project.projectDir, "logo.webp").also {
            it.parentFile.mkdirs()
            it.writeBytes(ByteArray(0))
        }

        task.backgroundColor.set("#0000FF")
        task.logoSourceFile.set(logoFile)
        task.generateAppIcon.set(true)

        val error = assertFailsWith<GradleException> { task.generate() }
        assertTrue(
            error.message!!.contains("png, jpg, jpeg, gif, bmp"),
            "expected the supported formats to be listed, got: ${error.message}"
        )
    }
}
