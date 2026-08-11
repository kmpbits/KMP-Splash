package io.kmpbits.splash

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun BufferedImage.argbAt(x: Int, y: Int): Int = getRGB(x, y)
private fun Int.alpha(): Int = (this ushr 24) and 0xFF
private fun Int.red(): Int = (this ushr 16) and 0xFF

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

    @Test
    fun `renderForeground centers the trimmed logo on a transparent canvas`() {
        val foreground = AppIconGenerator.renderForeground(paddedTestLogo(), canvasPx = 100)

        assertEquals(0, foreground.argbAt(0, 0).alpha(), "expected the canvas corner to stay transparent")
        assertTrue(foreground.argbAt(50, 50).alpha() > 0, "expected the canvas center to be drawn")
        assertTrue(foreground.argbAt(50, 50).red() > 150, "expected the drawn logo's red channel to dominate")
    }

    @Test
    fun `renderLegacy composites the trimmed logo over an opaque background`() {
        val legacy = AppIconGenerator.renderLegacy(paddedTestLogo(), Color.WHITE, canvasPx = 100, round = false)

        val corner = legacy.argbAt(0, 0)
        assertEquals(255, corner.alpha(), "expected the corner to be filled with the opaque background")
        assertEquals(255, corner.red(), "expected the corner to be the white background color")
        assertTrue(legacy.argbAt(50, 50).red() > 150, "expected the drawn logo's red channel to dominate at the center")
    }

    @Test
    fun `renderLegacy with round clips corners to transparent`() {
        val legacy = AppIconGenerator.renderLegacy(paddedTestLogo(), Color.WHITE, canvasPx = 100, round = true)

        assertEquals(0, legacy.argbAt(0, 0).alpha(), "expected the corner to be clipped outside the circle")
        assertTrue(legacy.argbAt(50, 50).alpha() > 0, "expected the center to remain inside the circle")
    }

    @Test
    fun `needsUpscalingWarning with an explicit target is true when content is smaller than the target`() {
        assertTrue(AppIconGenerator.needsUpscalingWarning(500, targetContentPx = 737))
    }

    @Test
    fun `needsUpscalingWarning with an explicit target is false when content meets the target`() {
        assertFalse(AppIconGenerator.needsUpscalingWarning(737, targetContentPx = 737))
    }

    @Test
    fun `renderSplashIcon pads a logo drawn edge-to-edge so it isn't clipped by the icon mask`() {
        val edgeToEdgeLogo = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val g = edgeToEdgeLogo.createGraphics()
        g.color = Color.RED
        g.fillRect(0, 0, 40, 40)
        g.dispose()

        val splashIcon = AppIconGenerator.renderSplashIcon(edgeToEdgeLogo)

        assertTrue(
            splashIcon.width > edgeToEdgeLogo.width,
            "expected the canvas to grow to make room for padding, was ${splashIcon.width}",
        )
        assertEquals(0, splashIcon.argbAt(0, 0).alpha(), "expected the canvas corner to be padding, not logo content")
        val center = splashIcon.width / 2
        assertTrue(splashIcon.argbAt(center, center).red() > 150, "expected the logo to still be drawn at the center")
    }

    @Test
    fun `renderSplashIcon centers an already-padded logo without growing the canvas much`() {
        val splashIcon = AppIconGenerator.renderSplashIcon(paddedTestLogo())

        assertEquals(0, splashIcon.argbAt(0, 0).alpha(), "expected the canvas corner to stay transparent")
        val center = splashIcon.width / 2
        assertTrue(splashIcon.argbAt(center, center).red() > 150, "expected the drawn logo's red channel to dominate")
    }

    @Test
    fun `parseHexColor parses a hash-prefixed hex string`() {
        val color = AppIconGenerator.parseHexColor("#FF0000")

        assertEquals(Color(255, 0, 0), color)
    }

    @Test
    fun `parseHexColor accepts a hex string without a hash prefix`() {
        val color = AppIconGenerator.parseHexColor("00FF00")

        assertEquals(Color(0, 255, 0), color)
    }
}
