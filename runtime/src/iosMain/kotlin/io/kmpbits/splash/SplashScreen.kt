package io.kmpbits.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
    val nightColor = SplashDefaults.backgroundColorNight
    val background = if (nightColor != null && isSystemInDarkTheme()) nightColor else SplashDefaults.backgroundColor
    
    val logoNight = SplashDefaults.logoPainterNight
    val logo = if (logoNight != null && isSystemInDarkTheme()) {
        logoNight.invoke()
    } else {
        SplashDefaults.logoPainter?.invoke()
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(background),
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
