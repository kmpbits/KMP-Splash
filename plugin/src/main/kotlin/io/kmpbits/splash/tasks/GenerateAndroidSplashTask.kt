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

    /** Points to the androidMain/res directory of the consuming module. */
    @get:OutputDirectory
    abstract val resOutputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val resDir = resOutputDir.asFile.get()

        val valuesDir = resDir.resolve("values").also { it.mkdirs() }
        valuesDir.resolve("themes.xml").writeText(
            AndroidSplashTemplate.generateThemes(
                backgroundColor = backgroundColor.get(),
                logoDrawableName = logoDrawableName.orNull,
            )
        )
        logger.lifecycle("KmpSplash: wrote ${valuesDir.resolve("themes.xml").absolutePath}")

        backgroundColorNight.orNull?.let { nightColor ->
            val nightDir = resDir.resolve("values-night").also { it.mkdirs() }
            nightDir.resolve("themes.xml").writeText(
                AndroidSplashTemplate.generateNightThemes(
                    backgroundColorNight = nightColor,
                    logoDrawableName = logoDrawableName.orNull,
                )
            )
            logger.lifecycle("KmpSplash: wrote ${nightDir.resolve("themes.xml").absolutePath}")
        }

        logoSourceFile.orNull?.asFile?.let { src ->
            if (src.exists()) {
                val drawableDir = resDir.resolve("drawable").also { it.mkdirs() }
                src.copyTo(drawableDir.resolve(src.name), overwrite = true)
                logger.lifecycle("KmpSplash: copied logo to ${drawableDir.resolve(src.name).absolutePath}")
            }
        }
    }
}
