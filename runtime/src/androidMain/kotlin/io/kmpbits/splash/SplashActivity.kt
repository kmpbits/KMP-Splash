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
 * A base [ComponentActivity] that handles the Android 12+ splash screen API.
 *
 * It uses the `androidx.core:core-splashscreen` library to manage the splash screen
 * visibility based on the [isReady] state.
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
     * Usually you want to start your main activity here and finish this one.
     */
    abstract fun onFinished()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { !ready }

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
