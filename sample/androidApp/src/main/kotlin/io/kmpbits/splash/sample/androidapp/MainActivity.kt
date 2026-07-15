package io.kmpbits.splash.sample.androidapp

import android.widget.TextView
import io.kmpbits.splash.SplashActivity

class MainActivity : SplashActivity() {

    override suspend fun isReady(): Boolean = true

    override fun onFinished() {
        setContentView(TextView(this).apply { text = "KMP Splash — androidApp sample" })
    }
}
