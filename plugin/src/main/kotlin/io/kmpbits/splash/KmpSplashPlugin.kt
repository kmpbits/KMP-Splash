package io.kmpbits.splash

import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.Variant
import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
import io.kmpbits.splash.tasks.PatchSplashManifestTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.jetbrains.compose.ComposeExtension
import org.jetbrains.compose.resources.ResourcesExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

/**
 * Gradle plugin for configuring splash screens in Kotlin Multiplatform projects.
 *
 * This plugin automates the creation and configuration of native splash screens
 * for both Android and iOS, using a unified DSL.
 */
class KmpSplashPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("splashScreen", KmpSplashExtension::class.java)
        ext.iosProjectPath.convention("iosApp/iosApp")
        registerIosTask(project, ext)
        registerAndroidTask(project, ext)
    }

    private fun registerIosTask(project: Project, ext: KmpSplashExtension) {
        val task = project.tasks.register(
            "generateLaunchScreen",
            GenerateLaunchScreenTask::class.java,
            Action<GenerateLaunchScreenTask> {
                group = "kmp-splash"
                description = "Generates Assets.xcassets launch color and SplashConfig.kt for iOS"

                if (!ext.backgroundColor.isPresent) {
                    // This will be caught by the task, but we can set a dummy here to avoid Gradle validation errors
                    // if the task isn't actually run.
                }

                backgroundColor.set(ext.backgroundColor.map { it.hex })
                backgroundColorNight.set(ext.backgroundColorNight.map { it.hex })

                xcassetsDir.set(
                    project.rootProject.layout.dir(
                        ext.iosProjectPath.map { iosPath ->
                            project.rootProject.file("$iosPath/Assets.xcassets")
                        }
                    )
                )
                splashConfigFile.set(
                    project.layout.buildDirectory
                        .file("generated/kmpSplash/iosMain/kotlin/io/kmpbits/splash/SplashInit.kt")
                )
                pbxprojFile.set(
                    project.rootProject.layout.file(
                        ext.iosProjectPath.map { iosPath ->
                            val iosDir = project.rootProject.file(iosPath)
                            iosDir.parentFile
                                .listFiles()
                                ?.firstOrNull { it.name.endsWith(".xcodeproj") }
                                ?.resolve("project.pbxproj")
                                ?: error("KmpSplash: no .xcodeproj found next to $iosPath")
                        }
                    )
                )
                logoFilePath.set(ext.logo.map { it.fileName })
                logoSourceFile.set(
                    project.layout.file(ext.logo.map { logo ->
                        project.file(logo.resolvedPath())
                    })
                )
                logoResourceName.set(
                    ext.logo.map { it.fileName.substringAfterLast('/').substringBeforeLast('.') }
                )
                logoNightFilePath.set(ext.logoDark.map { it.fileName })
                logoNightSourceFile.set(
                    project.layout.file(ext.logoDark.map { logo ->
                        project.file(logo.resolvedPath())
                    })
                )
                logoNightResourceName.set(
                    ext.logoDark.map { it.fileName.substringAfterLast('/').substringBeforeLast('.') }
                )
                resourcePackage.set(composeResourcePackage(project, ext))
                exitAnimation.set(ext.exitAnimation)
            }
        )

        val generatedSrcDir = project.layout.buildDirectory
            .dir("generated/kmpSplash/iosMain/kotlin")

        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            sourceSets.matching { it.name == "iosMain" }.configureEach {
                kotlin.srcDir(generatedSrcDir)
            }
        }

        project.tasks.configureEach {
            if (name == "embedAndSignAppleFrameworkForXcode" || name.startsWith("compileKotlinIos")) {
                dependsOn(task)
            }
        }
    }

    /**
     * Resolves the Compose Multiplatform resource package, in priority order:
     * 1. `splashScreen { resourcePackage = "..." }`, if set.
     * 2. The real `compose { resources { packageOfResClass = "..." } }` value, if the Compose
     *    Resources extension is present and that value is non-empty.
     * 3. Compose's own default formula (`{group}.{module name}.generated.resources`), if the
     *    extension is present but packageOfResClass wasn't set.
     * 4. A legacy heuristic, only if the Compose Resources extension isn't found at all.
     *
     * Wrapped in [Project.provider] because `compose { resources { ... } }` in the consumer's
     * build script runs *after* this plugin's `apply()` returns — reading the extension eagerly
     * here would see its pre-configuration empty default.
     *
     * `compose-gradle-plugin` is only a `compileOnly` dependency of this plugin, so on projects
     * that never apply the Compose Gradle plugin, `ComposeExtension`/`ResourcesExtension` aren't
     * merely absent as extensions — their classes aren't on the runtime classpath at all, which
     * throws [NoClassDefFoundError] (a [LinkageError]) rather than returning null. Guard for that.
     */
    private fun composeResourcePackage(project: Project, ext: KmpSplashExtension): Provider<String> =
        project.provider {
            ext.resourcePackage.orNull?.takeIf { it.isNotBlank() } ?: run {
                val resourcesExt = try {
                    project.extensions.findByType(ComposeExtension::class.java)
                        ?.extensions?.findByType(ResourcesExtension::class.java)
                } catch (e: LinkageError) {
                    null
                }
                when {
                    resourcesExt == null -> legacyResourcePackageHeuristic(project)
                    resourcesExt.packageOfResClass.isNotEmpty() -> resourcesExt.packageOfResClass
                    else -> composeDefaultResourcePackage(project)
                }
            }
        }

    /** Replicates Compose Resources' own default package formula (`ResourcesDSL.kt`, internal there). */
    private fun composeDefaultResourcePackage(project: Project): String {
        fun String.asUnderscoredIdentifier() =
            replace('-', '_').let { if (it.isNotEmpty() && it.first().isDigit()) "_$it" else it }
        val groupName = project.group.toString().lowercase().asUnderscoredIdentifier()
        val moduleName = project.name.lowercase().asUnderscoredIdentifier()
        val id = if (groupName.isNotEmpty()) "$groupName.$moduleName" else moduleName
        return "$id.generated.resources"
    }

    /** Last-resort fallback when the Compose Resources extension isn't present at all. */
    private fun legacyResourcePackageHeuristic(project: Project): String {
        fun String.normalize() = replace(Regex("[^a-zA-Z0-9]"), "_").lowercase()
        val root = project.rootProject.name.normalize()
        val sub = project.path.trimStart(':').split(':').joinToString(".") { it.normalize() }
        return if (sub.isEmpty()) "$root.generated.resources" else "$root.$sub.generated.resources"
    }

    private fun registerAndroidTask(project: Project, ext: KmpSplashExtension) {
        val generatedResDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/res")
        val generatedKotlinDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/kotlin")
        val generatedManifestDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/manifest")

        val task = project.tasks.register(
            "generateAndroidSplash",
            GenerateAndroidSplashTask::class.java,
            Action<GenerateAndroidSplashTask> {
                group = "kmp-splash"
                description = "Generates Android splash screen theme.xml"

                backgroundColor.set(ext.backgroundColor.map { it.hex })
                backgroundColorNight.set(ext.backgroundColorNight.map { it.hex })

                // Everything is generated into the build folder; source files are never modified.
                resOutputDir.set(generatedResDir)
                splashConfigFile.set(generatedKotlinDir.map { it.file("io/kmpbits/splash/SplashInit.kt") })
                manifestFile.set(generatedManifestDir.map { it.file("AndroidManifest.xml") })

                resourcePackage.set(composeResourcePackage(project, ext))

                logoSourceFile.set(
                    project.layout.file(ext.logo.map { logo ->
                        project.file(logo.resolvedPath())
                    })
                )
                logoDrawableName.set(
                    ext.logo.map { it.fileName.substringAfterLast('/').substringBeforeLast('.') }
                )
                logoNightSourceFile.set(
                    project.layout.file(ext.logoDark.map { logo ->
                        project.file(logo.resolvedPath())
                    })
                )
                exitAnimation.set(ext.exitAnimation)
            }
        )

        project.afterEvaluate {
            // Register generated Kotlin sources in the KMP androidMain source set.
            project.extensions.configure(KotlinMultiplatformExtension::class.java) {
                sourceSets.matching { it.name == "androidMain" }.configureEach {
                    kotlin.srcDir(generatedKotlinDir)
                }
            }

            // Gradle 9 requires explicit task dependency for directories used as sources that
            // are not declared as @OutputDirectory on the generating task (splashConfigFile is
            // @OutputFile, so the parent kotlin dir has no implicit dependency).
            project.tasks.configureEach {
                if (name == "preBuild" || (name.startsWith("compile") && "AndroidMain" in name)) {
                    dependsOn(task)
                }
            }

            if (ext.androidAppPath.isPresent) {
                // androidAppPath mode: wire the generated res directory and a manifest patch
                // directly into the androidApp module's own variants, via AGP's public Variant
                // API. This is a documented, stable extension point — unlike the reflection this
                // plugin used before (broken by AGP 9's removal of getSrcFile()) or the doFirst
                // drawable-only copy hack that replaced it, which never covered values/manifest.
                val androidDir = project.rootProject.file(ext.androidAppPath.get())
                val androidProject = project.rootProject.allprojects.find { it.projectDir == androidDir }
                if (androidProject == null) {
                    project.logger.warn(
                        "KmpSplash: no Gradle project found at '${ext.androidAppPath.get()}'. " +
                        "Run :generateAndroidSplash manually before building the Android app."
                    )
                } else if (androidProject.state.executed) {
                    // Gradle evaluates subprojects in path-alphabetical order by default. If
                    // androidProject sorts before this project, it will already be fully evaluated
                    // by the time we get here — including AGP's own variant computation — and
                    // registering onVariants now would be silently ignored or throw AGP's cryptic
                    // "too late to add actions" exception. Fail loud with an actionable message
                    // instead of letting that happen.
                    project.logger.warn(
                        "KmpSplash: '${androidProject.path}' was already evaluated before " +
                        "'${project.path}' (this project), so splash resources/manifest could not " +
                        "be wired in. Add `evaluationDependsOn(\"${project.path}\")` to " +
                        "'${androidProject.path}'s build file to fix the evaluation order."
                    )
                } else {
                    androidProject.plugins.withId("com.android.application") {
                        val components = androidProject.extensions.getByType(
                            ApplicationAndroidComponentsExtension::class.java
                        )
                        components.onVariants { variant ->
                            wireVariantResourcesAndManifest(androidProject, variant, task)
                        }
                    }
                }
            } else {
                // Classic KMP library mode: wire the generated res into the shared module's AGP
                // source sets so the drawable is compiled into the library's AAR.
                project.plugins.withId("com.android.base") {
                    val android = project.extensions.findByName("android") ?: return@withId
                    try {
                        val sourceSets = android.javaClass.getMethod("getSourceSets")
                            .invoke(android) as org.gradle.api.NamedDomainObjectContainer<*>
                        val main = sourceSets.getByName("main")
                        val res = main.javaClass.getMethod("getRes").invoke(main)
                        res.javaClass.getMethod("srcDir", Any::class.java).invoke(res, generatedResDir)
                    } catch (e: Exception) {
                        project.logger.warn(
                            "KmpSplash: failed to wire res into ${project.path}: ${e.message}"
                        )
                    }
                }

                // Manifest redirect (best-effort).
                val android = project.extensions.findByName("android")
                if (android != null) {
                    tryWireManifest(android, task, generatedManifestDir, project)
                }

                project.tasks.configureEach {
                    if (name == "preBuild") dependsOn(task)
                }
            }
        }
    }

    /**
     * Attempts to redirect AGP's manifest to the generated (patched) copy in the build folder.
     * Best-effort: [getSrcFile] was removed in AGP 9, so this may log a warning and return.
     */
    private fun tryWireManifest(
        android: Any,
        task: org.gradle.api.tasks.TaskProvider<GenerateAndroidSplashTask>,
        generatedManifestDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
        agpProject: Project,
    ) {
        try {
            val sourceSets = android.javaClass.getMethod("getSourceSets").invoke(android)
                as org.gradle.api.NamedDomainObjectContainer<*>
            val main = sourceSets.getByName("main")
            val manifestObj = main.javaClass.getMethod("getManifest").invoke(main)
            val originalManifest = manifestObj.javaClass.getMethod("getSrcFile").invoke(manifestObj) as java.io.File

            val manifestToUse = if (originalManifest.exists()) {
                originalManifest
            } else {
                agpProject.file("src/androidMain/AndroidManifest.xml").takeIf { it.exists() }
            }

            if (manifestToUse != null) {
                task.configure {
                    inputManifestFile.set(manifestToUse)
                }
            }

            manifestObj.javaClass.getMethod("srcFile", Any::class.java)
                .invoke(manifestObj, generatedManifestDir.map { it.file("AndroidManifest.xml") })
        } catch (e: Exception) {
            agpProject.logger.warn(
                "KmpSplash: manifest redirect skipped for ${agpProject.path} (AGP API mismatch?): ${e.message}"
            )
        }
    }
}

