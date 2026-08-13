# Phase 1 — Shared Media-Browse Components & Compliance Deletions — Design Spec

**Date:** 2026-08-12
**Status:** approved design, pending implementation plan
**Parent:** `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` (roadmap phase 1)
**Goal:** Extract the three genuinely duplicated leaf components of the Live/Movies/Series browse surfaces into `ui/components/`, adopt them across all three features, and delete every focus-recolor and static-accent site they replace — carrying the onboarding overhaul's standard into the app's highest-exposure surfaces.

## Why this shape (and not a full scaffold)

The audit's headline suggestion was a pane-owning `MediaBrowseScaffold`. Code reality: the pane level is **already shared** — `CategoryRail`, `PreviewPane`, `SearchBar`, `SortChip`, `PosterCard`, `roundedPanel`, and the panel-width system (`rememberPanelShares`/`computePanelWidths`) serve all three screens. What is actually duplicated is the leaf layer: list rows, context menus, and the middle-pane header — plus the compliance drift attached to them. The middle-pane *state* logic (CH± paging via `chNavPaging`, per-category scroll memory, three distinct focus-restoration dances) is behavior-heavy, subtly different per screen, and interleaved with ViewModel calls; extracting it is a D-pad-regression risk far beyond a design-compliance phase. It stays put, explicitly.

## Scope

**In:**
- Three new shared components in `ui/components/`: `MediaListRow`, `MediaContextMenu`, `CategoryHeader`.
- Adoption in `features/live/` (channel row, header), `features/movies/` (`MovieListRow`, `MovieContextMenu`, header), `features/series/` (`SeriesListRow`, `EpisodeRow`, `SeriesContextMenu`, header).
- Compliance deletions riding the adoption (see §4).
- The Series back-affordance fix (forward chevron → back icon).
- The dialog-PRIMARY ruling documented (§5).

**Out (explicitly):**
- Any pane-owning scaffold; any change to `chNavPaging`, per-category scroll state, focus-restoration logic, panel widths, `CategoryRail`, `PreviewPane`.
- Grid `PosterCard` (already compliant — grid titles stay white on focus).
- Other areas' recolor/accent sites (Home, Search, Settings, EPG, Player — later phases).
- Any `res/values*` change. Every string is reused verbatim.

## 1. `MediaListRow` (new: `ui/components/MediaListRow.kt`)

One list-row component for browse lists, built on `FocusableSurface`, slot API in the SetupScaffold idiom:

```kotlin
@Composable
fun MediaListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,          // callers attach focusRequester/gridFocusTarget etc.
    leading: (@Composable () -> Unit)? = null,   // channel number+logo, poster thumb, episode number
    meta: (@Composable () -> Unit)? = null,      // secondary line (provider, date, duration, progress)
    trailing: (@Composable () -> Unit)? = null,  // star, download state, progress ring
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,               // selection (not focus) may use the sanctioned selected treatment
)
```

- **Title is always `colors.onSurface`** (`titleMedium`, ellipsized single line). There is no focused-color parameter — the white ring from `FocusableSurface` is the focus signal, enforced by construction.
- `meta` content renders inside a `CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant)` + `ProvideTextStyle(bodySmall)` so call sites carry no styles.
- `selected` maps to `FocusableSurface`'s existing selected treatment (`primaryContainer`) — selection ≠ focus, and stays sanctioned.
- Replaces: `MoviesScreen.kt` `MovieListRow` (:920), `SeriesScreen.kt` `SeriesListRow` (:1465) and `EpisodeRow` (:1360), and the Live channel row in `features/live/`. Each caller keeps its own data mapping (what goes in the slots); only the skeleton and the color contract are shared.

## 2. `MediaContextMenu` (new: `ui/components/MediaContextMenu.kt`)

The long-press menu shared by Movies and Series (current twins: `MoviesScreen.kt:688`, `SeriesScreen.kt:151`):

```kotlin
data class MenuEntry(
    val label: String,                       // resolved by the caller via stringResource — component takes text
    val onClick: () -> Unit,
    val icon: OwnTVIcon? = null,
)

@Composable
fun MediaContextMenu(
    title: String,
    entries: List<MenuEntry>,
    onDismiss: () -> Unit,
    closeLabel: String,                      // caller passes stringResource(R.string.content_close)
)
```

