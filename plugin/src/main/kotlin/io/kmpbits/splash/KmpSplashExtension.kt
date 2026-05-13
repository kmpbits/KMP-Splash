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
 *     logoFile = "logo.png"
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
     * Name of the logo file (with extension) located in `src/commonMain/composeResources/drawable/`.
     * e.g. "logo.png".
     */
    @get:Input
    @get:Optional
    abstract val logoFile: Property<String>

    /**
     * Optional logo for dark mode. If set, this file will be used when the system is in dark mode.
     * Must be located in `src/commonMain/composeResources/drawable/`.
     */
    @get:Input
    @get:Optional
    abstract val logoFileNight: Property<String>

    /**
     * Path to the Xcode project folder that contains `Info.plist`.
     * Defaults to `"iosApp/iosApp"`.
     */
    @get:Input
    abstract val iosProjectPath: Property<String>
}
