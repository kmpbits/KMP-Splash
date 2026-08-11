package io.kmpbits.splash

import io.kmpbits.splash.AppIconGenerator.ALPHA_TRIM_THRESHOLD
import io.kmpbits.splash.AppIconGenerator.FOREGROUND_CONTENT_SCALE
import io.kmpbits.splash.AppIconGenerator.LEGACY_CONTENT_SCALE
import io.kmpbits.splash.AppIconGenerator.SPLASH_ICON_CONTENT_SCALE
import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage

/**
 * Renders Android app icon assets (adaptive foreground + legacy square/round PNGs) from a single
 * source logo image, trimming its transparent border and re-centering it into each icon's safe
 * zone. Pure image logic — no Gradle types — so it's testable without a Gradle project.
 */
internal object AppIconGenerator {

    /** Fraction of the 108dp adaptive-icon canvas the trimmed logo is scaled to fill. */
    const val FOREGROUND_CONTENT_SCALE = 0.60

    /** Fraction of the legacy (no launcher mask) icon canvas the trimmed logo is scaled to fill. */
    const val LEGACY_CONTENT_SCALE = 0.72

    /**
     * Fraction of the splash icon canvas the trimmed logo is scaled to fill. Android's
     * SplashScreen API (API 31+) masks `windowSplashScreenAnimatedIcon` into a circle and crops
     * any artwork outside the inner ~2/3 "safe zone" — the same convention as adaptive launcher
     * icons — so a logo drawn edge-to-edge gets clipped by the system unless padded first.
     */
    const val SPLASH_ICON_CONTENT_SCALE = 0.66

    /** Alpha values at or below this (0-255) are treated as transparent when trimming. */
    private const val ALPHA_TRIM_THRESHOLD = 10

    /** Rasterizable logo file extensions (no dot), accepted for app icon generation on both platforms. */
    val SUPPORTED_LOGO_EXTENSIONS: Set<String> = setOf("png", "jpg", "jpeg", "gif", "bmp")

    /** Parses a "#RRGGBB" (or "RRGGBB") hex string into an opaque [Color]. */
    fun parseHexColor(hex: String): Color {
        val clean = hex.trimStart('#')
        return Color(
            clean.substring(0, 2).toInt(16),
            clean.substring(2, 4).toInt(16),
            clean.substring(4, 6).toInt(16),
        )
    }

    /** Legacy (pre-adaptive-icon) launcher icon size in px, keyed by density qualifier. */
    val LEGACY_DENSITIES: Map<String, Int> = mapOf(
        "mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192,
    )

    /** Adaptive-icon foreground canvas size in px (108dp vs. legacy's 48dp = 2.25x), keyed by density. */
    val FOREGROUND_DENSITIES: Map<String, Int> =
        LEGACY_DENSITIES.mapValues { (_, legacyPx) -> (legacyPx * 2.25).toInt() }

