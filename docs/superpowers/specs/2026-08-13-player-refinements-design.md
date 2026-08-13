# Phase 4 — Player Refinements — Design Spec

**Date:** 2026-08-13
**Status:** approved design, pending implementation plan
**Parent:** `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` (roadmap phase 4; site inventory from the player sweep + on-device HUD captures)
**Goal:** Close the player's audit findings — the one real legibility risk (unscrimmed text over video), the timeline duplication, the remaining focus recolors and accent-as-decoration sites — completing the audit's player inventory.

## Binding context

The HUD **deliberately opts out of glass** (`PlayerHud.kt` ~:422: `CompositionLocalProvider(LocalActionSurface provides null)` — "the player sits over opaque video, never a glass surface"). All scrim work must use plain dark backdrops matching the existing OSD cards, never glass.

## Scope

**In:**
1. `hudTextScrim` — private modifier/helper in `player/`: rounded `Color.Black.copy(alpha ≈ 0.45f)` backdrop + padding matching the existing OSD-card look. Applied to (a) the center error/status text block (`PlayerHud.kt:634-660` region) and (b) the top title/meta row (channel/title + engine·resolution·fps·bitrate line).
2. `TimelineBar` unification — merge `SeekBar` (`PlayerHud.kt:1179`) and `LiveTimelineBar` (`:1232`) into one component if their differences parameterize cleanly (VOD: position/duration/seek — live: timeshift offset, live-edge marker, go-to-live). **Pre-authorized fallback:** if live semantics genuinely diverge, keep both but extract the shared track/fill/thumb drawing primitives; record the decision.
3. Focus-recolor deletions: `OptionRow` (`PlayerHud.kt:1575`) and `AudioNowPlayingBar.kt:345` — content color no longer focus-dependent; the existing focus treatment is the signal.
4. Count badges neutral: the audio/subtitle track-count bubbles on the bottom-strip icons switch from accent fill to `surfaceContainerHigh` bg + `onSurface` text (counts are information, not state).
5. `StreamInfoOverlay.kt:62` heading: static `colors.primary` → `onSurface`.
6. `SubtitleOverlay.kt:88-94` ruling: subtitle text is user-styled rendering (size/color/background settings). If the TextStyle derives from the user's subtitle-style settings → **sanctioned** (same ruling as phase 2's SubtitlePreview) and gets a documenting comment; only hardcoded literals get tokenized to the type scale. Implementer verifies in situ; decision recorded.
7. Tint-ternary dedup: the icon-tint ternary triplicated at `PlayerHud.kt:1063/:1145-1146/:1164` collapses into one small private helper (name and exact shape lifted from the real code).

**Out:** zap/tune OSD cards (coherent already, not findings); playback engines/logic; the red LIVE badge and buffering spinner (sanctioned semantic state); mini-player sizing; any `res/values*` change.

## Color/keep contract (exhaustive for touched files)

| Site | Today | After |
|---|---|---|
| `PlayerHud.kt:1575` OptionRow focus recolor | accent on focus | focus-independent content color |
| `AudioNowPlayingBar.kt:345` focus recolor | accent on focus | focus-independent |
| Track-count badges (subtitle/audio icons) | accent fill | `surfaceContainerHigh` + `onSurface` |
| `StreamInfoOverlay.kt:62` heading | static `primary` | `onSurface` |

**KEPT (semantic, must survive):** red LIVE badge, accent buffering spinner, white-filled focused transport control, favorite star's teal fill when favorited (`favorite = true` state — accent marks state, sanctioned), engine badges, EPG progress fills in the live guide card.

## Constraints & invariants

- **Behavior unchanged:** seek/scrub/timeshift/go-to-live semantics, zap keys, dialog focus flow, auto-hide timing, direct-tune. Composition/visual only.
- **i18n:** zero `res/values*` changes.
- **Git hygiene:** explicit-path staging only (user's uncommitted gradle files in the working tree).
- **Gates:** compile + lint per task (0 errors); full unit suite + on-device verification at the end.
- **Verification:** emulator, Audit profile (DemoAudit source): VOD playback (Big Buck Bunny) + live playback (BipBop/Mux) — scrim behind center text (error state if a demo URL obliges, else the buffering/status text) and top meta row over bright video; both timeline modes render and seek/scrub correctly; count badges neutral; OptionRow dialog rows don't recolor on focus.
- Branch: `player-refinements` off `main`.

## Risks

- **TimelineBar merge** is the riskiest piece — seek thumb focus handling and live-edge semantics differ; the fallback exists precisely for this. D-pad seek behavior must be exercised on-device in both modes.
- **Line drift:** player files untouched by phases 0-3 EXCEPT none — audit lines hold, but the standard verify-context-first rule applies.
- **Scrim over-darkening:** alpha tuned to match the existing OSD cards; verify legibility over bright video AND that it doesn't create a "dialog" look on the transient status text.
