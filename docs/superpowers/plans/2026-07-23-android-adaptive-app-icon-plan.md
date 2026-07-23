# Android Adaptive App Icon Generation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Generate an Android adaptive app icon (background color + foreground layer) and legacy square/round PNG fallbacks for API < 26, derived entirely from the existing `logo` and `backgroundColor` splash screen inputs, gated behind a new `generateAppIcon` opt-in flag.

**Architecture:** A new pure-Kotlin `AppIconGenerator` object (Java2D/`ImageIO`, no Gradle types) trims the logo's transparent border and renders it at each required density. A new `GenerateAppIconTask` drives it and writes `mipmap-anydpi-v26/*.xml`, `mipmap-*dpi/*.png`, and `values/kmp_app_icon.xml` into a new generated res directory, wired into `variant.sources.res` the same way `GenerateAndroidSplashTask` already is. `AndroidSplashTemplate.generateManifest()` gains an `iconEnabled` parameter that patches `android:icon`/`android:roundIcon` the same deterministic way it already patches `android:theme`, threaded through `PatchSplashManifestTask`.

**Tech Stack:** `javax.imageio.ImageIO` + `java.awt.Graphics2D` (JDK-only, no new dependency), AGP Variant API (already a `compileOnly` dependency).

**Spec:** `docs/superpowers/specs/2026-07-23-android-adaptive-app-icon-design.md`

---

### Task 1: `AppIconGenerator` — trimming and upscaling check

**Files:**
- Create: `plugin/src/main/kotlin/io/kmpbits/splash/AppIconGenerator.kt`
- Create: `plugin/src/test/kotlin/io/kmpbits/splash/AppIconGeneratorTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `plugin/src/test/kotlin/io/kmpbits/splash/AppIconGeneratorTest.kt`:

```kotlin
package io.kmpbits.splash

