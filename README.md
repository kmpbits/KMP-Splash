# KMP Splash

**Professional Splash Screens for Compose Multiplatform, configured in seconds.**

Creating a seamless startup experience in Compose Multiplatform is notoriously difficult. Between the native Android `SplashScreen` API and iOS `UILaunchScreen`, developers often face a "white flash" gap between the native boot sequence and the Jetpack Compose Multiplatform runtime.

**KMP Splash** bridges this gap. It automates the generation of native splash assets and provides a Compose-ready transition layer, ensuring your app feels premium from the very first pixel.

---

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmpbits/splash-runtime.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kmpbits/splash-runtime)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

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
- Android: `androidx.core:core-splashscreen` **1.2.0+**
- iOS: Xcode 14+ (uses `UILaunchScreen` plist key)

---

## Installation

### 1. Configure the Version Catalog

In your `gradle/libs.versions.toml`:

```toml
[versions]
kmpSplash = "1.0.0"

[libraries]
kmpSplash-runtime = { module = "io.github.kmpbits:splash-runtime", version.ref = "kmpSplash" }

[plugins]
kmpSplash = { id = "io.github.kmpbits.splash", version.ref = "kmpSplash" }
```

### 2. Apply the plugin

In your Compose App module `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kmpSplash)
}

splashScreen {
    backgroundColor = "#FFFFFF"           // Light mode background
    backgroundColorNight = "#1A1A2E"      // Optional: dark mode background
    logoFile = "logo.png"                 // File name in src/commonMain/composeResources/drawable/
    logoFileNight = "logo_dark.png"       // Optional: dark mode logo
    iosProjectPath = "iosApp/iosApp"      // Optional: defaults to "iosApp/iosApp"
}
```

> **`iosProjectPath`** should point to the inner folder that contains `Info.plist` and `Assets.xcassets` — typically `iosApp/iosApp`, not the root `iosApp` folder.

### 3. Add the dependencies

In your Compose App module `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(libs.kmpSplash.runtime)
}

androidMain.dependencies {
    implementation("androidx.core:core-splashscreen:1.2.0")
}
```

### 3. Run the generation task (Optional)

KMP Splash is integrated into the Gradle build process. On both **Android** and **iOS**, the splash assets are generated automatically when you build or run your app.

If you ever want to trigger the generation manually, you can run:

```bash
# Generate iOS assets (Info.plist, pbxproj, xcassets)
./gradlew generateLaunchScreen

# Generate Android assets (themes.xml, logo)
./gradlew generateAndroidSplash
```

> [!IMPORTANT]
> **iOS Simulator Caching:** iOS heavily caches the launch screen. If you change the background color or logo and don't see the changes in the simulator, you must **restart the simulator** (or sometimes even delete and reinstall the app) for the new assets to be reflected.

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
