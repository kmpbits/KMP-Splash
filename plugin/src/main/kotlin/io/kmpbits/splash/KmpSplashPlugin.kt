package io.kmpbits.splash

import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
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

        val task = project.tasks.register("generateLaunchScreen", GenerateLaunchScreenTask::class.java) { t: GenerateLaunchScreenTask ->
            t.group = "kmp-splash"
            t.description = "Generates LaunchScreen.storyboard for iOS"

            t.backgroundColor.set(ext.backgroundColor)
            t.outputFile.set(project.rootProject.file("$iosPath/LaunchScreen.storyboard"))

            ext.logoFile.orNull?.let { logoPath ->
                val logoFile = project.file(logoPath)
                val resourceName = logoFile.nameWithoutExtension
                t.logoResourceName.set(resourceName)
                t.logoSourceFile.set(logoFile)
            }
        }

        // Hook into the Xcode build lifecycle when the Apple framework task is present.
        project.tasks.configureEach { t ->
            if (t.name == "embedAndSignAppleFrameworkForXcode") {
                t.dependsOn(task)
            }
        }
    }

    private fun registerAndroidTask(project: Project, ext: KmpSplashExtension) {
        val task = project.tasks.register("generateAndroidSplash", GenerateAndroidSplashTask::class.java) { t: GenerateAndroidSplashTask ->
            t.group = "kmp-splash"
            t.description = "Generates Android splash screen themes.xml"

            t.backgroundColor.set(ext.backgroundColor)
            t.backgroundColorNight.set(ext.backgroundColorNight)

            // Default to androidMain/res inside the applying module.
            t.resOutputDir.set(project.file("src/androidMain/res"))

            ext.logoFile.orNull?.let { logoPath ->
                val logoFile = project.file(logoPath)
                t.logoDrawableName.set(logoFile.nameWithoutExtension)
                t.logoSourceFile.set(logoFile)
            }
        }

        project.tasks.configureEach { t ->
            if (t.name == "preBuild") {
                t.dependsOn(task)
            }
        }
    }
}
