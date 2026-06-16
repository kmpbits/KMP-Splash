package io.kmpbits.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * A Composable that displays a splash screen on iOS.
 *
 * This is used to provide a seamless transition from the native launch screen
 * to the Compose content. The exit animation is driven by [SplashDefaults.exitAnimation].
 *
 * @param isReady A suspend function that returns true when the app is ready to hide the splash screen.
 * @param content The composable to display once [isReady] returns true.
 */
@Composable
fun SplashScreen(
    isReady: suspend () -> Boolean,
    content: @Composable () -> Unit,
) {
    var splashVisible by remember { mutableStateOf(true) }
    var contentVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isReady()
        // Show content first, then trigger the splash exit so it animates over ready content.
        contentVisible = true
        withFrameNanos { }
        splashVisible = false
    }

    val anim = SplashDefaults.exitAnimation

    Box(modifier = Modifier.fillMaxSize()) {
        if (contentVisible) {
            content()
        }

        AnimatedVisibility(
            visible = splashVisible,
            enter = EnterTransition.None,
            exit = when (anim) {
                is ExitAnimation.None -> fadeOut(animationSpec = snap())
                is ExitAnimation.FadeOut -> fadeOut(
                    animationSpec = tween(anim.durationMs, easing = LinearEasing),
                )
                is ExitAnimation.SlideUp -> slideOutVertically(
                    animationSpec = tween(anim.durationMs, easing = FastOutLinearInEasing),
                    targetOffsetY = { -it },
                )
                is ExitAnimation.SlideDown -> slideOutVertically(
                    animationSpec = tween(anim.durationMs, easing = FastOutLinearInEasing),
                    targetOffsetY = { it },
                )
            },
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
        }
    }
}
