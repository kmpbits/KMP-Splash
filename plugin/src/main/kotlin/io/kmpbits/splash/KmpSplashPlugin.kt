package io.kmpbits.splash

import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

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

                backgroundColor.set(ext.backgroundColor)
                backgroundColorNight.set(ext.backgroundColorNight)

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
                logoFilePath.set(ext.logoFile)
                logoSourceFile.set(
                    project.layout.file(ext.logoFile.map { fileName ->
                        project.file("src/commonMain/composeResources/drawable/$fileName")
                    })
                )
                logoResourceName.set(
                    ext.logoFile.map { fileName ->
                        fileName.substringBeforeLast('.')
                    }
                )
                logoNightFilePath.set(ext.logoFileNight)
                logoNightSourceFile.set(
                    project.layout.file(ext.logoFileNight.map { fileName ->
                        project.file("src/commonMain/composeResources/drawable/$fileName")
                    })
                )
                logoNightResourceName.set(
                    ext.logoFileNight.map { fileName ->
                        fileName.substringBeforeLast('.')
                    }
                )
                resourcePackage.set(composeResourcePackage(project))
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

                backgroundColor.set(ext.backgroundColor)
                backgroundColorNight.set(ext.backgroundColorNight)
                resOutputDir.set(generatedResDir)
                splashConfigFile.set(generatedKotlinDir.map { it.file("io/kmpbits/splash/SplashInit.kt") })
                manifestFile.set(generatedManifestDir.map { it.file("AndroidManifest.xml") })
                resourcePackage.set(composeResourcePackage(project))

                logoSourceFile.set(
                    project.layout.file(ext.logoFile.map { fileName ->
                        project.file("src/commonMain/composeResources/drawable/$fileName")
                    })
                )
                logoDrawableName.set(
                    ext.logoFile.map { fileName ->
                        fileName.substringBeforeLast('.')
                    }
                )
                logoNightSourceFile.set(
                    project.layout.file(ext.logoFileNight.map { fileName ->
                        project.file("src/commonMain/composeResources/drawable/$fileName")
                    })
                )
            }
        )

        project.extensions.configure(KotlinMultiplatformExtension::class.java) {
            sourceSets.matching { it.name == "androidMain" }.configureEach {
                kotlin.srcDir(generatedKotlinDir)
            }
        }

        project.plugins.withId("com.android.base") {
            val android = project.extensions.findByName("android")
            if (android != null) {
                try {
                    val sourceSets = android.javaClass.getMethod("getSourceSets").invoke(android) as org.gradle.api.NamedDomainObjectContainer<*>
                    val main = sourceSets.getByName("main")
                    
                    // Capture original manifest before we redirect it
                    val getManifest = main.javaClass.getMethod("getManifest")
                    val manifestObj = getManifest.invoke(main)
                    val getSrcFile = manifestObj.javaClass.getMethod("getSrcFile")
                    val originalManifest = getSrcFile.invoke(manifestObj) as java.io.File
                    
                    val manifestToUse = if (originalManifest.exists()) {
                        originalManifest
                    } else {
                        project.file("src/androidMain/AndroidManifest.xml").takeIf { it.exists() }
                    }

                    if (manifestToUse != null) {
                        task.configure {
                            inputManifestFile.set(manifestToUse)
                        }
                    }

                    // Add generated res
                    val res = main.javaClass.getMethod("getRes").invoke(main)
                    res.javaClass.getMethod("srcDir", Any::class.java).invoke(res, generatedResDir)

                    // Add generated manifest (replaces original in AGP's view, so we copied it in the task)
                    val srcFile = manifestObj.javaClass.getMethod("srcFile", Any::class.java)
                    srcFile.invoke(manifestObj, generatedManifestDir.map { it.file("AndroidManifest.xml") })
                } catch (_: Exception) {
                }
            }
        }

        project.tasks.configureEach {
            if (name == "preBuild") {
                dependsOn(task)
            }
        }
    }
}
