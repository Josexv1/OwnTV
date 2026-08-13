# Design-Compliance Audit Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.
>
> **Execution note:** This is an audit, not a build. On-device walkthrough steps are **controller-executed** (they need the emulator session and vision judgment of screenshots). Code-sweep steps are **subagent-dispatched** (one per area, read-only). There is no per-task code review; the synthesis task (Task 10) is the quality gate — it rejects any scorecard whose findings lack file:line or screenshot evidence.

**Goal:** Produce a screenshot- and code-evidence-backed design-compliance audit of the eight main app areas, ending in a ranked overhaul roadmap.

**Architecture:** One-time test-source setup populates the emulator with public demo content; then per area, a controller D-pad walkthrough (screenshots) runs alongside a read-only subagent code sweep; both feed a per-area scorecard file; a final synthesis task merges scorecards into the canonical report + roadmap, commits it, and publishes an artifact copy.

**Tech Stack:** adb (emulator-5554), `python3 -m http.server`, read-only Kotlin/Compose inspection (codegraph/grep), markdown report, Artifact publish.

## Global Constraints

- **No app code changes.** Bugs found are logged in the report, never fixed here. (Spec: "Evaluation only.")
- Device: `emulator-5554` (arm64, `standard` flavor APK already installed from merged `main`), English locale, dark theme.
- Public demo streams only; no real provider credentials.
- Six audit axes per area, verdict per axis: **compliant / drifted / non-compliant**, each backed by file:line or a screenshot path.
- The report states what was NOT covered rather than silently skipping.
- Evidence root: `<scratchpad>/audit/` (`<scratchpad>` = the session scratchpad dir). Scorecards: `<scratchpad>/audit/scorecard-<area>.md`. Screenshots: `<scratchpad>/audit/<area>/NN-<state>.png`.
- Canonical report: `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` (committed, `[skip ci]`).

### The six axes (copy verbatim into every sweep dispatch)

- (a) Typography/hierarchy: `OwnTVTypography` scale only; clear hero per screen; no ad-hoc `TextStyle`/`fontSize` literals
- (b) Accent discipline: solid `primary` = focus/active/selected only; tonal `primaryContainer` only for sanctioned icon tiles; no static accent emphasis on text
- (c) Focus: white ring sole focus signal; one sane auto-focus target; no focus-triggered recolor of titles/labels
- (d) Structure: shared components over copy-pasted skeletons; one-off layouts justified or named as extraction candidates
- (e) Sizing honesty: dp/sp only; no `graphicsLayer` pixel-scale; no shrink-to-fit hacks
- (f) 10-foot legibility: readable at couch distance in 1080p screenshots

### Scorecard template (every area task writes this shape)

```markdown
# Scorecard: <Area>
Packages: <paths>   Screens judged: <list>
| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | compliant/drifted/non-compliant | file:line / png |
| (b) Accent | … | … |
| (c) Focus | … | … |
| (d) Structure | … | … |
| (e) Sizing | … | … |
| (f) Legibility | … | … |
## Top findings (max 5, severity-ordered)
1. <finding> — <evidence>
## Extraction candidates
- <component name>: <which screens would share it>
## Not covered
- <state and why>
```

---

### Task 1: Test source setup (controller)

**Files:** Create `<scratchpad>/audit/playlist.m3u`. No repo files.

**Interfaces — Produces:** a synced source on the emulator with ≥4 live channels, ≥3 movies, ≥1 series (2 seasons); HTTP server on host port 8000 (emulator reaches it at `http://10.0.2.2:8000`).

- [ ] **Step 1: Author the playlist.** Write `<scratchpad>/audit/playlist.m3u` exactly:

