package io.kmpbits.splash

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * A base [ComponentActivity] that handles the Android splash screen API via
 * `androidx.core:core-splashscreen`.
 *
 * Splash visibility and exit animation are both driven by [isReady] and
 * [SplashDefaults.exitAnimation] respectively, set automatically by the Gradle plugin.
 */
abstract class SplashActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var ready = false

    /**
     * Override this to provide the condition for when the splash screen should be dismissed.
     */
    abstract suspend fun isReady(): Boolean

    /**
     * Callback invoked when [isReady] returns true.
     * Usually you want to call [setContent] here to display your app content.
     */
    abstract fun onFinished()

    /**
     * Called after [installSplashScreen] but before [super.onCreate][ComponentActivity.onCreate].
     * Override to perform setup that must happen in this exact window — e.g. [enableEdgeToEdge].
     */
    protected open fun onPreCreate() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        onPreCreate()
        super.onCreate(savedInstanceState)

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

        scope.launch {
            ready = isReady()
            onFinished()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
