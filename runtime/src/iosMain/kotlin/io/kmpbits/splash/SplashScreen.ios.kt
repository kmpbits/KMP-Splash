package io.kmpbits.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

@Composable
fun SplashScreen(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
) {
    val logo = SplashDefaults.logoPainter?.invoke()

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(SplashDefaults.backgroundColor),
    ) {
        if (logo != null) {
            Image(
                painter = logo,
                contentDescription = null,
            )
        }
    }

    LaunchedEffect(Unit) {
        if (isReady()) onFinished()
    }
}
