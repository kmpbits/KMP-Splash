package io.kmpbits.splash

import io.kmpbits.splash.SplashColor.Companion.rgb
import java.io.Serializable

/**
 * Represents a color for the splash screen background.
 *
 * Create via [hex], [rgb], or use a named constant:
 * ```kotlin
 * splashScreen {
 *     backgroundColor = SplashColor.hex("#FFFFFF")
 *     backgroundColorNight = SplashColor.rgb(26, 26, 46)
 * }
 * ```
 */
class SplashColor private constructor(internal val hex: String) : Serializable {

    companion object {

        /**
         * Creates a [SplashColor] from a hex string.
         * Accepts both `#RRGGBB` and `RRGGBB` formats.
         */
        @JvmStatic
        fun hex(value: String): SplashColor {
            val clean = value.trimStart('#')
            require(clean.length == 6 && clean.all { it.isDigit() || it in 'A'..'F' || it in 'a'..'f' }) {
                "KmpSplash: invalid hex color '$value'. Expected format: #RRGGBB (e.g. #FFFFFF)"
            }
            return SplashColor("#${clean.uppercase()}")
        }

        /**
         * Creates a [SplashColor] from RGB values (0–255 each).
         */
        @JvmStatic
        fun rgb(r: Int, g: Int, b: Int): SplashColor {
            require(r in 0..255 && g in 0..255 && b in 0..255) {
                "KmpSplash: RGB values must be between 0 and 255, got ($r, $g, $b)"
            }
            return SplashColor("#%02X%02X%02X".format(r, g, b))
        }

        @JvmField val white = hex("#FFFFFF")
        @JvmField val black = hex("#000000")
    }

    override fun toString(): String = hex
}
