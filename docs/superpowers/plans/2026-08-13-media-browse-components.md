# Phase-1 Media-Browse Components Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract `MediaListRow`, `MediaContextMenu`, and `CategoryHeader` into `ui/components/` and adopt them across Live/Movies/Series, deleting the focus-recolor and static-accent sites they replace, plus the Series back-chevron fix.

**Architecture:** Three new stateless slot-API components (SetupScaffold idiom: provided text styles/content colors, no `style =` at call sites). Feature screens keep all state, focus logic, and policy; only the duplicated leaf skeletons move. Compliance deletions ride the adoption commits that replace them.

**Tech Stack:** Kotlin, Jetpack Compose for TV (`androidx.tv:tv-material3`), existing `ui/components/` (`FocusableSurface`, `OwnTVButton`, `dialogPanel`, `trapAllFocusExit`, `longPressMenuGuard`).

## Global Constraints

- **Behavior unchanged:** ViewModels, navigation, focus order/traversal, CH± paging, per-category scroll memory, and every menu action untouched. The context-menu focus-return contract (re-focus same item / nearest neighbor) lives in the screens and must keep working.
- **i18n:** no `res/values*` changes; components take resolved strings; `stringResource` stays in feature composables.
- **Accent rule:** solid `primary` = focus/active/selected only. Semantic state accents STAY: Live catchup chip (LiveScreen.kt:934 — commented as deliberate), "LIVE now" label (:971), EPG/episode progress bars (:984, :1015, :1422), selected-row `primaryContainer` (SeriesScreen.kt:1347). Only the sites named in tasks change.
- **Title color contract:** `MediaListRow` titles are always `colors.onSurface` — no focused-color parameter exists.
- **Dialog ruling:** in a modal dialog exactly one PRIMARY button = the single default action (Close); option rows SECONDARY.
- Gates per task: `./gradlew :app:compileStandardDebugKotlin` and `./gradlew lintStandardDebug` (0 errors; PluralsCandidate fatal). Final task adds `testStandardDebugUnitTest` + on-device verification.
- Branch: `media-browse-components` off `main`.

---

### Task 1: `MediaListRow` component

**Files:**
- Create: `app/src/main/java/tv/own/owntv/ui/components/MediaListRow.kt`

**Interfaces — Produces (Tasks 4-6 rely on this exact signature):**
```kotlin
@Composable
fun MediaListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    meta: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
)
```

- [ ] **Step 1: Write the component.** Full content:

```kotlin
package tv.own.owntv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.ProvideTextStyle
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * The shared browse list row (Live channels, Movies/Series list view, episode lists).
 *
 * Design contract (see docs/superpowers/specs/2026-08-12-media-browse-components-design.md §1):
 * the title is ALWAYS [OwnTVTheme.colors.onSurface] — the white focus ring from [FocusableSurface]
 * is the sole focus signal. There is deliberately no focused-color parameter. [selected] maps to
 * the sanctioned selected treatment (selection ≠ focus).
 */
@Composable
fun MediaListRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    meta: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    selected: Boolean = false,
) {
    val colors = OwnTVTheme.colors
    FocusableSurface(
        onClick = onClick,
        onLongClick = onLongClick,
        selected = selected,
        selectedContainerColor = colors.primaryContainer,
        modifier = modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            leading?.invoke()
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                meta?.let {
                    CompositionLocalProvider(LocalContentColor provides colors.onSurfaceVariant) {
                        ProvideTextStyle(MaterialTheme.typography.bodySmall) { it() }
                    }
                }
            }
            trailing?.invoke()
        }
    }
}
```

  **Before committing, verify `FocusableSurface`'s actual signature** (`ui/components/FocusableSurface.kt`): parameter names for `onLongClick`/`selected`/`selectedContainerColor` must match what it exposes (the audit confirmed `selected` + `selectedContainerColor` exist — SeriesScreen.kt:1347 passes them). If `FocusableSurface` has no `onLongClick`, check how `SeriesListRow`/`MovieListRow` wire long-press today (e.g. a `combinedClickable` or an `onLongClick` param) and mirror that exact mechanism. Adjust the component to the real API — the PUBLIC signature above must not change.

