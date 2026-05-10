package io.kmpbits.splash

import androidx.compose.runtime.Composable

@Composable
actual fun SplashScreen(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
) = Unit
