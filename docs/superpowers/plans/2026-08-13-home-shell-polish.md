# Phase-3 Home/Shell Polish Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Extract `HomeRowHeader` and `StaticGlassChip`, and delete the shell's remaining focus-recolor and static-accent sites per the audit's Home/shell inventory.

**Architecture:** Two local extractions (feature-internal `HomeRowHeader`; file-private `StaticGlassChip`) plus targeted color edits. No public `ui/components/` additions (no cross-feature consumers — YAGNI, per spec).

**Tech Stack:** Kotlin, Jetpack Compose for TV (tv-material3), existing `FocusableSurface`/glass modifiers.

## Global Constraints

- **Behavior unchanged:** focus order, auto-focus targets, shelf data flow, overlay open/close, chip content. Composition/visual only.
- **i18n:** zero `res/values*` changes; strings resolved where they are today.
- **Git hygiene:** stage edited files by explicit path only — the working tree carries the user's uncommitted gradle-wrapper changes; NEVER `git commit -am` or `git add -A`.
- **Color contract (exhaustive; nothing else changes color):** spec §3 table — three focus-recolors → always `onSurface`; overlay titles (`CategoryBrowserOverlay.kt:95`, `ChannelListOverlay.kt:102`) → `onSurface`; `MediaDetailsScreen.kt:169` genre → `onSurfaceVariant`; `HomeScreen.kt:905` hero stat → `onSurfaceVariant`. Semantic accents (EPG progress fills, live badges, selected states) STAY.
- Gates per task: `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` (0 errors; PluralsCandidate fatal); `git status --porcelain -- app/src/main/res` empty.
- Line numbers are from the audit sweep; the cited files are untouched since (verified against phases 1-2 commit lists) — still verify surrounding context before editing.
- Branch: `home-shell-polish` off `main`.

---

### Task 1: `HomeRowHeader` + Home recolor/static deletions

**Files:**
- Create: `app/src/main/java/tv/own/owntv/features/home/HomeRowHeader.kt`
- Modify: `app/src/main/java/tv/own/owntv/features/home/HomeScreen.kt` (:502-509, :727, :905, :1064-1073, :1216), `app/src/main/java/tv/own/owntv/features/home/HomeGuideSlice.kt` (:104-113, :149, :262-285)

**Interfaces — Produces:** `@Composable internal fun HomeRowHeader(title: String, modifier: Modifier = Modifier)`

- [ ] **Step 1 (commit A: extraction + adoption).** Read the three copy-pasted header blocks (`HeroRowSection` HomeScreen.kt:502-509 — the canonical version — `ContinueWatchingRow` :1064-1073, `ChannelCardsRow` HomeGuideSlice.kt:104-113). Lift the canonical block VERBATIM (its exact `Text(style = MaterialTheme.typography.titleMedium, color = colors.onSurface)`, spacer, and `padding(start = HomeRowPaddingH)` — whatever the real constants are) into the new file as `internal fun HomeRowHeader(title: String, modifier: Modifier = Modifier)`. Before consolidating, diff the three blocks: if any differs beyond the title string (padding, style), report the difference and preserve it via the `modifier` param rather than silently normalizing. Adopt in all three call sites. Fourth consumer: `OnNowRow`'s inline title Row (HomeGuideSlice.kt:262-285) — adopt it if its title carries no extra trailing content the header can't express; otherwise adopt only the three and record the decision in the report (pre-authorized fallback). Commit: `git add app/src/main/java/tv/own/owntv/features/home/HomeRowHeader.kt app/src/main/java/tv/own/owntv/features/home/HomeScreen.kt app/src/main/java/tv/own/owntv/features/home/HomeGuideSlice.kt && git commit -m "Home: shared HomeRowHeader for shelf titles"`
- [ ] **Step 2 (commit B: color deletions in the same two files).** `HomeScreen.kt:727` and `:1216`: `color = if (focused) colors.primary else colors.onSurface` → `color = colors.onSurface` (drop the ternary; if `focused` becomes unused in that lambda, rename to `_` only when genuinely unused elsewhere in the block). `HomeGuideSlice.kt:149`: same recolor deletion. `HomeScreen.kt:905` hero stat label: `colors.primary` → `colors.onSurfaceVariant`. Commit: `git add <the two files> && git commit -m "Home: ring-only focus for cards; neutral hero stat"`
- [ ] **Step 3: Gate** after each commit.

