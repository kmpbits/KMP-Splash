package io.kmpbits.splash.tasks

import io.kmpbits.splash.template.AndroidSplashTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * AGP `MERGED_MANIFEST` transform task for `androidAppPath` mode.
 *
 * Patches the androidApp module's already-merged manifest to set the splash theme on
 * `<application>` and inject the `KmpSplashInitProvider` `<provider>` entry, reusing the same
 * [AndroidSplashTemplate.generateManifest] logic classic mode already applies to its own
 * manifest. Because this runs on the already-merged manifest (not a pre-merge library manifest),
 * it deterministically overwrites any existing `android:theme` on `<application>` instead of
 * risking an AGP manifest-merge conflict.
 */
abstract class PatchAndroidAppManifestTask : DefaultTask() {

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @TaskAction
    fun patch() {
        val patched = AndroidSplashTemplate.generateManifest(mergedManifest.asFile.get().readText())
        val outputFile = updatedManifest.asFile.get()
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(patched)
    }
}
