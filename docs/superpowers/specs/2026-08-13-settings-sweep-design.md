# Phase 2 — Settings Sweep & Context-Menu Twins — Design Spec

**Date:** 2026-08-13
**Status:** approved design, pending implementation plan
**Parent:** `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` (roadmap phase 2) + phase-1 final review (menu-twin log entry)
**Goal:** Close every Settings finding from the audit — static accents, duplicated steppers, Customize focus recolors, toggle-chip state legibility — and adopt `MediaContextMenu` in the two remaining hand-rolled menus.

## Scope

**In:**
1. Shared `StepperDialog` in `ui/components/`, routed through all 7 hand-rolled stepper sites.
2. Static-accent deletions in `features/shell/components/SettingsScreen.kt`, `features/settings/ManageSourcesScreen.kt` (VideoPlayerSettingsScreen's two stepper values are covered by workstream 1).
3. Focus-recolor fixes in `features/customize/CustomizeScreen.kt`.
4. `QuickToggleChip` Off-state differentiation in `SettingsScreen.kt`.
5. `ChannelContextMenu` (`features/live/LiveScreen.kt:679-733`) and `EpisodeContextMenu` (`features/series/SeriesScreen.kt:867+`) → `MediaContextMenu`.

**Out:** the 17 skipped Settings subscreens listed in the audit sweep (phase 5 territory); `SettingsRow`/`SourceRow`/tonal icon tiles (already well-factored and sanctioned); any `res/values*` change.

## 1. Shared `StepperDialog` (`ui/components/StepperDialog.kt`)

Promote the existing `VideoPlayerSettingsScreen.kt:796` component (title + `[−] value [+]` row + reset/done actions) to `ui/components/`, generalized only as far as its 8 consumers need — no speculative parameters. The **value text is `colors.onSurface` by construction**: a stepper's number is the sole readout, so hierarchy comes from the type scale; accent added no selection information (audit finding 1, stepper ruling). Consumers:
- `SettingsScreen.kt`: `ZoomDialog` (:1550-1570), `GlassEffectDialog` alpha (:1711-1721) and blur (:1732-1742), `CatchupTimezoneDialog` (:1997-2009), `EpgOffsetSettingDialog` (:2072-2084)
- `VideoPlayerSettingsScreen.kt`: `SubtitleTransparencyDialog` (:1280-1355) plus the two existing `StepperDialog` call sites (`Dialog.AUDIO_SYNC` :425, `Dialog.LIVE_CUSTOM` :454), whose local component is deleted after the promotion.

Each dialog keeps its own strings, ranges, step sizes, and reset/done behavior — the component owns the frame and the value-color contract, not the policy.

## 2. Static-accent deletions

| Site | Today | After |
|---|---|---|
| `SettingsScreen.kt:1192` version string | `colors.primary` | `onSurfaceVariant` |
| `:1203` GitHub URL, `:1211` Telegram URL | `colors.primary` | `onSurfaceVariant` |
| `:1345` backup path | `colors.primary` | `onSurfaceVariant` |
| `:1318` playback-error-log kind text (ERROR rows) | `colors.primary` | `colors.favorite` — semantic error severity, matching the theme's error mapping (`Theme.kt`: `error = c.favorite`) |
| `ManageSourcesScreen.kt:249` in-progress import/resync count | `colors.primary` | `onSurfaceVariant` |
| Stepper values (`SettingsScreen.kt:1563/:1716/:1737/:2004/:2079`, `VideoPlayerSettingsScreen.kt:837/:1333`) | `colors.primary` | `onSurface` via workstream 1 |

**Explicitly kept (sanctioned):** tonal icon tiles (`SettingsRow` TileTone system), "Default"/"Deleting"/sync-% badges in ManageSourcesScreen, the catchup-player selection buttons (`SettingsScreen.kt:2019-2028` — PRIMARY marks the one selected option among three, the correct pattern).

## 3. Customize focus fixes (`CustomizeScreen.kt`)

- `SectionChip` (:645-667): the `focused -> colors.primary` label arm dies; label is `onSurface`, except **selected** which keeps `onPrimaryContainer` on the tonal fill (selection ≠ focus; ring signals focus).
- `CategoryRow` name button (:708-739): drop the `focusedContainerColor = colors.primaryContainer` override (:724) and the `focused -> onPrimaryContainer` label arm (:734). Ring only; selected state (if the row has one) keeps its sanctioned treatment.

## 4. `QuickToggleChip` Off state (`SettingsScreen.kt`)

On keeps `TileTone.PRIMARY` tonal (on = active — sanctioned accent). Off switches from `TileTone.SECONDARY` (reads accent-adjacent on screen — audit hub screenshot) to visibly neutral: `colors.surfaceContainerHigh` fill + `colors.onSurfaceVariant` icon/label. The trailing On/Off word keeps its current colors (`fg` when on, `onSurfaceVariant` when off). Glass edge treatment unchanged.

## 5. Context-menu twins → `MediaContextMenu`

Same recipe as phase 1's Movies/Series adoptions: keep each function and its parameters, replace the body with `MediaContextMenu(title, entries, onDismiss, closeLabel)`, build `List<MenuEntry>` reproducing the exact labels, icons, conditional gates, and order. Parity is per-screen against its own current menu. The internal `Spacer(4.dp)` groupings drop to the uniform 8dp rhythm (settled phase-1 ruling).
- `ChannelContextMenu` — `features/live/LiveScreen.kt:679-733`
- `EpisodeContextMenu` — `features/series/SeriesScreen.kt:867+`

## Constraints & invariants

- **Behavior unchanged:** every dialog's step/range/reset/done logic, every menu action and gate, all focus traversal. Composition/visual only.
- **i18n:** zero `res/values*` changes; components take resolved strings; `stringResource` stays in feature composables.
- **Accent rule:** solid `primary` = focus/active/selected only; the kept list in §2 is exhaustive for the touched files.
- **Gates:** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` per task (0 errors, PluralsCandidate fatal); full `testStandardDebugUnitTest` + on-device verification at the end (hub chips On/Off contrast, one stepper dialog, Customize chips/rows, both context menus).
- Branch: `settings-sweep` off `main`.

## Risks

- **StepperDialog generalization:** the 7 sites differ in value formatting (%, ms, ±hours, minutes) and action sets (some have Reset, some only Done). The component takes the formatted value as a `String` and actions as slots/params — if a site genuinely doesn't fit, it keeps its local layout but adopts the value-color contract in place (decision recorded in the report, mirroring phase 1's EpisodeRow fallback).
- **Line drift:** SettingsScreen/CustomizeScreen/VideoPlayerSettingsScreen line numbers come from the audit sweep (files untouched since); LiveScreen/SeriesScreen menu line numbers come from the phase-1 final review (post-adoption). Implementers verify context before editing.
- **TileTone.SECONDARY dependents:** only QuickToggleChip's usage changes; other TileTone.SECONDARY consumers (icon tiles) are untouched.
