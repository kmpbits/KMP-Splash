package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Platform-specific splash screen entry point.
 *
 * On Android this bridges the AndroidX SplashScreen exit condition to
 * [onDestinationResolved] — no Compose UI is rendered.
 * On iOS it renders a full-screen surface matching [backgroundColor] while
 * the destination resolves, providing a seamless hand-off from the storyboard.
 */
@Composable
expect fun SplashScreen(
    viewModel: SplashViewModel,
    backgroundColor: Color = Color.White,
    onDestinationResolved: (FirstDestination) -> Unit,
)
