package io.kmpbits.splash.tasks

import io.kmpbits.splash.template.AndroidSplashTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateAndroidSplashTask : DefaultTask() {

    @get:Input
    abstract val backgroundColor: Property<String>

    @get:Input
    @get:Optional
    abstract val backgroundColorNight: Property<String>

    @get:Input
    @get:Optional
    abstract val logoDrawableName: Property<String>

    @get:InputFile
    @get:Optional
    abstract val logoSourceFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    abstract val logoNightSourceFile: RegularFileProperty

    /** Compose resources package of the consuming module, e.g. com_example.myapp.generated.resources */
    @get:Input
    @get:Optional
    abstract val resourcePackage: Property<String>

    @get:OutputFile
    @get:Optional
    abstract val splashConfigFile: RegularFileProperty

    @get:OutputFile
    @get:Optional
    abstract val manifestFile: RegularFileProperty

    @get:InputFile
    @get:Optional
    @get:org.gradle.api.tasks.PathSensitive(org.gradle.api.tasks.PathSensitivity.RELATIVE)
    abstract val inputManifestFile: RegularFileProperty

    /** Points to the androidMain/res directory of the consuming module. */
    @get:OutputDirectory
    abstract val resOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val resDir = resOutputDir.asFile.get()
        val androidLogoName = logoDrawableName.orNull?.let { "ic_kmp_splash_logo" }

        val valuesDir = resDir.resolve("values").also { it.mkdirs() }
        valuesDir.resolve("theme.xml").writeText(
            AndroidSplashTemplate.generateThemes(
                backgroundColor = backgroundColor.get(),
                logoDrawableName = androidLogoName,
            )
        )
        logger.lifecycle("KmpSplash: wrote ${valuesDir.resolve("theme.xml").absolutePath}")

        backgroundColorNight.orNull?.let { nightColor ->
            val nightDir = resDir.resolve("values-night").also { it.mkdirs() }
            nightDir.resolve("theme.xml").writeText(
                AndroidSplashTemplate.generateNightThemes(
                    backgroundColorNight = nightColor,
                    logoDrawableName = androidLogoName,
                )
            )
            logger.lifecycle("KmpSplash: wrote ${nightDir.resolve("theme.xml").absolutePath}")
        }

        logoSourceFile.orNull?.asFile?.let { src ->
            if (!src.exists()) {
                val configuredName = logoDrawableName.get()
                throw org.gradle.api.GradleException(
                    "KmpSplash: logoFile '$configuredName' was not found at ${src.absolutePath}.\n" +
                    "  The file must be located in 'src/commonMain/composeResources/drawable/'."
                )
            }
            val drawableDir = resDir.resolve("drawable").also { it.mkdirs() }
            val destFile = drawableDir.resolve("ic_kmp_splash_logo.${src.extension}")
            src.copyTo(destFile, overwrite = true)
            logger.lifecycle("KmpSplash: copied logo to ${destFile.absolutePath}")
        }

        logoNightSourceFile.orNull?.asFile?.let { src ->
            if (src.exists()) {
                val drawableNightDir = resDir.resolve("drawable-night").also { it.mkdirs() }
                val destFile = drawableNightDir.resolve("ic_kmp_splash_logo.${src.extension}")
                src.copyTo(destFile, overwrite = true)
                logger.lifecycle("KmpSplash: copied night logo to ${destFile.absolutePath}")
            }
        }

        generateManifest()
        generateSplashConfig()
    }

    private fun generateManifest() {
        val file = manifestFile.orNull?.asFile ?: return
        val baseContent = inputManifestFile.orNull?.asFile?.takeIf { it.exists() }?.readText()
        
        file.parentFile.mkdirs()
        file.writeText(AndroidSplashTemplate.generateManifest(baseContent))
        logger.lifecycle("KmpSplash: wrote ${file.absolutePath}")
    }

    private fun generateSplashConfig() {
        val configFile = splashConfigFile.orNull?.asFile ?: return
        val hexColor = backgroundColor.get()
        val lightArgb = "0xFF${hexColor.trimStart('#').uppercase()}"
        val nightColor = backgroundColorNight.orNull
        val nightArgb = if (nightColor != null) "0xFF${nightColor.trimStart('#').uppercase()}" else null

        configFile.parentFile.mkdirs()

        val resPkg = resourcePackage.orNull
        val logoName = logoDrawableName.orNull
        val logoNightSource = logoNightSourceFile.orNull?.asFile
        val logoNightName = if (logoNightSource?.exists() == true) logoNightSource.nameWithoutExtension else null

        val logoImports = mutableListOf<String>()
        if (resPkg != null) {
            logoImports.add("import $resPkg.Res")
            if (logoName != null) logoImports.add("import $resPkg.$logoName")
            if (logoNightName != null && logoNightName != logoName) logoImports.add("import $resPkg.$logoNightName")
            if (logoName != null || logoNightName != null) logoImports.add("import org.jetbrains.compose.resources.painterResource")
        }
        val logoImportsStr = if (logoImports.isNotEmpty()) logoImports.joinToString("\n", prefix = "\n") else ""

        val bgColorLine = "    SplashDefaults.backgroundColor = Color($lightArgb)"
        val nightColorLine = if (nightArgb != null) {
            "\n    SplashDefaults.backgroundColorNight = Color($nightArgb)"
        } else ""

        val logoLine = if (logoName != null && resPkg != null) {
            "\n    SplashDefaults.logoPainter = @Composable { painterResource(Res.drawable.$logoName) }"
        } else ""

        val logoNightLine = if (logoNightName != null && resPkg != null) {
            "\n    SplashDefaults.logoPainterNight = @Composable { painterResource(Res.drawable.$logoNightName) }"
        } else ""

        configFile.writeText(
            """// Generated by kmp-splash — do not edit manually
package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color$logoImportsStr

@Suppress("DEPRECATION")
private val _kmpSplashInit: Unit = run {
$bgColorLine$nightColorLine$logoLine$logoNightLine
}
"""
        )
        logger.lifecycle("KmpSplash: wrote ${configFile.absolutePath}")
    }
}