### Task 2: `StaticGlassChip` in TopBar

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt` (:116, :212, :225, :273 regions)

- [ ] **Step 1:** Read the four chip composables (`SectionChip`, `ClockChip`, `PlaylistChip`, `WeatherChip`). Extract the duplicated static shell — `Box/Row` with `clip(shape)`, the glass fill modifier chain, `topBarGlassRim(shape)`, `padding(horizontal = 14.dp, vertical = 7.dp)` (verify exact values in situ) — into `@Composable private fun StaticGlassChip(modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit)` at the top of the file, using whatever row/box arrangement the current chips share. Route the four static usages through it: `ClockChip`, `WeatherChip`, the NON-interactive branch of `PlaylistChip`, and `SectionChip` IF its usage is genuinely static (if `SectionChip` has focus/interactive behavior, leave its interactive path on `FocusableSurface` and share only the static shell where applicable — record what you found). Interactive chips (Search/Continue pills) untouched.
- [ ] **Step 2: Gate + commit.** `git add app/src/main/java/tv/own/owntv/features/shell/components/TopBar.kt && git commit -m "TopBar: shared StaticGlassChip shell for static chips"`

### Task 3: Shell overlay/detail statics

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/features/shell/components/CategoryBrowserOverlay.kt` (:95), `.../ChannelListOverlay.kt` (:102), `.../MediaDetailsScreen.kt` (:169) — verify exact paths with a grep for the file names under `features/shell/`.

- [ ] **Step 1:** `CategoryBrowserOverlay.kt:95` overlay title `colors.primary` → `colors.onSurface`. `ChannelListOverlay.kt:102` same. `MediaDetailsScreen.kt:169` genre line `colors.primary` → `colors.onSurfaceVariant`. Verify each site's context first (these are the sweep's static-emphasis findings; if a site turns out to be state-gated on inspection, leave it and report — none are expected to be).
- [ ] **Step 2: Gate + commit.** `git add <the three files> && git commit -m "Shell overlays: neutral headings and genre line"`

### Task 4: Verification sweep + finish

**Files:** none (fix-forward only).

- [ ] **Step 1: Suite.** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.
- [ ] **Step 2: On-device (controller).** Build + install `standard` debug on `emulator-5554`, Audit profile (has real history: a watched movie + last channel). Verify with screenshots: (a) POPULATED Home — hero/continue-watching rows render, shared header rhythm across shelves; (b) focus a hero card and a continue card — ring only, title stays white; (c) top-bar chips visually unchanged at rest (compare against audit capture `home/01-rest.png`); (d) open the channel-list or category-browser overlay (from Live fullscreen or the Home guide slice) — title reads `onSurface`, not accent; (e) MediaDetails genre line if reachable (TMDB details window from a movie's context menu). Fallback per spec: if hero/continue rows don't materialize, code-verify those rows and say so.
- [ ] **Step 3: Fix findings, re-verify, commit each fix.** When clean: final whole-branch review (most capable model), ONE fix wave if findings, then `superpowers:finishing-a-development-branch`.

## Self-Review

1. **Spec coverage:** §1→Task 1 commit A (incl. OnNowRow fallback), §2→Task 2 (incl. SectionChip interactive-path caveat), §3 table→Task 1 commit B + Task 3 (all seven rows covered), verification→Task 4 (populated-Home + fallback). No gaps.
2. **Placeholder scan:** clean — extraction steps name canonical sources and exact color edits; both fallbacks are bounded with report-recording.
3. **Type consistency:** `HomeRowHeader(title: String, modifier: Modifier = Modifier)` consistent between Produces and adoption steps; `StaticGlassChip` is file-private with the RowScope content slot named once.
