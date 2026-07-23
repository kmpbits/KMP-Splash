package io.kmpbits.splash.tasks

import io.kmpbits.splash.AppIconGenerator
import io.kmpbits.splash.template.AndroidSplashTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Task that generates the Android adaptive app icon (background color + foreground drawable)
 * and legacy square/round PNG fallbacks for API < 26, from the splash screen's own `logo` and
 * `backgroundColor`.
 *
 * Always registered (see [io.kmpbits.splash.KmpSplashPlugin]); no-ops when [enabled] is false,
 * following the same "task always registers, guards on presence" convention as
 * [GenerateAndroidSplashTask]'s optional inputs.
 */
abstract class GenerateAppIconTask : DefaultTask() {

    @get:Input
    val enabled: Property<Boolean> = project.objects.property(Boolean::class.java)

    @get:Input
    @get:Optional
    abstract val backgroundColor: Property<String>

    @get:InputFile
    @get:Optional
    abstract val logoSourceFile: RegularFileProperty

    @get:OutputDirectory
    abstract val resOutputDir: DirectoryProperty

    private val supportedExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp")

    @TaskAction
    fun generate() {
        if (!enabled.getOrElse(false)) return

        val logoFile = logoSourceFile.orNull?.asFile
            ?: throw org.gradle.api.GradleException(
                "KmpSplash: 'generateAppIcon' is enabled but no 'logo' is set in splashScreen { ... }. " +
                "An app icon requires a logo to derive its foreground from."
            )

        val extension = logoFile.extension.lowercase()
        if (extension !in supportedExtensions) {
            throw org.gradle.api.GradleException(
                "KmpSplash: 'generateAppIcon' requires logo '${logoFile.name}' to be one of " +
                "${supportedExtensions.joinToString(", ")}. Vector formats (.svg, Android .xml) " +
                "and WebP can't be rasterized for app icon generation."
            )
        }

        val logo = ImageIO.read(logoFile)
            ?: throw org.gradle.api.GradleException(
                "KmpSplash: could not decode logo file '${logoFile.absolutePath}' as an image."
            )

        val hexColor = backgroundColor.get().trimStart('#')
        val background = Color(
            hexColor.substring(0, 2).toInt(16),
            hexColor.substring(2, 4).toInt(16),
            hexColor.substring(4, 6).toInt(16),
        )

        checkUpscaling(logo)

        val resDir = resOutputDir.asFile.get()

        val anydpiDir = resDir.resolve("mipmap-anydpi-v26").also { it.mkdirs() }
        val adaptiveIconXml = AndroidSplashTemplate.generateAdaptiveIconXml()
        anydpiDir.resolve("ic_kmp_app_icon.xml").writeText(adaptiveIconXml)
        anydpiDir.resolve("ic_kmp_app_icon_round.xml").writeText(adaptiveIconXml)

        val valuesDir = resDir.resolve("values").also { it.mkdirs() }
        valuesDir.resolve("kmp_app_icon.xml").writeText(
            AndroidSplashTemplate.generateAppIconColor(backgroundColor.get())
        )

        for ((density, legacySize) in AppIconGenerator.LEGACY_DENSITIES) {
            val foregroundSize = AppIconGenerator.FOREGROUND_DENSITIES.getValue(density)
            val mipmapDir = resDir.resolve("mipmap-$density").also { it.mkdirs() }

            ImageIO.write(
                AppIconGenerator.renderForeground(logo, foregroundSize),
                "png",
                mipmapDir.resolve("ic_kmp_app_icon_foreground.png"),
            )
            ImageIO.write(
                AppIconGenerator.renderLegacy(logo, background, legacySize, round = false),
                "png",
                mipmapDir.resolve("ic_kmp_app_icon.png"),
            )
            ImageIO.write(
                AppIconGenerator.renderLegacy(logo, background, legacySize, round = true),
                "png",
                mipmapDir.resolve("ic_kmp_app_icon_round.png"),
            )
        }

        logger.lifecycle("KmpSplash: wrote app icon resources to ${resDir.absolutePath}")
    }

    private fun checkUpscaling(logo: BufferedImage) {
        val trimmed = AppIconGenerator.trimTransparentBorder(logo)
        val smallerDimension = minOf(trimmed.width, trimmed.height)
        if (AppIconGenerator.needsUpscalingWarning(smallerDimension)) {
            logger.warn(
                "KmpSplash: logo's trimmed content is ${trimmed.width}x${trimmed.height}px, smaller than " +
                "ideal for the largest app icon density and will be upscaled. Consider a higher-resolution logo."
            )
        }
    }
}
