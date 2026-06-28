# Changelog

## 1.1.4

### Fixed
- **AGP 9 + Gradle 9 compatibility** in `androidAppPath` mode. Previous versions used Java reflection to register the generated `res/` directory into the Android app's AGP source sets via `sourceSets.main.res.srcDir()`, which silently failed in AGP 9 (due to `getSrcFile()` removal) and in Gradle 9 (source set registration in `projectsEvaluated` is too late for variant finalization).
- The generated logo drawable is now copied directly into the app's `src/main/res/drawable/` directory as a `doFirst` action on the `preBuild` task. This avoids Gradle 9's implicit-dependency validation, which previously triggered for every AGP task that reads `src/main/res` when a `Copy` task declared it as an `@OutputDirectory`.
- Fixed `compileAndroidMain` failing with "implicit dependency" on the generated Kotlin directory — added explicit `dependsOn(generateAndroidSplash)` to all `compile*AndroidMain*` tasks.

---

## 1.1.2

### Fixed
- Android splash generation no longer modifies source files. In the `androidAppPath` (separate `androidApp` module) mode, the manifest is now patched into a **copy** under `build/` instead of in-place, and the logo drawable and `SplashInit.kt` are generated into `build/` as well. The generated directories are registered into the Android app's source sets, so AGP picks them up automatically — `src/main/` is left untouched.

---

## 1.1.1

### Changed
- Update Android splash generation to use native resource identifiers
- Update iOS SplashScreen to use intrinsic logo size and add logo naming guidelines to README
- 
---

## 1.0.0

### New
- Exit animations — `FadeOut`, `SlideUp`, `SlideDown` — on both **Android and iOS**
- `exitAnimation` property in the Gradle DSL; duration is optional with sensible defaults
- `onPreCreate()` hook in `SplashActivity` for setup that must run after `installSplashScreen()` but before `super.onCreate()` (e.g. `enableEdgeToEdge()`)
- Android manifest is now fully auto-patched: splash theme and a `ContentProvider` that initialises runtime config before any `Activity` starts — no manual manifest changes needed

### Changed
- Android exit animations use `setOnExitAnimationListener` from `core-splashscreen`, running over the system splash view
- iOS exit animations use `AnimatedVisibility` with proper easing (`LinearEasing` for fade, `FastOutLinearInEasing` for slides), replacing the previous `graphicsLayer` approach

---

## 0.2.1

### New
- `SplashLogo` type for logo configuration — use `SplashLogo.resource("logo.png")` for files in `composeResources/drawable` or `SplashLogo.path("custom/path/logo.png")` for a custom path
- `logoDark` property for dark mode logo support

### Changed
- `logoFile` and `logoFileNight` replaced by `logo` and `logoDark` — update your config accordingly

### Migration from 0.2.0

```kotlin
// Before
splashScreen {
    logoFile = "logo.png"
    logoFileNight = "logo_dark.png"
}

// After
splashScreen {
    logo = SplashLogo.resource("logo.png")
    logoDark = SplashLogo.resource("logo_dark.png")
}
```

---

## 0.2.0

### New
- `SplashColor` type for background colors — use `SplashColor.hex("#FFFFFF")`, `SplashColor.rgb(255, 255, 255)`, or named constants like `SplashColor.white` instead of raw strings
- Dark mode logo support via `logoFileNight`
- Android manifest theme is now patched automatically — no need to set it manually
- `iosProjectPath` defaults to `"iosApp/iosApp"` — no need to set it for standard project structures

### Changed
- `logoFile` now accepts just the filename (e.g. `"logo.png"`) — the plugin resolves the path from `src/commonMain/composeResources/drawable/` automatically
- `backgroundColor` and `backgroundColorNight` are now `SplashColor` instead of `String` — update your config accordingly

### Migration from 0.1.x

```kotlin
// Before
splashScreen {
    backgroundColor = "#FFFFFF"
    backgroundColorNight = "#1A1A2E"
    logoFile = "src/commonMain/composeResources/drawable/logo.png"
}

// After
splashScreen {
    backgroundColor = SplashColor.hex("#FFFFFF")
    backgroundColorNight = SplashColor.hex("#1A1A2E")
    logoFile = "logo.png"
}
```

---

## 0.1.0-alpha01

Initial release.

- Generates `Assets.xcassets` for iOS (color + logo imageset)
- Patches `Info.plist` with `UILaunchScreen`
- Patches `project.pbxproj` automatically
- Generates `themes.xml` for Android
- Dark mode background color support on both platforms
- `SplashConfig` composable for iOS transition layer
- `SplashActivity` for Android transition layer
