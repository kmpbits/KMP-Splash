package io.kmpbits.splash

import android.os.Bundle
import androidx.activity.ComponentActivity

/**
 * A base [ComponentActivity] that handles the Android splash screen API via
 * `androidx.core:core-splashscreen`.
 *
 * Splash visibility and exit animation are both driven by [isReady] and
 * [SplashDefaults.exitAnimation] respectively, set automatically by the Gradle plugin.
 *
 * If your `MainActivity` needs to extend a different base class — e.g. `AppCompatActivity` —
 * use [installKmpSplash] directly instead of extending this class.
 */
abstract class SplashActivity : ComponentActivity() {

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
     * Called after [installKmpSplash] installs the splash screen but before
     * [super.onCreate][ComponentActivity.onCreate]. Override to perform setup that must happen
     * in this exact window — e.g. `enableEdgeToEdge()`.
     */
    protected open fun onPreCreate() {}

    override fun onCreate(savedInstanceState: Bundle?) {
        installKmpSplash(isReady = ::isReady, onFinished = ::onFinished)
        onPreCreate()
        super.onCreate(savedInstanceState)
    }
}
