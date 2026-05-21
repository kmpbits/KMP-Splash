package io.kmpbits.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A Composable that displays a splash screen on iOS.
 *
 * This is used to provide a seamless transition from the native launch screen
 * to the Compose content.
 *
 * @param isReady A suspend function that returns true when the app is ready to hide the splash screen.
 * @param content The composable to display once [isReady] returns true.
 */
@Composable
fun SplashScreen(
    isReady: suspend () -> Boolean,
    content: @Composable () -> Unit,
) {
    var isFinished by remember { mutableStateOf(false) }

    if (isFinished) {
        content()
    } else {
        val nightColor = SplashDefaults.backgroundColorNight
        val background = if (nightColor != null && isSystemInDarkTheme()) nightColor else SplashDefaults.backgroundColor

        val logoNight = SplashDefaults.logoPainterNight
        val logo = if (logoNight != null && isSystemInDarkTheme()) {
            logoNight.invoke()
        } else {
            SplashDefaults.logoPainter?.invoke()
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(background),
        ) {
            if (logo != null) {
                Image(
                    painter = logo,
                    contentDescription = null,
                )
            }
        }

        LaunchedEffect(Unit) {
            if (isReady()) isFinished = true
        }
    }
}