- Preserves the existing behavior verbatim: full-screen scrim (`Color.Black.copy(alpha = .7f)`), `trapAllFocusExit().focusGroup().longPressMenuGuard()`, `BackHandler(onDismiss)`, auto-focus on the first entry, `dialogPanel()` column, 8dp spacing, `titleMedium` single-line title.
- Entries render as `OwnTVButton(style = SECONDARY, fillMaxWidth)`; the close button renders PRIMARY (§5 ruling).
- Callers build their own `List<MenuEntry>` with their existing conditional logic (favorite/move/history/TMDB gates) — the component owns the frame, not the policy. Both call sites must produce the exact same visible entries, in the same order, under the same conditions as today.

## 3. `CategoryHeader` (new: `ui/components/CategoryHeader.kt`)

```kotlin
@Composable
fun CategoryHeader(
    title: String,      // e.g. "Live TV / All Channels" breadcrumb, as each screen composes it today
    subtitle: String?,  // e.g. "All Channels (4 channels)" — same strings as today
)
```

- Title: `headlineMedium`, `colors.onSurface`. Subtitle: `bodyLarge`, **`colors.onSurfaceVariant`** — neutral, killing Live's accent count line while making all three headers identical in rhythm (the audit found Live accent / Movies neutral inconsistency).
- Adopted by the middle pane of all three screens; existing title/subtitle strings reused verbatim.

## 4. Compliance deletions (each must land with the adoption that replaces it)

| Site | Today | After |
|---|---|---|
| Live channel row title | `if (focused) primary else onSurface` | `MediaListRow` (always onSurface) |
| Movies `MovieListRow` title (:920 area) | focus recolor | `MediaListRow` |
| Series `SeriesListRow` (:1465) + `EpisodeRow` (:1397-1404) titles | focus recolor | `MediaListRow` |
| Live category count subtitle | static `primary` | `CategoryHeader` (onSurfaceVariant) |
| Series "Next up" label (SeriesScreen.kt:839) | static `primary` | `onSurfaceVariant` |
| Movies resume label / completed badge / favorite tint (per audit sweep) | static `primary` | neutral (`onSurfaceVariant`) or existing semantic token; favorite indicator uses the same treatment the audit found sanctioned elsewhere (`colors.favorite`) |
| Series Back chip icon (SeriesScreen.kt:1095) | forward `OwnTVIcon.CHEVRON` | the back affordance (`OwnTVIcon.BACK`; verify exact enum name at implementation — the player top bar already uses the correct left-arrow icon) |

No other color, layout, or behavior changes ride along.

## 5. Ruling: PRIMARY in modal dialogs

In a modal dialog, **exactly one button renders PRIMARY: the single default action** (typically Close/confirm); all option rows are SECONDARY. This matches today's context menus and the profile editor, and becomes the documented rule later phases apply app-wide (including onboarding's deferred PRIMARY-at-rest list). Non-modal screens keep the SECONDARY-at-rest / accent-on-focus convention.

## Constraints & invariants

- **Behavior unchanged:** ViewModels, navigation, focus order/traversal, CH± paging, scroll memory, and every menu action are untouched. The context-menu focus-return contract (re-focus same item, or nearest neighbor when removed) must keep working — it lives in the screens, not the component.
- **i18n:** no `res/values*` changes; components take resolved strings; string resolution stays in the feature composables via `stringResource`.
- **Lint + unit tests green** (`lintStandardDebug` with fatal PluralsCandidate; `testStandardDebugUnitTest`).
- **Verification:** emulator walkthrough with the existing `DemoAudit` source — list view in all three areas (rows: ring-only focus, no title recolor), long-press menus in Movies + Series (identical entries as before), headers in all three (neutral counts), Series back icon. Screenshot evidence per screen.

## Risks

- **Live channel row variance:** the Live row may carry extra states (playing indicator, catchup badge, EPG line) — the slot API must absorb them via `leading`/`meta`/`trailing` without behavior loss; if the Live row proves structurally incompatible, adopt `MediaListRow` in Movies/Series/Episodes only and fix Live's recolor in place (decision recorded in the plan, not silently).
- **List view reachability on-device:** Movies/Series default to grid view; verification must flip to list view (the Grid/List toggle chip) to exercise the rows.
- **Menu parity:** the two context menus have different entry sets (e.g. per-episode vs per-series download labels); parity is per-screen against its own current menu, not cross-screen.
