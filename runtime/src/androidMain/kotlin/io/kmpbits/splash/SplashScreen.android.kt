package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun SplashScreen(
    backgroundColor: Color,
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
) = Unit
