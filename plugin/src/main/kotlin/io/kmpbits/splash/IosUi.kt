package io.kmpbits.splash

/**
 * Selects which iOS UI toolkit the consuming app uses. Determines what `generateLaunchScreen`
 * emits for the transition layer; native assets (`Assets.xcassets`, `Info.plist`, `.pbxproj`
 * Resources wiring) are generated identically regardless of this value.
 */
enum class IosUi {
    /** Compose Multiplatform iOS UI. Generates `SplashInit.kt` feeding `SplashDefaults`. */
    Compose,

    /** SwiftUI iOS UI. Generates `KmpSplashView.swift` into the Xcode project instead. */
    SwiftUI,
}
