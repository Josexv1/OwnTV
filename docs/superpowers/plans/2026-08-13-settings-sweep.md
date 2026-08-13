# Phase-2 Settings Sweep Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close every Settings audit finding (shared stepper, static accents, Customize focus recolors, toggle-chip Off state) and adopt `MediaContextMenu` in the two remaining hand-rolled menus.

**Architecture:** Promote the existing `StepperDialog` from `VideoPlayerSettingsScreen.kt` to `ui/components/` with a neutral value-color contract and a nullable `onReset`; route all hand-rolled stepper sites through it. The remaining work is targeted color/state edits and two menu-body swaps following phase 1's proven `MediaContextMenu` recipe.

**Tech Stack:** Kotlin, Jetpack Compose for TV (tv-material3), existing `ui/components/` (`MediaContextMenu`/`MenuEntry`, `FocusableSurface`, `dialogPanel`, `OwnTVButton`).

## Global Constraints

- **Behavior unchanged:** every dialog's step/range/reset/done logic, every menu action and gate, all focus traversal. Composition/visual only.
- **i18n:** zero `res/values*` changes; `stringResource` resolution stays in composables (the shared StepperDialog keeps resolving its own Reset/Done chrome internally, as the original already does — `ui/components` precedent: StorageBrowser.kt).
- **Accent rule:** solid `primary` = focus/active/selected only. KEPT (sanctioned, exhaustive for touched files): tonal icon tiles (TileTone), ManageSources "Default"/"Deleting"/sync-% badges, catchup-player selected-option PRIMARY buttons (SettingsScreen.kt:2019-2028), QuickToggleChip **On** state tonal.
- Menu parity = per screen against its own current menu: labels, icons, conditional gates, order identical; internal `Spacer(4.dp)` groupings drop to the uniform 8dp rhythm (settled phase-1 ruling).
- Gates per task: `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` (0 errors; PluralsCandidate fatal); `git status --porcelain -- app/src/main/res` empty.
- Line numbers for SettingsScreen/VideoPlayerSettingsScreen/CustomizeScreen are from the audit sweep (files untouched since); LiveScreen/SeriesScreen menu lines from the phase-1 final review. **Implementers verify surrounding context before editing.**
- Branch: `settings-sweep` off `main`.

---

### Task 1: Promote `StepperDialog` to `ui/components/`; route VideoPlayerSettingsScreen

**Files:**
- Create: `app/src/main/java/tv/own/owntv/ui/components/StepperDialog.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/settings/VideoPlayerSettingsScreen.kt` (delete local `StepperDialog` ~:796 + its private `StepBtn` if unused elsewhere; convert `SubtitleTransparencyDialog` :1280-1355; keep the two existing call sites `Dialog.AUDIO_SYNC` :425 / `Dialog.LIVE_CUSTOM` :454 compiling against the shared import)

**Interfaces — Produces (Task 2 relies on this exact signature):**
```kotlin
@Composable
fun StepperDialog(
    title: String,
    value: Int,
    step: Int,
    min: Int,
    max: Int,
    format: @Composable (Int) -> String,
    onSet: (Int) -> Unit,
    onDismiss: () -> Unit,
    onReset: (() -> Unit)? = null,     // null → no Reset button (several Settings dialogs have none)
)
```

- [ ] **Step 1: Move the component.** Copy the existing `internal fun StepperDialog` (VideoPlayerSettingsScreen.kt ~:796-855) into the new file as `fun StepperDialog` with the signature above, preserving VERBATIM: the disabled-stepper focus-handoff logic (frPlus/frMinus `LaunchedEffect`s and the comment explaining the "+/- unreachable" trap), `BackHandler`, `PopupFontTheme`, scrim + `trapAllFocusExit().focusGroup()`, `dialogPanel(width = 360.dp, corner = 16.dp, padding = 16.dp)`, the `[−] value [+]` row, and the Reset/Done action row. Two changes ONLY: (a) the center value `Text` color `colors.primary` → **`colors.onSurface`** (design contract: the value is the sole readout — add a one-line comment saying so); (b) `onReset` nullable — render the Reset `OwnTVButton` only when `onReset != null` (when null, the action row is just Done, right-aligned). Copy the private `StepBtn` helper into the new file as a private composable (verify its source in VideoPlayerSettingsScreen.kt; if `StepBtn` has other users in that file, leave the original in place too).
- [ ] **Step 2: Route VideoPlayerSettingsScreen.** Delete the local `StepperDialog`; import the shared one (the two existing call sites pass `onReset = {...}` — reorder args if needed to match the new signature). Convert `SubtitleTransparencyDialog` (:1280-1355): replace its hand-rolled `StepBtn` + value-text + `StepBtn` row with a call to the shared `StepperDialog`, keeping its exact title/format strings, step/min/max, and set/reset/dismiss behavior. If its layout carries extra content the dialog can't express (e.g. a live preview row), keep the local layout and apply only the value-color contract in place — record the choice in the report.
- [ ] **Step 3: Gate.** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` → green. **Step 4: Commit.** `git commit -am "Promote StepperDialog to ui/components with neutral value readout"`

### Task 2: SettingsScreen — route 5 steppers, quiet statics, fix QuickToggleChip Off

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/SettingsScreen.kt`

**Interfaces — Consumes:** shared `StepperDialog` (Task 1 signature).

