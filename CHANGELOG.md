# Changelog

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
