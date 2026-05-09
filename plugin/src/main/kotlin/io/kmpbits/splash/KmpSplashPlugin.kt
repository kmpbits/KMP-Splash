package io.kmpbits.splash

import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project

class KmpSplashPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val ext = project.extensions.create("splashScreen", KmpSplashExtension::class.java)

        project.afterEvaluate {
            registerIosTask(project, ext)
            registerAndroidTask(project, ext)
        }
    }

    private fun registerIosTask(project: Project, ext: KmpSplashExtension) {
        val iosPath = ext.iosProjectPath.orNull
            ?: error("KmpSplash: iosProjectPath must be set in the splashScreen { } block")

        val task = project.tasks.register(
            "generateLaunchScreen",
            GenerateLaunchScreenTask::class.java,
            Action<GenerateLaunchScreenTask> {
                group = "kmp-splash"
                description = "Generates LaunchScreen.storyboard and SplashConfig.kt for iOS"

                backgroundColor.set(ext.backgroundColor)
                outputFile.set(project.rootProject.file("$iosPath/LaunchScreen.storyboard"))
                splashConfigFile.set(
                    project.file("src/iosMain/kotlin/io/kmpbits/splash/SplashConfig.kt")
                )

                ext.logoFile.orNull?.let { logoPath ->
                    val logoFile = project.file(logoPath)
                    logoResourceName.set(logoFile.nameWithoutExtension)
                    logoSourceFile.set(logoFile)
                }
            }
        )

        project.tasks.configureEach {
            if (name == "embedAndSignAppleFrameworkForXcode") {
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

                ext.logoFile.orNull?.let { logoPath ->
                    val logoFile = project.file(logoPath)
                    logoDrawableName.set(logoFile.nameWithoutExtension)
                    logoSourceFile.set(logoFile)
                }
            }
        )

        project.tasks.configureEach {
            if (name == "preBuild") {
                dependsOn(task)
            }
        }
    }
}
