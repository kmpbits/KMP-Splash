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
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

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
        val out = outputFile.asFile.get()
        out.parentFile.mkdirs()

        out.writeText(
            LaunchScreenTemplate.generate(
                backgroundColor = backgroundColor.get(),
                logoResourceName = logoResourceName.orNull,
            )
        )
        logger.lifecycle("KmpSplash: wrote ${out.absolutePath}")

        patchInfoPlist(out.parentFile.resolve("Info.plist"))

        logoSourceFile.orNull?.asFile?.let { src ->
            if (src.exists()) {
                val xcassets = out.parentFile.resolve("Assets.xcassets/${logoResourceName.get()}.imageset")
                xcassets.mkdirs()
                src.copyTo(xcassets.resolve(src.name), overwrite = true)
                xcassets.resolve("Contents.json").writeText(contentsJson(src.name))
            }
        }
    }

    /**
     * Ensures Info.plist contains:
     *   <key>UILaunchStoryboardName</key>
     *   <string>LaunchScreen</string>
     *
     * If the key already exists with a different value it is updated.
     * If Info.plist doesn't exist the task logs a warning and skips.
     */
    private fun patchInfoPlist(plist: java.io.File) {
        if (!plist.exists()) {
            logger.warn("KmpSplash: Info.plist not found at ${plist.absolutePath} — skipping UILaunchStoryboardName patch")
            return
        }

        val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(plist)
        doc.documentElement.normalize()

        val rootDict = doc.documentElement
            .getElementsByTagName("dict")
            .item(0) as? Element
            ?: run {
                logger.warn("KmpSplash: could not find root <dict> in Info.plist — skipping patch")
                return
            }

        val children = rootDict.childNodes
        var keyIndex = -1
        for (i in 0 until children.length) {
            val node = children.item(i)
            if (node.nodeName == "key" && node.textContent.trim() == "UILaunchStoryboardName") {
                keyIndex = i
                break
            }
        }

        if (keyIndex >= 0) {
            // Key exists — update the sibling <string> value.
            for (i in keyIndex + 1 until children.length) {
                val sibling = children.item(i)
                if (sibling.nodeName == "string") {
                    sibling.textContent = "LaunchScreen"
                    break
                }
            }
        } else {
            // Key missing — append key+value pair to the dict.
            val keyEl = doc.createElement("key").also { it.textContent = "UILaunchStoryboardName" }
            val valueEl = doc.createElement("string").also { it.textContent = "LaunchScreen" }
            rootDict.appendChild(keyEl)
            rootDict.appendChild(valueEl)
        }

        val transformer = TransformerFactory.newInstance().newTransformer().apply {
            setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, "-//Apple//DTD PLIST 1.0//EN")
            setOutputProperty(OutputKeys.DOCTYPE_SYSTEM, "http://www.apple.com/DTDs/PropertyList-1.0.dtd")
            setOutputProperty(OutputKeys.INDENT, "yes")
            setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4")
            setOutputProperty(OutputKeys.ENCODING, "UTF-8")
        }
        transformer.transform(DOMSource(doc), StreamResult(plist))

        logger.lifecycle("KmpSplash: patched UILaunchStoryboardName in ${plist.absolutePath}")
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
