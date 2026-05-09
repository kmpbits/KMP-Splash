package io.kmpbits.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

/**
 * iOS actual: renders a full-screen Compose surface matching [backgroundColor]
 * while the destination resolves, providing a seamless hand-off from the
 * LaunchScreen.storyboard. Pass the same color you set in `splashScreen { }`.
 */
@Composable
actual fun SplashScreen(
    viewModel: SplashViewModel,
    backgroundColor: Color,
    onDestinationResolved: (FirstDestination) -> Unit,
) {
    val destination = viewModel.destination.collectAsState().value

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    )

    LaunchedEffect(destination) {
        destination?.let(onDestinationResolved)
    }
}
