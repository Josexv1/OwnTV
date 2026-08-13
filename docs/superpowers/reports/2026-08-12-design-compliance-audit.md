# Design-Compliance Audit — Main App Surfaces

**Date:** 2026-08-12 · **Method:** per-area D-pad walkthrough on a Google TV emulator (populated with a public-demo M3U source: 4 live / 3 movies / 1 series) + read-only code sweeps of each feature package. Spec: `docs/superpowers/specs/2026-08-12-design-compliance-audit-design.md`. Evidence screenshots live in the session scratchpad (`audit/`); every finding carries file:line or a screenshot reference.

## Executive summary

The app's foundations are in better shape than expected: typography is on the `OwnTVTypography` scale nearly everywhere, sizing is honest (zero pixel-scale hits outside the now-deleted setup scale), and the `FocusableSurface` ring model is correct at the source. The drift is concentrated in two systemic habits applied on top of those good foundations: **(1) recoloring titles/labels to accent on focus** (found in 7 of 8 areas — exactly the pattern the onboarding overhaul just removed from its cards), and **(2) static accent used as emphasis/decoration** (worst in Settings; also overlay titles, count subtitles, chips-at-rest, HUD badges). Structure debt is dominated by one big item: **Movies and Series duplicate each other nearly wholesale**, and Live shares the same 3-pane browse skeleton by copy.

Per-area one-liners:
- **Shell/Home** — chrome clean at rest; accent ✗ (static-primary family), focus/structure drifted (card recolor ×3, row-header dup ×3, chip dup ×4).
- **Live TV** — browse solid; accent count-subtitle static + channel-row focus-recolor (on-device); shares browse skeleton by copy.
- **Player HUD** — best accent discipline of the content areas (monochrome HUD, red LIVE = semantic); localized failures: unscrimmed text over video, OptionRow recolor, accent count badges.
- **Movies** — accent ✗ / focus ✗ / structure ✗: list-row recolor (grid is correct!), static resume/completed accents, wholesale duplication vs Series.
- **Series** — episode-row recolor confirmed on-device; **Back chip renders a forward chevron** (spec violation); "Next up" static accent.
- **Search** — jump-to chips accent-at-rest; ResultRow recolor + triplication.
- **Settings** — largest static-accent concentration (13+ sites: values, links, steppers); quick-toggle chips accent regardless of On/Off; stepper dialog duplicated ~6×. `embedded = true` verified at all four Manage-sources call sites (no onboarding chrome leaks).
- **Downloads/EPG/Profiles** — EPG has the focus-recolor in its guide grid; Downloads one static accent; Profiles clean.


---

# Scorecard: Shell / Home
Packages: `features/shell/`, `features/home/`   Screens judged: shell chrome (top bar, nav rail), Home (empty-state hero)

| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | compliant | Sweep: zero `TextStyle(`/`fontSize=` literals; consistent scale (HomeScreen.kt:724-730, 871-878; TopBar/Sidebar labels). Walkthrough: clear hierarchy in chrome (home/01-rest.png). |
| (b) Accent | **non-compliant** | Sweep: static `colors.primary` text with no state semantics — MediaDetailsScreen.kt:169 (genre), CategoryBrowserOverlay.kt:95 + ChannelListOverlay.kt:102 (overlay titles), HomeScreen.kt:905 (hero stat), plus systemic hits in SettingsScreen.kt (also charged to the Settings scorecard). Walkthrough: at-rest chrome is clean (nav active = accent-in-tonal, correct — home/01-rest.png). |
| (c) Focus | drifted | Sweep: three card sites recolor the title on focus instead of relying on the ring — HomeScreen.kt:727, HomeScreen.kt:1216, HomeGuideSlice.kt:149 (FocusableSurface itself is correct: ring white on focus, primary on selected — FocusableSurface.kt:85-89). Walkthrough: rail focus = white ring, active stays tonal — correct at the rail (home/02-down1.png). Auto-focus sane (OwnTVShell.kt:325). |
| (d) Structure | drifted | Sweep: row-header skeleton copy-pasted ×3 (HomeScreen.kt:502-509, 1064-1073; HomeGuideSlice.kt:104-113); static glass-chip shell duplicated ×4 in TopBar.kt (116, 212, 225, 273). FocusableSurface reused well (9 files). |
| (e) Sizing | compliant | Sweep: no pixel-scale; only alpha fades (TopBar.kt:131,169); responsive dp math with coerceIn (HomeScreen.kt:454-459). |
| (f) Legibility | compliant | Walkthrough: chrome + empty state read clearly at 1080p (home/01-rest.png). Sweep: no sub-bodyMedium primary content. |

