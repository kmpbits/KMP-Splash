package io.kmpbits.splash.tasks

import io.kmpbits.splash.template.AndroidSplashTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * AGP `MERGED_MANIFEST` transform task, used by both `androidAppPath` mode (patching a separate
 * androidApp module's manifest) and classic mode (patching the same module's own manifest).
 *
 * Patches the already-merged manifest to set the splash theme on `<application>` and inject the
 * `KmpSplashInitProvider` `<provider>` entry, via [AndroidSplashTemplate.generateManifest].
 * Because this runs on the already-merged manifest (not a pre-merge library manifest), it
 * deterministically overwrites any existing `android:theme` on `<application>` instead of risking
 * an AGP manifest-merge conflict. When [iconEnabled] is true, `android:icon`/`android:roundIcon`
 * are patched the same deterministic way.
 */
abstract class PatchSplashManifestTask : DefaultTask() {

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val iconEnabled: Property<Boolean>

    @TaskAction
    fun patch() {
        val patched = AndroidSplashTemplate.generateManifest(
            mergedManifest.asFile.get().readText(),
            iconEnabled.getOrElse(false),
        )
        val outputFile = updatedManifest.asFile.get()
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(patched)
    }
}
