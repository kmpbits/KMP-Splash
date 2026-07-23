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
- Android: `androidx.core:core-splashscreen` **1.2.0+**, AGP **8.0+**
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
    logo = SplashLogo.resource("splash_logo.png")      // File in composeResources/drawable/ — 512×512 px recommended
    logoDark = SplashLogo.resource("logo_dark.png")    // Optional: dark mode logo
    exitAnimation = ExitAnimation.FadeOut(300)         // Optional: exit animation (Android + iOS)
    iosProjectPath = "iosApp/iosApp"                   // Optional: defaults to "iosApp/iosApp"
    androidAppPath = "androidApp"                      // Required if using the new KMP module structure
    resourcePackage = "com.example.myapp.generated.resources"  // Optional: override the inferred Compose resource package
    generateAppIcon = true                             // Optional: generate the app icon (Android + iOS) from logo + backgroundColor
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

When `androidAppPath` is set, the plugin generates everything into the module's `build/generated/kmpSplash/` folder — the resources (including dark mode), the splash Kotlin source, and the splash theme + provider — and wires all of it directly into the `androidApp` module's own build variants via AGP's Variant API. Your `src/main/` files are never modified.

This assumes `androidApp` has a project dependency on the module applying the plugin (e.g. `implementation(project(":shared"))`), so the generated splash initialization class is on its runtime classpath — which is the case for every current KMP-wizard "separate androidApp module" template.

**One required setup step:** add `evaluationDependsOn(":shared")` (replace `":shared"` with the actual path of the module that applies the splash plugin) to the top of your `androidApp` module's `build.gradle.kts`. Gradle evaluates subprojects in path-alphabetical order by default, and if `androidApp` happens to sort before the module applying the plugin, the plugin's wiring would otherwise run too late — the plugin detects this case and logs a warning naming the exact fix if you forget this step.

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

#### Custom Compose resource package

By default, the plugin reads the Compose resource package from `compose { resources { packageOfResClass = ... } }` if you've set it, or replicates Compose's own default naming if you haven't. If auto-detection doesn't find the right value for your project (e.g. an unusual module setup), override it explicitly:

```kotlin
splashScreen {
    resourcePackage = "com.example.myapp.generated.resources"
}
```

#### App icon (Android + iOS)

Set `generateAppIcon = true` to also generate the app icon on both platforms from your existing `logo` and `backgroundColor` — no separate icon assets required:

```kotlin
splashScreen {
    backgroundColor = SplashColor.white
    logo = SplashLogo.resource("logo.png")
    generateAppIcon = true
}
```

This requires `logo` to be set, in a rasterizable format (PNG, JPEG, GIF, or BMP — not WebP, `.svg`, or Android vector `.xml`). The plugin generates:

- **Android:** an adaptive icon (`mipmap-anydpi-v26`) with `backgroundColor` as the background layer and a trimmed, re-centered copy of `logo` as the foreground, legacy square/round PNG fallbacks at all five mipmap densities for devices below Android 13 (API 26), and `android:icon`/`android:roundIcon` in the manifest pointing at the generated icon.
- **iOS:** a single 1024×1024 `AppIcon.appiconset` image (Xcode 14+ derives every other required size from it automatically), composited from `logo` over `backgroundColor` the same way as Android's legacy fallback icon.

Off by default — changing your app's launcher icon is a visible, home-screen-facing change, so it's opt-in rather than automatic whenever `logo` is set. `backgroundColorNight` and `logoDark` aren't used for the icon on either platform: launchers/springboards don't resolve dark-mode resource qualifiers for app icons.

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

> [!WARNING]
> **Avoid naming your logo file `logo.png` on iOS.** The filename becomes the `UIImageName` used by `UILaunchScreen`. The name `logo` conflicts with iOS internals and causes the image to be displayed fullscreen instead of at its natural size. Use a more specific name such as `splash_logo.png`, `app_logo.png`, or `ic_splash.png`.

> [!TIP]
> **Recommended logo size: 512×512 px.** On iOS, the native launch screen renders the image at its natural point size (pixels ÷ screen scale). A 512 px PNG displays at ~171 pt on @3x iPhones — roughly 45% of the screen width, which looks correct as a centred logo. Smaller sources (e.g. 250 px) will appear too large; larger sources will appear smaller.

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

- **Android:** generates `themes.xml` (and `values-night`), copies your logo drawable, and writes a patched **copy** of the `AndroidManifest.xml` — all into the `build/` folder — to apply the splash theme and register a `ContentProvider` that initialises runtime config before your `Activity` starts. Your source files are never modified.
- **iOS:** generates the `SplashBackground` color asset and logo imageset in `Assets.xcassets`, and patches `Info.plist` and `project.pbxproj` to wire up `UILaunchScreen` — no Storyboard or Xcode required.

| Platform | Native (Booting) | Compose (Loading) |
| :--- | :--- | :--- |
| **Android** | `themes.xml` + a patched copy of `AndroidManifest.xml` generated into `build/`. Uses `installSplashScreen()`. | `SplashActivity` controls visibility and runs the exit animation via `setOnExitAnimationListener`. |
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
