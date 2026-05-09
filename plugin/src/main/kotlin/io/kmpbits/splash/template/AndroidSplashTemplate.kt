package io.kmpbits.splash.template

internal object AndroidSplashTemplate {

    /**
     * Generates the splash screen theme XML for `res/values/themes.xml`.
     *
     * Uses the AndroidX SplashScreen API (core-splashscreen).
     * [backgroundColor] is a hex string like "#FF0000".
     * [logoDrawableName] is the drawable resource name without prefix, e.g. "splash_logo".
     */
    fun generateThemes(
        backgroundColor: String,
        logoDrawableName: String? = null,
    ): String {
        val logoItem = if (logoDrawableName != null) {
            """        <item name="windowSplashScreenAnimatedIcon">@drawable/$logoDrawableName</item>"""
        } else ""

        return """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App.SplashScreen" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">$backgroundColor</item>
$logoItem
        <item name="postSplashScreenTheme">@style/Theme.App</item>
    </style>

    <style name="Theme.App" parent="Theme.MaterialComponents.DayNight.NoActionBar"/>
</resources>"""
    }

    /**
     * Generates the night-mode override for `res/values-night/themes.xml`.
     * Only needed when dark mode uses a different background color.
     */
    fun generateNightThemes(
        backgroundColorNight: String,
        logoDrawableName: String? = null,
    ): String {
        val logoItem = if (logoDrawableName != null) {
            """        <item name="windowSplashScreenAnimatedIcon">@drawable/$logoDrawableName</item>"""
        } else ""

        return """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <style name="Theme.App.SplashScreen" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">$backgroundColorNight</item>
$logoItem
        <item name="postSplashScreenTheme">@style/Theme.App</item>
    </style>
</resources>"""
    }
}
