package io.kmpbits.splash

/** Sealed type representing where the app should navigate after the splash. */
sealed interface FirstDestination {
    data object Onboarding : FirstDestination
    data object Home : FirstDestination
}
