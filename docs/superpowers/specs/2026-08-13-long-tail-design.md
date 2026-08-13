# Phase 5 — Long Tail — Design Spec

**Date:** 2026-08-13
**Status:** approved design, pending implementation plan
**Parent:** `docs/superpowers/reports/2026-08-12-design-compliance-audit.md` (roadmap phase 5) + phase-4 final-review follow-ups (ledger, merged branch `player-refinements`)
**Goal:** Close the audit's remaining inventory — Search/EPG/Downloads/HomeGuideSlice accent sites, the PRIMARY-at-rest ruling applied app-wide, the phase-4 review follow-ups, and the orphaned-strings chore — completing the design-compliance roadmap.

## Ruling (user-approved, binding)

**PRIMARY-at-rest = the single default action.** `OwnTVButtonStyle.PRIMARY` (solid accent fill at rest) is sanctioned as the marker of exactly ONE default action per dialog/action row/screen surface. Any surface presenting more than one PRIMARY at rest demotes the extras to SECONDARY. This ratifies the shipped phase-1 pattern (MediaContextMenu: SECONDARY entries + one PRIMARY Close). `OwnTVButton` itself is unchanged; a doc comment on `OwnTVButtonStyle` records the ruling.

## Scope

**In:**

1. **CTA enforcement sweep** — audit all implicit-PRIMARY `OwnTVButton` call sites (~104); demote extra PRIMARYs on multi-PRIMARY surfaces to `OwnTVButtonStyle.SECONDARY`. Onboarding's deferred CTA list gets the same check. Doc comment on `OwnTVButtonStyle`.
2. **Search** (`features/search/SearchScreen.kt`):
   - `:362` detail-pane subtitle `colors.primary` → `onSurfaceVariant`; drop ad-hoc `fontWeight` where the type token carries it (audit sites `:362/:389/:490`).
   - Jump-to chips `:378-388`: rest = `surfaceContainerHigh` bg + `onSurface` content (was accent-on-tonal); focused solid-`primary` fill-swap deleted — ring only.
   - `ResultRow` `:439` focus recolor → always `onSurface`. `:448` favorite icon accent KEPT (semantic).
   - Triplicated ResultRow skeleton (`:252-290` region) → one file-private `ResultRow` composable (no cross-feature consumer — not promoted to `ui/components/`).
3. **EPG** (`features/epg/`):
   - `EpgScreen.kt:769` guide-grid channel-label focus recolor → always `onSurface`.
   - `EpgScreen.kt:385` stats + `:392` match summary → `onSurfaceVariant`.
   - `GuideCore.kt:238` channel-name label → `onSurfaceVariant`.
   - KEPT: `GuideCore.kt:135` catchup glyph `↻` accent (capability marker), red "now" line (`colors.favorite`), EPG progress fills.
4. **Downloads** (`features/downloads/DownloadsScreen.kt`):
   - `:230` storage-info text → `onSurfaceVariant`; `:297` "Completed" status text → `onSurfaceVariant` (matches phase-1 Movies resume/completed ruling).
   - KEPT: progress fills `:234`/`:304`.
5. **HomeGuideSlice + TopBar leftovers** (`features/home/HomeGuideSlice.kt`, `features/shell/components/TopBar.kt`):
   - HomeGuideSlice `:248-249` and `:387-388`: focused-arm accent alphas deleted — color no longer focus-dependent.
   - HomeGuideSlice `:455-463` tile: drop focused `primaryContainer` bg + `onPrimaryContainer` text swap — ring only.
   - TopBar interactive pills: delete `focusedContainerColor = colors.primary` overrides (`:201` and any sibling with the same override) — ring only. Static `SectionChip` accent tint (marks current section) is out of scope (phase-3 ruling stands).
