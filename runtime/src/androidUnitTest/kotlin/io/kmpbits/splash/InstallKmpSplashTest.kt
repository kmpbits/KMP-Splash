package io.kmpbits.splash

import android.os.Bundle
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertTrue

class FakeInstallActivity : ComponentActivity() {
    var isReadyCalled = false
    var onFinishedCalled = false

    override fun onCreate(savedInstanceState: Bundle?) {
        installKmpSplash(
            isReady = {
                isReadyCalled = true
                true
            },
            onFinished = { onFinishedCalled = true },
        )
        super.onCreate(savedInstanceState)
    }
}

@RunWith(AndroidJUnit4::class)
class InstallKmpSplashTest {

    @Test
    fun `test installKmpSplash lifecycle on a plain ComponentActivity`() = runTest {
        val controller = Robolectric.buildActivity(FakeInstallActivity::class.java)
        controller.create()

        val activity = controller.get()

        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(activity.isReadyCalled)
        assertTrue(activity.onFinishedCalled)
    }
}
