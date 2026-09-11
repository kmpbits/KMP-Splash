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
        task.swiftViewFile.set(File(project.projectDir, "iosApp/KmpSplashView.swift"))
        task.iosUi.set(io.kmpbits.splash.IosUi.Compose)
        return task
    }

    private val classicPbxproj = """
        // !${'$'}*UTF8*${'$'}!
        {
        /* Begin PBXBuildFile section */
        /* End PBXBuildFile section */
        /* Begin PBXFileReference section */
        		B310DBB52A7B8C1B00943F69 /* Info.plist */ = {isa = PBXFileReference; path = Info.plist; sourceTree = "<group>"; };
        /* End PBXFileReference section */
        /* Begin PBXSourcesBuildPhase section */
        		B310DBC02A7B8C1C00943F69 /* Sources */ = {
        			isa = PBXSourcesBuildPhase;
        			buildActionMask = 2147483647;
        			files = (
        			);
        			runOnlyForDeploymentPostprocessing = 0;
        		};
        /* End PBXSourcesBuildPhase section */
        }
    """.trimIndent()

    private fun swiftUiTask(project: Project, pbxprojText: String): GenerateLaunchScreenTask {
        val task = newTask(project)
        File(project.projectDir, "iosApp.xcodeproj").mkdirs()
        File(project.projectDir, "iosApp.xcodeproj/project.pbxproj").writeText(pbxprojText)
        File(project.projectDir, "iosApp/Info.plist").also { it.parentFile.mkdirs() }
            .writeText("<?xml version=\"1.0\"?><plist version=\"1.0\"><dict></dict></plist>")
        task.backgroundColor.set("#0000FF")
        task.iosUi.set(io.kmpbits.splash.IosUi.SwiftUI)
        task.exitAnimation.set(io.kmpbits.splash.ExitAnimation.None)
        return task
    }

    @Test
    fun `SwiftUI mode writes KmpSplashView-swift and skips SplashInit-kt`() {
        val project = ProjectBuilder.builder().build()
        val task = swiftUiTask(project, classicPbxproj)
        val logoFile = File(project.projectDir, "logo.png").also { writeTestLogo(it) }
        task.logoSourceFile.set(logoFile)
        task.logoFilePath.set("logo.png")
        task.logoResourceName.set("logo")
        task.exitAnimation.set(io.kmpbits.splash.ExitAnimation.FadeOut(300))
        task.generate()

        val swift = File(project.projectDir, "iosApp/KmpSplashView.swift")
        assertTrue(swift.exists(), "expected KmpSplashView.swift to be generated")
        assertTrue(swift.readText().contains("public struct SplashView"))
        assertTrue(swift.readText().contains("""Image("logo")"""))

        assertFalse(
            File(project.projectDir, "generated/SplashInit.kt").exists(),
            "SplashInit.kt must not be generated in SwiftUI mode",
        )
        assertTrue(File(project.projectDir, "iosApp/Assets.xcassets/SplashBackground.colorset").exists())
    }

    @Test
    fun `Compose mode still writes SplashInit-kt and no Swift file`() {
        val project = ProjectBuilder.builder().build()
        val task = newTask(project)
        File(project.projectDir, "iosApp.xcodeproj").mkdirs()
        File(project.projectDir, "iosApp.xcodeproj/project.pbxproj").writeText("")

        task.backgroundColor.set("#0000FF")
        task.generate()

        assertTrue(File(project.projectDir, "generated/SplashInit.kt").exists())
        assertFalse(File(project.projectDir, "iosApp/KmpSplashView.swift").exists())
    }

    @Test
    fun `classic pbxproj gets the Swift file into the Sources build phase`() {
        val project = ProjectBuilder.builder().build()
        val task = swiftUiTask(project, classicPbxproj)
        task.generate()

        val pbx = File(project.projectDir, "iosApp.xcodeproj/project.pbxproj").readText()
        assertTrue(pbx.contains("KmpSplashView.swift in Sources"))
        assertTrue(pbx.contains("sourcecode.swift"))
        assertTrue(pbx.contains("B310DBF32A7B8C1C00943F70 /* KmpSplashView.swift in Sources */,"))
    }

    @Test
    fun `pbxproj patch is idempotent`() {
        val project = ProjectBuilder.builder().build()
        val task = swiftUiTask(project, classicPbxproj)
        task.generate()
        val once = File(project.projectDir, "iosApp.xcodeproj/project.pbxproj").readText()
        task.generate()
        val twice = File(project.projectDir, "iosApp.xcodeproj/project.pbxproj").readText()
        assertEquals(once, twice)
    }

    @Test
    fun `synchronized-group pbxproj is left untouched`() {
        val project = ProjectBuilder.builder().build()
        val sync = classicPbxproj +
            "\n\t\tB310DBF42A7B8C1C00943F71 /* iosApp */ = {isa = PBXFileSystemSynchronizedRootGroup; path = iosApp; sourceTree = \"<group>\"; };"
        val task = swiftUiTask(project, sync)
        task.generate()

        val pbx = File(project.projectDir, "iosApp.xcodeproj/project.pbxproj").readText()
        assertFalse(pbx.contains("KmpSplashView.swift"))
        assertTrue(File(project.projectDir, "iosApp/KmpSplashView.swift").exists())
    }

    @Test
    fun `missing Sources phase warns instead of failing`() {
        val project = ProjectBuilder.builder().build()
        val task = swiftUiTask(project, "{ /* no sources phase */ }")
        task.generate()
        assertTrue(File(project.projectDir, "iosApp/KmpSplashView.swift").exists())
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
