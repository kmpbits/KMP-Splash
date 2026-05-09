package io.kmpbits.splash.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import io.kmpbits.splash.SplashConfig
import io.kmpbits.splash.SplashScreen
import kotlinx.coroutines.delay

fun MainViewController() = ComposeUIViewController {
    var splashDone by remember { mutableStateOf(false) }

    if (!splashDone) {
        SplashScreen(
            backgroundColor = SplashConfig.backgroundColor,
            isReady = {
                delay(1000)
                true
            },
            onFinished = { splashDone = true },
        )
    } else {
        App()
    }
}
