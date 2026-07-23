package io.kmpbits.splash

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppIconGeneratorTest {

    private fun paddedTestLogo(): BufferedImage {
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color.RED
        g.fillRect(10, 10, 20, 20)
        g.dispose()
        return image
    }

    @Test
    fun `trimTransparentBorder returns the opaque bounding box`() {
        val rect = AppIconGenerator.trimTransparentBorder(paddedTestLogo())

        assertEquals(10, rect.x)
        assertEquals(10, rect.y)
        assertEquals(20, rect.width)
        assertEquals(20, rect.height)
    }

    @Test
    fun `trimTransparentBorder falls back to full bounds for a fully transparent image`() {
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)

        val rect = AppIconGenerator.trimTransparentBorder(image)

        assertEquals(0, rect.x)
        assertEquals(0, rect.y)
        assertEquals(40, rect.width)
        assertEquals(40, rect.height)
    }

    @Test
    fun `needsUpscalingWarning is true for content smaller than the largest foreground target`() {
        assertTrue(AppIconGenerator.needsUpscalingWarning(20))
    }

    @Test
    fun `needsUpscalingWarning is false for content at or above the largest foreground target`() {
        assertFalse(AppIconGenerator.needsUpscalingWarning(300))
    }
}
