package io.kmpbits.splash.sample

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.ComposeUIViewController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.kmpbits.splash.SplashConfig
import kotlinx.coroutines.delay

fun MainViewController() = ComposeUIViewController {
    val viewModel = viewModel { SplashViewModel() }
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

    if (isLoading) {
        SplashConfig(
            isReady = { !isLoading },
            onFinished = {
                // Not needed here. The ViewModel manages the loading state.
            },
        )
    } else {
        App()
    }
}
