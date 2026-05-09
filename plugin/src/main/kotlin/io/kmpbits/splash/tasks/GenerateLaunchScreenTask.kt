package io.kmpbits.splash.tasks

import io.kmpbits.splash.template.LaunchScreenTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class GenerateLaunchScreenTask : DefaultTask() {

    @get:Input
    abstract val backgroundColor: Property<String>

    @get:Input
    @get:Optional
    abstract val logoResourceName: Property<String>

    @get:InputFile
    @get:Optional
    abstract val logoSourceFile: RegularFileProperty

    /** The storyboard destination, e.g. <root>/iosApp/iosApp/LaunchScreen.storyboard */
    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val storyboard = LaunchScreenTemplate.generate(
            backgroundColor = backgroundColor.get(),
            logoResourceName = logoResourceName.orNull,
        )

        val out = outputFile.asFile.get()
        out.parentFile.mkdirs()
        out.writeText(storyboard)

        logoSourceFile.orNull?.asFile?.let { src ->
            if (src.exists()) {
                val xcassets = out.parentFile.resolve("Assets.xcassets/${logoResourceName.get()}.imageset")
                xcassets.mkdirs()
                src.copyTo(xcassets.resolve(src.name), overwrite = true)
                xcassets.resolve("Contents.json").writeText(contentsJson(src.name))
            }
        }

        logger.lifecycle("KmpSplash: wrote ${out.absolutePath}")
    }

    private fun contentsJson(filename: String) = """{
  "images" : [
    { "idiom" : "universal", "filename" : "$filename", "scale" : "1x" },
    { "idiom" : "universal", "scale" : "2x" },
    { "idiom" : "universal", "scale" : "3x" }
  ],
  "info" : { "author" : "kmp-splash", "version" : 1 }
}"""
}
