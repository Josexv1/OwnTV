# Onboarding Redesign — Design Spec

**Date:** 2026-08-12
**Status:** approved design, pending implementation plan
**Goal:** Bring the whole first-run onboarding flow up to the design standard set by the makeover and the welcome-screen refresh, and extract the shared structure into a correct, reusable Compose scaffold that the rest of the app will later adopt.

## Why

The design makeover (Figtree, charcoal, white-ring focus, "accent = active/selected/focused only") and the welcome-screen refresh established a clear visual language, but only the welcome screen was brought into compliance. The remaining onboarding screens drifted: they float small in the frame (a shared `0.62` pixel-scale), carry a decorative accent dash that informs nothing, and re-implement the same skeleton inline so no two are guaranteed consistent. Only 4 of 11 screens even share the current `MainSetupPage`; the rest roll their own layout.

This is also the first step of a larger codebase overhaul: the scaffold and patterns established here are meant to be **correct and idiomatic** so they can be carried into the rest of the app, not a local shortcut.

## Scope

**In (all adopt `SetupScaffold`):** welcome, disclaimer, setup-choice, add-content, add-source chooser, import-backup chooser, existing-sources, import-backup, import-progress, remote-setup, manual source form (`AddSourceScreen`).
**Component work:** create `SetupScaffold`; redesign `ChoiceCard`; delete `SetupAccentRule` and `MAIN_SETUP_CONTENT_SCALE`.
**Out (deferred to the later app-wide overhaul — shared with other features/dialogs):** `ProfileEditorDialog` (create-profile step, shared with Profiles), `RemoteBackupRestoreScreen` (shared with Settings), `EpgSyncDialog`.
**Copy:** layout/visual only. Every existing translated string is reused **verbatim**; no string keys are added, changed, or removed, so none of the 24 translations desync.

## Section 1 — The shared scaffold (`SetupScaffold`)

A single stateless composable in a new file `features/setup/SetupScaffold.kt`, replacing `MainSetupPage`. Every onboarding screen is built from it, so the compliant layout is enforced by construction.

### API (idiomatic slot API with provided defaults)

```kotlin
@Composable
fun SetupScaffold(
    title: @Composable () -> Unit,          // interior pages: the hero text title; welcome: the wordmark lockup itself
    subtitle: (@Composable () -> Unit)? = null,
    showLogoBadge: Boolean = true,          // welcome passes false — its wordmark IS the brand, so no separate badge
    content: @Composable ColumnScope.() -> Unit,
)
```

There is no separate "hero" flag: welcome simply passes its `BrandLockup` wordmark as the `title` slot and `showLogoBadge = false`, so its wordmark leads and no small badge duplicates it. Interior pages pass a text title and keep the default badge.

- Follows the androidx pattern (`Scaffold`, `TopAppBar`): `title`/`subtitle` are `@Composable` **slots**, not `String`. The scaffold provides the canonical style around each slot via the tv-material3 text-style CompositionLocal (`LocalTextStyle` + `ProvideTextStyle`-equivalent; bind the exact tv-material3 symbol at implementation — it mirrors material3) plus the default content color, so a caller writing bare `title = { Text(stringResource(R.string.setup_before_you_start)) }` gets the hero style with **no `style =` at the call site**. Rare overrides are still possible. This is more drift-proof than `String` params because the default lives in one place and call sites carry no styling.
- `content` is a `ColumnScope` slot so screens keep `Modifier.weight`, `Arrangement`, etc.
- Stateless: holds no state. Each screen keeps its own `FocusRequester` / `LaunchedEffect(Unit)` auto-focus. State is hoisted to the screens; data flows down, scaffold renders.

### What the scaffold owns (one place)

- The **ambient backdrop** (the concentric rings + glow), kept inside a safe frame so it does not clip the screen edges.
- A small **OwnTV logo badge** at the top (badge, not hero — see §2), shown when `showLogoBadge = true`; welcome sets it false so its wordmark title is the only brand mark.
- The **title** slot (hero style) and optional **subtitle** slot (secondary style, width-capped for readability).
- The **content slot** for the screen's controls.
- One set of **vertical-rhythm constants** (logo→title→subtitle→content), so every screen breathes identically.
- **`verticalScroll`** so any screen that overflows scrolls rather than clipping.

### Sizing — true dp/sp, no pixel-scale

