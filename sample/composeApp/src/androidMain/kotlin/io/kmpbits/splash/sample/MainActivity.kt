package io.kmpbits.splash.sample

import androidx.activity.compose.setContent
import androidx.activity.viewModels
import io.kmpbits.splash.SplashActivity
import kotlinx.coroutines.flow.first

class MainActivity : SplashActivity() {

    private val viewModel: SplashViewModel by viewModels()

    override suspend fun isReady(): Boolean {
        viewModel.isLoading.first { !it }
        return true
    }

    override fun onFinished() {
        setContent {
            App()
        }
    }
}
