# OwnTV Design Makeover Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Restyle OwnTV to the approved design spec (`docs/superpowers/specs/2026-08-12-design-makeover-design.md`): Figtree typography, charcoal palette, white-ring focus, pill/shape consistency, and four layout-area passes.

**Architecture:** Token-first cascade. Phase 0 rewrites the theme token layer (`ui/theme/`), which restyles every screen through the existing `OwnTVTheme` CompositionLocals. Phases 1–4 then adjust layout in `ui/components/` and `features/` files. Each phase is independently shippable and verified on an Android TV emulator before the next begins.

**Tech Stack:** Kotlin, Jetpack Compose for TV (`androidx.tv:tv-material`), variable fonts via `FontVariation`, existing Koin/DataStore settings plumbing (unchanged).

## Global Constraints

- Work on branch `design-makeover` (already exists; spec is committed there).
- Never edit strings/resources under `res/values*` — the i18n boundary is out of scope; all copy stays as-is.
- No new user-facing settings; existing settings (ThemeMode, accent presets, custom hex, glass scope, AnimationLevel, UI zoom) keep working unchanged.
- No raw `RoundedCornerShape(N.dp)` left behind in files this plan touches — use `Dimens` tokens or `RoundedCornerShape(50)` (pill).
- No `Color(0x…)` literals in chrome code; pictorial canvas art (weather glyphs, brand logo) draws from named constants in the theme layer.
- All motion routes through `AnimationLevel` (OFF = instant snap).
- Quality gates every task: `./gradlew :app:compileStandardDebugKotlin` (fast), and before each phase-final commit `./gradlew testStandardDebugUnitTest lintStandardDebug`.
- Emulator verification: the session's ATV emulator is arm64 → use the **standard** flavor APK. If a fresh emulator must be created, an x86_64 image needs the **x86_64** flavor instead (libmpv only loads on a matching ABI).
- Commit messages: imperative mood (they become the changelog), `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>` trailer.
- Screenshots for review go to the session scratchpad directory, not the repo.

### Emulator verification ritual (referenced by phase-end tasks as "RITUAL")

```bash
adb devices                                   # confirm emulator id (e.g. emulator-5554)
./gradlew :app:assembleStandardDebug
adb -s emulator-5554 install -r app/build/outputs/apk/standard/debug/app-standard-debug.apk
adb -s emulator-5554 shell am start -n tv.own.owntv/.MainActivity
# navigate with: adb -s emulator-5554 shell input keyevent KEYCODE_DPAD_DOWN / _UP / _LEFT / _RIGHT / KEYCODE_DPAD_CENTER / KEYCODE_BACK
# capture:      adb -s emulator-5554 exec-out screencap -p > <scratchpad>/phaseN-<screen>.png
```

Read each captured PNG (the Read tool renders images) and check it against the spec section for the phase. A finding = fix in this phase, re-capture, then commit.

---

## Phase 0 — Token layer

### Task 1: Figtree font + `FigtreeFamily`

**Files:**
- Create: `app/src/main/res/font/figtree_variable.ttf` (downloaded binary)
- Create: `app/src/main/java/tv/own/owntv/ui/theme/Fonts.kt`

**Interfaces:**
- Produces: `val FigtreeFamily: FontFamily` in package `tv.own.owntv.ui.theme` — consumed by Tasks 2 and 3.

- [ ] **Step 1: Download the variable font**

```bash
curl -sL -o app/src/main/res/font/figtree_variable.ttf \
  "https://github.com/google/fonts/raw/main/ofl/figtree/Figtree%5Bwght%5D.ttf"
file app/src/main/res/font/figtree_variable.ttf   # expect: TrueType font data
```

- [ ] **Step 2: Create `Fonts.kt`** (same `FontVariation` pattern `PopupTheme.kt` uses for Lora)

```kotlin
package tv.own.owntv.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import tv.own.owntv.R

/**
 * Figtree — the brand sans (SIL OFL), a single variable file (weight axis 300–900).
 * Non-Latin scripts fall back to the platform Noto stack automatically.
 */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun figtree(weight: FontWeight) = Font(
    R.font.figtree_variable,
    weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

val FigtreeFamily = FontFamily(
    figtree(FontWeight.Normal),
    figtree(FontWeight.Medium),
    figtree(FontWeight.SemiBold),
    figtree(FontWeight.Bold),
    figtree(FontWeight.ExtraBold),
)
```

- [ ] **Step 3: Compile**

