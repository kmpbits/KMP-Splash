package io.kmpbits.splash

import androidx.compose.runtime.Composable

/**
 * Entry point for configuring the splash screen on iOS.
 *
 * Example usage in `MainViewController`:
 * ```kotlin
 * fun MainViewController() = ComposeUIViewController {
 *     SplashConfig(isReady = { delay(1500); true }) {
 *         App()
 *     }
 * }
 * ```
 */
object SplashConfig {
    @Composable
    operator fun invoke(
        isReady: suspend () -> Boolean,
        content: @Composable () -> Unit,
    ) {
        SplashScreen(isReady = isReady, content = content)
    }
}
