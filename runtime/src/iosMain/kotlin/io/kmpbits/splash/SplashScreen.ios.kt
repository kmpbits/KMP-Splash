package io.kmpbits.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

// Internal lookup keys - not intended for public use
val LocalSplashBackgroundColor = staticCompositionLocalOf { Color.White }
val LocalSplashLogoPainter = staticCompositionLocalOf<(@Composable () -> Painter?)?> { null }

@Composable
fun SplashScreen(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
) {
    val backgroundColor = LocalSplashBackgroundColor.current
    val logoPainter = LocalSplashLogoPainter.current

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
