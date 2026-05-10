package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

@Composable
expect fun SplashScreen(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
    backgroundColor: Color = Color.White,
    logoPainter: (@Composable () -> Painter?)? = null
)
