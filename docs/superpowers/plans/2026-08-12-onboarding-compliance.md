# Onboarding Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring every first-run onboarding screen up to the makeover's design standard, built on one correct, reusable `SetupScaffold` so the pattern can be carried into the rest of the app.

**Architecture:** A new stateless `SetupScaffold` composable (idiomatic slot API with scaffold-provided text styles, true dp/sp sizing, shared ambient backdrop) replaces the ad-hoc `MainSetupPage`/`Centered` helpers. Every onboarding screen is rebuilt on it. `ChoiceCard` is corrected to the white-ring focus language. Two stages: scaffold + straightforward screens first (validated on device), then the custom/dense screens.

**Tech Stack:** Kotlin, Jetpack Compose for TV (`androidx.tv:tv-material`), Koin/DataStore (untouched — this is composition/visual only).

## Global Constraints

- Work on branch `onboarding-compliance` (exists; spec committed there at `bbd85f7`).
- **No `res/values*` changes.** Reuse every existing translated string verbatim by its current key. No key added/changed/removed.
- **No behavior/flow/ViewModel changes.** Onboarding step transitions, navigation, focus order, and `SetupViewModel` are unchanged. The single accepted behavior change: the dense manual source form scrolls at full size instead of pixel-shrinking.
- **Accent rule (two tiers):** solid `colors.primary` fill = focus/active/selected only (exactly one per state); `colors.primaryContainer` tonal = sanctioned standing affordance (icon tiles). White ring is the sole focus signal — never color-shift a title/label to signal focus.
- **No pixel-scale:** the `graphicsLayer { scaleX/scaleY }` content scaling is removed; size in dp/sp at 1.0; overflow scrolls.
- **Icon semantics:** `OwnTVIcon.CHEVRON` for proceed/next; play triangle only for actual playback.
- Staged files only per task; never bare `git add -A` (the repo has pre-existing dirty gradle-wrapper files — never stage them). Commit trailer:
  `Co-Authored-By: Claude Fable 5 <noreply@anthropic.com>`
- Gate every task: `./gradlew :app:compileStandardDebugKotlin`; before each stage-final commit `./gradlew testStandardDebugUnitTest lintStandardDebug` (lint is CI-gating, `PluralsCandidate` fatal).
- **Emulator ritual (RITUAL):** `./gradlew :app:assembleStandardDebug` → `adb -s emulator-5554 install -r app/build/outputs/apk/standard/debug/app-standard-debug.apk` → `adb -s emulator-5554 shell pm clear tv.own.owntv` → force English by writing the locale pref (`run-as tv.own.owntv` → `shared_prefs/owntv_locale.xml` with `<string name="ui_language">en-US</string>`) → launch `am start -n tv.own.owntv/.MainActivity` → walk the flow with `adb shell input keyevent KEYCODE_DPAD_*` / `KEYCODE_DPAD_CENTER` / `KEYCODE_BACK` → capture `adb exec-out screencap -p > <scratchpad>/<name>.png` and Read each PNG.

---

## Stage A — scaffold, component, straightforward screens

### Task 1: `SetupScaffold`

**Files:**
- Create: `app/src/main/java/tv/own/owntv/features/setup/SetupScaffold.kt`

**Interfaces:**
- Produces: `@Composable fun SetupScaffold(title: @Composable () -> Unit, subtitle: (@Composable () -> Unit)? = null, showLogoBadge: Boolean = true, content: @Composable ColumnScope.() -> Unit)` and `@Composable fun SetupAmbientBackdrop()` (moved here from SetupWizard.kt).

- [ ] **Step 1: Verify the tv-material3 text-style API resolves.** In a scratch spot (or a throwaway top-level `private val` in the new file), confirm these imports compile:
  `import androidx.tv.material3.LocalTextStyle`, `import androidx.tv.material3.ProvideTextStyle`, `import androidx.tv.material3.LocalContentColor`.
  Run `./gradlew :app:compileStandardDebugKotlin`. If `ProvideTextStyle` does not resolve, use `androidx.compose.runtime.CompositionLocalProvider(LocalTextStyle provides style) { ... }` instead throughout this task. Note which path you used in the report.

