package io.kmpbits.splash

/**
 * Defines the exit animation for the splash screen.
 *
 * Set via the Gradle plugin; consumed at runtime by [SplashDefaults].
 */
sealed class ExitAnimation {

    /** No exit animation. The splash disappears immediately. */
    object None : ExitAnimation()

    /**
     * Fades the splash screen out.
     * @param durationMs Duration in milliseconds.
     */
    data class FadeOut(val durationMs: Int = 300) : ExitAnimation()

    /**
     * Slides the splash screen upward to reveal the app.
     * @param durationMs Duration in milliseconds.
     */
    data class SlideUp(val durationMs: Int = 400) : ExitAnimation()

    /**
     * Slides the splash screen downward to reveal the app.
     * @param durationMs Duration in milliseconds.
     */
    data class SlideDown(val durationMs: Int = 400) : ExitAnimation()
}
