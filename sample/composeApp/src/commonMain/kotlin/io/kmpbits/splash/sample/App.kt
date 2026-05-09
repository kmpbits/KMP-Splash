package io.kmpbits.splash.sample

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import io.kmpbits.splash.FirstDestination
import io.kmpbits.splash.GetStartDestinationUseCase
import io.kmpbits.splash.SplashScreen
import io.kmpbits.splash.SplashViewModel
import kotlinx.coroutines.delay

private val splashBackground = Color(0xFF1A1A2E)

@Composable
fun App() {
    val viewModel = remember {
        SplashViewModel(
            getStartDestination = GetStartDestinationUseCase {
                delay(500) // simulate auth/prefs check
                FirstDestination.Home
            }
        )
    }

    var destination by remember { mutableStateOf<FirstDestination?>(null) }

    if (destination == null) {
        SplashScreen(
            viewModel = viewModel,
            backgroundColor = splashBackground,
            onDestinationResolved = { destination = it },
        )
    } else {
        when (destination) {
            FirstDestination.Home -> Text("Home screen")
            FirstDestination.Onboarding -> Text("Onboarding screen")
            null -> Unit
        }
    }
}