- [ ] **Step 2: Write `SetupScaffold.kt`.** Move `SetupAmbientBackdrop` verbatim from `SetupWizard.kt` (the `rememberInfiniteTransition` + `Canvas` two-ring + glow treatment) into this file, changing its visibility from `private` to `internal` — `MainSetupPage` (still in `SetupWizard.kt`, same package, deleted later in Task 9) calls it, and Kotlin `private` top-level functions are file-scoped, so `internal` is required for the cross-file call to resolve until Task 9. Add the scaffold:

```kotlin
package tv.own.owntv.features.setup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.LocalTextStyle
import androidx.tv.material3.ProvideTextStyle
import androidx.compose.runtime.CompositionLocalProvider
import tv.own.owntv.ui.components.BrandLockup
import tv.own.owntv.ui.theme.OwnTVTheme
import tv.own.owntv.ui.theme.OwnTVTypography

// (SetupAmbientBackdrop moved here from SetupWizard.kt, visibility changed private -> internal.)

/**
 * The shared frame for every first-run setup page: ambient backdrop, an optional small OwnTV badge,
 * a hero title, an optional subtitle, and the page's controls — all at true dp/sp size (no pixel
 * scaling; long pages scroll). Title/subtitle are slots that inherit a canonical style provided here,
 * so call sites carry no `style =` and cannot drift.
 */
@Composable
fun SetupScaffold(
    title: @Composable () -> Unit,
    subtitle: (@Composable () -> Unit)? = null,
    showLogoBadge: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = OwnTVTheme.colors
    Box(Modifier.fillMaxSize()) {
        SetupAmbientBackdrop()
        Box(
            modifier = Modifier.fillMaxSize().padding(48.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                if (showLogoBadge) {
                    BrandLockup(markSize = 40, textSize = 30)
                    Spacer(Modifier.height(22.dp))
                }
                // Title slot: provide the canonical hero style + on-surface content colour so a bare
                // Text() in the slot needs no styling. (If ProvideTextStyle is unavailable, wrap with
                // CompositionLocalProvider(LocalTextStyle provides ...) instead — see Step 1.)
                ProvideTextStyle(OwnTVTypography.headlineLarge) {
                    CompositionLocalProvider(LocalContentColor provides colors.onSurface) {
                        title()
                    }
                }
                if (subtitle != null) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.widthIn(max = 620.dp)) {
                        ProvideTextStyle(
                            OwnTVTypography.bodyLarge.copy(textAlign = TextAlign.Center),
                        ) {
                            CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                                subtitle()
                            }
                        }
                    }
                }
                Spacer(Modifier.height(44.dp))
                content()
            }
        }
    }
}
```

Note: `androidx.tv.material3.Text` reads `LocalTextStyle`/`LocalContentColor` for its defaults, so a slot passing `Text(stringResource(...))` with no `style`/`color` inherits what the scaffold provides. Keep `SetupAmbientBackdrop` `private` to this file only if nothing else references it; it's used solely by the scaffold now.

