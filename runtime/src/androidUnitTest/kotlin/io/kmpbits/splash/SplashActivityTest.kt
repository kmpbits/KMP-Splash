package io.kmpbits.splash

import android.os.Looper
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import kotlin.test.assertTrue

class TestSplashActivity : SplashActivity() {
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
        val controller = Robolectric.buildActivity(TestSplashActivity::class.java)
        controller.create()
        
        val activity = controller.get()
        
        shadowOf(Looper.getMainLooper()).idle()
        
        assertTrue(activity.isReadyCalled)
        assertTrue(activity.onFinishedCalled)
    }
}
