package io.kmpbits.splash

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertTrue

class FakeSplashActivity : SplashActivity() {
    var isReadyCalled = false
    var onFinishedCalled = false

    override suspend fun isReady(): Boolean {
        isReadyCalled = true
        return true
    }

    override fun onFinished() {
        onFinishedCalled = true
    }
}

@RunWith(AndroidJUnit4::class)
class SplashActivityTest {

    @Test
    fun `test splash activity lifecycle`() = runTest {
        val controller = Robolectric.buildActivity(FakeSplashActivity::class.java)
        controller.create()

        val activity = controller.get()

        shadowOf(Looper.getMainLooper()).idle()

        assertTrue(activity.isReadyCalled)
        assertTrue(activity.onFinishedCalled)
    }

    // Note: animation-specific tests (FadeOut, SlideUp, SlideDown) are not tested here.
    // Robolectric's SplashScreen API inflates a view that requires theme attributes not
    // available in the test environment, causing InflateException when setOnExitAnimationListener
    // fires. Visual animation behaviour is covered by functional/integration tests instead.
}
