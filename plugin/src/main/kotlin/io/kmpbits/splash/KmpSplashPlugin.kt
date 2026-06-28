package io.kmpbits.splash

import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
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
                resourcePackage.set(composeResourcePackage(project))
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

    private fun composeResourcePackage(project: Project): String {
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

                resourcePackage.set(composeResourcePackage(project))

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

        // Defer sourcesets registration and AGP manipulation until after the user's splashScreen { }
        // block has been evaluated, so androidAppPath is reliably readable.
        project.afterEvaluate {
            if (ext.androidAppPath.isPresent) {
                // External androidApp mode: generate into build/ and register those generated
                // dirs into the androidApp project's AGP source sets — no source files touched.
                project.gradle.projectsEvaluated {
                    val androidDir = project.rootProject.file(ext.androidAppPath.get())
                    val androidProject = project.rootProject.allprojects.find { it.projectDir == androidDir }
                    if (androidProject == null) {
                        project.logger.warn(
                            "KmpSplash: no Gradle project found at '${ext.androidAppPath.get()}'. " +
                            "Run :generateAndroidSplash manually before building the Android app."
                        )
                        return@projectsEvaluated
                    }

                    wireAgpSources(
                        agpProject = androidProject,
                        task = task,
                        generatedResDir = generatedResDir,
                        generatedManifestDir = generatedManifestDir,
                        generatedKotlinDir = generatedKotlinDir,
                        registerKotlin = true,
                    )

                    androidProject.tasks.configureEach {
                        if (name == "preBuild") {
                            dependsOn(task)
                        }
                    }
                }
                return@afterEvaluate
            }

            // Classic KMP structure: register generated Kotlin sources in androidMain.
            project.extensions.configure(KotlinMultiplatformExtension::class.java) {
                sourceSets.matching { it.name == "androidMain" }.configureEach {
                    kotlin.srcDir(generatedKotlinDir)
                }
            }

            project.plugins.withId("com.android.base") {
                wireAgpSources(
                    agpProject = project,
                    task = task,
                    generatedResDir = generatedResDir,
                    generatedManifestDir = generatedManifestDir,
                    generatedKotlinDir = generatedKotlinDir,
                    registerKotlin = false,
                )
            }

            project.tasks.configureEach {
                if (name == "preBuild") {
                    dependsOn(task)
                }
            }
        }
    }

    /**
     * Registers the generated res, manifest (and optionally Kotlin) directories into [agpProject]'s
     * `main` AGP source set, and points the task's manifest input at the project's original manifest
     * so it can be patched into the build folder rather than in place.
     *
     * @param registerKotlin when true, the generated Kotlin dir is added to the AGP `main` source set
     *   (standalone Android app module). For classic KMP modules the Kotlin dir is registered via the
     *   Kotlin Multiplatform `androidMain` source set instead.
     */
    private fun wireAgpSources(
        agpProject: Project,
        task: org.gradle.api.tasks.TaskProvider<GenerateAndroidSplashTask>,
        generatedResDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
        generatedManifestDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
        generatedKotlinDir: org.gradle.api.provider.Provider<org.gradle.api.file.Directory>,
        registerKotlin: Boolean,
    ) {
        val android = agpProject.extensions.findByName("android") ?: return
        try {
            val sourceSets = android.javaClass.getMethod("getSourceSets").invoke(android) as org.gradle.api.NamedDomainObjectContainer<*>
            val main = sourceSets.getByName("main")

            // Capture original manifest before we redirect it, so the task patches a copy.
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

            // Add generated res
            val res = main.javaClass.getMethod("getRes").invoke(main)
            res.javaClass.getMethod("srcDir", Any::class.java).invoke(res, generatedResDir)

            // Redirect AGP's manifest to the generated (patched) copy in build/
            manifestObj.javaClass.getMethod("srcFile", Any::class.java)
                .invoke(manifestObj, generatedManifestDir.map { it.file("AndroidManifest.xml") })

            // For a standalone Android app module, register the generated Kotlin source via AGP.
            if (registerKotlin) {
                val javaSrc = main.javaClass.getMethod("getJava").invoke(main)
                javaSrc.javaClass.getMethod("srcDir", Any::class.java).invoke(javaSrc, generatedKotlinDir)
            }
        } catch (e: Exception) {
            agpProject.logger.warn(
                "KmpSplash: failed to wire generated splash sources into ${agpProject.path}: ${e.message}"
            )
        }
    }
}
