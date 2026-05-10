# 🚀 KMP Splash

**Professional Splash Screens for Kotlin Multiplatform, configured in seconds.**

Creating a seamless startup experience in Kotlin Multiplatform is notoriously difficult. Between the native Android `SplashScreen` API and the iOS `LaunchScreen.storyboard` (or the newer `UILaunchScreen` plist), developers often face a "white flash" gap between the native boot sequence and the Jetpack Compose Multiplatform runtime.

**KMP Splash** bridges this gap. It automates the generation of native splash assets and provides a Compose-ready transition layer, ensuring your app feels premium from the very first pixel.

---

## ✨ Why KMP Splash?

*   **🎯 Single Source of Truth:** Configure your background color and logo once in `build.gradle.kts`.
*   **📱 Native Integration:** Generates real `Assets.xcassets` for iOS and `themes.xml` for Android.
*   **🎨 Seamless Transitions:** Provides a generated `SplashConfig` composable to prevent the "flicker" when shifting from native boot to Compose UI.
*   **🛠 No Xcode Required:** Patches `.pbxproj` and `Info.plist` automatically. No more messing with Storyboards.
*   **🌓 Dark Mode Ready:** Built-in support for dark mode background colors on both Android and iOS.

---

## 📦 Installation

Add the plugin to your `build.gradle.kts` (Root or Compose App module):

```kotlin
plugins {
    id("io.kmpbits.splash") version "0.1.0"
}

splashScreen {
    backgroundColor = "#1A1A2E" // Your brand color
    backgroundColorNight = "#0F0F1B" // Optional: Dark mode for Android
    logoFile = "src/commonMain/composeResources/drawable/logo.png"
    iosProjectPath = "iosApp/iosApp" // Path to your Xcode project folder
}
```

---

## 🚀 Usage

### 1. Android Implementation
KMP Splash integrates with the official `androidx.core:core-splashscreen` library.

In your `MainActivity.kt`:

```kotlin
class MainActivity : SplashActivity() {
    
    override suspend fun isReady(): Boolean {
        // Sync data, check database, or delay
        delay(1000)
        return true
    }

    override fun onFinished() {
        setContent {
            App() // Your main Composable
        }
    }
}
```

### 2. iOS Implementation
The plugin generates a native `LaunchScreen` for the boot sequence and a `SplashConfig` composable for the Compose transition.

In your `MainViewController.kt`:

```kotlin
fun MainViewController() = ComposeUIViewController {
    var isAppReady by remember { mutableStateOf(false) }

    if (!isAppReady) {
        // Automatically uses your Gradle configuration!
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

---

## 🛠 How it Works

| Platform | Native Step (Booting) | Compose Step (Loading) |
| :--- | :--- | :--- |
| **Android** | Generates `themes.xml` and `values-night`. Uses native `installSplashScreen()`. | Controlled by `SplashActivity`. |
| **iOS** | Patches `Info.plist` with `UILaunchScreen` and generates a `SplashBackground` color asset (with dark mode support). | Generates `SplashConfig.kt` which uses `isSystemInDarkTheme()` to match. |

---

## 💡 The "Gap" Problem
When a KMP app starts on iOS, the native OS displays a static image/color immediately. Once the Kotlin runtime and Compose start (which can take 500ms+), the screen usually turns white/black until your first Composable is rendered.

**KMP Splash** ensures the `SplashConfig` rendered by Compose is **visually identical** to the native screen, providing a "fake" persistence that keeps the logo on screen until your app is actually ready to be interactive.

---

## 🤝 Contributing
Contributions are welcome! If you find a bug or have a feature request, please open an issue or a pull request.

---

## 📄 License
Copyright 2024 kmpbits

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0
