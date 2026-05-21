package io.kmpbits.splash.sample

import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kmpbits.splash.SplashConfig
import kotlinx.coroutines.flow.first

fun MainViewController() = ComposeUIViewController {
    val viewModel = viewModel { SplashViewModel() }

    SplashConfig(isReady = { viewModel.isLoading.first { !it }; true }) {
        App()
    }
}
