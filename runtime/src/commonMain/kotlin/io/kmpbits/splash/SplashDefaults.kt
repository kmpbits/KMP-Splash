package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

/**
 * Default configuration for the KMP Splash screen.
 *
 * These values are typically populated by the generated `SplashInit.kt` file
 * based on your Gradle configuration.
 */
object SplashDefaults {
    /** The background color to use for the splash screen. */
    var backgroundColor: Color = Color.White

    /** Optional background color for dark mode. */
    var backgroundColorNight: Color? = null

    /** Composable provider for the logo painter. */
    var logoPainter: (@Composable () -> Painter?)? = null

    /** Composable provider for the logo painter in dark mode. */
    var logoPainterNight: (@Composable () -> Painter?)? = null

    /** Exit animation applied when the splash screen is dismissed. */
    var exitAnimation: ExitAnimation = ExitAnimation.None
}
