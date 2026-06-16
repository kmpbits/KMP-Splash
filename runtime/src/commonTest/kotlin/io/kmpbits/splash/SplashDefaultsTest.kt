package io.kmpbits.splash

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SplashDefaultsTest {

    @Test
    fun testDefaults() {
        assertEquals(Color.White, SplashDefaults.backgroundColor)
        assertNull(SplashDefaults.backgroundColorNight)
        assertNull(SplashDefaults.logoPainter)
        assertNull(SplashDefaults.logoPainterNight)
        assertEquals(ExitAnimation.None, SplashDefaults.exitAnimation)
    }

    @Test
    fun testModification() {
        val customColor = Color.Red
        SplashDefaults.backgroundColor = customColor
        assertEquals(customColor, SplashDefaults.backgroundColor)

        // Reset to default for other tests
        SplashDefaults.backgroundColor = Color.White
    }

    @Test
    fun testExitAnimationDefault() {
        assertEquals(ExitAnimation.None, SplashDefaults.exitAnimation)
    }

    @Test
    fun testExitAnimationFadeOut() {
        SplashDefaults.exitAnimation = ExitAnimation.FadeOut(500)
        assertEquals(ExitAnimation.FadeOut(500), SplashDefaults.exitAnimation)

        // Reset
        SplashDefaults.exitAnimation = ExitAnimation.None
    }

    @Test
    fun testExitAnimationSlideUp() {
        SplashDefaults.exitAnimation = ExitAnimation.SlideUp(600)
        assertEquals(ExitAnimation.SlideUp(600), SplashDefaults.exitAnimation)

        // Reset
        SplashDefaults.exitAnimation = ExitAnimation.None
    }

    @Test
    fun testExitAnimationSlideDown() {
        SplashDefaults.exitAnimation = ExitAnimation.SlideDown(250)
        assertEquals(ExitAnimation.SlideDown(250), SplashDefaults.exitAnimation)

        // Reset
        SplashDefaults.exitAnimation = ExitAnimation.None
    }
}
