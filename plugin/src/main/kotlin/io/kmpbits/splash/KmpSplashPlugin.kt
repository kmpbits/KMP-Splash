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
                    project.layout.file(ext.logoFile.map { project.file(it) })
                )
                logoResourceName.set(
                    ext.logoFile.map { project.file(it).nameWithoutExtension }
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
        val task = project.tasks.register(
            "generateAndroidSplash",
            GenerateAndroidSplashTask::class.java,
            Action<GenerateAndroidSplashTask> {
                group = "kmp-splash"
                description = "Generates Android splash screen themes.xml"

                backgroundColor.set(ext.backgroundColor)
                backgroundColorNight.set(ext.backgroundColorNight)
                resOutputDir.set(project.file("src/androidMain/res"))
                logoSourceFile.set(
                    project.layout.file(ext.logoFile.map { project.file(it) })
                )
                logoDrawableName.set(
                    ext.logoFile.map { project.file(it).nameWithoutExtension }
                )
            }
        )

        project.tasks.configureEach {
            if (name == "preBuild") {
                dependsOn(task)
            }
        }
    }
}
