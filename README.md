# KMP Splash

**Professional Splash Screens for Compose Multiplatform, configured in seconds.**

Creating a seamless startup experience in Compose Multiplatform is notoriously difficult. Between the native Android `SplashScreen` API and iOS `UILaunchScreen`, developers often face a "white flash" gap between the native boot sequence and the Jetpack Compose Multiplatform runtime.

**KMP Splash** bridges this gap. It automates the generation of native splash assets and provides a Compose-ready transition layer, ensuring your app feels premium from the very first pixel.

📖 **[Full documentation at kmpbits.com/libraries/kmp-splash](https://kmpbits.com/libraries/kmp-splash)**
---

[![Maven Central](https://img.shields.io/maven-central/v/io.github.kmpbits/splash-runtime.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.kmpbits/splash-runtime)
[![Tests](https://github.com/kmpbits/KMP-Splash/actions/workflows/test.yml/badge.svg)](https://github.com/kmpbits/netflow/actions/workflows/test.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Documentation](https://img.shields.io/badge/Documentation-kmpbits.com-orange)](https://kmpbits.com/libraries/kmp-splash)

---

## Why KMP Splash?

- **Single Source of Truth:** Configure your background color, logo, and exit animation once in `build.gradle.kts`.
- **Native Integration:** Generates real `Assets.xcassets` for iOS and `themes.xml` for Android.
- **Seamless Transitions:** Provides a `SplashConfig` composable to prevent the flicker when shifting from native boot to Compose UI.
- **Exit Animations:** Fade, slide up, or slide down — consistent across Android and iOS, zero extra code.
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

### 1. Configure Repositories

Ensure you have `mavenCentral()` in your `settings.gradle.kts`:

```kotlin
pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
```

### 2. Configure the Version Catalog

In your `gradle/libs.versions.toml`:

```toml
[versions]
kmpSplash = "<version>"

[libraries]
kmpSplash-runtime = { module = "io.github.kmpbits:splash-runtime", version.ref = "kmpSplash" }

[plugins]
kmpSplash = { id = "io.github.kmpbits.splash", version.ref = "kmpSplash" }
```

### 3. Apply the plugin

In your Compose App module `build.gradle.kts`:

```kotlin
plugins {
    alias(libs.plugins.kmpSplash)
}

splashScreen {
    backgroundColor = SplashColor.hex("#FFFFFF")       // Light mode background
    backgroundColorNight = SplashColor.hex("#1A1A2E")  // Optional: dark mode background
    logo = SplashLogo.resource("logo.png")             // File in composeResources/drawable/
    logoDark = SplashLogo.resource("logo_dark.png")    // Optional: dark mode logo
    exitAnimation = ExitAnimation.FadeOut(300)         // Optional: exit animation (Android + iOS)
    iosProjectPath = "iosApp/iosApp"                   // Optional: defaults to "iosApp/iosApp"
    androidAppPath = "androidApp"                      // Required if using the new KMP module structure
}
```

#### New KMP module structure (`androidApp` + `shared`)

Newer KMP project templates separate the Android entry point into a dedicated `androidApp` module, independent from `composeApp`. In this case, **`androidAppPath` is required** — without it the plugin targets the current module's `androidMain` sourcesets and the Android app module will not be configured.

```kotlin
// composeApp/build.gradle.kts
splashScreen {
    backgroundColor = SplashColor.white
    androidAppPath = "androidApp"   // Path to the androidApp module, relative to the root project
}
```

When `androidAppPath` is set, the plugin writes the generated resources and Kotlin sources directly into `androidApp/src/main/` (which AGP picks up automatically) and patches `AndroidManifest.xml` in-place. The `preBuild` task in `androidApp` is automatically wired to run `generateAndroidSplash` first — no manual setup required.

#### SplashColor alternatives

```kotlin
SplashColor.hex("#FFFFFF")        // Hex string — accepts both #RRGGBB and RRGGBB
SplashColor.rgb(255, 255, 255)    // RGB values (0–255 each)
SplashColor.white                 // Named constant
SplashColor.black                 // Named constant
```

#### SplashLogo alternatives

```kotlin
SplashLogo.resource("logo.png")                              // File in composeResources/drawable/
SplashLogo.path("src/commonMain/composeResources/drawable/logo.png")  // Custom path relative to module
```

#### ExitAnimation options

```kotlin
ExitAnimation.None                // No animation — splash disappears instantly (default)
ExitAnimation.FadeOut(300)        // Fade out over 300ms
ExitAnimation.SlideUp(400)        // Slide upward to reveal the app
ExitAnimation.SlideDown(400)      // Slide downward to reveal the app
```

The duration parameter is optional — the values above are the defaults.

> **`iosProjectPath`** should point to the inner folder that contains `Info.plist` and `Assets.xcassets` — typically `iosApp/iosApp`, not the root `iosApp` folder.

> **`androidAppPath`** is required when your project uses the new KMP module structure where the Android app lives in a dedicated module separate from `composeApp`. Set it to the path of that module relative to the root project (e.g. `"androidApp"`). Leave it unset for the classic structure where Android is part of `composeApp`.

### 4. Add the dependencies

In your Compose App module `build.gradle.kts`:

```kotlin
commonMain.dependencies {
    implementation(libs.kmpSplash.runtime)
}

androidMain.dependencies {
    implementation("androidx.core:core-splashscreen:1.2.0")
}
```

### 5. Run the generation task (Optional)

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

#### Edge-to-Edge

If you need to call `enableEdgeToEdge()`, override `onPreCreate()` instead of `onCreate()`. This hook runs at exactly the right moment — after `installSplashScreen()` but before `super.onCreate()` — which is the order Android requires:

```kotlin
class MainActivity : SplashActivity() {

    override fun onPreCreate() {
        enableEdgeToEdge()
    }

    override suspend fun isReady(): Boolean { ... }

    override fun onFinished() { ... }
}
```

> [!IMPORTANT]
> Do **not** call `enableEdgeToEdge()` inside your own `onCreate()` override. Doing so runs it before `installSplashScreen()`, which causes a stray toolbar to appear on the first frame.

### iOS

Call `SplashConfig` in your `MainViewController`, passing your app content as the trailing lambda:

```kotlin
fun MainViewController() = ComposeUIViewController {
    SplashConfig(
        isReady = {
            delay(1500) // Your initialization logic
            true
        }
    ) {
        App()
    }
}
```

`SplashConfig` manages the splash/content transition internally — no state boilerplate needed. Colors and logo are picked up automatically from your Gradle configuration.

---

## How it Works

The Gradle plugin does the heavy lifting at build time so you never touch XML or native config files manually:

- **Android:** generates `themes.xml` (and `values-night`), copies your logo drawable, patches the `AndroidManifest.xml` to apply the splash theme, and registers a `ContentProvider` that initialises runtime config before your `Activity` starts.
- **iOS:** generates the `SplashBackground` color asset and logo imageset in `Assets.xcassets`, and patches `Info.plist` and `project.pbxproj` to wire up `UILaunchScreen` — no Storyboard or Xcode required.

| Platform | Native (Booting) | Compose (Loading) |
| :--- | :--- | :--- |
| **Android** | `themes.xml` + auto-patched `AndroidManifest.xml`. Uses `installSplashScreen()`. | `SplashActivity` controls visibility and runs the exit animation via `setOnExitAnimationListener`. |
| **iOS** | Patches `Info.plist` with `UILaunchScreen`, generates `SplashBackground` color asset and logo imageset in `Assets.xcassets`. | `SplashConfig` uses `isSystemInDarkTheme()` to match the native screen exactly, then animates the exit with `AnimatedVisibility`. |

---

## The "Gap" Problem

When a KMP app starts on iOS, the OS displays the native launch screen immediately. Once the Kotlin runtime and Compose initialize (which can take 500ms+), the screen usually flashes white or black before your first Composable is rendered.

**KMP Splash** ensures the `SplashConfig` composable is **visually identical** to the native launch screen, providing seamless continuity that keeps your branding on screen until the app is actually ready.

---

## Known Limitations

**App-level dark mode overrides**

If your app has its own appearance setting (e.g. a dark mode toggle independent of the system setting), the native splash screen will not respect it. Both iOS `UILaunchScreen` and Android's `SplashScreen` API are rendered by the OS before any app code runs, they read the system dark mode setting directly. There is no way for any library to work around this.

The Compose layer (`SplashConfig` / `SplashActivity`) does run app code, so it can respond to your app's own preference. For the native layer, the options are:

- Use a single background color that works in both light and dark modes
- Accept the brief mismatch, the native splash shows the system color, and the Compose layer immediately corrects to your app's preferred color

This is a system limitation, not a bug in the library.

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