The current `graphicsLayer { scaleX/scaleY = 0.62 }` scales rendered pixels (softening text, no reflow) and is the wrong thing to propagate app-wide. `SetupScaffold` sizes everything in **dp/sp via the type scale and `Dimens` at 1.0**:
- Sparse pages (disclaimer, choice) read large because the *type* is large, not because a layer was blown up; they simply carry more whitespace.
- The content-dense manual source form **scrolls at full size** instead of being pixel-shrunk. (Behavior change from today's shrink-to-fit; accepted: legible-and-scrolling beats cramped.)
- `MAIN_SETUP_CONTENT_SCALE` and the `contentScale`/`SetupDensity` parameterization are removed entirely — there is no scale to parameterize once sizing is honest.

## Section 2 — Visual language (applied through the scaffold)

**Hierarchy.** On interior pages the **page title is the hero**; the logo is a small consistent badge above it. Welcome is the exception: it passes its wordmark as the title with `showLogoBadge = false`, so the wordmark is the hero. Whatever a screen is *about* leads.

**Accent — two tiers, matching the makeover's actual usage (not a stricter local rule):**
- **Solid `primary` fill = focus / active / selected, and nothing else** — exactly one per state. The auto-focused primary action shows accent fill + white ring; non-focused actions are `SECONDARY` (tonal at rest, lifting to accent only on focus).
- **`primaryContainer` tonal = a sanctioned standing affordance** (icon tiles), consistent with the Settings tiles the makeover kept. `ChoiceCard` icon tiles **stay tonal**.
- **The white ring is the sole focus signal.** No color-shifting a title/label to signal focus.

**`ChoiceCard` redesign (narrow, correct):** keep the tonal `primaryContainer` icon tile; **remove the `focused → primary` title recolor** (redundant with the ring). Title stays `onSurface` in all states; the ring signals focus. Card at rest reads fully neutral except its tonal icon tile.

**Delete `SetupAccentRule`** everywhere — a decorative dash that encodes nothing (violates "structure is information" and "accent = active only").

**Icon semantics.** Chevron for "proceed / next", the appropriate back affordance for Back; the play triangle is reserved for actual playback, never "continue."

**Spacing & rhythm** come from the scaffold's constants — one rhythm shared by every screen.

**Focus.** One auto-focused target per screen (primary/default action or first card); white ring as the signal — already the makeover default, made uniform here.

## Section 3 — Screen inventory & sequencing

Two execution stages so correctness is proven before breadth.

### Stage A — scaffold + component + straightforward screens
Establishes and validates the pattern on-device before it propagates.
- `SetupScaffold.kt` (new backbone); `ChoiceCard` redesign; delete `SetupAccentRule` + `MAIN_SETUP_CONTENT_SCALE`.
- Convert: **Welcome** (pass wordmark as title, showLogoBadge=false, drop 0.92 scale), **Disclaimer**, **Setup choice**, **Add content**, **Add-source chooser** (`AddSourceChooserScreen.kt`), **Import-backup chooser**.
- These are all `scaffold(title, subtitle) { cards | buttons }`-shaped.

### Stage B — custom-layout screens
Each adopts the scaffold frame and keeps its own body.
- **Existing sources** (selectable list)
- **Import backup**
- **Import progress** (spinner/progress state)
- **Remote setup** (QR + PIN pairing — scaffold frames it, QR content stays)
- **Manual source form** (`AddSourceScreen.kt`) — the dense exception: scaffold header, form in the content slot, scrolls at full size.

## Constraints & invariants

- **i18n:** no `res/values*` changes; all copy reused verbatim; string resolution stays in Compose. RTL: the scaffold and screens must be start/end-aware; verify RTL in the on-device pass.
- **D-pad first:** no change to focus order or focusability intent; auto-focus target per screen preserved.
- **Behavior:** onboarding step flow, navigation, and the ViewModel are unchanged — this is composition/visual only. The one accepted behavior change is the dense source form scrolling at full size instead of shrinking.
- **Lint stays green** (`lintStandardDebug`, `PluralsCandidate` fatal); unit tests keep passing.
- **Verification:** each stage ends with a debug build on the Android TV emulator, `adb screencap` review of every touched screen, and a D-pad walkthrough of the full onboarding flow.

## Risks

- **tv-material3 text-style local:** confirm the exact symbol (`LocalTextStyle` / `ProvideTextStyle` equivalent) exists in the pinned tv-material version; fall back to `CompositionLocalProvider(LocalTextStyle provides …)` if the `ProvideTextStyle` helper is absent. Verified at implementation before the scaffold is written.
- **Scroll on TV:** the dense form scrolling with a D-pad must keep the focused field in view (Compose brings focused nodes into view by default; verify on-device).
- **Shared `ChoiceCard`:** used by 3 screens — the redesign must be verified on all three.
- **Welcome regression:** folding the just-shipped welcome screen onto the scaffold must not lose its refinements (single-line language label, focus ladder, chevron, ambient rings). Re-verify welcome specifically after the fold.