import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppIconGeneratorTest {

    private fun paddedTestLogo(): BufferedImage {
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color.RED
        g.fillRect(10, 10, 20, 20)
        g.dispose()
        return image
    }

    @Test
    fun `trimTransparentBorder returns the opaque bounding box`() {
        val rect = AppIconGenerator.trimTransparentBorder(paddedTestLogo())

        assertEquals(10, rect.x)
        assertEquals(10, rect.y)
        assertEquals(20, rect.width)
        assertEquals(20, rect.height)
    }

    @Test
    fun `trimTransparentBorder falls back to full bounds for a fully transparent image`() {
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)

        val rect = AppIconGenerator.trimTransparentBorder(image)

        assertEquals(0, rect.x)
        assertEquals(0, rect.y)
        assertEquals(40, rect.width)
        assertEquals(40, rect.height)
    }

    @Test
    fun `needsUpscalingWarning is true for content smaller than the largest foreground target`() {
        assertTrue(AppIconGenerator.needsUpscalingWarning(20))
    }

    @Test
    fun `needsUpscalingWarning is false for content at or above the largest foreground target`() {
        assertFalse(AppIconGenerator.needsUpscalingWarning(300))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.AppIconGeneratorTest"`
Expected: FAIL — `AppIconGenerator` is unresolved (the class doesn't exist yet).

- [ ] **Step 3: Create `AppIconGenerator.kt` with the trimming and upscaling logic**

Create `plugin/src/main/kotlin/io/kmpbits/splash/AppIconGenerator.kt`:

```kotlin
package io.kmpbits.splash

import java.awt.Rectangle
import java.awt.image.BufferedImage

/**
 * Renders Android app icon assets (adaptive foreground + legacy square/round PNGs) from a single
 * source logo image, trimming its transparent border and re-centering it into each icon's safe
 * zone. Pure image logic — no Gradle types — so it's testable without a Gradle project.
 */
internal object AppIconGenerator {

    /** Fraction of the 108dp adaptive-icon canvas the trimmed logo is scaled to fill. */
    const val FOREGROUND_CONTENT_SCALE = 0.60

    /** Fraction of the legacy (no launcher mask) icon canvas the trimmed logo is scaled to fill. */
    const val LEGACY_CONTENT_SCALE = 0.72

    /** Alpha values at or below this (0-255) are treated as transparent when trimming. */
    private const val ALPHA_TRIM_THRESHOLD = 10

    /** Legacy (pre-adaptive-icon) launcher icon size in px, keyed by density qualifier. */
    val LEGACY_DENSITIES: Map<String, Int> = mapOf(
        "mdpi" to 48, "hdpi" to 72, "xhdpi" to 96, "xxhdpi" to 144, "xxxhdpi" to 192,
    )

    /** Adaptive-icon foreground canvas size in px (108dp vs. legacy's 48dp = 2.25x), keyed by density. */
    val FOREGROUND_DENSITIES: Map<String, Int> =
        LEGACY_DENSITIES.mapValues { (_, legacyPx) -> (legacyPx * 2.25).toInt() }

    /**
     * Returns the smallest rectangle containing every pixel whose alpha exceeds
     * [ALPHA_TRIM_THRESHOLD]. Falls back to the full image bounds if the image has no
     * sufficiently opaque pixel at all (e.g. a blank placeholder).
     */
    fun trimTransparentBorder(image: BufferedImage): Rectangle {
        var minX = image.width
        var minY = image.height
        var maxX = -1
        var maxY = -1

        for (y in 0 until image.height) {
            for (x in 0 until image.width) {
                val alpha = (image.getRGB(x, y) ushr 24) and 0xFF
                if (alpha > ALPHA_TRIM_THRESHOLD) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                    if (y < minY) minY = y
                    if (y > maxY) maxY = y
                }
            }
        }

        return if (maxX < minX || maxY < minY) {
            Rectangle(0, 0, image.width, image.height)
        } else {
            Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1)
        }
    }

    /**
     * True if [trimmedContentPx] (the smaller dimension of a trimmed logo's bounding box) is too
     * small to fill [FOREGROUND_CONTENT_SCALE] of the largest generated foreground canvas without
     * visible upscaling.
     */
    fun needsUpscalingWarning(trimmedContentPx: Int): Boolean {
        val largestCanvas = FOREGROUND_DENSITIES.values.max()
        val targetContentPx = (largestCanvas * FOREGROUND_CONTENT_SCALE).toInt()
        return trimmedContentPx < targetContentPx
    }
}
```

- [ ] **Step 4: Run the test to verify it passes**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.AppIconGeneratorTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add plugin/src/main/kotlin/io/kmpbits/splash/AppIconGenerator.kt plugin/src/test/kotlin/io/kmpbits/splash/AppIconGeneratorTest.kt
git commit -m "Add AppIconGenerator trimming and upscaling-check logic"
```

---

### Task 2: `AppIconGenerator` — foreground and legacy rendering

**Files:**
- Modify: `plugin/src/main/kotlin/io/kmpbits/splash/AppIconGenerator.kt`
- Modify: `plugin/src/test/kotlin/io/kmpbits/splash/AppIconGeneratorTest.kt`

- [ ] **Step 1: Write the failing tests**

In `plugin/src/test/kotlin/io/kmpbits/splash/AppIconGeneratorTest.kt`, change the import block:

```kotlin
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
```

to:

```kotlin
import java.awt.Color
import java.awt.image.BufferedImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private fun BufferedImage.argbAt(x: Int, y: Int): Int = getRGB(x, y)
private fun Int.alpha(): Int = (this ushr 24) and 0xFF
private fun Int.red(): Int = (this ushr 16) and 0xFF
```

Then add these tests just before the final closing `}` of the `AppIconGeneratorTest` class:

```kotlin
    @Test
    fun `renderForeground centers the trimmed logo on a transparent canvas`() {
        val foreground = AppIconGenerator.renderForeground(paddedTestLogo(), canvasPx = 100)

        assertEquals(0, foreground.argbAt(0, 0).alpha(), "expected the canvas corner to stay transparent")
        assertTrue(foreground.argbAt(50, 50).alpha() > 0, "expected the canvas center to be drawn")
        assertTrue(foreground.argbAt(50, 50).red() > 150, "expected the drawn logo's red channel to dominate")
    }

    @Test
    fun `renderLegacy composites the trimmed logo over an opaque background`() {
        val legacy = AppIconGenerator.renderLegacy(paddedTestLogo(), Color.WHITE, canvasPx = 100, round = false)

        val corner = legacy.argbAt(0, 0)
        assertEquals(255, corner.alpha(), "expected the corner to be filled with the opaque background")
        assertEquals(255, corner.red(), "expected the corner to be the white background color")
        assertTrue(legacy.argbAt(50, 50).red() > 150, "expected the drawn logo's red channel to dominate at the center")
    }

    @Test
    fun `renderLegacy with round clips corners to transparent`() {
        val legacy = AppIconGenerator.renderLegacy(paddedTestLogo(), Color.WHITE, canvasPx = 100, round = true)

        assertEquals(0, legacy.argbAt(0, 0).alpha(), "expected the corner to be clipped outside the circle")
        assertTrue(legacy.argbAt(50, 50).alpha() > 0, "expected the center to remain inside the circle")
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.AppIconGeneratorTest"`
Expected: FAIL — `renderForeground` and `renderLegacy` are unresolved.

- [ ] **Step 3: Add the rendering functions to `AppIconGenerator.kt`**

Change:

```kotlin
package io.kmpbits.splash

import java.awt.Rectangle
import java.awt.image.BufferedImage
```

to:

```kotlin
package io.kmpbits.splash

import java.awt.Color
import java.awt.Graphics2D
import java.awt.Rectangle
import java.awt.RenderingHints
import java.awt.geom.Ellipse2D
import java.awt.image.BufferedImage
```

Then, add these functions just before the final closing `}` of the `AppIconGenerator` object:

```kotlin

    /**
     * Renders the adaptive-icon foreground layer: the trimmed [logo] scaled to
     * [FOREGROUND_CONTENT_SCALE] of a transparent [canvasPx] x [canvasPx] canvas, centered.
     */
    fun renderForeground(logo: BufferedImage, canvasPx: Int): BufferedImage {
        val canvas = BufferedImage(canvasPx, canvasPx, BufferedImage.TYPE_INT_ARGB)
        val g = canvas.createGraphics()
        configureQuality(g)
        drawTrimmedAndScaled(g, logo, canvasPx, FOREGROUND_CONTENT_SCALE)
        g.dispose()
        return canvas
    }

    /**
     * Renders a legacy launcher icon: an opaque [backgroundColor] fill (clipped to a circle when
     * [round] is true) with the trimmed [logo] scaled to [LEGACY_CONTENT_SCALE] on top.
     */
    fun renderLegacy(
        logo: BufferedImage,
        backgroundColor: Color,
        canvasPx: Int,
        round: Boolean,
    ): BufferedImage {
        val canvas = BufferedImage(canvasPx, canvasPx, BufferedImage.TYPE_INT_ARGB)
        val g = canvas.createGraphics()
        configureQuality(g)

        if (round) {
            g.clip = Ellipse2D.Float(0f, 0f, canvasPx.toFloat(), canvasPx.toFloat())
        }

        g.color = backgroundColor
        g.fillRect(0, 0, canvasPx, canvasPx)
        drawTrimmedAndScaled(g, logo, canvasPx, LEGACY_CONTENT_SCALE)
        g.dispose()
        return canvas
    }

    private fun configureQuality(g: Graphics2D) {
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
    }

    /** Draws [logo], trimmed and scaled to [contentScale] of [canvasPx], centered, onto [g]. */
    private fun drawTrimmedAndScaled(g: Graphics2D, logo: BufferedImage, canvasPx: Int, contentScale: Double) {
        val trimmed = trimTransparentBorder(logo)
        val targetSize = canvasPx * contentScale
        val scale = minOf(targetSize / trimmed.width, targetSize / trimmed.height)
        val drawWidth = (trimmed.width * scale).toInt().coerceAtLeast(1)
        val drawHeight = (trimmed.height * scale).toInt().coerceAtLeast(1)
        val destX = (canvasPx - drawWidth) / 2
        val destY = (canvasPx - drawHeight) / 2

        g.drawImage(
            logo,
            destX, destY, destX + drawWidth, destY + drawHeight,
            trimmed.x, trimmed.y, trimmed.x + trimmed.width, trimmed.y + trimmed.height,
            null,
        )
    }
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.AppIconGeneratorTest"`
Expected: `BUILD SUCCESSFUL`, 7 tests passed.

- [ ] **Step 5: Commit**

```bash
git add plugin/src/main/kotlin/io/kmpbits/splash/AppIconGenerator.kt plugin/src/test/kotlin/io/kmpbits/splash/AppIconGeneratorTest.kt
git commit -m "Add AppIconGenerator adaptive foreground and legacy icon rendering"
```

---

### Task 3: `AndroidSplashTemplate` and `PatchSplashManifestTask` — icon XML and manifest attributes

**Files:**
- Modify: `plugin/src/main/kotlin/io/kmpbits/splash/template/AndroidSplashTemplate.kt`
- Modify: `plugin/src/main/kotlin/io/kmpbits/splash/tasks/PatchSplashManifestTask.kt`
- Modify: `plugin/src/test/kotlin/io/kmpbits/splash/tasks/PatchSplashManifestTaskTest.kt`

- [ ] **Step 1: Write the failing manifest tests**

Replace the entire content of `plugin/src/test/kotlin/io/kmpbits/splash/tasks/PatchSplashManifestTaskTest.kt` with:

```kotlin
package io.kmpbits.splash.tasks

import org.gradle.testfixtures.ProjectBuilder
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class PatchSplashManifestTaskTest {

    @Test
    fun `patch adds theme and provider, overwriting an existing theme attribute`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("patchTest", PatchSplashManifestTask::class.java)

        val input = File(project.projectDir, "input/AndroidManifest.xml").also {
            it.parentFile.mkdirs()
            it.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
                |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
                |    <application android:theme="@style/AppTheme">
                |    </application>
                |</manifest>
                """.trimMargin()
            )
        }
        val output = File(project.projectDir, "output/AndroidManifest.xml")

        task.mergedManifest.set(input)
        task.updatedManifest.set(output)
        task.patch()

        val result = output.readText()
        assertTrue(result.contains("Theme.App.SplashScreen"), "expected the splash theme to be applied")
        assertTrue(
            result.contains("io.kmpbits.splash.KmpSplashInitProvider"),
            "expected the provider entry to be injected"
        )
        assertTrue(!result.contains("@style/AppTheme"), "expected the app's own theme to be overwritten, not merged")
    }

    @Test
    fun `patch does not add icon attributes when iconEnabled is false`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("patchTest", PatchSplashManifestTask::class.java)

        val input = File(project.projectDir, "input/AndroidManifest.xml").also {
            it.parentFile.mkdirs()
            it.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
                |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
                |    <application>
                |    </application>
                |</manifest>
                """.trimMargin()
            )
        }
        val output = File(project.projectDir, "output/AndroidManifest.xml")

        task.mergedManifest.set(input)
        task.updatedManifest.set(output)
        task.iconEnabled.set(false)
        task.patch()

        assertTrue(!output.readText().contains("android:icon"), "expected no icon attribute when iconEnabled is false")
    }

    @Test
    fun `patch adds icon attributes, overwriting an existing icon attribute, when iconEnabled is true`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("patchTest", PatchSplashManifestTask::class.java)

        val input = File(project.projectDir, "input/AndroidManifest.xml").also {
            it.parentFile.mkdirs()
            it.writeText(
                """<?xml version="1.0" encoding="utf-8"?>
                |<manifest xmlns:android="http://schemas.android.com/apk/res/android">
                |    <application android:icon="@mipmap/ic_launcher">
                |    </application>
                |</manifest>
                """.trimMargin()
            )
        }
        val output = File(project.projectDir, "output/AndroidManifest.xml")

        task.mergedManifest.set(input)
        task.updatedManifest.set(output)
        task.iconEnabled.set(true)
        task.patch()

        val result = output.readText()
        assertTrue(
            result.contains("""android:icon="@mipmap/ic_kmp_app_icon""""),
            "expected the generated icon to be set, got:\n$result"
        )
        assertTrue(
            result.contains("""android:roundIcon="@mipmap/ic_kmp_app_icon_round""""),
            "expected the generated round icon to be set, got:\n$result"
        )
        assertTrue(!result.contains("@mipmap/ic_launcher"), "expected the app's own icon to be overwritten, not merged")
    }
}
```

- [ ] **Step 2: Run the tests to verify the new ones fail**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.tasks.PatchSplashManifestTaskTest"`
Expected: FAIL — `task.iconEnabled` doesn't exist yet on `PatchSplashManifestTask`.

- [ ] **Step 3: Add `generateAdaptiveIconXml`/`generateAppIconColor` and the `iconEnabled` parameter to `AndroidSplashTemplate.kt`**

Replace the entire content of `plugin/src/main/kotlin/io/kmpbits/splash/template/AndroidSplashTemplate.kt` with:

```kotlin
package io.kmpbits.splash.template

internal object AndroidSplashTemplate {

    /**
     * Generates the splash screen theme XML for `res/values/themes.xml`.
     *
     * Uses the AndroidX SplashScreen API (core-splashscreen).
     * [backgroundColor] is a hex string like "#FF0000".
     * [logoDrawableName] is the drawable resource name without prefix, e.g. "splash_logo".
     */
    fun generateThemes(
        backgroundColor: String,
        logoDrawableName: String? = null,
    ): String {
        val logoItem = if (logoDrawableName != null) {
            """        <item name="windowSplashScreenAnimatedIcon">@drawable/$logoDrawableName</item>"""
        } else ""

        return """<?xml version="1.0" encoding="utf-8"?>
<!-- Note: Theme.App.SplashScreen is auto-generated by kmp-splash during build -->
<resources>
    <style name="Theme.App.SplashScreen" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">$backgroundColor</item>
$logoItem
        <item name="postSplashScreenTheme">@style/Theme.App</item>
    </style>

    <style name="Theme.App" parent="android:Theme.Material.NoActionBar"/>
</resources>"""
    }

    /**
     * Generates the night-mode override for `res/values-night/themes.xml`.
     * Only needed when dark mode uses a different background color.
     */
    fun generateNightThemes(
        backgroundColorNight: String,
        logoDrawableName: String? = null,
    ): String {
        val logoItem = if (logoDrawableName != null) {
            """        <item name="windowSplashScreenAnimatedIcon">@drawable/$logoDrawableName</item>"""
        } else ""

        return """<?xml version="1.0" encoding="utf-8"?>
<!-- Note: Theme.App.SplashScreen is auto-generated by kmp-splash during build -->
<resources>
    <style name="Theme.App.SplashScreen" parent="Theme.SplashScreen">
        <item name="windowSplashScreenBackground">$backgroundColorNight</item>
$logoItem
        <item name="postSplashScreenTheme">@style/Theme.App</item>
    </style>
</resources>"""
    }

    /**
     * Generates `mipmap-anydpi-v26/ic_kmp_app_icon.xml` (also reused verbatim for the round
     * variant — launchers apply their own mask). References the background color and foreground
     * drawable the app icon generation task writes alongside it.
     */
    fun generateAdaptiveIconXml(): String = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@color/kmp_app_icon_background"/>
    <foreground android:drawable="@mipmap/ic_kmp_app_icon_foreground"/>
</adaptive-icon>"""

    /** Generates `values/kmp_app_icon.xml`, holding the adaptive icon's background color. */
    fun generateAppIconColor(backgroundColor: String): String = """<?xml version="1.0" encoding="utf-8"?>
<resources>
    <color name="kmp_app_icon_background">$backgroundColor</color>
</resources>"""

    /**
     * Generates a manifest by patching the [baseContent] or creating a new one.
     * It ensures the `<application>` tag has the correct splash theme and registers
     * [KmpSplashInitProvider] so [SplashDefaults] is initialized before any Activity runs.
     * When [iconEnabled] is true, also sets `android:icon`/`android:roundIcon` to the generated
     * app icon, deterministically overwriting any existing value the same way the theme is.
     */
    fun generateManifest(baseContent: String?, iconEnabled: Boolean = false): String {
        val splashTheme = "@style/Theme.App.SplashScreen"
        val themeAttr = "android:theme=\"$splashTheme\""
        val iconAttr = if (iconEnabled) {
            " android:icon=\"@mipmap/ic_kmp_app_icon\" android:roundIcon=\"@mipmap/ic_kmp_app_icon_round\""
        } else ""
        val providerEntry = """        <provider
            android:name="io.kmpbits.splash.KmpSplashInitProvider"
            android:authorities="${'$'}{applicationId}.kmp_splash_init"
            android:exported="false" />"""

        if (baseContent == null) {
            return """<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android">
    <application $themeAttr$iconAttr>
$providerEntry
    </application>
</manifest>"""
        }

        // Patch theme (and, when enabled, icon) attributes on <application> tag
        val applicationTagRegex = """<application(\s+[^>]*?)(/?)>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        var result = applicationTagRegex.replace(baseContent) { match ->
            val attrs = match.groups[1]?.value ?: ""
            val selfClosing = match.groups[2]?.value ?: ""
            val themeRegex = """android:theme="[^"]*"""".toRegex()

            var newAttrs = if (themeRegex.containsMatchIn(attrs)) {
                themeRegex.replace(attrs, themeAttr)
            } else {
                " $themeAttr$attrs"
            }

            if (iconEnabled) {
                val iconRegex = """\s*android:icon="[^"]*"""".toRegex()
                val roundIconRegex = """\s*android:roundIcon="[^"]*"""".toRegex()
                newAttrs = if (iconRegex.containsMatchIn(newAttrs)) {
                    iconRegex.replace(newAttrs, " android:icon=\"@mipmap/ic_kmp_app_icon\"")
                } else {
                    "$newAttrs android:icon=\"@mipmap/ic_kmp_app_icon\""
                }
                newAttrs = if (roundIconRegex.containsMatchIn(newAttrs)) {
                    roundIconRegex.replace(newAttrs, " android:roundIcon=\"@mipmap/ic_kmp_app_icon_round\"")
                } else {
                    "$newAttrs android:roundIcon=\"@mipmap/ic_kmp_app_icon_round\""
                }
            }

            "<application$newAttrs$selfClosing>"
        }

        // Inject provider only if not already present (guards against in-place re-runs)
        if (!result.contains("io.kmpbits.splash.KmpSplashInitProvider")) {
            val selfClosingRegex = """(<application[^>]*/)>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            result = if (selfClosingRegex.containsMatchIn(result)) {
                selfClosingRegex.replace(result) { match ->
                    "${match.groupValues[1]}>\n$providerEntry\n    </application>"
                }
            } else {
                result.replace("</application>", "$providerEntry\n    </application>")
            }
        }

        return result
    }
}
```

- [ ] **Step 4: Add the `iconEnabled` input to `PatchSplashManifestTask.kt`**

Replace the entire content of `plugin/src/main/kotlin/io/kmpbits/splash/tasks/PatchSplashManifestTask.kt` with:

```kotlin
package io.kmpbits.splash.tasks

import io.kmpbits.splash.template.AndroidSplashTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * AGP `MERGED_MANIFEST` transform task, used by both `androidAppPath` mode (patching a separate
 * androidApp module's manifest) and classic mode (patching the same module's own manifest).
 *
 * Patches the already-merged manifest to set the splash theme on `<application>` and inject the
 * `KmpSplashInitProvider` `<provider>` entry, via [AndroidSplashTemplate.generateManifest].
 * Because this runs on the already-merged manifest (not a pre-merge library manifest), it
 * deterministically overwrites any existing `android:theme` on `<application>` instead of risking
 * an AGP manifest-merge conflict. When [iconEnabled] is true, `android:icon`/`android:roundIcon`
 * are patched the same deterministic way.
 */
abstract class PatchSplashManifestTask : DefaultTask() {

    @get:InputFile
    abstract val mergedManifest: RegularFileProperty

    @get:OutputFile
    abstract val updatedManifest: RegularFileProperty

    @get:Input
    @get:Optional
    abstract val iconEnabled: Property<Boolean>

    @TaskAction
    fun patch() {
        val patched = AndroidSplashTemplate.generateManifest(
            mergedManifest.asFile.get().readText(),
            iconEnabled.getOrElse(false),
        )
        val outputFile = updatedManifest.asFile.get()
        outputFile.parentFile?.mkdirs()
        outputFile.writeText(patched)
    }
}
```

- [ ] **Step 5: Run the tests to verify they pass**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.tasks.PatchSplashManifestTaskTest"`
Expected: `BUILD SUCCESSFUL`, 3 tests passed.

- [ ] **Step 6: Commit**

```bash
git add plugin/src/main/kotlin/io/kmpbits/splash/template/AndroidSplashTemplate.kt plugin/src/main/kotlin/io/kmpbits/splash/tasks/PatchSplashManifestTask.kt plugin/src/test/kotlin/io/kmpbits/splash/tasks/PatchSplashManifestTaskTest.kt
git commit -m "Add adaptive icon XML templates and manifest icon attribute patching"
```

---

### Task 4: `KmpSplashExtension` — `generateAppIcon` property

**Files:**
- Modify: `plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashExtension.kt`

- [ ] **Step 1: Add the property**

In `plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashExtension.kt`, change:

```kotlin
    @get:Input
    @get:Optional
    abstract val resourcePackage: Property<String>
}
```

to:

```kotlin
    @get:Input
    @get:Optional
    abstract val resourcePackage: Property<String>

    /**
     * Whether to generate an Android adaptive app icon (and legacy square/round PNG fallbacks)
     * from [logo] and [backgroundColor].
     *
     * Off by default — changing the app's launcher icon is a visible, home-screen-facing change,
     * so it's opt-in rather than automatic whenever [logo] is set.
     *
     * Requires [logo] to be set, in a rasterizable format (PNG, JPEG, GIF, or BMP — not WebP,
     * `.svg`, or Android vector `.xml`).
     *
     * Example:
     * ```kotlin
     * splashScreen {
     *     backgroundColor = SplashColor.white
     *     logo = SplashLogo.resource("logo.png")
     *     generateAppIcon = true
     * }
     * ```
     */
    @get:Input
    abstract val generateAppIcon: Property<Boolean>
}
```

- [ ] **Step 2: Verify it compiles**

Run: `./gradlew -p plugin compileKotlin`
Expected: `BUILD SUCCESSFUL` — this is an additive property with no other consumers yet, so nothing else should break.

- [ ] **Step 3: Commit**

```bash
git add plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashExtension.kt
git commit -m "Add generateAppIcon property to KmpSplashExtension"
```

---

### Task 5: `GenerateAppIconTask`

**Files:**
- Create: `plugin/src/main/kotlin/io/kmpbits/splash/tasks/GenerateAppIconTask.kt`
- Create: `plugin/src/test/kotlin/io/kmpbits/splash/tasks/GenerateAppIconTaskTest.kt`

- [ ] **Step 1: Write the failing tests**

Create `plugin/src/test/kotlin/io/kmpbits/splash/tasks/GenerateAppIconTaskTest.kt`:

```kotlin
package io.kmpbits.splash.tasks

import org.gradle.api.GradleException
import org.gradle.testfixtures.ProjectBuilder
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GenerateAppIconTaskTest {

    private fun writeTestLogo(file: File) {
        file.parentFile.mkdirs()
        val image = BufferedImage(40, 40, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color.RED
        g.fillRect(10, 10, 20, 20)
        g.dispose()
        ImageIO.write(image, "png", file)
    }

    @Test
    fun `generates the full adaptive and legacy icon file tree when enabled`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        val logoFile = File(project.projectDir, "logo.png").also { writeTestLogo(it) }
        val resDir = File(project.projectDir, "generated/res")

        task.enabled.set(true)
        task.backgroundColor.set("#0000FF")
        task.logoSourceFile.set(logoFile)
        task.resOutputDir.set(resDir)
        task.generate()

        val adaptiveXml = File(resDir, "mipmap-anydpi-v26/ic_kmp_app_icon.xml")
        assertTrue(adaptiveXml.exists())
        assertTrue(adaptiveXml.readText().contains("@color/kmp_app_icon_background"))
        assertTrue(adaptiveXml.readText().contains("@mipmap/ic_kmp_app_icon_foreground"))
        assertTrue(File(resDir, "mipmap-anydpi-v26/ic_kmp_app_icon_round.xml").exists())

        val colorXml = File(resDir, "values/kmp_app_icon.xml")
        assertTrue(colorXml.exists())
        assertTrue(colorXml.readText().contains("#0000FF"))

        for (density in listOf("mdpi", "hdpi", "xhdpi", "xxhdpi", "xxxhdpi")) {
            assertTrue(
                File(resDir, "mipmap-$density/ic_kmp_app_icon_foreground.png").exists(),
                "missing foreground for $density"
            )
            assertTrue(
                File(resDir, "mipmap-$density/ic_kmp_app_icon.png").exists(),
                "missing legacy icon for $density"
            )
            assertTrue(
                File(resDir, "mipmap-$density/ic_kmp_app_icon_round.png").exists(),
                "missing legacy round icon for $density"
            )
        }
    }

    @Test
    fun `does nothing when disabled`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        val resDir = File(project.projectDir, "generated/res")
        task.enabled.set(false)
        task.resOutputDir.set(resDir)
        task.generate()

        assertFalse(resDir.exists(), "expected no output directory to be created when disabled")
    }

    @Test
    fun `fails with an actionable message when enabled but no logo is set`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        task.enabled.set(true)
        task.backgroundColor.set("#FFFFFF")
        task.resOutputDir.set(File(project.projectDir, "generated/res"))

        val error = assertFailsWith<GradleException> { task.generate() }
        assertTrue(error.message!!.contains("no 'logo' is set"), "expected an actionable message, got: ${error.message}")
    }

    @Test
    fun `fails with an actionable message for an unsupported logo format`() {
        val project = ProjectBuilder.builder().build()
        val task = project.tasks.create("generateAppIconTest", GenerateAppIconTask::class.java)

        val logoFile = File(project.projectDir, "logo.webp").also {
            it.parentFile.mkdirs()
            it.writeBytes(ByteArray(0))
        }

        task.enabled.set(true)
        task.backgroundColor.set("#FFFFFF")
        task.logoSourceFile.set(logoFile)
        task.resOutputDir.set(File(project.projectDir, "generated/res"))

        val error = assertFailsWith<GradleException> { task.generate() }
        assertTrue(
            error.message!!.contains("png, jpg, jpeg, gif, bmp"),
            "expected the supported formats to be listed, got: ${error.message}"
        )
    }
}
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.tasks.GenerateAppIconTaskTest"`
Expected: FAIL — `GenerateAppIconTask` is unresolved (the class doesn't exist yet).

- [ ] **Step 3: Create `GenerateAppIconTask.kt`**

Create `plugin/src/main/kotlin/io/kmpbits/splash/tasks/GenerateAppIconTask.kt`:

```kotlin
package io.kmpbits.splash.tasks

import io.kmpbits.splash.AppIconGenerator
import io.kmpbits.splash.template.AndroidSplashTemplate
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.awt.Color
import java.awt.image.BufferedImage
import javax.imageio.ImageIO

/**
 * Task that generates the Android adaptive app icon (background color + foreground drawable)
 * and legacy square/round PNG fallbacks for API < 26, from the splash screen's own `logo` and
 * `backgroundColor`.
 *
 * Always registered (see [io.kmpbits.splash.KmpSplashPlugin]); no-ops when [enabled] is false,
 * following the same "task always registers, guards on presence" convention as
 * [GenerateAndroidSplashTask]'s optional inputs.
 */
abstract class GenerateAppIconTask : DefaultTask() {

    @get:Input
    abstract val enabled: Property<Boolean>

    @get:Input
    @get:Optional
    abstract val backgroundColor: Property<String>

    @get:InputFile
    @get:Optional
    abstract val logoSourceFile: RegularFileProperty

    @get:OutputDirectory
    abstract val resOutputDir: DirectoryProperty

    private val supportedExtensions = setOf("png", "jpg", "jpeg", "gif", "bmp")

    @TaskAction
    fun generate() {
        if (!enabled.getOrElse(false)) return

        val logoFile = logoSourceFile.orNull?.asFile
            ?: throw org.gradle.api.GradleException(
                "KmpSplash: 'generateAppIcon' is enabled but no 'logo' is set in splashScreen { ... }. " +
                "An app icon requires a logo to derive its foreground from."
            )

        val extension = logoFile.extension.lowercase()
        if (extension !in supportedExtensions) {
            throw org.gradle.api.GradleException(
                "KmpSplash: 'generateAppIcon' requires logo '${logoFile.name}' to be one of " +
                "${supportedExtensions.joinToString(", ")}. Vector formats (.svg, Android .xml) " +
                "and WebP can't be rasterized for app icon generation."
            )
        }

        val logo = ImageIO.read(logoFile)
            ?: throw org.gradle.api.GradleException(
                "KmpSplash: could not decode logo file '${logoFile.absolutePath}' as an image."
            )

        val hexColor = backgroundColor.get().trimStart('#')
        val background = Color(
            hexColor.substring(0, 2).toInt(16),
            hexColor.substring(2, 4).toInt(16),
            hexColor.substring(4, 6).toInt(16),
        )

        checkUpscaling(logo)

        val resDir = resOutputDir.asFile.get()

        val anydpiDir = resDir.resolve("mipmap-anydpi-v26").also { it.mkdirs() }
        val adaptiveIconXml = AndroidSplashTemplate.generateAdaptiveIconXml()
        anydpiDir.resolve("ic_kmp_app_icon.xml").writeText(adaptiveIconXml)
        anydpiDir.resolve("ic_kmp_app_icon_round.xml").writeText(adaptiveIconXml)

        val valuesDir = resDir.resolve("values").also { it.mkdirs() }
        valuesDir.resolve("kmp_app_icon.xml").writeText(
            AndroidSplashTemplate.generateAppIconColor(backgroundColor.get())
        )

        for ((density, legacySize) in AppIconGenerator.LEGACY_DENSITIES) {
            val foregroundSize = AppIconGenerator.FOREGROUND_DENSITIES.getValue(density)
            val mipmapDir = resDir.resolve("mipmap-$density").also { it.mkdirs() }

            ImageIO.write(
                AppIconGenerator.renderForeground(logo, foregroundSize),
                "png",
                mipmapDir.resolve("ic_kmp_app_icon_foreground.png"),
            )
            ImageIO.write(
                AppIconGenerator.renderLegacy(logo, background, legacySize, round = false),
                "png",
                mipmapDir.resolve("ic_kmp_app_icon.png"),
            )
            ImageIO.write(
                AppIconGenerator.renderLegacy(logo, background, legacySize, round = true),
                "png",
                mipmapDir.resolve("ic_kmp_app_icon_round.png"),
            )
        }

        logger.lifecycle("KmpSplash: wrote app icon resources to ${resDir.absolutePath}")
    }

    private fun checkUpscaling(logo: BufferedImage) {
        val trimmed = AppIconGenerator.trimTransparentBorder(logo)
        val smallerDimension = minOf(trimmed.width, trimmed.height)
        if (AppIconGenerator.needsUpscalingWarning(smallerDimension)) {
            logger.warn(
                "KmpSplash: logo's trimmed content is ${trimmed.width}x${trimmed.height}px, smaller than " +
                "ideal for the largest app icon density and will be upscaled. Consider a higher-resolution logo."
            )
        }
    }
}
```

- [ ] **Step 4: Run the tests to verify they pass**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.tasks.GenerateAppIconTaskTest"`
Expected: `BUILD SUCCESSFUL`, 4 tests passed.

- [ ] **Step 5: Commit**

```bash
git add plugin/src/main/kotlin/io/kmpbits/splash/tasks/GenerateAppIconTask.kt plugin/src/test/kotlin/io/kmpbits/splash/tasks/GenerateAppIconTaskTest.kt
git commit -m "Add GenerateAppIconTask"
```

---

### Task 6: `KmpSplashPlugin` — register and wire the app icon task

**Files:**
- Modify: `plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashPlugin.kt`
- Modify: `plugin/src/test/kotlin/io/kmpbits/splash/KmpSplashPluginTest.kt`

- [ ] **Step 1: Write the failing plugin-level tests**

In `plugin/src/test/kotlin/io/kmpbits/splash/KmpSplashPluginTest.kt`, add these two tests just after the `` `plugin registers generateLaunchScreen task` `` test (i.e. right after its closing `}`, before the `ExitAnimation None...` test):

```kotlin

    @Test
    fun `plugin registers generateAppIcon task`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("io.github.kmpbits.splash")

        assertNotNull(project.tasks.findByName("generateAppIcon"))
    }

    @Test
    fun `generateAppIcon defaults to false`() {
        val project = ProjectBuilder.builder().build()
        project.plugins.apply("org.jetbrains.kotlin.multiplatform")
        project.plugins.apply("io.github.kmpbits.splash")

        val ext = project.extensions.getByType(KmpSplashExtension::class.java)
        assertEquals(false, ext.generateAppIcon.get())
    }
```

- [ ] **Step 2: Run the tests to verify they fail**

Run: `./gradlew -p plugin test --tests "io.kmpbits.splash.KmpSplashPluginTest"`
Expected: FAIL — no `generateAppIcon` task is registered yet, and `generateAppIcon` isn't a member of `KmpSplashExtension` from the plugin's own convention wiring perspective (compiles, but `findByName` returns null and the default isn't set).

- [ ] **Step 3: Add the `GenerateAppIconTask` import and set the `generateAppIcon` convention**

In `plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashPlugin.kt`, change:

```kotlin
import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
import io.kmpbits.splash.tasks.PatchSplashManifestTask
```

to:

```kotlin
import io.kmpbits.splash.tasks.GenerateAndroidSplashTask
import io.kmpbits.splash.tasks.GenerateAppIconTask
import io.kmpbits.splash.tasks.GenerateLaunchScreenTask
import io.kmpbits.splash.tasks.PatchSplashManifestTask
```

Change:

```kotlin
    override fun apply(project: Project) {
        val ext = project.extensions.create("splashScreen", KmpSplashExtension::class.java)
        ext.iosProjectPath.convention("iosApp/iosApp")
        registerIosTask(project, ext)
        registerAndroidTask(project, ext)
    }
```

to:

```kotlin
    override fun apply(project: Project) {
        val ext = project.extensions.create("splashScreen", KmpSplashExtension::class.java)
        ext.iosProjectPath.convention("iosApp/iosApp")
        ext.generateAppIcon.convention(false)
        registerIosTask(project, ext)
        registerAndroidTask(project, ext)
    }
```

- [ ] **Step 4: Register the `generateAppIcon` task in `registerAndroidTask`**

In `plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashPlugin.kt`, change:

```kotlin
    private fun registerAndroidTask(project: Project, ext: KmpSplashExtension) {
        val generatedResDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/res")
        val generatedKotlinDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/kotlin")
```

to:

```kotlin
    private fun registerAndroidTask(project: Project, ext: KmpSplashExtension) {
        val generatedResDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/res")
        val generatedKotlinDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/kotlin")
        val generatedAppIconResDir = project.layout.buildDirectory.dir("generated/kmpSplash/androidMain/resAppIcon")
```

Then, change:

```kotlin
        // Classic-mode Variant API wiring is registered eagerly here (NOT inside the
```

to:

```kotlin
        val appIconTask = project.tasks.register(
            "generateAppIcon",
            GenerateAppIconTask::class.java,
            Action<GenerateAppIconTask> {
                group = "kmp-splash"
                description = "Generates the Android adaptive app icon and legacy fallbacks"

                enabled.set(ext.generateAppIcon)
                backgroundColor.set(ext.backgroundColor.map { it.hex })
                logoSourceFile.set(
                    project.layout.file(ext.logo.map { logo -> project.file(logo.resolvedPath()) })
                )
                resOutputDir.set(generatedAppIconResDir)
            }
        )

        // Classic-mode Variant API wiring is registered eagerly here (NOT inside the
```

- [ ] **Step 5: Thread the app icon task through the three `wireVariantResourcesAndManifest` call sites**

In `plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashPlugin.kt`, change:

```kotlin
        project.plugins.withId("com.android.application") {
            val components = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            components.onVariants { variant ->
                if (!ext.androidAppPath.isPresent) {
                    wireVariantResourcesAndManifest(project, variant, task)
                }
            }
        }
        project.plugins.withId("com.android.library") {
            val components = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            components.onVariants { variant ->
                if (!ext.androidAppPath.isPresent) {
                    wireVariantResourcesAndManifest(project, variant, task)
                }
            }
        }
```

to:

```kotlin
        project.plugins.withId("com.android.application") {
            val components = project.extensions.getByType(ApplicationAndroidComponentsExtension::class.java)
            components.onVariants { variant ->
                if (!ext.androidAppPath.isPresent) {
                    wireVariantResourcesAndManifest(project, variant, task, appIconTask, ext.generateAppIcon)
                }
            }
        }
        project.plugins.withId("com.android.library") {
            val components = project.extensions.getByType(LibraryAndroidComponentsExtension::class.java)
            components.onVariants { variant ->
                if (!ext.androidAppPath.isPresent) {
                    wireVariantResourcesAndManifest(project, variant, task, appIconTask, ext.generateAppIcon)
                }
            }
        }
```

Then change:

```kotlin
                        components.onVariants { variant ->
                            wireVariantResourcesAndManifest(androidProject, variant, task)
                        }
```

to:

```kotlin
                        components.onVariants { variant ->
                            wireVariantResourcesAndManifest(androidProject, variant, task, appIconTask, ext.generateAppIcon)
                        }
```

- [ ] **Step 6: Update the `wireVariantResourcesAndManifest` function itself**

In `plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashPlugin.kt`, change:

```kotlin
private fun wireVariantResourcesAndManifest(
    androidProject: Project,
    variant: Variant,
    task: TaskProvider<GenerateAndroidSplashTask>,
) {
    val res = variant.sources.res
    if (res == null) {
        androidProject.logger.warn(
            "KmpSplash: variant '${variant.name}' has no res sources — " +
            "splash resources were not wired in. This is unexpected."
        )
    } else {
        res.addGeneratedSourceDirectory(
            task,
            GenerateAndroidSplashTask::resOutputDir,
        )
    }

    val patchManifest = androidProject.tasks.register(
        "patchKmpSplash${variant.name.replaceFirstChar { it.uppercase() }}Manifest",
        PatchSplashManifestTask::class.java,
    ) {
        group = "kmp-splash"
        description = "Patches the manifest with the splash theme and provider (${variant.name})"
    }
    variant.artifacts.use(patchManifest)
        .wiredWithFiles(
            PatchSplashManifestTask::mergedManifest,
            PatchSplashManifestTask::updatedManifest,
        )
        .toTransform(SingleArtifact.MERGED_MANIFEST)
}
```

to:

```kotlin
private fun wireVariantResourcesAndManifest(
    androidProject: Project,
    variant: Variant,
    splashTask: TaskProvider<GenerateAndroidSplashTask>,
    appIconTask: TaskProvider<GenerateAppIconTask>,
    generateAppIconEnabled: Provider<Boolean>,
) {
    val res = variant.sources.res
    if (res == null) {
        androidProject.logger.warn(
            "KmpSplash: variant '${variant.name}' has no res sources — " +
            "splash resources were not wired in. This is unexpected."
        )
    } else {
        res.addGeneratedSourceDirectory(
            splashTask,
            GenerateAndroidSplashTask::resOutputDir,
        )
        res.addGeneratedSourceDirectory(
            appIconTask,
            GenerateAppIconTask::resOutputDir,
        )
    }

    val patchManifest = androidProject.tasks.register(
        "patchKmpSplash${variant.name.replaceFirstChar { it.uppercase() }}Manifest",
        PatchSplashManifestTask::class.java,
    ) {
        group = "kmp-splash"
        description = "Patches the manifest with the splash theme and provider (${variant.name})"
        iconEnabled.set(generateAppIconEnabled)
    }
    variant.artifacts.use(patchManifest)
        .wiredWithFiles(
            PatchSplashManifestTask::mergedManifest,
            PatchSplashManifestTask::updatedManifest,
        )
        .toTransform(SingleArtifact.MERGED_MANIFEST)
}
```

- [ ] **Step 7: Run the tests to verify they pass**

Run: `./gradlew -p plugin test`
Expected: `BUILD SUCCESSFUL` — all tests pass, including the two new `KmpSplashPluginTest` cases.

- [ ] **Step 8: Commit**

```bash
git add plugin/src/main/kotlin/io/kmpbits/splash/KmpSplashPlugin.kt plugin/src/test/kotlin/io/kmpbits/splash/KmpSplashPluginTest.kt
git commit -m "Register and wire GenerateAppIconTask into Android variants"
```

---

### Task 7: Full verification

**Files:** none (verification only)

- [ ] **Step 1: Run the full plugin test suite**

Run: `./gradlew -p plugin test`
Expected: `BUILD SUCCESSFUL`, all tests pass (existing splash/iOS tests plus every test added in Tasks 1–6).

- [ ] **Step 2: Manually sanity-check with the sample app (optional, requires a local Android SDK)**

Add `generateAppIcon = true` to `sample/composeApp`'s (or `sample/androidApp`'s) `splashScreen { }` block, then run:

```bash
./gradlew :sample:androidApp:assembleDebug
```

Expected: `BUILD SUCCESSFUL`. Inspect `sample/androidApp/build/generated/kmpSplash/androidMain/resAppIcon/` (or the merged resources under `build/intermediates`) for the `mipmap-anydpi-v26`, `mipmap-*dpi`, and `values/kmp_app_icon.xml` files, and confirm the merged manifest at `build/intermediates/merged_manifest/debug/AndroidManifest.xml` contains `android:icon="@mipmap/ic_kmp_app_icon"`. Revert the sample change afterward unless you want to keep it as a demo.
