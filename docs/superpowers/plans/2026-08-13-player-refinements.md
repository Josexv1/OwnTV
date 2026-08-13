# Phase-4 Player Refinements Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Close the player's audit findings: text-over-video scrim protection, timeline dedup, the last player focus-recolors, neutral count badges, and the subtitle-style ruling.

**Architecture:** All work stays inside `player/`. A private `hudTextScrim` modifier supplies the (non-glass) text backdrop; a unified `TimelineBar` (or shared drawing primitives via the pre-authorized fallback) replaces the duplicated bars; the rest are targeted color edits and one small tint helper.

**Tech Stack:** Kotlin, Jetpack Compose for TV (tv-material3). The HUD opts out of glass (`LocalActionSurface provides null`, PlayerHud.kt ~:422) — scrims are plain dark backdrops, NEVER glass.

## Global Constraints

- **Behavior unchanged:** seek/scrub/timeshift/go-to-live semantics, zap keys, dialog focus flow, auto-hide timing, direct-tune. Composition/visual only.
- **i18n:** zero `res/values*` changes.
- **Git hygiene:** stage edited files by explicit path only; NEVER `git commit -am`/`git add -A` (the working tree carries the user's uncommitted gradle files).
- **Color/keep contract (exhaustive):** spec table — OptionRow (PlayerHud.kt:1575) + AudioNowPlayingBar.kt:345 lose focus-dependent color; track-count badges → `surfaceContainerHigh` bg + `onSurface` text; StreamInfoOverlay.kt:62 heading → `onSurface`. KEPT: red LIVE badge, accent buffering spinner, white-filled focused transport control, favorite star teal when `favorite = true`, engine badges, EPG progress fills.
- Gates per task: `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` (0 errors; PluralsCandidate fatal); `git status --porcelain -- app/src/main/res` empty.
- Audit line numbers hold (player files untouched by phases 0-3) — still verify surrounding context before editing.
- Branch: `player-refinements` off `main`.

---

### Task 1: PlayerHud color edits + tint helper

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/player/PlayerHud.kt` (:1575 OptionRow; count badges near the bottom-strip subtitle/audio buttons; tint ternary at :1063, :1145-1146, :1164)

- [ ] **Step 1:** Read `OptionRow` (~:1575) — remove the focus-dependent accent from its content color (the row's existing focus treatment — fill/ring from its surface — is the signal). If the accent arm ALSO encodes "selected option" (e.g. the active audio track), keep the SELECTED accent and remove only the FOCUSED arm — selected ≠ focused; record what the conditional actually encoded.
- [ ] **Step 2:** Find the track-count badge composable(s) used on the bottom-strip subtitle/audio icons (accent-filled count bubbles, visible in the audit HUD capture). Change fill → `colors.surfaceContainerHigh`, text/icon → `colors.onSurface`. Badge shape/size unchanged.
- [ ] **Step 3:** Read the three tint ternaries (:1063, :1145-1146, :1164). Extract one private helper with the exact shape the code shares (e.g. `@Composable private fun hudTint(active: Boolean): Color` or a non-composable `fun hudTint(active: Boolean, colors: OwnTVColors)` — match how the call sites access colors) and route all three through it. Pure dedup — resulting colors identical.
- [ ] **Step 4: Gate + commit.** `git add app/src/main/java/tv/own/owntv/player/PlayerHud.kt && git commit -m "PlayerHud: neutral count badges, focus-independent option rows, shared tint"`

### Task 2: Satellite files — AudioNowPlayingBar, StreamInfoOverlay, SubtitleOverlay

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/player/AudioNowPlayingBar.kt` (:345), `app/src/main/java/tv/own/owntv/player/StreamInfoOverlay.kt` (:62), `app/src/main/java/tv/own/owntv/player/SubtitleOverlay.kt` (:88-94)

- [ ] **Step 1:** `AudioNowPlayingBar.kt:345` — remove the focus-dependent recolor (same rule as Task 1 Step 1, including the selected-vs-focused distinction if present).
- [ ] **Step 2:** `StreamInfoOverlay.kt:62` — heading `colors.primary` → `colors.onSurface`.
- [ ] **Step 3:** `SubtitleOverlay.kt:88-94` ruling — read the TextStyle construction in situ. If it derives from the user's subtitle-style settings (size scale, colors, background from `SubtitleStyle`/settings) → SANCTIONED: leave the values, add a comment `// User-styled subtitle rendering — sizes/colors come from SubtitleStyle settings, not the type scale (sanctioned, mirrors SubtitlePreview ruling).` If any piece is a hardcoded literal unrelated to user settings → replace that piece with the corresponding `OwnTVTypography` token. Record exactly what you found and did.
- [ ] **Step 4: Gate + commit.** `git add <the three files> && git commit -m "Player satellites: neutral heading, focus-independent now-playing bar, subtitle style ruling"`

### Task 3: `hudTextScrim` + apply to the two unprotected text surfaces

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/player/PlayerHud.kt` (center error/status block :634-660 region; the top title/meta row)

**Interfaces — Produces:** `private fun Modifier.hudTextScrim(): Modifier` in PlayerHud.kt.

- [ ] **Step 1:** Add the modifier near the top of PlayerHud.kt's private helpers:
```kotlin
/** Rounded dark backdrop for HUD text drawn directly over video. Plain color, deliberately NOT
 *  glass — the player opts out of glass surfaces (see the LocalActionSurface provider above). */
private fun Modifier.hudTextScrim(): Modifier = this
    .clip(RoundedCornerShape(12.dp))
    .background(Color.Black.copy(alpha = 0.45f))
    .padding(horizontal = 16.dp, vertical = 10.dp)
```
(Verify `clip`/`RoundedCornerShape`/`background` imports exist; match the file's existing OSD-card corner radius if it differs from 12.dp — use the OSD card's actual value.)
- [ ] **Step 2:** Apply to the center error/status text block (:634-660 region): wrap the text column (NOT the transport buttons) so the scrim hugs the text. If the block already sits on a card in some states, apply only to the bare-text state.
- [ ] **Step 3:** Apply to the top title/meta row (channel/title + engine·res·fps·bitrate line at TopStart) — scrim behind the text stack, not the full-width strip.
- [ ] **Step 4: Gate + commit.** `git add app/src/main/java/tv/own/owntv/player/PlayerHud.kt && git commit -m "PlayerHud: scrim behind text drawn over video"`

### Task 4: TimelineBar unification

**Files:**
- Modify: `app/src/main/java/tv/own/owntv/player/PlayerHud.kt` (`SeekBar` :1179, `LiveTimelineBar` :1232, their call sites in `BottomBar`)

- [ ] **Step 1:** Read both bars in full plus their call sites. Decide: (a) MERGE into one `TimelineBar` if the differences are parameterizable (VOD: position/duration/onSeek — live: timeshiftOffsetSec, live-edge marker, onScrubLive/onGoToLive), or (b) pre-authorized FALLBACK: keep both composables but extract the shared drawing primitives (track, fill, thumb/handle, focus treatment) into private helpers both consume. Choose on the merits; record the decision and reasoning in the report.
- [ ] **Step 2:** Implement the chosen shape. Behavior must be pixel/behavior-identical in both modes: D-pad seek stepping, scrub hold, live-edge clamp, go-to-live, focus visuals.
- [ ] **Step 3: Gate + commit.** `git add app/src/main/java/tv/own/owntv/player/PlayerHud.kt && git commit -m "PlayerHud: unified timeline drawing for VOD and live"` (adjust message to "shared timeline primitives" if fallback chosen).

### Task 5: Verification sweep + finish

**Files:** none (fix-forward only).

- [ ] **Step 1: Suite.** `./gradlew testStandardDebugUnitTest lintStandardDebug` → green.
- [ ] **Step 2: On-device (controller).** Build + install `standard` on `emulator-5554` (Audit profile, DemoAudit source). Verify with screenshots: (a) VOD playback (Big Buck Bunny) — top meta row + any center status text carry the scrim over bright video; seek bar renders, D-pad seek steps work; (b) live playback (Apple BipBop) — live timeline (with timeshift affordances if shown), zap still works, count badges on subtitle/audio icons neutral; (c) open the audio/subtitle dialog — option rows don't recolor on focus (selected-track accent, if any, persists); (d) error state if a demo URL fails — scrimmed center text.
- [ ] **Step 3: Fix findings, re-verify, commit each fix.** When clean: final whole-branch review (most capable model), ONE fix wave if findings, then `superpowers:finishing-a-development-branch`.

## Self-Review

1. **Spec coverage:** spec §1→Task 3, §2→Task 4 (with fallback), §3→Tasks 1-2, §4→Task 1 Step 2, §5→Task 2 Step 2, §6→Task 2 Step 3, §7→Task 1 Step 3; keeps/constraints→Global Constraints; verification→Task 5. No gaps.
2. **Placeholder scan:** clean — scrim code given concretely with a verify-against-OSD-card instruction; all decisions bounded with report-recording.
3. **Type consistency:** `hudTextScrim` defined once, used in Task 3 only; `TimelineBar` naming resolved inside Task 4 with the commit message adjusted to match the chosen shape.
