package io.kmpbits.splash

/**
 * Selects which UI toolkit the consuming app's iOS frontend uses. Determines what
 * `generateLaunchScreen` emits for the transition layer; native assets (`Assets.xcassets`,
 * `Info.plist`, `.pbxproj` Resources wiring) are generated identically regardless of this value.
 *
 * Only the iOS side branches on this — Android is always Compose.
 */
enum class UiFramework {
    /** Compose Multiplatform iOS UI. Generates `SplashInit.kt` feeding `SplashDefaults`. */
    Compose,

    /** Native SwiftUI iOS UI. Generates `KmpSplashView.swift` into the Xcode project instead. */
    Native,
}
