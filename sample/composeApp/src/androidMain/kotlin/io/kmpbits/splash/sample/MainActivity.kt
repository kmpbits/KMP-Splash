package io.kmpbits.splash.sample

import androidx.compose.ui.graphics.Color
import io.kmpbits.splash.SplashActivity
import kotlinx.coroutines.delay

class MainActivity : SplashActivity() {

    override suspend fun isReady(): Boolean {
        delay(1500) // simulate async work — replace with real logic
        return true
    }

    override fun onFinished() {
        setContent {
            App()
        }
    }
}
