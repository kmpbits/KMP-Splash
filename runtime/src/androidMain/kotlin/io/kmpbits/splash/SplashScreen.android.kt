package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.graphics.Color

/**
 * Android actual: the AndroidX SplashScreen API handles the visual layer
 * natively via the generated themes.xml. This composable only observes the
 * resolved destination so the host Activity can release the splash at the
 * right moment via `splashScreen.setKeepOnScreenCondition { false }`.
 *
 * Typical host Activity setup:
 * ```
 * val splashScreen = installSplashScreen()
 * splashScreen.setKeepOnScreenCondition { viewModel.destination.value == null }
 * ```
 */
@Composable
actual fun SplashScreen(
    viewModel: SplashViewModel,
    backgroundColor: Color,
    onDestinationResolved: (FirstDestination) -> Unit,
) {
    val destination = viewModel.destination.collectAsState().value
    LaunchedEffect(destination) {
        destination?.let(onDestinationResolved)
    }
}