```m3u
#EXTM3U
#EXTINF:-1 tvg-id="mux" tvg-chno="1" group-title="Demo Live",Mux Test Stream
https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
#EXTINF:-1 tvg-id="bipbop" tvg-chno="2" group-title="Demo Live",Apple BipBop
https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8
#EXTINF:-1 tvg-id="tears" tvg-chno="3" group-title="Demo Live",Tears of Steel HLS
https://demo.unified-streaming.com/k8s/features/stable/video/tears-of-steel/tears-of-steel.ism/.m3u8
#EXTINF:-1 tvg-id="sintel" tvg-chno="4" group-title="Demo Live 2",Sintel HLS
https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8
#EXTINF:-1 type="movie" group-title="Demo Movies",Big Buck Bunny
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4
#EXTINF:-1 type="movie" group-title="Demo Movies",Elephants Dream
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ElephantsDream.mp4
#EXTINF:-1 type="movie" group-title="Demo Movies",Sintel
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/Sintel.mp4
#EXTINF:-1 type="series" group-title="Demo Series",Demo Show S01E01
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerBlazes.mp4
#EXTINF:-1 type="series" group-title="Demo Series",Demo Show S01E02
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerEscapes.mp4
#EXTINF:-1 type="series" group-title="Demo Series",Demo Show S02E01
https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/ForBiggerFun.mp4
```

(Classification verified against source: `M3uEntry.isVod`/`isSeries` key off `type="movie"`/`"series"` — `core/parser/M3uParser.kt:34-39`; series lines named `Show SxxExx` are grouped by show into seasons/episodes — `core/sync/M3uSyncer.kt:251-254`.)

- [ ] **Step 2: Serve it.** `cd <scratchpad>/audit && python3 -m http.server 8000` as a background Bash task. Verify from the emulator: `adb -s emulator-5554 shell "curl -s http://10.0.2.2:8000/playlist.m3u | head -3"` (or `wget -qO-`) shows `#EXTM3U`.
- [ ] **Step 3: Add the source on-device.** On the emulator (app already at post-onboarding or Settings): navigate to the manual add-source form (onboarding: New → Manual; or Settings → Manage sources → Add), select the M3U/M3U8 tab, and enter URL `http://10.0.2.2:8000/playlist.m3u` via the D-pad/IME sequence (CENTER on field → wait `mInputShown=true` → `input text` → BACK). Name: `DemoAudit`. Start import.
- [ ] **Step 4: Verify sync.** Wait for the import-progress screen to finish; then confirm via UI that Live shows 4 channels in 2 groups, Movies shows 3, Series shows "Demo Show" with 2 seasons. Screenshot the finished import screen to `<scratchpad>/audit/setup/import-done.png`. If a demo URL is down, swap in the fallback (`https://cph-p2p-msl.akamaized.net/hls/live/2000341/test/master.m3u8`) and re-sync; note the swap in the scorecard's "Not covered" if playback for that slot stays broken.

### Task 2: Shell / Home audit

**Packages:** `features/shell/`, `features/home/`. **Writes:** `<scratchpad>/audit/scorecard-home.md`, screenshots in `<scratchpad>/audit/home/`.

- [ ] **Step 1 (controller walkthrough):** From app start with the synced source: capture (1) home at rest, (2) top-bar/nav focused, (3) a hero item focused, (4) a shelf row with a card focused, (5) any long-press/options dialog. Save as `home/01-rest.png` … `home/05-dialog.png`. Judge each against the six axes; note verdicts inline in the scorecard.
- [ ] **Step 2 (subagent code sweep):** Dispatch a read-only subagent: "Audit `app/src/main/java/tv/own/owntv/features/shell/` and `features/home/` against these six axes: [paste axes verbatim]. For each axis report compliant/drifted/non-compliant with file:line evidence. Specifically search for: `colors.primary` on Text outside focus/active/selected conditionals; `TextStyle(`/`fontSize =` literals outside `ui/theme`; `if (focused)`-style color switches on titles; `graphicsLayer` scale; duplicated header/scaffold skeletons; hardcoded `Alignment.Left/Right` or `left`/`right` padding. Name extraction candidates (the SetupScaffold treatment). Return the findings as the markdown scorecard body." Merge its output with Step 1 verdicts into `scorecard-home.md` (walkthrough wins on visual axes (c)/(f); sweep wins on (a)/(b)/(d)/(e) code claims; conflicts recorded as findings).
- [ ] **Step 3:** Confirm the scorecard has all six verdicts + evidence; list unreachable states under "Not covered".

