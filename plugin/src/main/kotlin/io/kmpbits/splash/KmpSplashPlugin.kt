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
                // androidAppPath mode: copy the generated drawables into the app's static res dir
                // as a doFirst action on preBuild. Using doFirst (instead of a Copy task) avoids
                // Gradle 9's implicit-dependency validation, which triggers for every AGP task that
                // reads src/main/res when a Copy task declares it as an @OutputDirectory.
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

                    val appResDir = androidProject.file("src/main/res")
                    val srcResDir = generatedResDir

                    androidProject.tasks.configureEach {
                        if (name == "preBuild") {
                            dependsOn(task)
                            doFirst {
                                val srcRes = srcResDir.get().asFile
                                listOf("drawable", "drawable-night").forEach { dirName ->
                                    val src = srcRes.resolve(dirName)
                                    if (src.exists()) {
                                        val dst = appResDir.resolve(dirName)
                                        dst.mkdirs()
                                        src.listFiles()?.forEach { f ->
                                            f.copyTo(dst.resolve(f.name), overwrite = true)
                                        }
                                    }
                                }
                            }
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
