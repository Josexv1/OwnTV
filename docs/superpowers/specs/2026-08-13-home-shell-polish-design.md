# Phase 3 — Home/Shell Polish — Design Spec

**Date:** 2026-08-13
**Status:** approved design, pending implementation plan
**Parent:** `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` (roadmap phase 3; site inventory from the Home/shell sweep)
**Goal:** Extract the two duplicated Home/TopBar skeletons and delete the shell's remaining focus-recolor and static-accent sites — completing the audit's Home/shell findings.

## Scope

**In:**
1. `HomeRowHeader` — new `features/home/HomeRowHeader.kt` (feature-internal; two consumer files).
2. `StaticGlassChip` — private composable inside `features/shell/components/TopBar.kt` (all four consumers are in that file).
3. Focus-recolor deletions: `HomeScreen.kt:727` (hero-card title), `:1216` (`LandscapeContinuationCard` title), `HomeGuideSlice.kt:149` (`ChannelCardsRow` channel name).
4. Static-accent deletions: `CategoryBrowserOverlay.kt:95` and `ChannelListOverlay.kt:102` overlay titles → `onSurface`; `MediaDetailsScreen.kt:169` genre line → `onSurfaceVariant`; `HomeScreen.kt:905` hero stat label → `onSurfaceVariant`.

**Out:** promotion of either extraction to `ui/components/` (no cross-feature consumer exists — YAGNI); Home's data/ViewModel logic; the top-bar Search/Continue pill behavior (`graphicsLayer` alpha fades are sanctioned); semantic accents (progress fills, live indicators) anywhere in Home.

## 1. `HomeRowHeader`

```kotlin
@Composable
internal fun HomeRowHeader(title: String)
```
The exact `Text(style = titleMedium, color = onSurface)` + spacing + `padding(start = HomeRowPaddingH)` block currently copy-pasted at `HomeScreen.kt:502-509` (`HeroRowSection`), `HomeScreen.kt:1064-1073` (`ContinueWatchingRow`), `HomeGuideSlice.kt:104-113` (`ChannelCardsRow`). Adopted by those three plus `OnNowRow`'s inline title Row (`HomeGuideSlice.kt:262-285`) as the fourth consumer — if OnNowRow's title carries extra trailing content the shared header can't express, adopt the other three and record the decision (the fallback convention from phases 1-2).

## 2. `StaticGlassChip`

```kotlin
@Composable
private fun StaticGlassChip(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit)
```
Private to `TopBar.kt`; owns the duplicated `clip(shape) → glass(...) → topBarGlassRim(shape) → padding(14.dp, 7.dp)` wrapper. Consumers: `SectionChip` (:116), `ClockChip` (:212), the non-interactive branch of `PlaylistChip` (:225), `WeatherChip` (:273). Interactive chip branches (focusable ones) keep their existing `FocusableSurface`-based paths — only the static glass shell is shared.

## 3. Color contract changes (exhaustive)

| Site | Today | After |
|---|---|---|
| `HomeScreen.kt:727` hero-card title | `if (focused) primary else onSurface` | always `onSurface` |
| `HomeScreen.kt:1216` continuation-card title | same recolor | always `onSurface` |
| `HomeGuideSlice.kt:149` channel name | same recolor | always `onSurface` |
| `CategoryBrowserOverlay.kt:95` overlay title | static `primary` | `onSurface` (heading) |
| `ChannelListOverlay.kt:102` overlay title | static `primary` | `onSurface` (heading) |
| `MediaDetailsScreen.kt:169` genre line | static `primary` | `onSurfaceVariant` |
| `HomeScreen.kt:905` hero stat label | static `primary` | `onSurfaceVariant` |

Nothing else changes color. Semantic accents (EPG progress fills `HomeGuideSlice.kt:374+` region, live badges, `FocusableSurface` selected states) stay.

## Constraints & invariants

- **Behavior unchanged:** focus order, auto-focus targets, shelf data flow, overlay open/close, chip content. Composition/visual only.
- **i18n:** zero `res/values*` changes; strings resolved where they are today.
- **Git hygiene:** stage edited files by explicit path only (the working tree carries the user's uncommitted gradle-wrapper changes).
- **Gates:** compile + lint per task (0 errors, PluralsCandidate fatal); full unit suite + on-device verification at the end.
- **Verification (now possible with populated data):** the emulator's Audit profile has real playback history (movie + last channel) — verify the POPULATED Home: hero/continue-watching rows render with the shared header rhythm; card focus = ring only, titles stay white; top-bar chips visually unchanged at rest; one overlay (category browser or channel list) opened to confirm the neutral title; MediaDetails genre line neutral if reachable.
- Branch: `home-shell-polish` off `main`.

## Risks

- **Line drift:** all cited files are untouched since the audit (verified by phases 1-2 commit lists), but implementers verify surrounding context before editing.
- **OnNowRow fourth consumer:** may carry trailing content; pre-authorized fallback = adopt 3, record the decision.
- **Populated-Home reachability:** hero/continue rows depend on history retention; if the emulator rows don't materialize, fall back to code-level verification for those two rows and say so (the audit's original gap, narrowed).
