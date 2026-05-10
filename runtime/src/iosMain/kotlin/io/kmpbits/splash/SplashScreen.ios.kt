package io.kmpbits.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

@Composable
actual fun SplashScreen(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
    backgroundColor: Color,
    logoPainter: (@Composable () -> Painter?)?
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    ) {
        val logo = logoPainter?.invoke()
        if (logo != null) {
            Image(
                painter = logo,
                contentDescription = null,
            )
        }
    }

    LaunchedEffect(Unit) {
        if (isReady()) onFinished()
    }
}