Run: `./gradlew :app:compileStandardDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add app/src/main/res/font/figtree_variable.ttf app/src/main/java/tv/own/owntv/ui/theme/Fonts.kt
git commit -m "Add Figtree variable font as the brand typeface"
```

### Task 2: Complete 15-style type scale

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/Type.kt` (full rewrite of `OwnTVTypography`)
- Test: `app/src/test/java/tv/own/owntv/ui/theme/TypeScaleTest.kt` (create)

**Interfaces:**
- Consumes: `FigtreeFamily` (Task 1).
- Produces: `OwnTVTypography` with all 15 M3 styles defined (same public name as today).

- [ ] **Step 1: Write the failing test**

```kotlin
package tv.own.owntv.ui.theme

import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import org.junit.Assert.assertEquals
import org.junit.Test

/** Locks the spec's scale: every style defined, Figtree, correct size/weight. */
class TypeScaleTest {
    @Test
    fun `all fifteen styles use Figtree`() {
        val t = OwnTVTypography
        listOf(
            t.displayLarge, t.displayMedium, t.displaySmall,
            t.headlineLarge, t.headlineMedium, t.headlineSmall,
            t.titleLarge, t.titleMedium, t.titleSmall,
            t.bodyLarge, t.bodyMedium, t.bodySmall,
            t.labelLarge, t.labelMedium, t.labelSmall,
        ).forEachIndexed { i, style ->
            assertEquals("style[$i] fontFamily", FigtreeFamily, style.fontFamily)
        }
    }