### Task 3: Live TV audit

**Packages:** `features/live/`. **Writes:** `scorecard-live.md`, `<scratchpad>/audit/live/`.

- [ ] **Step 1 (controller):** Open Live. Capture: (1) channel list/grid at rest, (2) a channel focused, (3) preview playing (channel selected, mini preview), (4) promote to full-screen, (5) channel switch (zap) overlay, (6) any category/group rail focused. Save `live/01…06`. Judge axes.
- [ ] **Step 2 (subagent sweep):** Same dispatch shape as Task 2 Step 2, package `features/live/`. Note: this package is large (LiveViewModel ~2k lines) — instruct the subagent to sweep composables only (files with `@Composable`), not the ViewModel logic, except where a composable reads a color/style from it.
- [ ] **Step 3:** Merge into `scorecard-live.md`; six verdicts + evidence; "Not covered" for states needing catchup/EPG data.

### Task 4: Player HUD + mini-player audit

**Packages:** `player/` (HUD/surface composables: `PlayerHud.kt`, mini-player, track dialogs). **Writes:** `scorecard-player.md`, `<scratchpad>/audit/player/`.

- [ ] **Step 1 (controller):** Start VOD playback (Big Buck Bunny — mpv path) and live playback (Mux — ExoPlayer path). Capture: (1) HUD visible over video, (2) seek in progress, (3) track/subtitle dialog open, (4) mini-player active in shell, (5) HUD on the live path (live badge, zap UI). Save `player/01…05`. Judge axes — HUD legibility over video is the (f) test here.
- [ ] **Step 2 (subagent sweep):** Same dispatch shape, scope: `app/src/main/java/tv/own/owntv/player/` composables only (`PlayerHud.kt` and any `@Composable` file; exclude the engine classes). Add: "Flag any color used as a live/state badge and judge whether it's semantic state (allowed) or decoration (drift)."
- [ ] **Step 3:** Merge into `scorecard-player.md`.

### Task 5: Movies audit

**Packages:** `features/movies/`. **Writes:** `scorecard-movies.md`, `<scratchpad>/audit/movies/`.

- [ ] **Step 1 (controller):** Open Movies. Capture: (1) shelves/browse at rest, (2) a poster card focused, (3) the detail page of Big Buck Bunny, (4) detail-page action row focused (Play/Favorite/etc.), (5) any sort/filter dialog. Save `movies/01…05`. Judge axes.
- [ ] **Step 2 (subagent sweep):** Same dispatch shape, package `features/movies/`.
- [ ] **Step 3:** Merge into `scorecard-movies.md`.

### Task 6: Series audit

**Packages:** `features/series/`. **Writes:** `scorecard-series.md`, `<scratchpad>/audit/series/`.

- [ ] **Step 1 (controller):** Open Series → Demo Show. Capture: (1) series browse, (2) detail with season selector, (3) episode list with one focused, (4) season switch (S02), (5) episode options/download dialog if reachable. Save `series/01…05`. Judge axes.
- [ ] **Step 2 (subagent sweep):** Same dispatch shape, package `features/series/` (composables only; SeriesViewModel is ~1k+ lines, same rule as Live).
- [ ] **Step 3:** Merge into `scorecard-series.md`.

### Task 7: Search audit

**Packages:** `features/search/`. **Writes:** `scorecard-search.md`, `<scratchpad>/audit/search/`.

- [ ] **Step 1 (controller):** Open Search. Capture: (1) idle state, (2) keyboard/input active, (3) results for "demo" (matches channels + series), (4) a result focused, (5) empty-result state for "zzzz". Save `search/01…05`. Judge axes.
- [ ] **Step 2 (subagent sweep):** Same dispatch shape, package `features/search/`.
- [ ] **Step 3:** Merge into `scorecard-search.md`.

### Task 8: Settings audit

**Packages:** `features/settings/`. **Writes:** `scorecard-settings.md`, `<scratchpad>/audit/settings/`.

