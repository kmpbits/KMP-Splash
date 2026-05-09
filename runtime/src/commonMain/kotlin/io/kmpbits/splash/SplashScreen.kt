package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
expect fun SplashScreen(
    backgroundColor: Color,
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
)