- [ ] **Step 2: Gate.** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` → green.
- [ ] **Step 3: Commit.** `git add app/src/main/java/tv/own/owntv/ui/components/MediaListRow.kt && git commit -m "Add MediaListRow: shared browse row with ring-only focus"`

### Task 2: `MediaContextMenu` component

**Files:**
- Create: `app/src/main/java/tv/own/owntv/ui/components/MediaContextMenu.kt`

**Interfaces — Produces (Tasks 4-5 rely on these exact names):**
```kotlin
data class MenuEntry(val label: String, val onClick: () -> Unit, val icon: OwnTVIcon? = null)
@Composable fun MediaContextMenu(title: String, entries: List<MenuEntry>, onDismiss: () -> Unit, closeLabel: String)
```

- [ ] **Step 1: Write the component.** Full content (behavior copied verbatim from `SeriesScreen.kt:151-215`'s frame):

```kotlin
package tv.own.owntv.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.OwnTVTheme

/** One option row in a [MediaContextMenu]. The label arrives resolved (stringResource at the caller). */
data class MenuEntry(
    val label: String,
    val onClick: () -> Unit,
    val icon: OwnTVIcon? = null,
)

/**
 * The shared long-press context menu (Movies + Series browse). Owns the frame — scrim, focus trap,
 * long-press guard, auto-focus, Back-dismiss, dialog panel — while callers own the policy (which
 * entries appear, in what order). Per the dialog ruling, entries render SECONDARY and the single
 * close action renders PRIMARY.
 */
@Composable
fun MediaContextMenu(
    title: String,
    entries: List<MenuEntry>,
    onDismiss: () -> Unit,
    closeLabel: String,
) {
    val colors = OwnTVTheme.colors
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
    BackHandler { onDismiss() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.7f))
            .trapAllFocusExit().focusGroup()
            .longPressMenuGuard(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.dialogPanel(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = colors.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(4.dp))
            entries.forEachIndexed { index, entry ->
                OwnTVButton(
                    entry.label,
                    onClick = entry.onClick,
                    style = OwnTVButtonStyle.SECONDARY,
                    icon = entry.icon,
                    modifier = if (index == 0) Modifier.fillMaxWidth().focusRequester(focus) else Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(4.dp))
            OwnTVButton(closeLabel, onClick = onDismiss, modifier = Modifier.fillMaxWidth())
        }
    }
}
```

  **Verify `OwnTVButton`'s icon parameter** accepts `OwnTVIcon?` or requires omission when null — if it is non-nullable, call it conditionally (`if (entry.icon != null) OwnTVButton(..., icon = entry.icon, ...) else OwnTVButton(..., ...)`) or match however the current menus pass icons (SeriesScreen.kt:188-197 passes `icon =` on some rows only).

- [ ] **Step 2: Gate.** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` → green.
- [ ] **Step 3: Commit.** `git add app/src/main/java/tv/own/owntv/ui/components/MediaContextMenu.kt && git commit -m "Add MediaContextMenu: shared long-press menu frame"`

### Task 3: `CategoryHeader` component

**Files:**
- Create: `app/src/main/java/tv/own/owntv/ui/components/CategoryHeader.kt`

**Interfaces — Produces:** `@Composable fun CategoryHeader(title: String, subtitle: String?, modifier: Modifier = Modifier)`

- [ ] **Step 1: Write the component.** Full content:

```kotlin
package tv.own.owntv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import tv.own.owntv.ui.theme.OwnTVTheme

/**
 * The browse middle-pane header: breadcrumb title + neutral count subtitle. One rhythm for
 * Live/Movies/Series; the subtitle is ALWAYS onSurfaceVariant (spec §3 — counts are information,
 * not state, so they never take accent).
 */
@Composable
fun CategoryHeader(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
) {
    val colors = OwnTVTheme.colors
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
```