/**
 * Wires the generated res directory and a manifest patch into [variant] via AGP's public
 * Variant API. Shared by `androidAppPath` mode (where [androidProject] is a separate resolved
 * project) and classic mode (where [androidProject] is the same project the plugin is applied
 * to) — the wiring itself is identical either way; only how the caller obtained [variant]
 * differs.
 *
 * Deliberately a top-level function rather than a member of [KmpSplashPlugin]: Gradle decorates
 * every Plugin implementation by reflecting over all of its declared methods (including private
 * ones), which eagerly resolves each method's parameter/return types. If this function were a
 * member and any consumer applied the plugin without AGP's Variant API on the classpath (e.g. an
 * iOS-only KMP module with no Android target), that reflection would throw
 * `NoClassDefFoundError: com/android/build/api/variant/Variant` and break plugin application
 * entirely — even for builds that never touch Android. As a top-level function it compiles into
 * a separate file-facade class that Gradle never decorates, so the type is only resolved when
 * this function actually runs (i.e. once AGP is already present and applying its plugin).
 */
private fun wireVariantResourcesAndManifest(
    androidProject: Project,
    variant: Variant,
    task: TaskProvider<GenerateAndroidSplashTask>,
) {
    val res = variant.sources.res
    if (res == null) {
        androidProject.logger.warn(
            "KmpSplash: variant '${variant.name}' has no res sources — " +
            "splash resources were not wired in. This is unexpected."
        )
    } else {
        res.addGeneratedSourceDirectory(
            task,
            GenerateAndroidSplashTask::resOutputDir,
        )
    }

    val patchManifest = androidProject.tasks.register(
        "patchKmpSplash${variant.name.replaceFirstChar { it.uppercase() }}Manifest",
        PatchSplashManifestTask::class.java,
    ) {
        group = "kmp-splash"
        description = "Patches the manifest with the splash theme and provider (${variant.name})"
    }
    variant.artifacts.use(patchManifest)
        .wiredWithFiles(
            PatchSplashManifestTask::mergedManifest,
            PatchSplashManifestTask::updatedManifest,
        )
        .toTransform(SingleArtifact.MERGED_MANIFEST)
}
