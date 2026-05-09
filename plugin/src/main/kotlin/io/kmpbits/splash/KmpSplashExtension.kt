package io.kmpbits.splash

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional

abstract class KmpSplashExtension {

    /** Background color in #RRGGBB format. Defaults to #FFFFFF with a build warning if not set. */
    @get:Input
    @get:Optional
    abstract val backgroundColor: Property<String>

    /**
     * Path to the logo relative to the module root, e.g. "drawable/splash_logo.png".
     * The file will be copied into the right resource directory on each platform.
     */
    @get:Input
    @get:Optional
    abstract val logoFile: Property<String>

    /**
     * Path to the Xcode project folder that contains `Info.plist`, e.g. "iosApp/iosApp".
     * The storyboard is written to `<iosProjectPath>/LaunchScreen.storyboard`.
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
