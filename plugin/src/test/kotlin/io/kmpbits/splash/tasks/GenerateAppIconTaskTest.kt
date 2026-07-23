package io.kmpbits.splash.tasks

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateAppIconTaskTest {

    private fun writeTestLogo(file: File) {
        file.parentFile.mkdirs()
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color.RED
        g.fillRect(10, 10, 20, 20)
        g.dispose()
        ImageIO.write(image, "png", file)
    }

    @Test
    fun `generates the full adaptive and legacy icon file tree when enabled`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        val logoFile = File(project.projectDir, "logo.png").also { writeTestLogo(it) }
        val resDir = File(project.projectDir, "generated/res")

        task.enabled.set(true)
        task.backgroundColor.set("#0000FF")
        task.logoSourceFile.set(logoFile)
        task.resOutputDir.set(resDir)
        task.generate()

        val adaptiveXml = File(resDir, "mipmap-anydpi-v26/ic_kmp_app_icon.xml")
        assertTrue(adaptiveXml.exists())
        assertTrue(adaptiveXml.readText().contains("@color/kmp_app_icon_background"))
        assertTrue(adaptiveXml.readText().contains("@mipmap/ic_kmp_app_icon_foreground"))
        assertTrue(File(resDir, "mipmap-anydpi-v26/ic_kmp_app_icon_round.xml").exists())

        val colorXml = File(resDir, "values/kmp_app_icon.xml")
        assertTrue(colorXml.exists())
        assertTrue(colorXml.readText().contains("#0000FF"))

        for (density in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) {
            assertTrue(
                File(resDir, "mipmap-$density/ic_kmp_app_icon_foreground.png").exists(),
                "missing foreground for $density"
            )
            assertTrue(
                File(resDir, "mipmap-$density/ic_kmp_app_icon.png").exists(),
                "missing legacy icon for $density"
            )
            assertTrue(
                File(resDir, "mipmap-$density/ic_kmp_app_icon_round.png").exists(),
                "missing legacy round icon for $density"
            )
        }
    }

    @Test
    fun `does nothing when disabled`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        val resDir = File(project.projectDir, "generated/res")
        task.enabled.set(false)
        task.resOutputDir.set(resDir)
        task.generate()

        assertFalse(resDir.exists(), "expected no output directory to be created when disabled")
    }

    @Test
    fun `fails with an actionable message when enabled but no logo is set`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        task.enabled.set(true)
        task.backgroundColor.set("#FFFFFF")
        task.resOutputDir.set(File(project.projectDir, "generated/res"))

        val error = assertFailsWith<GradleException> { task.generate() }
        assertTrue(error.message!!.contains("no 'logo' is set"), "expected an actionable message, got: ${error.message}")
    }

    @Test
    fun `fails with an actionable message for an unsupported logo format`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        val logoFile = File(project.projectDir, "logo.webp").also {
            it.parentFile.mkdirs()
            it.writeBytes(ByteArray(0))
        }

        task.enabled.set(true)
        task.backgroundColor.set("#FFFFFF")
        task.logoSourceFile.set(logoFile)
        task.resOutputDir.set(File(project.projectDir, "generated/res"))

        val error = assertFailsWith<GradleException> { task.generate() }
        assertTrue(
            error.message!!.contains("png, jpg, jpeg, gif, bmp"),
            "expected the supported formats to be listed, got: ${error.message}"
        )
    }
}