- [ ] **Step 2: Gate + commit.** Green, then `git add app/src/main/java/tv/own/owntv/ui/components/CategoryHeader.kt && git commit -m "Add CategoryHeader: shared browse header with neutral count"`

### Task 4: Movies adoption + deletions

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/movies/MoviesScreen.kt` (`MovieListRow` :920-area, `MovieContextMenu` :688-area, middle-pane header, statics :833, :974, :981)

**Interfaces — Consumes:** `MediaListRow`, `MediaContextMenu`/`MenuEntry`, `CategoryHeader` (Tasks 1-3 signatures above).

- [ ] **Step 1: Read the four sites in full** (`MovieContextMenu`, `MovieListRow`, the middle-pane header block, `MovieDetailsPane` around :833) before editing.
- [ ] **Step 2: Replace `MovieContextMenu`'s body** with a call to `MediaContextMenu`: keep the function and its parameters (so the call site is untouched), build `List<MenuEntry>` inside it reproducing the exact same buttons, order, icons, and conditional gates as today, pass `closeLabel = stringResource(R.string.content_close)`. Delete the now-unused frame code.
- [ ] **Step 3: Replace `MovieListRow`'s skeleton** with `MediaListRow`: keep the function + parameters; map poster-thumb → `leading`, title string → `title`, secondary line(s) → `meta`, star/badges → `trailing`; wire `onClick`/`onLongClick`/`selected` identically. This deletes the `:960` focus recolor by construction. In `trailing`: the completed badge (`:974`) loses its `colors.primary` background → use `colors.surfaceContainerHigh` with `colors.onSurface` check icon; the favorite star (`:981`) `tint = colors.primary` → `tint = colors.favorite`.
- [ ] **Step 4: Header + statics.** Replace the middle-pane title+count block with `CategoryHeader(title = <same title string expr>, subtitle = <same count string expr>)`. Change `:833` (static `colors.primary` text in the details pane) → `colors.onSurfaceVariant`.
- [ ] **Step 5: Gate.** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` green; `git status --porcelain -- app/src/main/res` empty.
- [ ] **Step 6: Commit.** `git commit -am "Movies: adopt shared browse components; quiet static accents"`

### Task 5: Series adoption + deletions + back-icon fix

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/series/SeriesScreen.kt` (`SeriesContextMenu` :151-215, `SeriesListRow` :1465-area, `EpisodeRow` :1360-area, header, statics :839, :1481, chevron :1095)

**Interfaces — Consumes:** Tasks 1-3 signatures.

- [ ] **Step 1: Read the sites in full.**
- [ ] **Step 2: `SeriesContextMenu` → `MediaContextMenu`** exactly as Task 4 Step 2 (keep function + params; identical entries/order/icons/gates; `closeLabel = stringResource(R.string.content_close)`).
- [ ] **Step 3: `SeriesListRow` + `EpisodeRow` → `MediaListRow`.** Keep both functions + parameters. SeriesListRow: this deletes the `:1468` recolor; favorite star `:1481` `tint = colors.primary` → `tint = colors.favorite`. EpisodeRow: deletes the `:1401` recolor; the episode-number tile (`:1388`, tonal `primaryContainer` when completed) goes in `leading` unchanged (sanctioned tonal state); the `:1422` progress bar stays `colors.primary` (progress = state) inside `meta` or `trailing` as it maps naturally. If EpisodeRow's internals genuinely don't fit the slots (e.g. multi-line meta + progress underlay), keep its own layout but delete the `:1401` recolor in place and note the decision in the report — the color contract is the requirement, the skeleton reuse is the goal.
- [ ] **Step 4: Header, Next-up, chevron.** Middle-pane header → `CategoryHeader` (same strings). `:839` "Next up" label `colors.primary` → `colors.onSurfaceVariant` (its `primaryContainer.copy(alpha=.22f)` container at `:837` stays — tonal). `:1095` `icon = OwnTVIcon.CHEVRON` → `icon = OwnTVIcon.BACK`.
- [ ] **Step 5: Gate** (as Task 4 Step 5). **Step 6: Commit.** `git commit -am "Series: adopt shared browse components; fix back affordance"`

### Task 6: Live adoption + deletions

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/live/LiveScreen.kt` (`ChannelRow` :640-area, header/count :422, recolor :690; site :1255 evaluated)