- [ ] **Step 1 (commit A): Route the five hand-rolled steppers** through the shared `StepperDialog`, preserving each dialog's exact strings, step/min/max, format, and set/reset/done wiring: `ZoomDialog` (:1550-1570), `GlassEffectDialog` alpha (:1711-1721) and blur (:1732-1742) — if GlassEffectDialog hosts TWO steppers in one dialog surface, it does not fit the single-value component: keep its local layout and apply the value-color contract (`primary` → `onSurface`) to both value texts in place, recording the choice — `CatchupTimezoneDialog` (:1997-2009), `EpgOffsetSettingDialog` (:2072-2084). Dialogs without a Reset pass `onReset = null`. Delete now-dead local stepper-row code (keep `StepButton` if other non-stepper UI uses it). Commit: `git commit -am "SettingsScreen: route stepper dialogs through shared StepperDialog"`
- [ ] **Step 2 (commit B): Static accents + QuickToggleChip.**
  - `:1192` (version), `:1203` (GitHub URL), `:1211` (Telegram URL), `:1345` (backup path): `color = colors.primary` → `color = colors.onSurfaceVariant`.
  - `:1318` (playback-error-log kind text when `Kind == ERROR`): `colors.primary` → `colors.favorite` (semantic error severity; Theme.kt maps `error = c.favorite`).
  - `QuickToggleChip` (~:405-445 region): keep `on` → `TileTone.PRIMARY.colors()`; replace the `off` branch's `TileTone.SECONDARY.colors()` with the neutral pair `colors.surfaceContainerHigh` (bg) + `colors.onSurfaceVariant` (fg). The trailing On/Off word logic and glass edge stay as they are.
  - Commit: `git commit -am "SettingsScreen: quiet static accents; neutral Off state for quick toggles"`
- [ ] **Step 3: Gate** after each commit (compile + lint green; res untouched).

### Task 3: CustomizeScreen focus fixes

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/customize/CustomizeScreen.kt`

- [ ] **Step 1:** `SectionChip` (:645-667): remove the `focused -> colors.primary` arm from the label color; label = `onSurface` normally, `onPrimaryContainer` when **selected** (tonal fill stays for selected). Ring signals focus.
- [ ] **Step 2:** `CategoryRow` name button (:708-739): remove the `focusedContainerColor = colors.primaryContainer` override (:724) and the `focused -> onPrimaryContainer` label arm (:734) — container/label no longer change on focus; any *selected* treatment stays as-is.
- [ ] **Step 3: Gate + commit.** `git commit -am "CustomizeScreen: ring-only focus for chips and category rows"`

### Task 4: Menu twins → `MediaContextMenu`

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/live/LiveScreen.kt` (`ChannelContextMenu` :679-733)
- Modify: `app/src/main/java/tv/own/owntv/features/series/SeriesScreen.kt` (`EpisodeContextMenu` :867+)

**Interfaces — Consumes:** `MediaContextMenu(title, entries, onDismiss, closeLabel)` + `MenuEntry(label, onClick, icon?)` from `ui/components/` (phase 1). Reference adoptions: `MoviesScreen.kt`'s and `SeriesScreen.kt`'s existing context-menu functions on `main`.

- [ ] **Step 1: LiveScreen.** Read `ChannelContextMenu` in full; keep the function + parameters; replace the body with `MediaContextMenu`, building `List<MenuEntry>` reproducing the exact labels, icons, gates, and order; `closeLabel = stringResource(R.string.content_close)` (verify the exact close-string key the old body uses and reuse THAT). Delete now-unused frame imports only if nothing else in the file uses them. Commit: `git commit -am "Live: adopt MediaContextMenu for channel long-press"`
- [ ] **Step 2: SeriesScreen.** Same recipe for `EpisodeContextMenu`. Commit: `git commit -am "Series: adopt MediaContextMenu for episode long-press"`
- [ ] **Step 3: Gate** after each commit.

### Task 5: Verification sweep

**Files:** none (fix-forward only).

- [ ] **Step 1: Suite.** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.
- [ ] **Step 2: On-device (controller).** Build + install `standard` debug on `emulator-5554` (Audit profile / DemoAudit source). Screenshot + judge: (a) Settings hub — quick-toggle chips: On = accent tonal, Off = visibly neutral gray; (b) open UI Zoom (or EPG offset) stepper — value reads `onSurface`, +/- works, Reset/Done as before; (c) Customize Categories — chip and row focus = ring only, no label/fill recolor; (d) Live channel long-press → shared menu, same entries; (e) Series episode long-press → shared menu, same entries.
- [ ] **Step 3: Fix findings, re-verify, commit each fix.** When clean: final whole-branch review, then `superpowers:finishing-a-development-branch`.

## Self-Review

1. **Spec coverage:** §1→Task 1 + Task 2 commit A (7 sites; GlassEffect two-stepper fallback mirrors spec's risk clause), §2 table→Task 2 commit B (+ VideoPlayer values via Task 1), §3→Task 3, §4→Task 2 commit B, §5→Task 4, constraints/gates→Global Constraints + Task 5. No gaps.
2. **Placeholder scan:** clean — the promotion names exact preserved behaviors and the two allowed changes; fallback clauses are bounded with report-recording requirements.
3. **Type consistency:** `StepperDialog` signature identical between Task 1 Produces and Task 2 Consumes; `MenuEntry`/`MediaContextMenu` match phase-1 shipped signatures.
