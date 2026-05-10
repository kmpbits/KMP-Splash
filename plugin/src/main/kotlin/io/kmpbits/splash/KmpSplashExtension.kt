package io.kmpbits.splash

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

abstract class KmpSplashExtension {

    /** Background color in #RRGGBB format. **Mandatory**. */
    @get:Input
    abstract val backgroundColor: Property<String>

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

    /**
     * Optional dark-mode background color. When set, a `values-night/themes.xml` is
     * generated for Android. iOS ignores this (storyboard has no dark-mode support).
     */
    @get:Input
    @get:Optional
    abstract val backgroundColorNight: Property<String>
}