    /**
     * Returns the smallest rectangle containing every pixel whose alpha exceeds
     * [ALPHA_TRIM_THRESHOLD]. Falls back to the full image bounds if the image has no
     * sufficiently opaque pixel at all (e.g. a blank placeholder).
     */
    fun trimTransparentBorder(image: BufferedImage): Rectangle {
        var minX = image.width
        var minY = image.height
        var maxX = -1
        var maxY = -1

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val alpha = (image.getRGB(x, y) ushr 24) and 0xFF
                if (alpha > ALPHA_TRIM_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (maxX < minX || maxY < minY) {
            Rectangle(0, 0, image.width, image.height)
        } else {
            Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
        }
    }

    /**
     * Convenience overload for Android: true if [trimmedContentPx] is too small to fill
     * [FOREGROUND_CONTENT_SCALE] of the largest generated foreground canvas without visible
     * upscaling. Delegates to the general two-argument overload below.
     */
    fun needsUpscalingWarning(trimmedContentPx: Int): Boolean {
        val largestCanvas = FOREGROUND_DENSITIES.values.max()
        val targetContentPx = (largestCanvas * FOREGROUND_CONTENT_SCALE).toInt()
        return needsUpscalingWarning(trimmedContentPx, targetContentPx)
    }

    /**
     * True if [trimmedContentPx] (the smaller dimension of a trimmed logo's bounding box) is
     * smaller than [targetContentPx] — the content size a specific icon format needs to fill
     * without visible upscaling.
     */
    fun needsUpscalingWarning(trimmedContentPx: Int, targetContentPx: Int): Boolean =
        trimmedContentPx < targetContentPx

    /**
     * Renders the adaptive-icon foreground layer: the trimmed [logo] scaled to
     * [FOREGROUND_CONTENT_SCALE] of a transparent [canvasPx] x [canvasPx] canvas, centered.
     */
    fun renderForeground(logo: BufferedImage, canvasPx: Int): BufferedImage {
        val canvas = BufferedImage(canvasPx, canvasPx, BufferedImage.TYPE_INT_ARGB)
        val g = canvas.createGraphics()
        configureQuality(g)
        drawTrimmedAndScaled(g, logo, canvasPx, FOREGROUND_CONTENT_SCALE)
        g.dispose()
        return canvas
    }

    /**
     * Renders a legacy launcher icon: an opaque [backgroundColor] fill (clipped to a circle when
     * [round] is true) with the trimmed [logo] scaled to [LEGACY_CONTENT_SCALE] on top.
     */
    fun renderLegacy(
        logo: BufferedImage,
        backgroundColor: Color,
        canvasPx: Int,
        round: Boolean,
    ): BufferedImage {
        val canvas = BufferedImage(canvasPx, canvasPx, BufferedImage.TYPE_INT_ARGB)
        val g = canvas.createGraphics()
        configureQuality(g)

        if (round) {
            g.clip = Ellipse2D.Float(0f, 0f, canvasPx.toFloat(), canvasPx.toFloat())
        }

        g.color = backgroundColor
        g.fillRect(0, 0, canvasPx, canvasPx)
        drawTrimmedAndScaled(g, logo, canvasPx, LEGACY_CONTENT_SCALE)
        g.dispose()
        return canvas
    }

    /**
     * Renders the splash screen icon: the trimmed [logo] scaled to [SPLASH_ICON_CONTENT_SCALE] of
     * a transparent square canvas sized off the trimmed content itself (no forced up/downscale),
     * centered. Pads a logo that would otherwise be drawn edge-to-edge so the system's icon mask
     * doesn't crop it.
     */
    fun renderSplashIcon(logo: BufferedImage): BufferedImage {
        val trimmed = trimTransparentBorder(logo)
        val canvasPx = (maxOf(trimmed.width, trimmed.height) / SPLASH_ICON_CONTENT_SCALE)
            .toInt()
            .coerceAtLeast(1)
        val canvas = BufferedImage(canvasPx, canvasPx, BufferedImage.TYPE_INT_ARGB)
        val g = canvas.createGraphics()
        configureQuality(g)
        drawTrimmedAndScaled(g, logo, canvasPx, SPLASH_ICON_CONTENT_SCALE)
        g.dispose()
        return canvas
    }

    private fun configureQuality(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }

    /** Draws [logo], trimmed and scaled to [contentScale] of [canvasPx], centered, onto [g]. */
    private fun drawTrimmedAndScaled(g: Graphics2D, logo: BufferedImage, canvasPx: Int, contentScale: Double) {
        val trimmed = trimTransparentBorder(logo)
        val targetSize = canvasPx * contentScale
        val scale = minOf(targetSize / trimmed.width, targetSize / trimmed.height)
        val drawWidth = (trimmed.width * scale).toInt().coerceAtLeast(1)
        val drawHeight = (trimmed.height * scale).toInt().coerceAtLeast(1)
        val destX = (canvasPx - drawWidth) / 2
        val destY = (canvasPx - drawHeight) / 2

        g.drawImage(
            logo,
            destX, destY, destX + drawWidth, destY + drawHeight,
            trimmed.x, trimmed.y, trimmed.x + trimmed.width, trimmed.y + trimmed.height,
            null,
        )
    }
}
