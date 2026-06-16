package io.kmpbits.splash

import java.io.Serializable

/**
 * Defines the exit animation for the splash screen.
 *
 * Used in the `splashScreen { }` Gradle DSL block:
 * ```kotlin
 * splashScreen {
 *     exitAnimation = ExitAnimation.FadeOut(300)
 * }
 * ```
 */
sealed class ExitAnimation : Serializable {

    /** No exit animation. The splash disappears immediately. */
    object None : ExitAnimation()

    /**
     * Fades the splash screen out.
     * @param durationMs Duration in milliseconds. Defaults to 300.
     */
    data class FadeOut(val durationMs: Int = 300) : ExitAnimation()

    /**
     * Slides the splash screen upward to reveal the app.
     * @param durationMs Duration in milliseconds. Defaults to 400.
     */
    data class SlideUp(val durationMs: Int = 400) : ExitAnimation()

    /**
     * Slides the splash screen downward to reveal the app.
     * @param durationMs Duration in milliseconds. Defaults to 400.
     */
    data class SlideDown(val durationMs: Int = 400) : ExitAnimation()
}

/** Converts a DSL [ExitAnimation] to its runtime Kotlin constructor expression for code generation. */
internal fun ExitAnimation.toKotlinExpression(): String? = when (this) {
    is ExitAnimation.None -> null
    is ExitAnimation.FadeOut -> "ExitAnimation.FadeOut($durationMs)"
    is ExitAnimation.SlideUp -> "ExitAnimation.SlideUp($durationMs)"
    is ExitAnimation.SlideDown -> "ExitAnimation.SlideDown($durationMs)"
}
