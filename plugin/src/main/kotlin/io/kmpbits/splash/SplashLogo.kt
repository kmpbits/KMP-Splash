package io.kmpbits.splash

import io.kmpbits.splash.SplashLogo.Companion.path
import io.kmpbits.splash.SplashLogo.Companion.resource
import java.io.Serializable

/**
 * Represents the logo for the splash screen.
 *
 * Create via [resource] or [path]:
 * ```kotlin
 * splashScreen {
 *     logo = SplashLogo.resource("logo.png")
 *     logoDark = SplashLogo.resource("logo_dark.png")
 * }
 * ```
 */
class SplashLogo private constructor(
    internal val fileName: String,
    internal val isResource: Boolean,
) : Serializable {

    companion object {

        /**
         * Logo file located in `src/commonMain/composeResources/drawable/`.
         * Just provide the filename with extension, e.g. `"logo.png"`.
         */
        @JvmStatic
        fun resource(fileName: String): SplashLogo {
            require(fileName.isNotBlank()) {
                "KmpSplash: logo resource filename must not be blank"
            }
            require(fileName.contains('.')) {
                "KmpSplash: logo resource filename must include extension, e.g. \"logo.png\""
            }
            return SplashLogo(fileName = fileName, isResource = true)
        }

        /**
         * Logo file at a custom path relative to the module directory.
         * e.g. `"src/commonMain/composeResources/drawable/logo.png"`.
         */
        @JvmStatic
        fun path(filePath: String): SplashLogo {
            require(filePath.isNotBlank()) {
                "KmpSplash: logo path must not be blank"
            }
            return SplashLogo(fileName = filePath, isResource = false)
        }
    }

    internal fun resolvedPath(): String = if (isResource) {
        "src/commonMain/composeResources/drawable/$fileName"
    } else {
        fileName
    }

    override fun toString(): String = fileName
}
