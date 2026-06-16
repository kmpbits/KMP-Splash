package io.kmpbits.splash

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

/**
 * Extension for configuring the KMP Splash screen.
 *
 * Example usage:
 * ```kotlin
 * splashScreen {
 *     backgroundColor = SplashColor.hex("#FFFFFF")
 *     backgroundColorNight = SplashColor.rgb(26, 26, 46)
 *     logo = SplashLogo.resource("logo.png")
 * }
 * ```
 */
abstract class KmpSplashExtension {

    /** Background color for light mode. Mandatory. */
    @get:Input
    abstract val backgroundColor: Property<SplashColor>

    /**
     * Optional dark-mode background color. When set, a `values-night/themes.xml` is
     * generated for Android and iOS will use it when the system is in dark mode.
     */
    @get:Input
    @get:Optional
    abstract val backgroundColorNight: Property<SplashColor>

    /**
     * Logo for light mode. Use [SplashLogo.resource] for files in composeResources/drawable,
     * or [SplashLogo.path] for a custom path relative to the module.
     */
    @get:Input
    @get:Optional
    abstract val logo: Property<SplashLogo>

    /**
     * Optional logo for dark mode. If set, this file will be used when the system is in dark mode.
     */
    @get:Input
    @get:Optional
    abstract val logoDark: Property<SplashLogo>

    /**
     * Exit animation for the splash screen.
     *
     * Currently applied on **iOS only**. Android exit animation support is planned
     * for a future release.
     *
     * Defaults to [ExitAnimation.None].
     *
     * Example:
     * ```kotlin
     * exitAnimation = ExitAnimation.FadeOut(300)
     * ```
     */
    @get:Input
    @get:Optional
    abstract val exitAnimation: Property<ExitAnimation>

    /**
     * Path to the Xcode project folder that contains `Info.plist`.
     * Defaults to `"iosApp/iosApp"`.
     */
    @get:Input
    abstract val iosProjectPath: Property<String>
}
