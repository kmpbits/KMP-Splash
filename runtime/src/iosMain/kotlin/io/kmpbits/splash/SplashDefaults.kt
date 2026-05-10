package io.kmpbits.splash

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter

object SplashDefaults {
    var backgroundColor: Color = Color.White
    var backgroundColorNight: Color? = null
    var logoPainter: (@Composable () -> Painter?)? = null
}