**Interfaces — Consumes:** `MediaListRow`, `CategoryHeader`.

- [ ] **Step 1: Read `ChannelRow` (:640+) in full.** It carries chips (catchup accent :934 — KEEP, commented as deliberate), "LIVE now" label (:971 — KEEP, semantic), EPG progress (:984, :1015 — KEEP).
- [ ] **Step 2: Adopt or fall back (spec §Risks).** If `ChannelRow`'s structure maps onto `MediaListRow` slots (leading = number+logo, meta = EPG/chips line, trailing = badges) without behavior loss → adopt, which deletes the `:690` recolor by construction. If it does NOT map cleanly (e.g. the EPG progress underlay spans the full row), keep `ChannelRow`'s layout and delete the `:690` recolor in place (`color = colors.onSurface`, drop the `focused` ternary). Either way the recolor dies; state which path you took and why in the report.
- [ ] **Step 3: Header.** Replace the title+count block with `CategoryHeader`; `:422` accent count → gone by construction (subtitle is onSurfaceVariant).
- [ ] **Step 4: Site `:1255`.** Read its context: if that `colors.primary` text is NOT gated on focus/active/selected/live state → change to `colors.onSurfaceVariant`; if it IS a genuine state indicator → leave it. Record the call in the report either way.
- [ ] **Step 5: Gate** (as Task 4 Step 5). **Step 6: Commit.** `git commit -am "Live: shared browse header and ring-only channel rows"`

### Task 7: Full verification sweep

**Files:** none (fix-forward only).

- [ ] **Step 1: Suite.** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.
- [ ] **Step 2: On-device (controller).** Build `:app:assembleStandardDebug`, install on `emulator-5554` (DemoAudit source present). Verify with screenshots: (a) Live browse — neutral count subtitle, focused channel row = ring only, white title; (b) Movies → toggle LIST view via the Grid/List chip — focused row ring-only; long-press menu opens with the same entries as before (compare against audit capture `series/05-options.png` for the frame); (c) Series → list view rows + episode list — ring-only focus; "Next up" neutral; Back chip shows the BACK icon; long-press menu identical entries; (d) headers in all three areas share the same rhythm.
- [ ] **Step 3: Focus-return contract.** In Movies list view: long-press a row → Close → focus returns to the same row. Favorite a row from the menu on the Favorites category → focus lands on a surviving neighbor. Repeat once in Series.
- [ ] **Step 4: Fix findings, re-verify, commit each fix.** When clean, hand off to `superpowers:finishing-a-development-branch`.

## Self-Review

1. **Spec coverage:** §1→Task 1, §2→Task 2, §3→Task 3, §4 deletions table→Tasks 4-6 (each site named with line), §5 ruling→Task 2 (PRIMARY close) + Global Constraints, Risks (Live fallback, list-view reachability, menu parity)→Tasks 6 Step 2 / 7 Step 2 / 4-5 parity wording. No gaps.
2. **Placeholder scan:** none — component code is complete; adoption steps name exact sites and exact color changes; the two verify-at-implementation notes (FocusableSurface long-press mechanism, OwnTVButton nullable icon) name where to look and what must not change.
3. **Type consistency:** `MediaListRow`/`MenuEntry`/`MediaContextMenu`/`CategoryHeader` signatures identical between Produces blocks and consuming tasks; `closeLabel` naming consistent.