- [ ] **Step 1 (controller):** Open Settings. Capture: (1) hub with a tile focused, (2) Manage sources (host of the embedded screens — verify no onboarding chrome leaks: no rings, no badge), (3) Appearance/Theme screen, (4) Playback settings, (5) one deep screen (e.g. Customize categories). Save `settings/01…05`. Judge axes.
- [ ] **Step 2 (subagent sweep):** Same dispatch shape, package `features/settings/` — but scope to the hub + the five representative screens captured in Step 1 (the package is the app's largest; full-package sweep is Task 10's roadmap concern, not this scorecard's). Add: "Confirm `ManageSourcesScreen` hosts `AddSourceChooserScreen`/`RemoteSetupScreen`/`AddSourceScreen` with `embedded = true` and that no onboarding chrome renders in Settings."
- [ ] **Step 3:** Merge into `scorecard-settings.md`.

### Task 9: Quick passes — Downloads, EPG guide, Profiles

**Packages:** `features/downloads/`, `features/epg/`, `features/profiles/`. **Writes:** `scorecard-quickpass.md`, `<scratchpad>/audit/quickpass/`.

- [ ] **Step 1 (controller):** Capture what's reachable without long-running data: (1) Downloads screen (start one episode download from Series if quick, else empty state), (2) EPG guide with the demo source (likely sparse — that's fine, judge the frame), (3) Profiles switcher + profile editor dialog. Save `quickpass/01…03`. Judge axes per screen, briefly.
- [ ] **Step 2 (subagent sweep):** ONE subagent, all three packages, same dispatch shape, instruction: "Brief pass: top 3 findings per package max, plus any extraction candidate; full scorecard table only where a package clearly drifts."
- [ ] **Step 3:** Merge into `scorecard-quickpass.md` (one file, three short sections).

### Task 10: Synthesis — report, roadmap, artifact

**Files:** Create `docs/superpowers/reports/2026-08-12-design-compliance-audit.md`. **Consumes:** all 8 scorecards + screenshots.

- [ ] **Step 1: Gate the scorecards.** Reject any scorecard verdict lacking file:line or screenshot evidence — re-run that area's missing half before proceeding.
- [ ] **Step 2: Write the report** with this structure: (1) Executive summary — one paragraph + a one-line verdict per area; (2) the eight scorecards verbatim; (3) **Cross-cutting findings** — patterns seen in ≥2 areas (these are the real overhaul targets); (4) **Extraction candidates** consolidated — proposed shared components with the screens each would serve; (5) **Roadmap** — areas ranked by drift × user exposure (exposure order: Home/Shell > Live > Player > Movies/Series > Settings > Search > quick-pass areas), a recommended phase-1 target, and the shared components phase 1 should establish; (6) **Not covered** — merged from all scorecards.
- [ ] **Step 3: Commit.** `git add docs/superpowers/reports/2026-08-12-design-compliance-audit.md && git commit -m "docs: design-compliance audit of the main app [skip ci]"` (with the repo's standard trailers).
- [ ] **Step 4: Publish the artifact.** Load the `artifact-design` skill, then build an HTML version of the report (scorecards as tables, key screenshots embedded as compressed data URIs — downscale to ≤960px wide, total page ≤16 MB) and publish it as a private artifact. Report the URL.
- [ ] **Step 5: Stop the HTTP server** (kill the background task) and note in the final message that the demo source `DemoAudit` is still on the emulator (useful for the overhaul phases; removable via Settings → Manage sources).

## Self-Review

1. **Spec coverage:** setup (Task 1 = spec Method/setup), eight areas (Tasks 2–9 = spec Scope 1–8), scorecard shape (Global Constraints = spec Criteria), report/roadmap/artifact (Task 10 = spec Output), no-code-change + not-covered rules (Global Constraints = spec Constraints), demo-stream fallback + series-format verification (Task 1 = spec Risks). No gaps.
2. **Placeholder scan:** none — playlist content, URLs, dispatch shapes, scorecard template, and report structure are all concrete.
3. **Type consistency:** scorecard filenames (`scorecard-home/live/player/movies/series/search/settings/quickpass.md`) and evidence dirs match between area tasks and Task 10. Axes text defined once in Global Constraints and referenced verbatim.