6. **Phase-4 final-review follow-ups** (`player/PlayerHud.kt`):
   - Error scrim: `widthIn(max = 560.dp)` on the scrimmed column; remove children's `fillMaxWidth(0.8f)`.
   - Normalize/clarify the live scrub-bubble placement comment (VOD `offset(-32.dp)` vs live `padding(bottom = 30.dp)` — document, don't change behavior).
   - `NextEpisodeCard` `:1119` "Next episode in Ns" → `onSurface` (informational, same species as StreamInfoOverlay heading).
   - Player dialog value readouts `:1418/:1498/:1529` (A/V sync, volume %, subtitle delay) → `onSurface` (phase-2 neutral-value stepper contract applied to its player twins).
   - Empty-metadata scrim: guard the TopBar scrim application (`takeIf`-style) so an empty title + empty chip row renders no empty black pill.
7. **Orphaned-strings prune:** lint `UnusedResources` against `res/values/strings.xml`; remove unused entries mirrored across ALL locale files (`values-*/strings.xml`). Zero new strings; zero string edits — deletions only.

**Out:** any behavior change (search logic, EPG data flow, downloads state machine); `OwnTVButton` component internals; the zap/tune OSD accents (sanctioned); populated-EPG/catchup visuals (no feed in demo source — code-level rules only); light-theme verification.

## Color/keep contract (exhaustive for touched files)

| Site | Today | After |
|---|---|---|
| SearchScreen.kt:362 subtitle | static `primary` + SemiBold | `onSurfaceVariant`, token weight |
| SearchScreen.kt:378-388 jump-to chips | accent-on-tonal at rest; solid `primary` on focus | `surfaceContainerHigh`+`onSurface` at rest; no focus fill-swap |
| SearchScreen.kt:439 ResultRow title | `if (focused) primary else onSurface` | always `onSurface` |
| EpgScreen.kt:769 channel label | same recolor | always `onSurface` |
| EpgScreen.kt:385/:392 stats/summary | static `primary` | `onSurfaceVariant` |
| GuideCore.kt:238 channel name | static `primary` | `onSurfaceVariant` |
| DownloadsScreen.kt:230 storage text | static `primary` + SemiBold | `onSurfaceVariant`, token weight |
| DownloadsScreen.kt:297 Completed text | static `primary` + SemiBold | `onSurfaceVariant`, token weight |
| HomeGuideSlice.kt:248/:387 | focused → accent alphas | focused arm deleted |
| HomeGuideSlice.kt:455-463 tile | focused → `primaryContainer`/`onPrimaryContainer` | ring only (rest colors keep) |
| TopBar.kt:201 (+ siblings) | `focusedContainerColor = primary` | override deleted — ring only |
| PlayerHud.kt:1119 NextEpisodeCard label | static `primary` | `onSurface` |
| PlayerHud.kt:1418/:1498/:1529 dialog values | static `primary` | `onSurface` |

**KEPT (semantic, must survive):** Search favorite icon (:448); EPG catchup glyph (GuideCore:135), red now-line (`colors.favorite`), progress fills; Downloads progress fills (:234/:304); HomeGuideSlice progress fill (:543); TopBar static SectionChip tint; all previously-sanctioned accents from phases 1-4.

## Constraints & invariants

- **Behavior unchanged:** search/EPG/downloads logic, focus order, chip actions, player timings. Composition/visual only — except the sanctioned PRIMARY→SECONDARY style demotions (visual by design).
- **i18n:** no new strings, no string edits; the prune is deletions-only, mirrored across every `values-*` locale file. `PluralsCandidate` stays fatal.
- **Git hygiene:** stage edited files by explicit path only; NEVER `git commit -am`/`git add -A` (user's uncommitted gradle files in the tree).
- **Gates per task:** `./gradlew :app:compileStandardDebugKotlin lintStandardDebug` (0 errors); `git status --porcelain -- app/src/main/res` empty for all NON-prune tasks; full suite + on-device sweep at the end.
- Line numbers verified live on `main` @ aa077d8 (Search/EPG/Downloads/HomeGuideSlice untouched since the audit; PlayerHud refs from the phase-4 final review) — implementers verify surrounding context before editing.
- Branch: `long-tail` off `main`.

## Risks

- **CTA sweep judgment:** "one surface" boundaries can be fuzzy (stacked dialogs, action rows + inline buttons). Rule of thumb: one PRIMARY per visually-contiguous action group; when in doubt, keep the affirmative/default action PRIMARY and demote the rest; record every demotion in the report.
- **Orphaned-strings false positives:** lint can miss reflection/indirect usages — verify each removal with a repo-wide grep of the key before deleting; when any doubt remains, keep the string.
- **TopBar pill focus-fill deletions** change a deliberate (commented) pattern — confirm the ring reads clearly on the glass chips on-device; if a pill becomes indistinct, report rather than reinventing a new treatment.
- **Line drift** in PlayerHud.kt (phase-4 merge just landed): the cited lines are from the final review at HEAD `aa077d8`; verify context first.
