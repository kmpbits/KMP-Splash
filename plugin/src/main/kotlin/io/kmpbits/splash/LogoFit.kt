package io.kmpbits.splash

/**
 * How the plugin treats [splashScreen.logo][KmpSplashExtension.logo] when rasterizing it for the
 * Android splash icon and the generated app icons (Android adaptive/legacy and iOS).
 */
enum class LogoFit {
    /**
     * Default. Trims the logo's transparent border, then scales the artwork into each target's
     * safe zone. Any padding baked into the source image is discarded and replaced by the
     * plugin's own.
     */
    Trim,

    /**
     * Uses the image exactly as drawn, with its own padding: no trimming, and no safe-zone
     * scaling unless [KmpSplashExtension.logoScale] is set. You are responsible for leaving
     * enough margin that Android's splash/launcher masks don't crop the artwork.
     */
    AsIs,
}
