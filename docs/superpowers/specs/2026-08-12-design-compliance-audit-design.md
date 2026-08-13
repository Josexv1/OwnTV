# Design-Compliance Audit of the Main App — Design Spec

**Date:** 2026-08-12
**Status:** approved design, pending implementation plan
**Goal:** Evaluate every main post-onboarding surface of OwnTV against the design standard proved out by the makeover and the onboarding overhaul, and produce a ranked roadmap for carrying that standard app-wide. **Evaluation only — no app code changes in this project.**

## Why

The design makeover set the language (Figtree, charcoal, white-ring focus, "accent = active/selected/focused only") and the onboarding overhaul proved the structural pattern (one shared scaffold, slot API, provided text styles, true dp/sp sizing). The rest of the app predates both. Before overhauling it area by area, we need an honest picture: which screens drifted, how badly, and what shared components the fixes should crystallize into. This audit is that picture, and its roadmap decides the overhaul order.

## Scope — eight areas, in audit order

1. **Shell / Home** — top bar, nav, hero, content shelves (`features/shell`, `features/home`)
2. **Live TV** — channel browse (list/grid), zapping, preview → full-screen promote (`features/live`)
3. **Player HUD + mini-player** — playback controls, seek, track/subtitle dialogs (`player/`)
4. **Movies** — shelves + detail page (`features/movies`)
5. **Series** — shelves, detail, episode list (`features/series`)
6. **Search** — input, results (`features/search`)
7. **Settings** — hub + representative subscreens (`features/settings`)
8. **Quick passes:** Downloads, EPG guide, Profiles (`features/downloads`, `features/epg`, `features/profiles`)

Out of scope: onboarding (just done), companion HTML, notifications, launcher integration.

## Criteria — six axes per area

| Axis | Compliant means |
|---|---|
| (a) Typography / hierarchy | `OwnTVTypography` scale only; a clear hero per screen; no ad-hoc `TextStyle`/`fontSize` literals |
| (b) Accent discipline | solid `primary` = focus/active/selected only; tonal `primaryContainer` only for sanctioned icon tiles; no static accent emphasis on text |
| (c) Focus | white ring is the sole focus signal; one sane auto-focus target; no focus-triggered recoloring of titles/labels |
| (d) Structure | shared components over copy-pasted skeletons; each one-off layout is either justified or named as an extraction candidate (the SetupScaffold treatment) |
| (e) Sizing honesty | dp/sp only; no `graphicsLayer` pixel-scales; no shrink-to-fit hacks |
| (f) 10-foot legibility | readable from couch distance at 1080p; spacing and type sizes hold up in screenshots |

Verdict per axis: **compliant / drifted / non-compliant**, each backed by a file:line or screenshot.

## Method

**One-time setup (emulator only, public demo content):**
- Serve a small M3U over HTTP from the Mac (`python3 -m http.server` from a scratch dir).
- ~5 live channels pointing at public demo HLS (Mux test stream, Apple bipbop), plus a few VOD movie entries and a series-episode group (M3U `group-title` syntax) so home/movies/series shelves populate.
- Add the source on the emulator through the manual add-source form (which also smoke-tests the freshly redesigned dense form with a real source).

**Per area, two tracks feeding one scorecard:**
- **(i) On-device walkthrough (controller):** D-pad navigation through the area's key states — rest, focused, dialog open, playing where relevant — with `adb screencap` evidence for each judged state.
- **(ii) Code sweep (one subagent per area, parallel where possible):** read the feature package's composables for: `colors.primary` used as static emphasis; ad-hoc text styles; focus-recolor patterns; duplicated layout skeletons; pixel-scale hacks; hardcoded left/right (RTL hazards). Return findings as file:line + classification per axis.

**Scorecard per area:** six axis verdicts + top findings (max ~5, severity-ordered) + named shared-component extraction candidates.

## Output

One audit report:
- **Canonical:** `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` — all scorecards, findings, extraction candidates, and the roadmap. Committed to the repo.
- **Readable copy:** published as a private Artifact page with key evidence screenshots embedded (compressed; page ≤ 16 MB).
- **Roadmap:** areas ranked by **drift × user exposure** (a badly drifted screen nobody visits ranks below a mildly drifted screen everyone lives in), ending with a recommended phase-1 overhaul target and the shared components it should establish.

## Constraints

- **No app code changes.** If the walkthrough trips over an actual bug, log it in the report; do not fix it here.
- Emulator (`emulator-5554`, arm64, `standard` flavor) is the device; English locale; dark theme (the app's default).
- Public demo streams only; no real provider credentials anywhere.
- The report states what was NOT covered (e.g., states unreachable without long-running data) rather than silently skipping.

## Risks

- **Demo-stream fragility:** public HLS endpoints occasionally go down; keep 2 fallback URLs per slot. If live playback is impossible, HUD evaluation falls back to VOD playback (mpv path) and says so.
- **Emulator vs. real TV rendering:** screenshots are 1080p emulator frames; where a finding hinges on real-device rendering (e.g., overscan), mark it "verify on Fire TV" instead of asserting.
- **Series parsing:** M3U series grouping must match what `M3uParser` expects for the series shelf to populate; verify the parser's expected `group-title`/naming convention against its source before authoring the playlist.
