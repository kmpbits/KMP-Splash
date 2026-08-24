# Changelog

## 1.5.0

### Added
- **`installKmpSplash()` — install the splash screen without extending `SplashActivity`.** Previously, `MainActivity` had to extend `SplashActivity` to get splash behavior, which conflicted with any other required base class — most commonly `AppCompatActivity`, needed for `AppCompatDelegate.setApplicationLocales()` and other AppCompat-only APIs, since Kotlin doesn't support extending two classes. `installKmpSplash(isReady, onFinished)` is a new extension function on `ComponentActivity` that does the same work `SplashActivity.onCreate()` always did — install the splash screen, drive its visibility from `isReady`, run the configured `SplashDefaults.exitAnimation` — but as a function call instead of a base class, so it works with `AppCompatActivity` or any other custom base class. Call it at the top of your own `onCreate()`, before `super.onCreate()`. `SplashActivity` is unchanged and now implemented in terms of this function internally — existing code needs no changes; use `installKmpSplash()` only if `SplashActivity` doesn't fit your class hierarchy. See the README's "Using a Different Base Class" section for a full example.

## 1.4.2

### Fixed
- **Android splash icon no longer gets clipped by the system's SplashScreen mask.** `windowSplashScreenAnimatedIcon` was set to a straight copy of the source logo, but Android's SplashScreen API (API 31+) masks that icon into a circle and crops any artwork outside the inner ~2/3 safe zone — the same convention as adaptive launcher icons. A logo drawn edge-to-edge (no built-in transparent padding) was therefore cropped on-device even though it rendered correctly everywhere else. Raster logos (`png`/`jpg`/`jpeg`/`gif`/`bmp`) are now trimmed and re-centered onto a padded transparent canvas before being written, via a new `AppIconGenerator.renderSplashIcon()` (reusing the same trim/scale/center approach already used for the launcher icon). Vector/SVG/WebP logos, which can't be rasterized this way, are unaffected and still copied through as before. The launcher/home-screen icon (`generateAppIcon`) is untouched by this change — it already had its own padding.

## 1.4.1

### Fixed
- **`KmpSplashInitProvider`'s manifest authority is no longer a dead `${applicationId}` placeholder in application modules.** `PatchSplashManifestTask` patches AGP's `MERGED_MANIFEST` artifact, but for application variants AGP has already resolved `${applicationId}` placeholders by that stage (confirmed against sibling providers like androidx-startup's, already substituted at the same artifact). Writing the literal placeholder there meant it was never substituted, so every app using this plugin shipped the exact same authority string (`${applicationId}.kmp_splash_init`) — any two such apps installed on the same device collided with `INSTALL_FAILED_CONFLICTING_PROVIDER`. The plugin now resolves the real `applicationId` for application variants and injects it directly. Library variants are unaffected and still receive the literal placeholder, correctly resolved later when their manifest is merged into the consuming app.

## 1.4.0

### Added
- **`generateAppIcon` option to generate the app icon on Android and iOS.** Set `splashScreen { generateAppIcon = true }` to derive a launcher/home-screen icon from your existing `logo` and `backgroundColor` — no separate icon assets needed. On Android, generates the adaptive icon (`mipmap-anydpi-v26`, background color + a trimmed, re-centered foreground layer), legacy square/round PNG fallbacks at all five mipmap densities for pre-Android-13 devices, and patches `android:icon`/`android:roundIcon` into the manifest. On iOS, generates a single 1024×1024 `AppIcon.appiconset` image (Xcode 14+ derives every other required size automatically), composited from `logo` over `backgroundColor`. Off by default on both platforms.

## 1.3.0

### Changed
- **Classic mode now also uses AGP's public Variant API**, retiring the last of the plugin's reflection into AGP internals (the `sourceSets.main.res`/`sourceSets.main.manifest` reflection, including the `getSrcFile()` call that AGP 9 removed). Behavior is unchanged — same generated theme, dark mode, drawable, and provider entry — this is a mechanism swap, not a feature change. **Requires AGP 8.0+**, now a plugin-wide requirement rather than `androidAppPath`-mode-only.
- `PatchAndroidAppManifestTask` renamed to `PatchSplashManifestTask`, since it's shared by both modes now rather than being androidApp-specific. Internal rename only — not part of the public DSL.

### Fixed
- Classic mode's Variant API wiring is now registered eagerly at plugin-apply time instead of inside a deferred `afterEvaluate` callback. The deferred version raced against AGP's own internal `afterEvaluate` (registered earlier, when `com.android.application`/`com.android.library` is applied) and lost, throwing "It is too late to add actions as the callbacks already executed" for any classic-mode project. Caught by rebuilding a real sample APK during this change, not by unit tests alone.
- **Generated `KmpSplashInitProvider` no longer breaks consumers using `explicitApi()`.** The generated Android `SplashInit.kt` declared the provider with default (public) visibility, which explicit API mode rejects with a compile error. The class is now generated as `internal`, which both satisfies explicit API mode and removes the provider from the module's public Kotlin API — Android can still instantiate it from the manifest because `internal` classes remain public in JVM bytecode. The sample's `shared` module now enables `explicitApi()` as a regression guard.

---

## 1.2.0

### Fixed
- **`androidAppPath` mode now fully integrates into the target module.** Previously only the logo drawable was copied into the `androidApp` module — `values`/`values-night` (the splash theme and `backgroundColorNight`) and the manifest patch (the `android:theme` attribute and the `KmpSplashInitProvider` entry) never reached it, so dark mode and the splash theme itself silently didn't apply. This is now wired directly into the `androidApp` module via AGP's public Variant API (`onVariants`, `addGeneratedSourceDirectory`, and a `MERGED_MANIFEST` transform), replacing the old drawable-only file-copy workaround. Requires AGP 8.0+, and one required one-time setup step: add `evaluationDependsOn(":yourSharedModule")` to your `androidApp` module's `build.gradle.kts` (see README for details) — Gradle's default subproject evaluation order otherwise runs the wiring too late; the plugin logs an actionable warning if this is missing.
- **The inferred Compose resource package now matches your actual configuration.** Previously the plugin guessed `{rootProject.name}.{module path}.generated.resources`, which didn't match Compose's own default formula and broke entirely with a custom `compose.resources.packageOfResClass`. The plugin now reads the real configured value, falls back to Compose's own default formula, and exposes a new `splashScreen { resourcePackage = "..." }` override as an escape hatch.
- **`splash-runtime`'s `androidx.activity` dependency is now `api` instead of `implementation`.** Any consumer subclassing `SplashActivity` in their own module (which every consumer does) previously failed to compile with "cannot access ComponentActivity" once dependency graphs got deep enough to expose the gap — e.g. the new androidApp-module scenario above. `ComponentActivity` (the supertype `SplashActivity` extends) is now correctly exposed transitively.

---

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
