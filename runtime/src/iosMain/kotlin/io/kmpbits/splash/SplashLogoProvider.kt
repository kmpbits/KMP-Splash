package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter

object SplashLogoProvider {
    var current: (@Composable () -> Painter?)? = null
}
