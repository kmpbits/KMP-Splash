# Android Adaptive App Icon Generation — Design

Date: 2026-07-23

## Problem Statement

`splashScreen { }` generates the splash screen (theme, background color, logo drawable) on Android, but it doesn't touch the app's launcher icon. Users who want an adaptive icon (`<adaptive-icon>` with a background layer and a foreground layer, required for a modern launcher appearance and for Android 12+'s icon-derived splash) have to build it themselves with Android Studio's Image Asset wizard, entirely outside the plugin.

The plugin already has the assets needed to build one: `logo` (a `SplashLogo`) and `backgroundColor` (a `SplashColor`), both mandatory-adjacent inputs of the splash screen itself. The core insight of this design is that these are the same conceptual assets an adaptive icon needs — foreground artwork and a background layer — so no new asset fields are required.

## Scope

Android only. Generate:
- An adaptive icon (`mipmap-anydpi-v26`) referencing a color background and a foreground drawable derived from `logo`.
- Legacy square + round PNG icons at all five mipmap densities, for API < 26 devices, composited from the same `logo` and `backgroundColor`.
- The manifest `android:icon` / `android:roundIcon` attributes pointing at the generated icon.

Out of scope:
- iOS `AppIcon.appiconset` generation. The DSL is intentionally not iOS-hostile (it lives under the existing platform-neutral `splashScreen { }` block), but no iOS work is implied or planned by this spec.
- Themed/monochrome icons (Android 13+ `<monochrome>` layer). Skipped for now — see "Rejected alternatives." Can be added later as a purely additive, optional field with no interaction with this design.
- Making `logo` mandatory on `KmpSplashExtension`. Considered and explicitly rejected — see below.
- `backgroundColorNight` for the icon. Launchers don't resolve `-night` resource qualifiers for launcher icons, so honoring it would silently produce a resource that's never loaded. The icon always uses `backgroundColor` (light) regardless of system theme.

## 1. DSL

One new property on `KmpSplashExtension`:

```kotlin
splashScreen {
    backgroundColor = SplashColor.hex("#FFFFFF")
    logo = SplashLogo.resource("logo.png")
    generateAppIcon = true   // default false
}
```

- `generateAppIcon: Property<Boolean>`, `@get:Input`, conventioned to `false`. Off by default: flipping the user's launcher icon is a visible, home-screen-facing change and must be opt-in, not something that happens silently on a plugin upgrade.
- `generateAppIcon = true` with `logo` unset is a configuration error. The generating task fails fast with a `GradleException` naming the missing property, in the same style as `GenerateAndroidSplashTask`'s existing "`backgroundColor` is mandatory" check.
- `logo` stays optional on `KmpSplashExtension` itself — a plain-color splash with no logo remains a valid, non-breaking configuration. The mandatory-ness only applies at the point `generateAppIcon` is turned on.

### Format constraint

`generateAppIcon` requires `logo` to point at a raster format `javax.imageio.ImageIO` can read out of the box: PNG, JPEG, GIF, or BMP. It cannot read WebP and cannot rasterize vector formats (`.svg`, Android `.xml` vector drawables). If `generateAppIcon` is on and the logo file's extension isn't one of `.png`/`.jpg`/`.jpeg`/`.gif`/`.bmp`, the task fails with a `GradleException` naming the file and the supported formats. Splash generation itself is unaffected — it only copies the file — so this constraint applies only when `generateAppIcon = true`.

### Upscaling warning

The largest generated asset is the `xxxhdpi` foreground at 432×432px, of which the trimmed logo fills 60% (~259×259px). If the trimmed logo's bounding box is smaller than that in either dimension, the task logs a Gradle warning naming the actual trimmed size and the target size, and proceeds anyway (upscaled, not blocked) — a visible warning is preferable to either a silent quality loss or a hard failure over something recoverable.

## 2. Icon geometry

An adaptive icon foreground lives on a 108dp canvas; content must stay inside the inner 66dp "safe zone" (~61%) to avoid being clipped by launcher-specific masks (circle, squircle, rounded square, etc.). This design fits the logo to 60% of the canvas, leaving a small buffer inside the 66% safe zone.

For each of the five standard mipmap densities (`mdpi` 48dp/px, `hdpi` 72px, `xhdpi` 96px, `xxhdpi` 144px, `xxxhdpi` 192px), the adaptive foreground is rendered at 2.25× that value (108dp canvas vs. 48dp legacy canvas), i.e. 108/162/216/324/432px.

Fitting algorithm, implemented once in `AppIconGenerator` and shared by adaptive-foreground and legacy rendering:
1. Decode the logo with `ImageIO.read`.
2. Compute its opaque bounding box (`trimTransparentBorder`): scan for the smallest rectangle containing all pixels with alpha above a small threshold. This handles both tightly-cropped logos and logos with generous transparent margins (common for splash artwork) with a single rule, rather than requiring the user to pre-crop their asset specifically for icon use.
3. Scale that trimmed content to fill 60% of the target canvas (adaptive foreground) or 72% (legacy, since the legacy canvas has no launcher mask eating into it), preserving aspect ratio, and center it.

Legacy icons composite the same trimmed-and-scaled logo over an opaque `backgroundColor` fill, at the 48dp-based legacy densities. The round variant additionally clips the composited square to a circle.

## 3. Generated files

All written into the existing `build/generated/kmpSplash/androidMain/res` directory, already wired into `variant.sources.res` alongside the splash screen's own generated resources — no new source directory needed.

| Path | Content |
|---|---|
| `mipmap-anydpi-v26/ic_kmp_app_icon.xml` | `<adaptive-icon>` referencing `@color/kmp_app_icon_background` + `@mipmap/ic_kmp_app_icon_foreground` |
| `mipmap-anydpi-v26/ic_kmp_app_icon_round.xml` | Same two layers — launchers apply their own mask; a separate round-specific adaptive-icon XML isn't needed |
| `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_kmp_app_icon_foreground.png` | Adaptive foreground, transparent background, trimmed logo scaled to 60% of the 108dp canvas |
| `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_kmp_app_icon.png` | Legacy square icon: trimmed logo at 72%, composited over `backgroundColor` |
| `mipmap-{m,h,xh,xxh,xxxh}dpi/ic_kmp_app_icon_round.png` | Same as above, circle-clipped |
| `values/kmp_app_icon.xml` | `<color name="kmp_app_icon_background">#RRGGBB</color>` |

The plugin-owned name `ic_kmp_app_icon` (rather than the conventional `ic_launcher`) is deliberate: it avoids a duplicate-resource build failure against the app module's own pre-existing `src/main/res/mipmap/ic_launcher`, the same technique `GenerateAndroidSplashTask` already uses for `ic_kmp_splash_logo` to avoid colliding with a user's own drawable of a similar name.

### Manifest

`PatchSplashManifestTask` gains an `@get:Input iconEnabled: Property<Boolean>` input. `AndroidSplashTemplate.generateManifest()` gains a parameter that, when true, sets (or overwrites, matching the existing deterministic-overwrite behavior for `android:theme`) `android:icon="@mipmap/ic_kmp_app_icon"` and `android:roundIcon="@mipmap/ic_kmp_app_icon_round"` on `<application>`. When `iconEnabled` is false, the manifest is untouched by this feature — existing `android:icon` (if any) is left exactly as-is.

## 4. Code structure

**New: `AppIconGenerator.kt`** (`plugin/src/main/kotlin/io/kmpbits/splash/`)

Pure image logic — no Gradle types — so it's testable with plain JUnit, no `ProjectBuilder` harness:
- `trimTransparentBorder(image: BufferedImage): Rectangle`
- `renderForeground(logo: BufferedImage, canvasPx: Int): BufferedImage` — transparent background, trimmed logo at 60%, centered
- `renderLegacy(logo: BufferedImage, backgroundColor: Color, canvasPx: Int, round: Boolean): BufferedImage` — trimmed logo at 72%, composited over `backgroundColor`, optionally circle-clipped
- Built on `javax.imageio.ImageIO` and `java.awt.Graphics2D` only — no new dependency.

**New: `GenerateAppIconTask.kt`** (`plugin/src/main/kotlin/io/kmpbits/splash/tasks/`)

A separate task from `GenerateAndroidSplashTask`, mirroring its shape (`@InputFile logoSourceFile`, `@Input backgroundColor`, `@OutputDirectory resOutputDir`), because its inputs (`generateAppIcon` flag) and output subtree (icon mipmaps vs. splash theme/drawables) are both distinct. `@TaskAction` validates the format/upscaling constraints above and delegates all pixel work to `AppIconGenerator`. Following the existing convention (e.g. how `backgroundColorNight` is handled), the task always registers; it no-ops when `generateAppIcon` is false rather than the plugin conditionally skipping registration.

**`AndroidSplashTemplate.kt`**: new `generateAdaptiveIconXml()`; `generateManifest()` gains the `iconEnabled` parameter described above.

**`KmpSplashPlugin.kt`**: `registerAndroidTask` registers `generateAppIcon` (the new task) alongside the existing `generateAndroidSplash` task, wires its `resOutputDir` into `variant.sources.res` via the same `wireVariantResourcesAndManifest` helper used today, and threads `iconEnabled` through to the `PatchSplashManifestTask` registration in that same helper.

## 5. Testing

- **`AppIconGeneratorTest`** (plain JUnit): trimming a padded logo returns the correct bounding box; scaling math is correct at each density; legacy composite pixel-samples confirm background fill and foreground placement; round variant is circle-clipped at the edges.
- **`GenerateAppIconTaskTest`** (mirrors existing `GenerateAndroidSplashTaskTest`): flag-off is a no-op; flag-on produces the full expected file tree; missing-logo and unsupported-format both fail with the expected `GradleException` message.
- **`PatchSplashManifestTaskTest`** (not `KmpSplashFunctionalTest` — revised during implementation): asserts that `iconEnabled = true` results in a patched manifest containing `android:icon="@mipmap/ic_kmp_app_icon"` and `android:roundIcon="@mipmap/ic_kmp_app_icon_round"`, overwriting any existing `android:icon`. `KmpSplashFunctionalTest` exercises `GradleRunner`/`GradleTestKit` but never applies `com.android.application`/`com.android.library` for any existing task in this codebase (including the pre-existing `android:theme` manifest patching) — doing so would need a real Android SDK in the test environment. `PatchSplashManifestTask` is exercised directly instead, the same way the existing theme-patching behavior already is, which is sufficient to cover `AndroidSplashTemplate.generateManifest()`'s icon-patching logic without that dependency.
- Upscaling warning: unit test asserting the warning log fires when the trimmed logo is smaller than the `xxxhdpi` foreground target, and does not fire otherwise.

## Rejected alternatives

**Foreground + background as two separate image assets.** Considered first; rejected because it doubles the assets the user must prepare, and the user explicitly asked to reuse the existing `logo`/`backgroundColor` fields rather than add new ones.

**Single square image, plugin splits it into layers.** Rejected — without a foreground/background distinction supplied by the user, there's no reliable way to separate "background" from "content" in an arbitrary square image; would produce badly-cropped icons.

**Deriving the monochrome (themed icon) layer automatically from the logo.** Considered so Android 13+ themed icons would be supported without a new asset. Rejected: Android expects a single-color flat silhouette for this layer, and auto-thresholding a full-color or detailed logo typically produces an unrecognizable blob — worse than simply not having a themed icon (in which case the launcher falls back to the normal icon, a fully graceful degradation). A future optional `logoMonochrome` field remains possible as a purely additive change.

**Making `logo` mandatory on `KmpSplashExtension`.** Raised during design: isn't a splash without a logo strange? Rejected as unrelated and breaking — some users intentionally run a brand-color-only splash with no logo, and forcing `logo` would break their builds on upgrade with no functional bug to justify it. The actual constraint this feature needs (can't generate an icon without a logo) is already enforced narrowly, only when `generateAppIcon = true`.

**Adaptive icon only, no legacy PNGs, requiring `minSdk 26`.** Rejected — Compose Multiplatform templates commonly target `minSdk 24`, and raising the plugin's effective minimum just for this feature would be a surprising, unrelated constraint on consumers.

**Adaptive icon only, falling back to AGP's default `@mipmap/ic_launcher` on pre-26 devices.** Rejected — this makes the icon differ by API level unless the user separately maintains their own legacy icon, defeating the purpose of generating one automatically.
