package io.kmpbits.splash.tasks

import io.kmpbits.splash.AppIconGenerator
import io.kmpbits.splash.ExitAnimation
import io.kmpbits.splash.template.AndroidSplashTemplate
import io.kmpbits.splash.toKotlinExpression
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
import java.io.File
import javax.imageio.ImageIO

/**
 * Task that generates Android-specific splash screen resources.
 *
 * This task:
 * 1. Generates `theme.xml` in `res/values` and `res/values-night`.
 * 2. Copies the logo drawable to `res/drawable` and `res/drawable-night`.
 * 3. Generates `SplashInit.kt` for Compose-side configuration.
 *
 * Manifest patching itself is handled separately by [PatchSplashManifestTask], which operates
 * directly on AGP's merged manifest artifact.
 */
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

    @get:Input
    @get:Optional
    abstract val exitAnimation: Property<ExitAnimation>

    /** Points to the generated res directory inside the build folder. */
    @get:OutputDirectory
    abstract val resOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        if (!backgroundColor.isPresent) {
            throw org.gradle.api.GradleException("KmpSplash: 'backgroundColor' is mandatory in splashScreen { ... } block.")
        }

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
            writeSplashLogo(src, drawableDir)
            logger.lifecycle("KmpSplash: wrote logo to $drawableDir")
        }

        logoNightSourceFile.orNull?.asFile?.let { src ->
            if (src.exists()) {
                val drawableNightDir = resDir.resolve("drawable-night").also { it.mkdirs() }
                writeSplashLogo(src, drawableNightDir)
                logger.lifecycle("KmpSplash: wrote night logo to $drawableNightDir")
            }
        }

        generateSplashConfig()
    }

    /**
     * Writes the splash logo into [destDir] as `ic_kmp_splash_logo.*`. Raster formats are
     * rasterized through [AppIconGenerator.renderSplashIcon] first, padding artwork drawn
     * edge-to-edge so Android's SplashScreen API icon mask (API 31+) doesn't crop it — see
     * [AppIconGenerator.SPLASH_ICON_CONTENT_SCALE]. Other formats (vector drawables, SVG, WebP)
     * can't be rasterized this way and are copied through unchanged.
     */
    private fun writeSplashLogo(src: File, destDir: File) {
        if (src.extension.lowercase() !in AppIconGenerator.SUPPORTED_LOGO_EXTENSIONS) {
            src.copyTo(destDir.resolve("ic_kmp_splash_logo.${src.extension}"), overwrite = true)
            return
        }

        val logo = ImageIO.read(src)
            ?: throw org.gradle.api.GradleException(
                "KmpSplash: could not decode logo file '${src.absolutePath}' as an image."
            )
        ImageIO.write(
            AppIconGenerator.renderSplashIcon(logo),
            "png",
            destDir.resolve("ic_kmp_splash_logo.png"),
        )
    }

    private fun generateSplashConfig() {
        val configFile = splashConfigFile.orNull?.asFile ?: return
        val hexColor = backgroundColor.get()
        val lightArgb = "0xFF${hexColor.trimStart('#').uppercase()}"
        val nightColor = backgroundColorNight.orNull
        val nightArgb = if (nightColor != null) "0xFF${nightColor.trimStart('#').uppercase()}" else null

        configFile.parentFile.mkdirs()

        val hasLogo = logoDrawableName.isPresent
        // Night logo uses the same drawable name in drawable-night/; Android's resource system
        // picks the right variant automatically, so no separate painter is needed.

        val bgColorLine = "        SplashDefaults.backgroundColor = Color($lightArgb)"
        val nightColorLine = if (nightArgb != null) {
            "\n        SplashDefaults.backgroundColorNight = Color($nightArgb)"
        } else ""

        val logoBlock = if (hasLogo) """
        val logoId = context!!.resources.getIdentifier("ic_kmp_splash_logo", "drawable", context!!.packageName)
        if (logoId != 0) SplashDefaults.logoPainter = @Composable { painterResource(logoId) }""" else ""

        val exitAnimLine = exitAnimation.orNull?.toKotlinExpression()
            ?.let { "\n        SplashDefaults.exitAnimation = $it" }
            ?: ""

        val painterImport = if (hasLogo) "\nimport androidx.compose.ui.res.painterResource" else ""

        configFile.writeText(
            """// Generated by kmp-splash — do not edit manually
package io.kmpbits.splash

import android.content.ContentProvider
import android.content.ContentValues
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color$painterImport

// internal keeps it out of the module's public API (and satisfies explicitApi mode);
// the JVM class stays public, so Android can still instantiate it from the manifest.
@Suppress("DEPRECATION")
internal class KmpSplashInitProvider : ContentProvider() {
    override fun onCreate(): Boolean {
$bgColorLine$nightColorLine$logoBlock$exitAnimLine
        return true
    }
    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?, sortOrder: String?) = null
    override fun getType(uri: Uri) = null
    override fun insert(uri: Uri, values: ContentValues?) = null
    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0
    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?) = 0
}
"""
        )
        logger.lifecycle("KmpSplash: wrote ${configFile.absolutePath}")
    }
}
