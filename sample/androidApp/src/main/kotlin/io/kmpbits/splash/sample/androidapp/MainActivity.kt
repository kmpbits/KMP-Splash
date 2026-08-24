package io.kmpbits.splash.sample.androidapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import io.kmpbits.splash.installKmpSplash
import kotlinx.coroutines.delay

// Demonstrates installKmpSplash() instead of extending SplashActivity — the escape hatch for
// when MainActivity must extend a different base class, here AppCompatActivity.
class MainActivity : AppCompatActivity() {

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        installKmpSplash(
            isReady = {
                delay(1500)
                true
            },
            onFinished = {
                setContentView(TextView(this).apply { text = "KMP Splash — androidApp sample (AppCompat)" })
            },
        )
        super.onCreate(savedInstanceState)
    }
}
