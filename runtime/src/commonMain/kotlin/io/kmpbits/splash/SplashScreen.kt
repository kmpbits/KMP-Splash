package io.kmpbits.splash

import androidx.compose.runtime.Composable

@Composable
expect fun SplashScreen(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
)