## Top findings (severity-ordered)
1. **Static-primary text family across shell overlays + settings** — MediaDetailsScreen.kt:169, CategoryBrowserOverlay.kt:95, ChannelListOverlay.kt:102, HomeScreen.kt:905 (+ SettingsScreen.kt series, charged to Settings scorecard). The single biggest accent drift.
2. **Focus-recolor of card titles ×3** — HomeScreen.kt:727, 1216; HomeGuideSlice.kt:149 — same violation the onboarding overhaul just removed from ChoiceCard/ChooserCard; copy-pasted card pattern.
3. **Row-header skeleton duplicated ×3** — no shared `HomeRowHeader`; drift risk on every future shelf.
4. **Glass-chip shell duplicated ×4 in TopBar.kt** — SectionChip/ClockChip/PlaylistChip/WeatherChip hand-roll the same wrapper.

## Extraction candidates
- `HomeRowHeader(title)` — shared shelf/row header (3 current consumers + OnNowRow as a 4th).
- `StaticGlassChip(content, fill)` in `ui/components/` — collapses 4 TopBar chips.
- Kill-pattern: `color = if (focused) primary else onSurface` — fix is deletion (ring is the signal), one shared card component would make it a one-place change.

## Not covered
- Populated hero/continue-watching shelves, top-bar focus states, and long-press dialogs — need playback history and EPG data a fresh demo source doesn't have (walkthrough hit the empty state: home/07-content-focus2.png). Code sweep partially compensates (card composables audited statically).

---

# Scorecard: Live TV
Packages: `features/live/` (composables)   Screens judged: 3-pane browse, channel list, preview pane, fullscreen entry, zap overlay
| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | compliant | sweep-live.md (no literals); browse hierarchy clean (live/10-live.png) |
| (b) Accent | drifted | Accent category-count subtitle "All Channels (4 channels)" is static emphasis (live/10-live.png) — Movies renders the same header neutral (movies/01-browse.png): inconsistent + non-semantic. Sweep: static primary sites (sweep-live.md). Selected rail category = accent bar+text (selection, acceptable). |
| (c) Focus | drifted | ON-DEVICE: focused channel row title renders accent while ring is present (live/15-preview-pane.png). Sweep confirms recolor sites (sweep-live.md). |
| (d) Structure | drifted | 3-pane browse skeleton shared with Movies/Series by copy, not component (see cross-cutting duplication, sweep-movies.md); chip/row dups in sweep-live.md. |
| (e) Sizing | compliant | sweep-live.md |
| (f) Legibility | drifted (minor) | sweep-live.md minor hazards; on-device browse legible; zap chip legible over bright video (live/13-zap.png). |
## Top findings
1. Channel-row focus-recolor (on-device + code) — same family as Home/Movies/Series/Search/EPG.
2. Accent "(N channels)" header subtitle — static emphasis, inconsistent with Movies/Series headers.
3. Browse 3-pane skeleton duplicated across Live/Movies/Series (extraction candidate: `MediaBrowseScaffold`).
## Extraction candidates
- `MediaBrowseScaffold` (category rail + list + detail pane) shared by Live/Movies/Series.
- `CategoryHeader(title, count)` — one component; kills the accent/neutral inconsistency.
## Not covered
- Catchup/timeshift UI, EPG-integrated states (demo source has no EPG data).

