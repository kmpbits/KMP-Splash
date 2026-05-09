package io.kmpbits.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun SplashScreen(
    backgroundColor: Color,
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor),
    )

    LaunchedEffect(Unit) {
        if (isReady()) onFinished()
    }
}
