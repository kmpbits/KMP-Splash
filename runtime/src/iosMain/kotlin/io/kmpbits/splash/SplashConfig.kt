package io.kmpbits.splash

import androidx.compose.runtime.Composable

/**
 * Entry point for configuring the splash screen on iOS.
 *
 * Example usage in `MainViewController`:
 * ```kotlin
 * fun MainViewController() = ComposeContainer {
 *     var showSplash by remember { mutableStateOf(true) }
 *     if (showSplash) {
 *         SplashConfig(
 *             isReady = { viewModel.isReady() },
 *             onFinished = { showSplash = false }
 *         )
 *     } else {
 *         App()
 *     }
 * }
 * ```
 */
object SplashConfig {
    @Composable
    operator fun invoke(
        isReady: suspend () -> Boolean,
        onFinished: () -> Unit = {},
    ) {
        SplashScreen(isReady = isReady, onFinished = onFinished)
    }
}