---

# Scorecard: Player HUD + mini-player
Packages: `player/` (composables)   Screens judged: VOD HUD (mpv), live HUD (ExoPlayer+mpv fallback), zap overlay, buffering states
| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | drifted (localized) | SubtitleOverlay.kt:88-94 ad-hoc TextStyle/fontSize (sweep-player.md); HUD chrome otherwise on scale (player/01-playing.png). |
| (b) Accent | drifted | PlayerHud.kt:1575 OptionRow focus-only accent recolor; StreamInfoOverlay.kt:62 static accent heading; ON-DEVICE: accent-filled track-count badges on subtitle/audio icons = informational, not state (live/14-hud-over-video.png). Red LIVE badge = correct semantic state. Loading spinner accent = activity, fine. |
| (c) Focus | drifted (localized) | PlayerHud.kt:1575, AudioNowPlayingBar.kt:345 recolor on focus (sweep-player.md); transport controls correctly use white-fill focus (player/01-playing.png). |
| (d) Structure | drifted | Shared TrackDialog/DialogScaffold good; tint ternary triplicated (PlayerHud.kt:1063/1145-1146/1164); SeekBar vs LiveTimelineBar near-dup (sweep-player.md). |
| (e) Sizing | compliant | sweep-player.md |
| (f) Legibility | drifted (localized) | Center error/status text unscrimmed (PlayerHud.kt:634-660); ON-DEVICE: top meta row text can collide with busy video (live/14-hud-over-video.png). Zap chip has glass bg — good (live/13-zap.png). |
## Top findings
1. Unscrimmed center/top HUD text over video — the one real 10-foot legibility risk found in the app.
2. OptionRow/AudioNowPlayingBar focus-recolor — same systemic family.
3. Accent track-count badges = decoration; StreamInfoOverlay static accent heading.
4. SeekBar/LiveTimelineBar near-duplicate skeletons; triplicated tint ternary.
## Extraction candidates
- `HudScrim`/text-protection modifier applied to every text-over-video surface.
- Single `TimelineBar` (seek + live) component; shared icon-badge component with a neutral count treatment.
## Not covered
- Track/subtitle dialog visuals on-device (focus wouldn't land; code-audited via sweep); PiP/mini-player overlay visual (BACK exited to browse instead — preview pane placeholder captured, live/15-preview-pane.png).

---

# Scorecard: Movies
Packages: `features/movies/`   Screens judged: browse grid (poster focus), detail pane w/ TMDB metadata
| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | compliant | sweep-movies.md; browse/detail hierarchy clean (player/07-afterback.png) |
| (b) Accent | non-compliant | Sweep: static primary on resume label, favorite tint, completed badge fill (sweep-movies.md). On-device detail pane genre/rating rendered neutral (player/07-afterback.png) — the drift is in states not shown (resume/completed). |
| (c) Focus | non-compliant | Sweep: MovieListRow title recolor on focus. Grid poster title stays white on focus (movies/02-grid-focus.png) — list vs grid inconsistent. |
| (d) Structure | non-compliant | Near-verbatim duplication vs Series: browse scaffold, context menu, list row, TMDB merge helpers (sweep-movies.md). |
| (e) Sizing | compliant | sweep-movies.md |
| (f) Legibility | compliant | sweep-movies.md; on-device browse/detail legible. |
## Top findings
1. Movies↔Series wholesale skeleton duplication — the largest single structure debt in the app.
2. MovieListRow focus-recolor (list only; grid is correct) — inconsistent within the same feature.
3. Static accent on resume/completed indicators — should be neutral or semantic tokens.
## Extraction candidates
- Shared `MediaBrowseScaffold` + `MediaListRow` + `MediaContextMenu` used by both Movies and Series (and largely Live).
## Not covered
- Resume/completed badge states (need playback progress ≥ threshold), sort/filter dialog visuals.

---

# Scorecard: Series
Packages: `features/series/`   Screens judged: browse (poster focus), detail (season chips, episode list), episode options dialog
| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | compliant | sweep-series.md; detail hierarchy clean (series/03-detail.png) |
| (b) Accent | drifted | SeriesScreen.kt:839 static accent "Next up" label (sweep-series.md); Close button solid-accent at rest in options dialog (series/05-options.png) = PRIMARY-at-rest family. Season chip selected = accent+tonal (selection, acceptable). |
| (c) Focus | non-compliant | ON-DEVICE: focused episode row title accent vs white unfocused (series/03-detail.png); code: EpisodeRow :1397-1404, SeriesListRow :1465-1471. Back chip uses forward CHEVRON instead of back affordance (:1095; visible series/03-detail.png) — icon-semantics violation. |
| (d) Structure | compliant (debt) | Shared components used; SeriesListRow duplicates MovieListRow near-verbatim (cross-charged to Movies). |
| (e) Sizing | compliant | sweep-series.md |
| (f) Legibility | drifted (minor) | EpisodeDetailPane plot lacks maxLines (:867) — unbounded-text risk (sweep-series.md). |
## Top findings
1. Episode/series row focus-recolor — confirmed on-device.
2. Back chip renders a FORWARD chevron — direct spec violation (chevron = proceed).
3. "Next up" static accent label; Close CTA solid-accent at rest.
## Extraction candidates
- Same shared browse/list/menu components as Movies (one set serves both).
## Not covered
- Download-episode flow past the options dialog; TMDB-hydrated series art (poster placeholder in demo data).

---

# Scorecard: Search
Packages: `features/search/`   Screens judged: idle state (hero, input, jump-to chips)
| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | drifted (minor) | ad-hoc fontWeight at :362/:389/:490 (sweep-search.md); hierarchy otherwise clean (search/01-idle.png) |
| (b) Accent | drifted | Static primary on detail-pane subtitle (:362, sweep-search.md); ON-DEVICE: three "Jump to" chips accent-text-on-tonal at rest (search/01-idle.png) — navigation shortcuts, not active state. |
| (c) Focus | drifted | ResultRow title recolor on focus (:439, sweep-search.md). |
| (d) Structure | drifted | ResultRow skeleton triplicated across SearchItem cases (:252-290, sweep-search.md). |
| (e) Sizing | compliant | sweep-search.md |
| (f) Legibility | compliant | sweep-search.md; idle screen legible (search/01-idle.png). |
## Top findings
1. Jump-to chips accent-at-rest (on-device) + static primary subtitle (code).
2. ResultRow focus-recolor — systemic family.
3. ResultRow triplication.
## Extraction candidates
- Single parameterized ResultRow call; shared subtitle/meta text component.
## Not covered
- Typed-query results & empty-result states on-device (IME/typing race; code audited via sweep).

---

# Scorecard: Settings
Packages: `features/settings/` (scoped: hub, ManageSources, appearance, playback, customize)   Screens judged: hub (chips, tiles, rows)
| Axis | Verdict | Evidence |
|---|---|---|
| (a) Typography | compliant | sweep-settings.md (one scale-derived fontSize, acceptable) |
| (b) Accent | non-compliant | 10 static primary sites in SettingsScreen.kt (:1192,:1203,:1211,:1318,:1345 + 5 stepper values) + ManageSourcesScreen.kt:249 + VideoPlayerSettingsScreen.kt:837,:1333 (sweep-settings.md; stepper values judged decoration, not selection). ON-DEVICE: quick-toggle chips accent-on-tonal REGARDLESS of On/Off state — "Preview sound Off" styled same as On chips (settings/01-hub.png) = accent as decoration. Playlists/EPG tonal icon tiles = sanctioned (settings/01-hub.png). |
| (c) Focus | non-compliant (localized) | CustomizeScreen.kt SectionChip (:662) + CategoryRow (:724,:734) recolor on focus (sweep-settings.md). |
| (d) Structure | drifted | Good reuse (SettingsRow/SourceRow/StepperDialog) BUT stepper dialog duplicated ~6× instead of reusing StepperDialog (sweep-settings.md). |
| (e) Sizing | compliant | sweep-settings.md |
| (f) Legibility | compliant | sweep-settings.md; hub legible (settings/01-hub.png). |
## Top findings
1. Largest static-accent concentration in the app (Settings values/links/steppers) — reads as "make it pop", not state.
2. Quick-toggle chips accent regardless of state (on-device) — accent must encode On, neutral must encode Off.
3. Stepper dialog duplicated ~6× despite an existing shared StepperDialog.
## Extraction candidates
- One `StepperDialog` reused everywhere; `ValueText` convention (neutral) for setting values; state-aware toggle-chip component.
## Verified
- `embedded = true` at all four ManageSources call sites — no onboarding chrome leaks into Settings (sweep-settings.md; matches onboarding-branch design).
## Not covered
- Visual pass of deep screens beyond hub (appearance/playback/customize audited in code only). Skipped list in sweep-settings.md.

---

# Scorecard: Quick passes — Downloads / EPG / Profiles
Packages: `features/downloads/`, `features/epg/`, `features/profiles/`   Screens judged: code-first (visual: profile editor dialog from onboarding session)
### Downloads
- Static `colors.primary` on storage-info text (:230, sweep-quickpass.md) — same static-accent family. Otherwise unremarkable.
### EPG
- Focus-triggered recolor of channel labels (:769) — systemic family, in the guide grid where focus moves constantly (high visibility).
- 2× static primary on informational text (stats/match summary).
### Profiles
- Clean per sweep; semantic error/badge colors used correctly. Profile editor dialog (out-of-scope for onboarding redesign) visually consistent (audit/setup/04-named.png).
## Not covered
- Downloads with active items; populated EPG grid (no EPG feed in demo source); profile switcher on-device.

---

## Cross-cutting findings (the real overhaul targets)

1. **Focus-recolor of titles/labels (7 areas).** `color = if (focused) primary else onSurface` appears in Home cards (HomeScreen.kt:727, :1216; HomeGuideSlice.kt:149), Live channel rows, Movies `MovieListRow`, Series `EpisodeRow`/`SeriesListRow` (:1397, :1465), Search `ResultRow` (:439), Settings Customize (:662, :724), EPG guide (:769), Player `OptionRow` (PlayerHud.kt:1575) and `AudioNowPlayingBar` (:345). The white ring is already everywhere via `FocusableSurface`; the fix is deletion, ideally by consolidating rows/cards into shared components so it's a one-place change. This is the same violation the onboarding overhaul removed from `ChoiceCard`/`ChooserCard`.
2. **Static accent as emphasis.** Settings values/links/steppers (10 sites in SettingsScreen.kt + 3 elsewhere), shell overlay titles (MediaDetailsScreen.kt:169, CategoryBrowserOverlay.kt:95, ChannelListOverlay.kt:102), Live count subtitle, Search subtitle + jump-to chips at rest, Series "Next up", Movies resume/completed, Downloads storage info, EPG stats, HUD track-count badges, Settings quick-toggle chips styled accent even when **Off**. One rule fixes all: accent encodes on/active/selected/focused — everything informational is `onSurface`/`onSurfaceVariant`.
3. **Movies ↔ Series wholesale duplication** (browse scaffold, list row, context menu, TMDB merge), with Live sharing the same 3-pane shape by copy. The extraction set — `MediaBrowseScaffold`, `MediaListRow`, `MediaContextMenu`, `CategoryHeader(title, count)` — is the app-wide analogue of what `SetupScaffold` did for onboarding.
4. **Smaller shared-component debt:** Home row-header ×3 + TopBar glass-chip ×4; Settings stepper dialog ~6×; Search ResultRow ×3; SeekBar/LiveTimelineBar near-dup; PlayerHud tint ternary ×3.
5. **Icon semantics:** Series Back chip uses a forward chevron (SeriesScreen.kt:1095). Spec: chevron = proceed.
6. **Text-over-video protection:** PlayerHud center error/status (:634-660) and top meta row lack scrims; every other HUD surface has glass. One `HudScrim` treatment closes it.
7. **PRIMARY-at-rest CTAs** (dialog "Close", "Try again", onboarding's deferred list): decide once — either PRIMARY-at-rest is the sanctioned "single default action" style, or SECONDARY-at-rest everywhere; today it's mixed.

## Consolidated extraction candidates

| Component | Serves | Kills |
|---|---|---|
| `MediaBrowseScaffold` + `MediaListRow` + `MediaContextMenu` | Live, Movies, Series | the largest duplication + 5 focus-recolor sites |
| `CategoryHeader(title, count)` | Live/Movies/Series headers | accent/neutral header inconsistency |
| `HomeRowHeader` | 3-4 Home shelves | row-header dup |
| `StaticGlassChip` | 4 TopBar chips | chip dup |
| Single `StepperDialog` reuse + neutral `ValueText` | Settings | ~6 dup dialogs + stepper-value accents |
| `HudScrim` text protection | Player | unscrimmed text over video |
| `TimelineBar` (seek+live) | Player | near-dup bars |
| State-aware toggle-chip | Settings quick toggles | accent-when-Off |

## Roadmap (drift × user exposure)

1. **Phase 1 — the two systemic rules, applied via the browse extraction.** Build `MediaBrowseScaffold`/`MediaListRow`/`MediaContextMenu`/`CategoryHeader`; migrate Live → Movies → Series onto them; delete every focus-recolor and static-accent site those components replace. This one phase touches the highest-exposure surfaces (Live browse, Movies/Series) and eliminates the majority of both systemic families at once. Include the Series back-chevron fix.
2. **Phase 2 — Settings sweep.** ValueText/toggle-chip/StepperDialog consolidation + the 13 static-accent sites. High visibility, fully mechanical after phase 1 establishes the conventions.
3. **Phase 3 — Home/shell polish.** HomeRowHeader, StaticGlassChip, hero-card recolor removal, overlay-title accents. (Home's populated states also deserve an on-device pass with real history/EPG.)
4. **Phase 4 — Player refinements.** HudScrim, TimelineBar, OptionRow recolor, count-badge treatment. Small, contained, high polish value.
5. **Phase 5 — long tail.** Search rows/chips, EPG recolor + accents, Downloads accent, PRIMARY-at-rest ruling applied app-wide (incl. onboarding's deferred list).

**Recommended phase-1 target:** the shared media-browse component set (Live/Movies/Series) — biggest drift, biggest exposure, and it establishes the app-wide shared-component pattern the rest of the roadmap builds on, exactly as SetupScaffold did for onboarding.

## Not covered

- Populated Home hero/shelves, long-press dialogs on Home (need playback history + EPG feed).
- Catchup/timeshift, populated EPG grid (demo source ships no XMLTV).
- Track/subtitle dialog + mini-player overlay on-device (code-audited; D-pad focus wouldn't land during playback).
- Typed search results on-device (IME race; code-audited).
- Settings deep screens beyond the hub visually (code-audited; skipped list in sweep).
- Resume/completed movie badges (need progress state).
- Light-theme rendering of main surfaces (theme flips post-Settings; token-based by construction).
- Real-TV overscan/rendering (emulator frames only — re-verify phase deliverables on Fire TV).
