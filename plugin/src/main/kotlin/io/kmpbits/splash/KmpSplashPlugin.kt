package io.kmpbits.splash

import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

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
                xcassetsDir.set(
                    project.rootProject.layout.dir(
                        ext.iosProjectPath.map { iosPath ->
                            project.rootProject.file("$iosPath/Assets.xcassets")
                        }
                    )
                )
                splashConfigFile.set(
                    project.file("src/iosMain/kotlin/io/kmpbits/splash/SplashConfig.kt")
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
                logoSourceFile.set(
                    project.layout.file(ext.logoFile.map { project.file(it) })
                )
                logoResourceName.set(
                    ext.logoFile.map { project.file(it).nameWithoutExtension }
                )
            }
        )

        project.tasks.configureEach {
            if (name == "embedAndSignAppleFrameworkForXcode" || name.startsWith("compileKotlinIos")) {
                dependsOn(task)
            }
        }
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
