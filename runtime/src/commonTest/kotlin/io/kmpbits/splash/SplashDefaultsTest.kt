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
    }

    @Test
    fun testModification() {
        val customColor = Color.Red
        SplashDefaults.backgroundColor = customColor
        assertEquals(customColor, SplashDefaults.backgroundColor)
        
        // Reset to default for other tests
        SplashDefaults.backgroundColor = Color.White
    }
}
