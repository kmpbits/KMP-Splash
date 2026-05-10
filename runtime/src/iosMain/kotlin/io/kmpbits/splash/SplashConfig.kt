package io.kmpbits.splash

import androidx.compose.runtime.Composable

object SplashConfig {
    @Composable
    operator fun invoke(
        isReady: suspend () -> Boolean,
        onFinished: () -> Unit = {},
    ) {
        SplashScreen(isReady = isReady, onFinished = onFinished)
    }
}
