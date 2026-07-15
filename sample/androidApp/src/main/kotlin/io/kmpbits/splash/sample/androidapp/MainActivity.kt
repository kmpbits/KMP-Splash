package io.kmpbits.splash.sample.androidapp

import android.widget.TextView
import io.kmpbits.splash.SplashActivity
import kotlinx.coroutines.delay

class MainActivity : SplashActivity() {

    override suspend fun isReady(): Boolean {
        delay(1500)
        return true
    }

    override fun onFinished() {
        setContentView(TextView(this).apply { text = "KMP Splash — androidApp sample" })
    }
}
