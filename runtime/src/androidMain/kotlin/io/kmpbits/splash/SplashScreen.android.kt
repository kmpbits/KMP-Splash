package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

@Composable
actual fun SplashScreen(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
    backgroundColor: Color,
    logoPainter: (@Composable () -> Painter?)?
) {
    // Android usually handles splash via SplashActivity and native API.
    // This composable can be used if calling SplashConfig in Compose code.
    LaunchedEffect(Unit) {
        if (isReady()) onFinished()
    }
}
