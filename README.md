# KMP Splash

**Professional Splash Screens for Compose Multiplatform, configured in seconds.**

Creating a seamless startup experience in Compose Multiplatform is notoriously difficult. Between the native Android `SplashScreen` API and iOS `UILaunchScreen`, developers often face a "white flash" gap between the native boot sequence and the Jetpack Compose Multiplatform runtime.

**KMP Splash** bridges this gap. It automates the generation of native splash assets and provides a Compose-ready transition layer, ensuring your app feels premium from the very first pixel.

---

## Why KMP Splash?

- **Single Source of Truth:** Configure your background color and logo once in `build.gradle.kts`.
- **Native Integration:** Generates real `Assets.xcassets` for iOS and `themes.xml` for Android.
- **Seamless Transitions:** Provides a `SplashConfig` composable to prevent the flicker when shifting from native boot to Compose UI.
- **No Xcode Required:** Patches `.pbxproj` and `Info.plist` automatically. No more Storyboards.
- **Dark Mode Ready:** Built-in support for dark mode background colors on both Android and iOS.

---

## Requirements

- Kotlin **2.1.0+**
- Compose Multiplatform **1.7.0+**
- Android: `androidx.core:core-splashscreen` **1.0.1+**
- iOS: Xcode 14+ (uses `UILaunchScreen` plist key)

---

## Installation

### 1. Apply the plugin

In your Compose App module `build.gradle.kts`:

```kotlin
plugins {
    id("io.kmpbits.splash") version "0.1.0"
}

splashScreen {
    backgroundColor = "#FFFFFF"           // Light mode background
    backgroundColorNight = "#1A1A2E"      // Optional: dark mode background
    logoFile = "src/commonMain/composeResources/drawable/logo.png"
    iosProjectPath = "iosApp/iosApp"      // Path to the folder containing Info.plist
}
```

> **`iosProjectPath`** should point to the inner folder that contains `Info.plist` and `Assets.xcassets` — typically `iosApp/iosApp`, not the root `iosApp` folder.

### 2. Add the Android dependency

In your Compose App module `build.gradle.kts`:

```kotlin
androidMain.dependencies {
    implementation("androidx.core:core-splashscreen:1.0.1")
}
```

### 3. Run the generation task

```bash
./gradlew generateLaunchScreen
```

This patches `Info.plist`, `project.pbxproj`, and generates the iOS assets. You only need to re-run it when you change the splash configuration.

---

## Usage

### Android

Extend `SplashActivity` in your `MainActivity`:

```kotlin
class MainActivity : SplashActivity() {

    override suspend fun isReady(): Boolean {
        delay(1000) // Load data, check auth, etc.
        return true
    }

    override fun onFinished() {
        setContent {
            App()
        }
    }
}
```

### iOS

Call `SplashConfig` in your `MainViewController`:

```kotlin
fun MainViewController() = ComposeUIViewController {
    var isAppReady by remember { mutableStateOf(false) }

    if (!isAppReady) {
        SplashConfig(
            isReady = {
                delay(1500) // Your initialization logic
                true
            },
            onFinished = { isAppReady = true }
        )
    } else {
        App()
    }
}
```

Colors and logo are picked up automatically from your Gradle configuration — no extra parameters needed.

---

## How it Works

| Platform | Native (Booting) | Compose (Loading) |
| :--- | :--- | :--- |
| **Android** | Generates `themes.xml` and `values-night`. Uses `installSplashScreen()`. | Controlled by `SplashActivity`. |
| **iOS** | Patches `Info.plist` with `UILaunchScreen`, generates `SplashBackground` color asset and logo imageset in `Assets.xcassets`. | `SplashConfig` uses `isSystemInDarkTheme()` to match the native screen exactly. |

---

## The "Gap" Problem

When a KMP app starts on iOS, the OS displays the native launch screen immediately. Once the Kotlin runtime and Compose initialize (which can take 500ms+), the screen usually flashes white or black before your first Composable is rendered.

**KMP Splash** ensures the `SplashConfig` composable is **visually identical** to the native launch screen, providing seamless continuity that keeps your branding on screen until the app is actually ready.

---

## Contributing

Contributions are welcome! If you find a bug or have a feature request, please open an issue or a pull request.

---

## License

Copyright 2026 KMP Bits

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