- [ ] **Step 3: Compile.** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL. (SetupWizard.kt still has its own `SetupAmbientBackdrop`; you'll delete that in Task 9. Two copies compile fine short-term, but to avoid a duplicate-symbol clash, in this step also DELETE the `SetupAmbientBackdrop` function from `SetupWizard.kt` now — every current caller is `MainSetupPage`, which you leave intact but point at the moved function via the same package, so no import change is needed since both files are in `tv.own.owntv.features.setup`.)

- [ ] **Step 4: Commit.**
```bash
git add app/src/main/java/tv/own/owntv/features/setup/SetupScaffold.kt app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt
git commit -m "Add SetupScaffold: shared slot-based frame for onboarding"
```

### Task 2: Correct `ChoiceCard`

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt` (the `ChoiceCard` composable)

**Interfaces:**
- Consumes: nothing new.
- Produces: `ChoiceCard` signature unchanged (`icon`, `title`, `desc`, `modifier`, `onClick`) — 7 call sites keep compiling.

- [ ] **Step 1: Remove the focus→accent title recolor.** In `ChoiceCard`, change the title `Text` color from `if (focused) colors.primary else colors.onSurface` to always `colors.onSurface`. The white focus ring (from `FocusableSurface`) is the focus signal. Keep the tonal `primaryContainer` icon tile exactly as-is (sanctioned standing affordance). The `focused` lambda param may now be unused — if so, rename it to `_`.

```kotlin
        Text(title, style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
```

- [ ] **Step 2: Compile.** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit.**
```bash
git add app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt
git commit -m "ChoiceCard: let the white ring signal focus, not an accent title"
```

### Task 3: Convert `WelcomeScreen`

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt` (`WelcomeScreen`)

**Interfaces:**
- Consumes: `SetupScaffold` (Task 1).

- [ ] **Step 1: Rebuild `WelcomeScreen` on the scaffold**, dropping the `MainSetupPage(contentScale = 0.92f)` wrapper and passing the wordmark as the title with `showLogoBadge = false`. Preserve the shipped refinements: `FirstRunLanguageSelector`, the 12dp ladder gap, the SECONDARY chevron Get Started.

```kotlin
@Composable
private fun WelcomeScreen(onNext: () -> Unit) {
    SetupScaffold(
        // The wordmark IS the hero here, so it replaces the text title and the small badge is off.
        title = { BrandLockup(markSize = 82, textSize = 62) },
        subtitle = { Text(stringResource(R.string.setup_welcome_tagline)) },
        showLogoBadge = false,
    ) {
        FirstRunLanguageSelector()
        Spacer(Modifier.height(12.dp))
        OwnTVButton(
            stringResource(R.string.setup_get_started),
            onClick = onNext,
            modifier = Modifier.width(216.dp).height(52.dp),
            style = OwnTVButtonStyle.SECONDARY,
            icon = OwnTVIcon.CHEVRON,
        )
    }
}
```

- [ ] **Step 2: Compile.** `./gradlew :app:compileStandardDebugKotlin` → BUILD SUCCESSFUL.

- [ ] **Step 3: Commit.**
```bash
git add app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt
git commit -m "Rebuild WelcomeScreen on SetupScaffold"
```

### Task 4: Convert `DisclaimerScreen`

**Files:** Modify `app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt` (`DisclaimerScreen`).

- [ ] **Step 1: Rebuild on the scaffold**, dropping `MainSetupPage` and the `SetupAccentRule()` line. Title/subtitle become slots (no `style =` at the call site). Keep the auto-focus on "I Understand", Back as SECONDARY.

```kotlin
@Composable
private fun DisclaimerScreen(onAgree: () -> Unit, onBack: () -> Unit) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    BackHandler { onBack() }
    SetupScaffold(
        title = { Text(stringResource(R.string.setup_before_you_start)) },
        subtitle = { Text(stringResource(R.string.setup_disclaimer)) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OwnTVButton(
                stringResource(R.string.common_back),
                onClick = onBack,
                modifier = Modifier.width(140.dp),
                style = OwnTVButtonStyle.SECONDARY,
            )
            OwnTVButton(
                stringResource(R.string.setup_i_understand),
                onClick = onAgree,
                modifier = Modifier.width(220.dp).focusRequester(fr),
            )
        }
    }
}
```
Note: `DisclaimerScreen` previously had no `BackHandler`; adding one keeps Back consistent with sibling screens and matches the existing `onBack` param — safe, no flow change (it already navigated back via the button). If you prefer zero behavior delta, omit the `BackHandler` line; either is acceptable — state which in the report.

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/SetupWizard.kt && git commit -m "Rebuild DisclaimerScreen on SetupScaffold; drop the accent rule"`

### Task 5: Convert `SetupChoiceScreen`

**Files:** Modify `SetupWizard.kt` (`SetupChoiceScreen`).

- [ ] **Step 1: Rebuild on the scaffold**, dropping `MainSetupPage` and `SetupAccentRule()`:

```kotlin
@Composable
private fun SetupChoiceScreen(onCreate: () -> Unit, onRestore: () -> Unit, onBack: () -> Unit) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    BackHandler { onBack() }
    SetupScaffold(
        title = { Text(stringResource(R.string.setup_set_up_owntv)) },
        subtitle = { Text(stringResource(R.string.setup_setup_choice_description)) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChoiceCard(icon = OwnTVIcon.PERSON, title = stringResource(R.string.setup_new_profile), desc = stringResource(R.string.setup_create_profile_add_sources), modifier = Modifier.focusRequester(fr), onClick = onCreate)
            ChoiceCard(icon = OwnTVIcon.DOWNLOADS, title = stringResource(R.string.setup_restore_backup), desc = stringResource(R.string.setup_import_profiles_playlists), onClick = onRestore)
        }
    }
}
```

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/SetupWizard.kt && git commit -m "Rebuild SetupChoiceScreen on SetupScaffold"`

### Task 6: Convert `AddContentScreen`

**Files:** Modify `SetupWizard.kt` (`AddContentScreen`).

- [ ] **Step 1: Rebuild on the scaffold**, dropping `MainSetupPage` and `SetupAccentRule()`; keep the conditional Existing card and the SECONDARY Skip button:

```kotlin
@Composable
private fun AddContentScreen(hasExisting: Boolean, onNew: () -> Unit, onExisting: () -> Unit, onImport: () -> Unit, onSkip: () -> Unit) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    SetupScaffold(
        title = { Text(stringResource(R.string.setup_add_playlist)) },
        subtitle = { Text(stringResource(R.string.setup_add_playlist_description)) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChoiceCard(icon = OwnTVIcon.ADD, title = stringResource(R.string.setup_new), desc = stringResource(R.string.setup_add_m3u_xtream), modifier = Modifier.focusRequester(fr), onClick = onNew)
            if (hasExisting) {
                ChoiceCard(icon = OwnTVIcon.PLAYLIST, title = stringResource(R.string.setup_existing), desc = stringResource(R.string.setup_use_other_profile_playlists), onClick = onExisting)
            }
            ChoiceCard(icon = OwnTVIcon.DOWNLOADS, title = stringResource(R.string.setup_import), desc = stringResource(R.string.setup_restore_backup_file), onClick = onImport)
        }
        Spacer(Modifier.height(24.dp))
        OwnTVButton(
            stringResource(R.string.setup_skip_for_now),
            onClick = onSkip,
            modifier = Modifier.width(190.dp),
            style = OwnTVButtonStyle.SECONDARY,
        )
    }
}
```

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/SetupWizard.kt && git commit -m "Rebuild AddContentScreen on SetupScaffold"`

### Task 7: Convert `AddSourceChooserScreen`

**Files:** Modify `app/src/main/java/tv/own/owntv/features/setup/AddSourceChooserScreen.kt`.

- [ ] **Step 1: Read the file.** It currently rolls its own centered layout with a title, subtitle, a Row of `ChoiceCard`s (Remote / Manual), and a Back button — and may have its own `SetupAccentRule`-equivalent or a `Centered` wrapper. Rebuild it on `SetupScaffold`: title slot = its existing title string, subtitle slot = its existing description string, content = the `ChoiceCard` Row + the SECONDARY Back button. Remove any decorative rule. Preserve the existing `stringResource` keys, `FocusRequester` auto-focus, `BackHandler`, and `onRemote`/`onManual`/`onBack` callbacks verbatim.

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/AddSourceChooserScreen.kt && git commit -m "Rebuild AddSourceChooserScreen on SetupScaffold"`

### Task 8: Convert `ImportBackupChooserScreen`

**Files:** Modify `SetupWizard.kt` (`ImportBackupChooserScreen`).

- [ ] **Step 1: Rebuild on the scaffold**, replacing the `Centered { }` wrapper. Keep the two `ChoiceCard`s (from-phone / local-file), the SECONDARY Back, auto-focus, and `BackHandler`:

```kotlin
@Composable
private fun ImportBackupChooserScreen(onRemote: () -> Unit, onLocal: () -> Unit, onBack: () -> Unit) {
    val fr = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { fr.requestFocus() } }
    BackHandler { onBack() }
    SetupScaffold(
        title = { Text(stringResource(R.string.setup_restore_a_backup)) },
        subtitle = { Text(stringResource(R.string.setup_restore_choice_description)) },
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ChoiceCard(icon = OwnTVIcon.PLAYLIST, title = stringResource(R.string.setup_from_phone), desc = stringResource(R.string.setup_upload_from_wifi_device), modifier = Modifier.focusRequester(fr), onClick = onRemote)
            ChoiceCard(icon = OwnTVIcon.DOWNLOADS, title = stringResource(R.string.setup_local_file), desc = stringResource(R.string.setup_pick_backup_local), onClick = onLocal)
        }
        Spacer(Modifier.height(24.dp))
        OwnTVButton(stringResource(R.string.common_back), onClick = onBack, style = OwnTVButtonStyle.SECONDARY)
    }
}
```

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/SetupWizard.kt && git commit -m "Rebuild ImportBackupChooserScreen on SetupScaffold"`

### Task 9: Stage-A cleanup + emulator verification

**Files:** Modify `SetupWizard.kt` (delete dead helpers); no new code.

- [ ] **Step 1: Delete the dead helpers** now that all Stage-A callers use `SetupScaffold`: remove `SetupAccentRule`, `MAIN_SETUP_CONTENT_SCALE`, and `MainSetupPage` from `SetupWizard.kt`. If a `Centered` helper exists and is now unused (its last users convert in Stage B — check with `grep -n "Centered(" app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt`), leave it until Stage B. Remove now-unused imports (`graphicsLayer`, etc.). Run `grep -n "SetupAccentRule\|MAIN_SETUP_CONTENT_SCALE\|MainSetupPage" app/src/main/java` — expect zero hits outside deletions.

- [ ] **Step 2: Gate.** `./gradlew :app:compileStandardDebugKotlin testStandardDebugUnitTest lintStandardDebug` → all green.

- [ ] **Step 3: RITUAL.** Build, install, clear, force English, launch. Walk: welcome → (DOWN, CENTER) disclaimer → (CENTER) setup-choice → focus each card → back out to disclaimer → forward to add-content (create a profile first if the flow requires it; or screenshot setup-choice + disclaimer + add-source chooser reachable without a profile). Capture each Stage-A screen. Read every PNG and check against the spec: no accent dash; title is the hero; cards neutral-at-rest with white-ring focus (no accent title); large/legible; single accent = focused control. **Re-verify welcome specifically** keeps its single-line language label, ladder, chevron, rings.

- [ ] **Step 4: Fix findings, re-capture, commit.**
```bash
git add app/src/main/java/tv/own/owntv/features/setup/SetupWizard.kt
git commit -m "Delete MainSetupPage/SetupAccentRule/content-scale after Stage-A migration"
```

---

## Stage B — custom-layout screens

### Task 10: Convert `ExistingSourcesScreen`

**Files:** Modify `SetupWizard.kt` (`ExistingSourcesScreen`).

- [ ] **Step 1: Wrap in the scaffold.** Replace the raw `Box … Column(widthIn 620)` header with `SetupScaffold(title = { Text(stringResource(R.string.setup_use_existing_playlists)) }, subtitle = { Text(stringResource(R.string.setup_pick_playlists)) })`, and put the existing `LazyColumn` selectable list + the Back/Add `Row` in the content slot. Keep: the `listMax` height cap, the per-row `FocusableSurface` with `selected`/`selectedContainerColor = colors.primaryContainer` (selection = accent is correct), the row's `titleMedium`/`bodySmall`, the star on selected, the SECONDARY Back, and the enabled-gated Add with its `pluralStringResource`. The selected-row text uses `onPrimaryContainer` — leave it (selected = active). No decorative accent to remove here.

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/SetupWizard.kt && git commit -m "Rebuild ExistingSourcesScreen on SetupScaffold"`

### Task 11: Convert `ImportBackupScreen`

**Files:** Modify `SetupWizard.kt` (`ImportBackupScreen`, ~line 471).

- [ ] **Step 1: Read the composable in full**, then wrap its header (title + any subtitle) in `SetupScaffold` and move its body (the local-file picker / restore controls) into the content slot. Replace any `Centered { }` wrapper. Apply the accent rule: any non-focused text currently using `colors.primary` purely for emphasis (not a selected/active state) → `colors.onSurface`/`onSurfaceVariant`; buttons follow the SECONDARY-at-rest / focused-accent pattern. Preserve all string keys and callbacks. List each accent decision in the report.

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/SetupWizard.kt && git commit -m "Rebuild ImportBackupScreen on SetupScaffold"`

### Task 12: Convert `ImportProgressScreen`

**Files:** Modify `SetupWizard.kt` (`ImportProgressScreen`).

- [ ] **Step 1: Wrap in the scaffold and fix the accent-on-static-text.** Replace the `Centered { }` wrapper with `SetupScaffold`. This screen has three states (Running/Idle, Success, Failed) in a `when`. The scaffold's title/subtitle slots don't fit a stateful body cleanly, so pass a minimal title (e.g. `title = { Text(stringResource(R.string.setup_importing_catalog)) }` for the running state) OR — cleaner — pass an empty title and keep the whole stateful `when` in the content slot with the spinner/headline/buttons. Choose the approach that keeps the three states intact; state which in the report. **Accent fix:** the progress `headlineLarge` primary-colored text (`color = colors.primary` on `display?.primaryText()` and the "preparing" fallback) is static emphasis, not focus/active — change to `colors.onSurface`. The "Run in background" button uses `icon = OwnTVIcon.PLAY` — it genuinely enters playback/app, so PLAY is defensible; leave it (note in report). Keep spinner, counts, retry/cancel/continue buttons and their focus requesters.

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/SetupWizard.kt && git commit -m "Rebuild ImportProgressScreen on SetupScaffold; quiet static accent text"`

### Task 13: Convert `RemoteSetupScreen`

**Files:** Modify `app/src/main/java/tv/own/owntv/features/setup/RemoteSetupScreen.kt`.

- [ ] **Step 1: Read the file**, then wrap its header (title/subtitle) in `SetupScaffold` and keep its QR + PIN pairing body in the content slot. Remove any decorative accent rule. Apply the accent rule to buttons and any emphasis text (QR/PIN codes that are informational should be `onSurface`, not `primary`, unless they represent an active state). Preserve all string keys, the pairing state handling, and callbacks. List accent decisions in the report.

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/RemoteSetupScreen.kt && git commit -m "Rebuild RemoteSetupScreen on SetupScaffold"`

### Task 14: Convert `AddSourceScreen` (dense manual form)

**Files:** Modify `app/src/main/java/tv/own/owntv/features/setup/AddSourceScreen.kt`.

- [ ] **Step 1: Read the file.** It's the Xtream/M3U/Stalker form (tabs + `OwnTVTextField`s + Start Import). Wrap its header (title/subtitle) in `SetupScaffold`; put the tab row + form fields + action buttons in the content slot. This is the **dense exception**: the scaffold already provides `verticalScroll`, so remove any local shrink/scale and let the form scroll at full size. Verify the focused field scrolls into view on D-pad (Compose does this by default; confirm on device in Task 15). Apply the accent rule to the tab selector (selected tab = accent is correct; unselected = tonal/neutral) and buttons. Preserve all string keys, the form state, validation, and the import trigger. List accent decisions in the report.

- [ ] **Step 2: Compile** → BUILD SUCCESSFUL.
- [ ] **Step 3: Commit.** `git add …/AddSourceScreen.kt && git commit -m "Rebuild AddSourceScreen on SetupScaffold; form scrolls at full size"`

### Task 15: Final verification sweep

**Files:** none (fix-forward only).

- [ ] **Step 1:** Delete `Centered` from `SetupWizard.kt` if now unused (`grep -n "Centered(" app/src/main/java/tv/own/owntv/features/setup/`). Remove any leftover unused imports. Gate: `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.

- [ ] **Step 2: RITUAL, full flow.** Walk the entire onboarding end to end: welcome → disclaimer → setup-choice → (new profile) create-profile → add-content → add-source chooser → manual source form (scroll through the fields with DOWN; confirm the focused field stays visible) → back out → import-backup chooser → import progress. Capture every screen. Read each PNG; verify against the spec: shared rhythm, title-as-hero, one accent per state, cards neutral-at-rest, dense form scrolls legibly, no accent dashes anywhere.

- [ ] **Step 3: Variation checks.** (a) Light theme — one onboarding screen, confirm the scaffold's provided content colors read correctly (ink title on light ground). (b) `en-XA` pseudolocale on one card screen — confirm titles/cards absorb ~40% text expansion without clipping. (c) RTL: if an Arabic build is feasible, spot-check one screen; otherwise confirm start/end-aware paddings by code inspection and note it.

- [ ] **Step 4: Fix findings, re-verify, commit each fix.** When clean, hand off to `superpowers:finishing-a-development-branch`.
