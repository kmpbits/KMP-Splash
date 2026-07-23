package io.kmpbits.splash

import java.awt.Rectangle
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

    /** Alpha values at or below this (0-255) are treated as transparent when trimming. */
    private const val ALPHA_TRIM_THRESHOLD = 10

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
     * True if [trimmedContentPx] (the smaller dimension of a trimmed logo's bounding box) is too
     * small to fill [FOREGROUND_CONTENT_SCALE] of the largest generated foreground canvas without
     * visible upscaling.
     */
    fun needsUpscalingWarning(trimmedContentPx: Int): Boolean {
        val largestCanvas = FOREGROUND_DENSITIES.values.max()
        val targetContentPx = (largestCanvas * FOREGROUND_CONTENT_SCALE).toInt()
        return trimmedContentPx < targetContentPx
    }
}