    @Test
    fun `key sizes and weights match the spec`() {
        val t = OwnTVTypography
        assertEquals(44.sp, t.displayLarge.fontSize)
        assertEquals(FontWeight.ExtraBold, t.displayLarge.fontWeight)
        assertEquals(28.sp, t.headlineLarge.fontSize)
        assertEquals(17.sp, t.titleMedium.fontSize)
        assertEquals(FontWeight.Normal, t.bodyLarge.fontWeight)
        assertEquals(11.sp, t.labelSmall.fontSize)
        assertEquals(FontWeight.Medium, t.labelSmall.fontWeight)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew :app:testStandardDebugUnitTest --tests "tv.own.owntv.ui.theme.TypeScaleTest"`
Expected: FAIL (`displayMedium` etc. currently fall back to tv-material defaults, family is `FontFamily.SansSerif`).
*(If the JVM test errors on Compose class initialization rather than asserting, delete the test file, note it in the commit message, and rely on Step 4's compile + Phase-0 screenshots instead — do not mock around it.)*

- [ ] **Step 3: Rewrite `Type.kt`** — replace the whole `OwnTVTypography` value:

```kotlin
package tv.own.owntv.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Typography

/**
 * Figtree scale tuned for the 10-foot experience (design spec 2026-08-12):
 * tracking tightens as size grows, weight caps at ExtraBold for the hero display,
 * and hierarchy is carried by size — components must not stack fontWeight overrides on top.
 */
private fun figtreeStyle(size: Int, line: Int, weight: FontWeight, trackingPercent: Float) = TextStyle(
    fontFamily = FigtreeFamily,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = (trackingPercent / 100f).em,
)

val OwnTVTypography = Typography(
    displayLarge = figtreeStyle(44, 52, FontWeight.ExtraBold, -2f),
    displayMedium = figtreeStyle(36, 44, FontWeight.Bold, -1.5f),
    displaySmall = figtreeStyle(30, 38, FontWeight.Bold, -1f),
    headlineLarge = figtreeStyle(28, 36, FontWeight.Bold, -1f),
    headlineMedium = figtreeStyle(24, 32, FontWeight.SemiBold, -0.5f),
    headlineSmall = figtreeStyle(20, 28, FontWeight.SemiBold, 0f),
    titleLarge = figtreeStyle(22, 28, FontWeight.SemiBold, -0.5f),
    titleMedium = figtreeStyle(17, 24, FontWeight.SemiBold, 0f),
    titleSmall = figtreeStyle(15, 20, FontWeight.SemiBold, 0.1f),
    bodyLarge = figtreeStyle(16, 24, FontWeight.Normal, 0.15f),
    bodyMedium = figtreeStyle(14, 20, FontWeight.Normal, 0.15f),
    bodySmall = figtreeStyle(12, 16, FontWeight.Normal, 0.2f),
    labelLarge = figtreeStyle(14, 20, FontWeight.SemiBold, 0.2f),
    labelMedium = figtreeStyle(12, 16, FontWeight.SemiBold, 0.4f),
    labelSmall = figtreeStyle(11, 16, FontWeight.Medium, 0.5f),
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew :app:testStandardDebugUnitTest --tests "tv.own.owntv.ui.theme.TypeScaleTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/tv/own/owntv/ui/theme/Type.kt app/src/test/java/tv/own/owntv/ui/theme/TypeScaleTest.kt
git commit -m "Define the complete Figtree type scale for 10-foot UI"
```

### Task 3: Popups on the brand font; remove Lora

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/PopupTheme.kt`
- Delete: `app/src/main/res/font/lora_variable.ttf`, `app/src/main/res/font/lora_italic_variable.ttf` (confirm exact filenames with `ls app/src/main/res/font/`)

**Interfaces:**
- Consumes: `FigtreeFamily` (Task 1).
- Produces: `PopupFontFamily` (same public name) now aliased to Figtree; `PopupFontTheme` API unchanged — its 47 call sites must not change.

- [ ] **Step 1: Rewrite the font-family block in `PopupTheme.kt`** — delete the `loraUpright`/`loraItalic` builders and the old `PopupFontFamily`, replace with:

```kotlin
/** Popups use the brand family — one typographic voice app-wide (design spec 2026-08-12). */
val PopupFontFamily = FigtreeFamily
```

Update the file's KDoc (the "Lora — a free, open-licensed serif…" comment) to say popups use Figtree via `PopupFontFamily`; keep the `fontScale` and idempotency documentation as-is. Remove now-unused imports (`Font`, `FontStyle`, `FontVariation`, `FontWeight` if unreferenced).

- [ ] **Step 2: Delete the Lora files and check for stragglers**

```bash
git rm app/src/main/res/font/lora_variable.ttf app/src/main/res/font/lora_italic_variable.ttf
grep -rn "lora" app/src/main --include="*.kt" --include="*.xml"   # expect: no hits
```

- [ ] **Step 3: Compile + unit tests**

Run: `./gradlew :app:compileStandardDebugKotlin testStandardDebugUnitTest`
Expected: BUILD SUCCESSFUL, tests pass.

- [ ] **Step 4: Commit**

```bash
git add -A app/src/main/res/font app/src/main/java/tv/own/owntv/ui/theme/PopupTheme.kt
git commit -m "Unify popups on Figtree and drop the Lora serif"
```

### Task 4: Charcoal palette + focus-token meaning change

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/Color.kt` (neutral + secondary values; tertiary untouched)
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/OwnTVColors.kt` (focusBorder/focusGlow assignment in `ownTvColors`)

**Interfaces:**
- Produces: same token names, new values. `focusBorder` = white (dark) / near-black ink (light); `focusGlow` = neutral black shadow color. Consumed by Task 5.

- [ ] **Step 1: Replace the dark + light neutral values in `Color.kt`** (spec Section 1 — exact values; `AccentCyan` and the `DarkTertiary*`/`LightTertiary*` block stay):

```kotlin
// ---------------- DARK (charcoal, no hue bias — design spec 2026-08-12) ----------------
val DarkBackground = Color(0xFF0B0D0E)
val DarkSurface = Color(0xFF121517)
val DarkSurfaceContainerLowest = Color(0xFF0E1113)
val DarkSurfaceContainerLow = Color(0xFF16191C)
val DarkSurfaceContainer = Color(0xFF1A1E21)
val DarkSurfaceContainerHigh = Color(0xFF23282C)
val DarkSurfaceContainerHighest = Color(0xFF2C3238)
val DarkOnSurface = Color(0xFFE7EAEC)
val DarkOnSurfaceVariant = Color(0xFFA9B0B5)
val DarkOutline = Color(0xFF7E868C)
val DarkOutlineVariant = Color(0xFF3A4046)
val DarkSecondary = Color(0xFFB6C1C9)
val DarkOnSecondary = Color(0xFF212A31)
val DarkSecondaryContainer = Color(0xFF39434B)
val DarkOnSecondaryContainer = Color(0xFFD5DFE7)
// DarkTertiary* unchanged (already a cool blue), DarkError unchanged.

// ---------------- LIGHT (neutral, de-greened) ----------------
val LightBackground = Color(0xFFFAFBFC)
val LightSurface = Color(0xFFFAFBFC)
val LightSurfaceContainerLowest = Color(0xFFFFFFFF)
val LightSurfaceContainerLow = Color(0xFFF1F3F5)
val LightSurfaceContainer = Color(0xFFEBEEF0)
val LightSurfaceContainerHigh = Color(0xFFE4E8EB)
val LightSurfaceContainerHighest = Color(0xFFDFE3E6)
val LightOnSurface = Color(0xFF191C1E)
val LightOnSurfaceVariant = Color(0xFF42474B)
val LightOutline = Color(0xFF72787D)
val LightOutlineVariant = Color(0xFFC1C7CC)
val LightSecondary = Color(0xFF4E5B66)
val LightOnSecondary = Color(0xFFFFFFFF)
val LightSecondaryContainer = Color(0xFFD5DFE7)
val LightOnSecondaryContainer = Color(0xFF0A1922)
// LightTertiary* unchanged, LightError unchanged.
```

- [ ] **Step 2: Change the focus tokens in `OwnTVColors.kt`** — in `ownTvColors`, dark branch:

```kotlin
focusBorder = Color.White,
focusGlow = Color.Black.copy(alpha = 0.45f),
```

light branch:

```kotlin
focusBorder = Color(0xFF191C1E),
focusGlow = Color.Black.copy(alpha = 0.22f),
```

(Add the `androidx.compose.ui.graphics.Color` import if not present.) `favorite` stays mapped to the error colors. Update the file's KDoc: focus is neutral; accent = selection.

- [ ] **Step 3: Compile + tests**

Run: `./gradlew :app:compileStandardDebugKotlin testStandardDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/tv/own/owntv/ui/theme/Color.kt app/src/main/java/tv/own/owntv/ui/theme/OwnTVColors.kt
git commit -m "Move the palette to neutral charcoal and make focus tokens accent-free"
```

### Task 5: FocusableSurface — white ring, accent selection, spring scale

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/Animations.kt` (add spring helper)
- Modify: `app/src/main/java/tv/own/owntv/ui/components/FocusableSurface.kt`

**Interfaces:**
- Produces: `ownTvSpring(): AnimationSpec<Float>` in `tv.own.owntv.ui.theme`; `FocusableSurface` keeps its exact parameter list (callers unchanged).

- [ ] **Step 1: Add the spring helper to `Animations.kt`**

```kotlin
/** Spring for focus scale (M3-expressive feel). AnimationLevel OFF collapses to an instant snap. */
@Composable
@ReadOnlyComposable
fun ownTvFocusSpring(): androidx.compose.animation.core.AnimationSpec<Float> =
    if (LocalAnimationLevel.current == AnimationLevel.OFF) androidx.compose.animation.core.snap()
    else androidx.compose.animation.core.spring(dampingRatio = 0.7f, stiffness = androidx.compose.animation.core.Spring.StiffnessMediumLow)
```

- [ ] **Step 2: Rework `FocusableSurface.kt` state visuals** (keep the parameter list identical):
  - Scale: `animateFloatAsState(if (focused) focusedScale else 1f, animationSpec = ownTvFocusSpring(), …)`.
  - Border: replace the `showBorder`/`borderColor` logic with:

```kotlin
val ringColor = when {
    focused -> colors.focusBorder                      // white ring (dark) / ink ring (light)
    selected -> colors.primary                          // accent = selected, the only accent on chrome
    else -> Color.Transparent
}
val showBorder = showFocusBorder && ringColor != Color.Transparent
```

  - Delete the glassy special-case (`if (glassy && (focused || selected)) Color.White.copy(alpha = 0.5f) …`) — the white ring is now universal; keep the `glassy` val only for the `glassIdleRimAlpha` idle-rim branch, which is unchanged.
  - Shadow: unchanged mechanics (`ambientColor/spotColor = colors.focusGlow`) — the color went neutral via Task 4.

- [ ] **Step 3: Compile + tests**

Run: `./gradlew :app:compileStandardDebugKotlin testStandardDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/tv/own/owntv/ui/theme/Animations.kt app/src/main/java/tv/own/owntv/ui/components/FocusableSurface.kt
git commit -m "Make focus a white ring with spring scale; reserve accent for selection"
```

### Task 6: Shape enforcement + chip weight cleanup (chrome)

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/SettingsScreen.kt` (ValueChip 8dp, QuickToggleChip 12dp)
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/Glass.kt` (default `cornerRadius = 22.dp` → `Dimens.CardCorner`)

**Interfaces:**
- Produces: `TopBarChipShape = RoundedCornerShape(50)` (private to TopBar.kt), replacing `TopBarChipCorner`.

- [ ] **Step 1: TopBar.kt** — replace `private val TopBarChipCorner = 14.dp` with `private val TopBarChipShape = RoundedCornerShape(50)`; replace every `RoundedCornerShape(TopBarChipCorner)` with `TopBarChipShape`. Delete every inline `fontWeight = FontWeight.Bold/SemiBold` on chip `Text` calls (`SectionChip`, `ContinueChip`, `PlaylistChip`, `WeatherChip`) — `labelLarge` now carries SemiBold from the scale. Remove the unused `FontWeight` import if nothing else uses it.
- [ ] **Step 2: SettingsScreen.kt** — `ValueChip`: `RoundedCornerShape(8.dp)` → `RoundedCornerShape(Dimens.CornerSmall)`; `QuickToggleChip`: `RoundedCornerShape(12.dp)` → `RoundedCornerShape(Dimens.CornerSmall)` (all three occurrences inside it); drop its inline `fontWeight = FontWeight.SemiBold` overrides.
- [ ] **Step 3: Glass.kt** — change the `Modifier.glass` default parameter `cornerRadius: Dp = 22.dp` to `cornerRadius: Dp = Dimens.CardCorner`.
- [ ] **Step 4: Compile**

Run: `./gradlew :app:compileStandardDebugKotlin`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt app/src/main/java/tv/own/owntv/features/shell/components/SettingsScreen.kt app/src/main/java/tv/own/owntv/ui/theme/Glass.kt
git commit -m "Enforce the shape scale: pill chips, tokenized corners, no inline bolding"
```

### Task 7: Hardcoded-color sweep (HUD + weather)

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/player/PlayerHud.kt` (the `private val TEAL = Color(0xFF52DBC8)`)
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt` (weather glyph palette)
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/Color.kt` (add `WeatherGlyph` object)

- [ ] **Step 1: PlayerHud.kt** — delete the `TEAL` constant; at each usage site substitute `OwnTVTheme.colors.primary` (usages are inside composables; where a non-composable scope needs it, hoist `val accent = OwnTVTheme.colors.primary` at the nearest composable). Find them all: `grep -n "TEAL" app/src/main/java/tv/own/owntv/player/PlayerHud.kt`.
- [ ] **Step 2: Weather palette** — add to `Color.kt`:

```kotlin
/** Pictorial palette for the top-bar weather glyph (canvas art, not chrome). */
object WeatherGlyph {
    val Sun = Color(0xFFFFD166)
    val Moon = Color(0xFFDDF8FF)
    val Cloud = Color(0xFFDDEFE9)
    val Rain = Color(0xFF76A7FF)
    val Snow = Color(0xFFF0FCFF)
    val Fog = Color(0xFFDDF8FF)
    val Thunder = Color(0xFFFFD166)
}
```

In `TopBar.kt`'s `WeatherConditionIcon`, replace the local `sunC`/`moonC`/… vals with references to `WeatherGlyph.*`.
- [ ] **Step 3: Audit the rest** — `grep -rn "Color(0x" app/src/main/java/tv/own/owntv/features app/src/main/java/tv/own/owntv/player --include="*.kt" | grep -v WeatherGlyph`. For each hit decide: chrome → token; pictorial/scrim (pure black/white overlays) → leave. Fix chrome hits in place.
- [ ] **Step 4: Compile + tests + lint**

Run: `./gradlew :app:compileStandardDebugKotlin testStandardDebugUnitTest lintStandardDebug`
Expected: all green.

- [ ] **Step 5: Commit**

```bash
git add -A app/src/main/java/tv/own/owntv
git commit -m "Route HUD and weather colors through theme tokens"
```

### Task 8: Phase 0 verification on the emulator

**Files:** none (fix-forward commits only if findings).

- [ ] **Step 1:** Run the RITUAL (Global Constraints). Capture: Home, Movies grid, a Settings screen, an open dialog (e.g. Glass surfaces), Live with preview pane, player HUD (start any stream, or if no source configured on the emulator, capture the setup/onboarding screens instead and note it).
- [ ] **Step 2:** Check against spec Section 1–3: Figtree rendering everywhere (including popups), charcoal surfaces (no green cast), white focus ring + spring, pills in the top bar, HUD accent (switch accent to Violet in Settings → verify HUD follows, then switch back).
- [ ] **Step 3:** D-pad walkthrough: sidebar ↔ panels ↔ preview pane on Home/Movies/Live; confirm focus lands where it did before (no focusability changes in this phase).
- [ ] **Step 4:** Fix findings, re-capture, then:

```bash
git add -A && git commit -m "Polish Phase 0 findings from emulator review"   # only if fixes were made
```

---

## Phase 1 — Cards & row rhythm

### Task 9: PosterCard — artwork-is-the-card

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/components/PosterCard.kt`

**Interfaces:**
- `PosterCard(...)` signature unchanged (6 call sites in HomeScreen/MoviesScreen/SeriesScreen keep compiling as-is).

- [ ] **Step 1: Rewrite the body.** Structure: root `Column(modifier)`; the `FocusableSurface` wraps **only the artwork box** (ring hugs the art, Google TV style); title+meta below, outside the ring. Key changes from the current body:
  - `FocusableSurface(shape = RoundedCornerShape(Dimens.CornerMedium), focusedScale = 1.05f, glowElevation = 12, …)` — containers stay `surfaceContainerHigh`.
  - Artwork `Box` fills the surface: keep `aspectRatio(2f / 3.2f)`, drop the inner `clip(RoundedCornerShape(Dimens.PosterArtCorner))` and the `padding(Dimens.PosterPadding)` wrapper (the surface's own clip does the rounding).
  - Rating/favorite/completed badges stay overlaid on the art exactly as today (their pill shapes stay `RoundedCornerShape(50)`).
  - Progress bar: keep at art bottom, `height(4.dp)`, fill `colors.primary` (already accent) — remove the `Dimens.PosterProgressHeight` black track's rounding changes (none today; unchanged).
  - Title below: `style = MaterialTheme.typography.labelLarge`, **color always `colors.onSurface`** (delete the `if (focused) colors.primary` branch), `textAlign = TextAlign.Start`, `maxLines = 2, minLines = 2`; add a `Text` metadata line only if the caller already passes rating — it does not, so no new line (YAGNI).
  - `Spacer(Modifier.height(6.dp))` between art and title.
- [ ] **Step 2:** Remove now-unused `Dimens.PosterPadding`/`PosterArtCorner`/`PosterCardCorner` references; in `Dimens.kt` delete `PosterCardCorner`, `PosterArtCorner`, `PosterPadding` if nothing else references them (`grep -rn "PosterArtCorner\|PosterCardCorner\|PosterPadding" app/src/main/java`).
- [ ] **Step 3: Compile:** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 4: Commit:** `git add -A app/src/main/java/tv/own/owntv/ui && git commit -m "Redesign poster cards: full-bleed art with title below"`

### Task 10: Row rhythm + section headers

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/home/HomeScreen.kt` (row headers in `HomeLiveRow`/`ContinueWatchingRow` areas; NOT the hero — Phase 3)
- Modify: `app/src/main/java/tv/own/owntv/features/movies/MoviesScreen.kt`, `app/src/main/java/tv/own/owntv/features/series/SeriesScreen.kt`, `app/src/main/java/tv/own/owntv/features/search/SearchScreen.kt` (grid/row spacing + headers)

- [ ] **Step 1:** Find every shelf/section header (`grep -n "titleSmall\|uppercase()" app/src/main/java/tv/own/owntv/features/home/HomeScreen.kt app/src/main/java/tv/own/owntv/features/movies/MoviesScreen.kt app/src/main/java/tv/own/owntv/features/series/SeriesScreen.kt app/src/main/java/tv/own/owntv/features/search/SearchScreen.kt`). Pattern to apply to each non-hero header: `style = MaterialTheme.typography.titleMedium`, `color = colors.onSurface`, no `.uppercase()`, no `fontWeight` override, no accent color.
- [ ] **Step 2:** Normalize spacing in those files: item gaps `Arrangement.spacedBy(Dimens.GapMedium)`, row-to-row `Dimens.GapLarge` (HomeScreen's `LazyColumn` already uses `GapLarge` — leave), header-to-row gap `Spacer(Modifier.height(Dimens.GapSmall))`. Only touch obvious hardcoded `.dp` gaps in shelf layouts; leave panel scaffolding alone.
- [ ] **Step 3: Compile:** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 4:** RITUAL — capture Home, Movies, Series, Search. Check: shelves read as groups, titles under cards start-aligned, ring hugs artwork, D-pad row-hopping still lands on first card per row.
- [ ] **Step 5: Commit:** `git add -A app/src/main/java/tv/own/owntv/features && git commit -m "Give shelves a consistent rhythm with quiet section headers"`

---

## Phase 2 — Sidebar & top bar

### Task 11: Sidebar geometry + active treatment

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/Dimens.kt` (`SidebarWidthCollapsed = 88.dp` → `72.dp`)
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/Sidebar.kt` (`NavItem` active treatment — the composable sits below line ~200; read it first)
- Possibly modify: `app/src/main/java/tv/own/owntv/ui/components/NavLadder.kt` (`rememberNavLadderColors` if `NavItem` sources its colors there)

- [ ] **Step 1:** Read `NavItem` (and `NavAccentBar`, `rememberNavLadderColors`) in full. Identify where the **active** item gets its container fill and icon/label tint.
- [ ] **Step 2:** Apply the spec treatment: active container = `colors.secondaryContainer` (neutral pill, `RoundedCornerShape(50)` if not already pill), active icon+label tint = `colors.primary`; inactive stays as today (transparent container, `onSurfaceVariant` content). If `NavAccentBar` draws an accent side-bar indicator, remove its call from `NavItem` (the pill is the indicator now) — leave the component file itself if other screens use it (`grep -rn "NavAccentBar" app/src/main/java`).
- [ ] **Step 3:** Change `SidebarWidthCollapsed` to `72.dp` in `Dimens.kt`. Note the hero-row width math reads this token (`HomeScreen.kt` `approxRowWidth`) — no edit needed there, it follows the token.
- [ ] **Step 4: Compile:** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 5:** RITUAL — capture Home with sidebar focused and unfocused. Check: 72dp rail doesn't clip icons/avatar, active pill reads clearly, D-pad entry still redirects to the selected section (the `onFocusChanged` redirect logic is untouched).
- [ ] **Step 6: Commit:** `git add -A && git commit -m "Slim the nav rail and mark the active section with a neutral pill"`

### Task 12: Top bar — one calm chip row

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt`

- [ ] **Step 1:** Unify fills per spec: `SectionChip` keeps `colors.primaryContainer` (the one accent); `ContinueChip` changes `unfocusedContainerColor` from `colors.primaryContainer.copy(alpha = 0.6f)` to `colors.surfaceContainer.copy(alpha = 0.6f)` and its idle content tint from `onPrimaryContainer` to `onSurfaceVariant` (focused state keeps `colors.primary` fill + `onPrimary` content); Search/Clock/Playlist/Weather already sit on `surfaceContainer.copy(alpha = 0.6f)` — verify, don't change.
- [ ] **Step 2: Compile:** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3:** RITUAL — capture top bar with and without glass mode (Settings → toggle glass). Check pills, uniform neutrals, single accent chip.
- [ ] **Step 4:** Phase gate: `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.
- [ ] **Step 5: Commit:** `git add -A app/src/main/java/tv/own/owntv/features/shell && git commit -m "Calm the top bar: uniform neutral chips with one accent"`

---

## Phase 3 — Hero carousel

### Task 13: Hero dressing

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/ui/theme/Dimens.kt` (`HeroCardCorner = 18.dp` → `24.dp` i.e. CornerLarge value)
- Modify: `app/src/main/java/tv/own/owntv/features/home/HomeScreen.kt` (`HeroRowSection` header + the hero card composable below it — read the card/scrim code first; it's past line 540)

- [ ] **Step 1:** Read the hero card item composable in `HomeScreen.kt` (from `itemsIndexed(items…)` in `HeroRowSection` down; also `HeroFallbackPane`). Locate: the expanded-card overlay gradient (scrim), the metadata text block, and the focus treatment.
- [ ] **Step 2:** Row header: `stringResource(R.string.home_keep_watching).uppercase()` + `titleSmall` + `colors.primary` + Bold → `stringResource(R.string.home_keep_watching)` (no `.uppercase()`), `titleMedium`, `colors.onSurface`, no fontWeight. (String resource untouched — only the call-site transform is removed.)
- [ ] **Step 3:** Scrim: replace the single overlay gradient with a two-axis scrim drawn over the expanded artwork:

```kotlin
// bottom-up leg
Brush.verticalGradient(0f to Color.Transparent, 0.55f to Color.Transparent, 1f to Color.Black.copy(alpha = 0.78f))
// start-side leg, drawn second (RTL-aware: use Brush.horizontalGradient with LocalLayoutDirection to flip)
Brush.horizontalGradient(0f to Color.Black.copy(alpha = 0.55f), 0.45f to Color.Transparent)
```

For RTL, flip the horizontal stops when `LocalLayoutDirection.current == LayoutDirection.Rtl` (pattern already used elsewhere via `horizontalDirection` in `core/i18n` — reuse it).
- [ ] **Step 4:** Metadata block inside the expanded card: eyebrow (genre/kind line if present) `labelMedium` + `.uppercase()` + `colors.onSurfaceVariant`-on-scrim (use `Color.White.copy(alpha = .8f)` — it sits on artwork scrim, not on surface); title `headlineLarge`, white; plot 2-line `bodyMedium`, `Color.White.copy(alpha = .85f)`; meta separated with the existing separator resource. Scrim overlays are pictorial-black — exempt from the token rule, per Global Constraints.
- [ ] **Step 5:** Hero focus: hero cards must pass `focusedScale = 1f` (ring only, no zoom) to `FocusableSurface` — verify/set.
- [ ] **Step 6: Compile:** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 7:** RITUAL — capture hero collapsed, focused, expanded (dwell 3s), and with video preview if a source is configured. Check corner radius, scrim legibility, type hierarchy, no scale on focus.
- [ ] **Step 8: Commit:** `git add -A && git commit -m "Dress the hero: larger radius, two-axis scrim, display-type metadata"`

---

## Phase 4 — Detail & preview panes

### Task 14: Movie/Series detail panes

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/movies/MoviesScreen.kt` (`MovieDetailsPane`, ~line 775)
- Modify: `app/src/main/java/tv/own/owntv/features/series/SeriesScreen.kt` (its equivalent pane — find with `grep -n "DetailsPane\|PreviewPane" app/src/main/java/tv/own/owntv/features/series/SeriesScreen.kt`)

- [ ] **Step 1:** Apply to both panes:
  - Title: `titleLarge` → `headlineMedium`.
  - Meta line (`metaLine(...)` output): `bodyMedium` → `labelMedium` + `colors.onSurfaceVariant`; wrap the Text with `fontFeatureSettings`: use `style = MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum")` (year/duration digits align).
  - Genres line: `colors.primary` → `colors.onSurfaceVariant` (accent is for active things, not metadata).
  - Plot: keep `bodyMedium`, add `modifier = Modifier.widthIn(max = 360.dp)` (≈46ch at 14sp).
  - Poster corner: `RoundedCornerShape(12.dp)` → `RoundedCornerShape(Dimens.CornerMedium)`.
  - The "OK to play" hint keeps `labelMedium`/`onSurfaceVariant` (already quiet).
- [ ] **Step 2: Compile:** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit:** `git add -A app/src/main/java/tv/own/owntv/features && git commit -m "Rebuild detail-pane hierarchy on the new type scale"`

### Task 15: Live preview pane + idle PreviewPane

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/live/LiveScreen.kt` (`LivePreviewPane`, ~line 770, plus `ChannelMetaRow`/`EpgSection` beneath it)
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/PreviewPane.kt`

- [ ] **Step 1: LiveScreen.kt** — channel name `titleLarge` → `headlineMedium`; the 16:9 video/logo box corner `RoundedCornerShape(12.dp)` → `RoundedCornerShape(Dimens.CornerMedium)`; the small black overlay chips (`RoundedCornerShape(6.dp)`) → `RoundedCornerShape(50)` pill and drop their `fontWeight = FontWeight.Bold`; in `EpgSection` (read it below `LivePreviewPane`), time text gets `.copy(fontFeatureSettings = "tnum")`.
- [ ] **Step 2: PreviewPane.kt** — hint hierarchy: first Text `titleMedium` stays; second Text stays `bodyMedium`; corner already `Dimens.CardCorner` — no change. (This file mostly verifies clean; touch nothing else.)
- [ ] **Step 3: Compile:** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.
- [ ] **Step 4:** RITUAL — capture Live with a channel focused (preview pane populated) and the idle pane (no selection). Check hierarchy and corners.
- [ ] **Step 5: Commit:** `git add -A && git commit -m "Align live and idle preview panes with the detail hierarchy"`

### Task 16: Final gate — full verification sweep

**Files:** none (fix-forward commits only).

- [ ] **Step 1:** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.
- [ ] **Step 2:** RITUAL, full pass: Home / Movies (grid + detail) / Series / Live / Search / Guide / Settings / a dialog / player HUD. Then three variation sweeps: (a) accent → Violet (verify neutrals stay neutral, HUD follows), back to default; (b) Light theme (verify ink focus ring is visible, surfaces read neutral); (c) glass Off then On (verify chips/panels in both).
- [ ] **Step 3:** Pseudolocale spot check: switch app language to `en-XA` (debug builds package it) and capture Settings + a dialog — confirm Figtree/fallback text doesn't clip in chips or buttons (spec risk: Figtree runs wider than Roboto). Switch back.
- [ ] **Step 4:** D-pad walkthrough across all sections; confirm no focus traps or order changes.
- [ ] **Step 5:** Fix findings, re-verify, commit each fix with a specific message. When clean, this plan is done — hand off to superpowers:finishing-a-development-branch for merge/PR.
