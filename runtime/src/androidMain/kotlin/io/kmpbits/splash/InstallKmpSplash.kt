package io.kmpbits.splash

import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Installs the Android splash screen (via `androidx.core:core-splashscreen`) on any
 * [ComponentActivity] subclass — including `AppCompatActivity` and other custom base classes —
 * without requiring inheritance from [SplashActivity].
 *
 * Must be called before `super.onCreate()`, matching the requirement of
 * `ComponentActivity.installSplashScreen()`:
 *
 * ```kotlin
 * class MainActivity : AppCompatActivity() {
 *     override fun onCreate(savedInstanceState: Bundle?) {
 *         installKmpSplash(
 *             isReady = { viewModel.isLoading.first { !it } },
 *             onFinished = { setContent { App() } },
 *         )
 *         super.onCreate(savedInstanceState)
 *     }
 * }
 * ```
 *
 * Splash visibility and exit animation are both driven by [isReady] and
 * [SplashDefaults.exitAnimation] respectively, set automatically by the Gradle plugin.
 *
 * @param isReady suspending condition; the splash screen stays on screen until this returns
 * `true`.
 * @param onFinished invoked once [isReady] returns `true`. Usually calls `setContent` to display
 * your app content.
 * @return the [SplashScreen] instance, in case further configuration is needed.
 */
fun ComponentActivity.installKmpSplash(
    isReady: suspend () -> Boolean,
    onFinished: () -> Unit,
): SplashScreen {
    val splashScreen = installSplashScreen()
    var ready = false
    splashScreen.setKeepOnScreenCondition { !ready }

    val anim = SplashDefaults.exitAnimation
    if (anim != ExitAnimation.None) {
        splashScreen.setOnExitAnimationListener { provider ->
            val view = provider.view
            when (anim) {
                is ExitAnimation.FadeOut -> view.animate()
                    .alpha(0f)
                    .setDuration(anim.durationMs.toLong())
                    .withEndAction { provider.remove() }
                    .start()
                is ExitAnimation.SlideUp -> view.animate()
                    .translationY(-view.height.toFloat())
                    .setDuration(anim.durationMs.toLong())
                    .withEndAction { provider.remove() }
                    .start()
                is ExitAnimation.SlideDown -> view.animate()
                    .translationY(view.height.toFloat())
                    .setDuration(anim.durationMs.toLong())
                    .withEndAction { provider.remove() }
                    .start()
                is ExitAnimation.None -> provider.remove()
            }
        }
    }

    val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    lifecycle.addObserver(
        LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_DESTROY) scope.cancel()
        },
    )
    scope.launch {
        ready = isReady()
        onFinished()
    }

    return splashScreen
}
