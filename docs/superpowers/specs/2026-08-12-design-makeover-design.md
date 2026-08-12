# OwnTV Design Makeover — Design Spec

**Date:** 2026-08-12
**Status:** approved design, pending implementation plan
**Goal:** a full visual makeover — typography, color, sizing, and layout — moving OwnTV toward the polish of the current Google TV release while keeping its own signature (Liquid Glass) and its user-configurable theming.

## Scope

- **In:** the token layer (type, color, shape, focus, motion), and four layout areas: cards & row rhythm, sidebar & top bar, hero carousel, detail/preview panes.
- **Out:** feature behavior, navigation structure, the fullscreen player HUD's layout (its *colors* are in scope — see token leaks), the glass rendering pipeline (its parameters are in scope), settings surface area (no new user-facing options).

## Execution model (approved: "Approach A")

Token-first cascade, then per-area layout passes. Five phases, each independently shippable:

- **Phase 0 — tokens:** Type.kt (Figtree + full scale), Color.kt (charcoal neutrals), Dimens shape enforcement, FocusableSurface defaults, PopupTheme unification, hardcoded-color sweep. The whole app restyles in this phase with near-zero layout risk.
- **Phase 1 — cards & row rhythm.** **Phase 2 — sidebar & top bar.** **Phase 3 — hero carousel.** **Phase 4 — detail/preview panes.**

Every phase ends with a debug build deployed to an Android TV emulator (the arm64 ATV image runs the `standard` flavor; an x86_64 image needs the `x86_64` flavor), `adb screencap` review shots of touched screens, and a D-pad navigation check (`adb shell input keyevent` DPAD keys) before the phase is called done.

## Section 1 — Color

Neutrals move from teal-tinted to true charcoal (barely-cool cast, no hue bias) so all five accent presets and custom hex accents sit cleanly on them. Accent presets are **unchanged**.

### Dark (primary experience)

| Token | Current | New |
|---|---|---|
| background | `#040E0B` | `#0B0D0E` |
| surface | `#0E1513` | `#121517` |
| surfaceContainerLowest | `#090F0E` | `#0E1113` |
| surfaceContainerLow (panel) | `#161D1B` | `#16191C` |
| surfaceContainer (rail) | `#1B211F` | `#1A1E21` |
| surfaceContainerHigh (card) | `#252B29` | `#23282C` |
| surfaceContainerHighest | `#303634` | `#2C3238` |
| onSurface | `#DEE4E1` | `#E7EAEC` |
| onSurfaceVariant | `#BFC9C4` | `#A9B0B5` |
| outline / outlineVariant | `#89938F` / `#3F4945` | `#7E868C` / `#3A4046` |

Secondary role family (chips, tonal tiles) is de-greened to neutral-cool — dark: secondary `#B6C1C9`, onSecondary `#212A31`, secondaryContainer `#39434B`, onSecondaryContainer `#D5DFE7`; light: secondary `#4E5B66`, onSecondary `#FFFFFF`, secondaryContainer `#D5DFE7`, onSecondaryContainer `#0A1922`. The tertiary family is already a cool blue with no green cast and is **unchanged**. Light theme neutrals get the same de-greening: `#FAFBFC` ground with neutral gray container steps mirroring the dark ramp (`#FFFFFF` lowest → `#DFE3E6` highest, ink `#191C1E` / `#42474B`).

### Token meaning changes

- `focusBorder`: **no longer the accent.** White (`#FFFFFF`) in dark theme, near-black (`onSurface`-strength ink) in light theme.
- `focusGlow`: neutral shadow (black-based elevation shadow), not an accent halo.
- Accent (`primary` roles) now exclusively means *selected / active / progress* — never *cursor position*.
- `AccentCyan` (`0xFF52DBC8`) remains only as the brand-mark constant for the logo.

### Hardcoded-color sweep

- `PlayerHud.kt` `TEAL` constant → theme tokens (HUD follows the user's accent).
- Weather-icon inline palette in `TopBar.kt` → named constants in the theme layer (values may stay identical; they're pictorial, not chrome).
- Rule going forward: no `Color(0xFF…)` literals in feature/component code; tokens only. (Pictorial canvas art — weather glyphs, logos — is exempt but must draw its palette from one named place.)

## Section 2 — Typography

**Figtree** variable font (SIL OFL — GPLv3-compatible), one upright file in `res/font` (~100 KB). No italic file (TV UI uses none; synthesis covers edge cases). **Lora is removed** (both files deleted, −75 KB). Non-Latin scripts (Arabic, Bengali, Devanagari, Malayalam, CJK, Cyrillic) fall back to system Noto exactly as they do today; verified via the existing debug `FontFallbackQaActivity` and pseudolocale sweep.

`PopupFontTheme` keeps its API, idempotency guard, and `fontScale` mechanics but resolves to Figtree — all 47 call sites untouched.

### The complete scale (all 15 styles defined; today only 8 are)

| Style | Size/Line (sp) | Weight | Tracking | Typical use |
|---|---|---|---|---|
| displayLarge | 44/52 | ExtraBold (800) | −2% | hero titles |
| displayMedium | 36/44 | Bold | −1.5% | — |
| displaySmall | 30/38 | Bold | −1% | — |
| headlineLarge | 28/36 | Bold | −1% | screen titles |
| headlineMedium | 24/32 | SemiBold | −0.5% | detail titles |
| headlineSmall | 20/28 | SemiBold | 0 | — |
| titleLarge | 22/28 | SemiBold | −0.5% | dialog titles |
| titleMedium | 17/24 | SemiBold | 0 | card/list titles |
| titleSmall | 15/20 | SemiBold | +0.1% | — |
| bodyLarge | 16/24 | Regular | +0.15% | synopsis |
| bodyMedium | 14/20 | Regular | +0.15% | descriptions |
| bodySmall | 12/16 | Regular | +0.2% | fine print |
| labelLarge | 14/20 | SemiBold | +0.2% | chips, buttons |
| labelMedium | 12/16 | SemiBold | +0.4% | badges |
| labelSmall | 11/16 | Medium | +0.5% | smallest labels |

Inline `fontWeight =` overrides in chrome components (top-bar chips, buttons, etc.) are **deleted** — hierarchy is carried by the scale, not per-call-site bolding. Times/durations/counts use tabular numerals where the layout aligns digits.

## Section 3 — Shape, focus, motion

**Shape.** The existing `Dimens` scale (CornerSmall 12 / CornerMedium 18 / CornerLarge 24 / CardCorner 20) is authoritative. Outliers pulled onto it: top-bar chips 14dp → **pill**; value-chips 8dp → CornerSmall; `PosterCardCorner` 14 → CornerMedium (see cards redesign); `IconTileCorner` folds into CornerSmall; `Glass.kt` default 22dp → CardCorner. Rule: no raw `RoundedCornerShape(Ndp)` in feature code — `Dimens` tokens only.

**Focus (FocusableSurface defaults, app-wide).** Focused = white ring (new `focusBorder`) + neutral drop shadow + spring scale (~1.05 for cards; 1.0 for chrome like chips/rows). `selected` = accent border — the only accent on card chrome. The glass white-rim focus branch merges into this single path.

**Motion.** Focus scale animates on a spring (M3-expressive medium bounce); color/border transitions stay on tweens. All motion still routes through `AnimationLevel` — OFF collapses springs to instant snaps.

## Section 4 — Layout areas

### ① Cards & row rhythm
- Artwork **is** the card: full-bleed at CornerMedium; the current 6dp inner padding and double-corner (14/10) treatment is removed.
- Title + metadata move **below** the artwork: `labelLarge` title (max 2 lines) + `bodySmall`/`onSurfaceVariant` metadata, start-aligned.
- Continue-watching progress: 4dp accent bar along the artwork's bottom edge.
- Rhythm: `GapMedium` between cards, ≥`GapLarge` between rows; section headers `titleMedium` with clear space above.
- Applies to Home, Movies, Series, Search shelves.

### ② Sidebar & top bar (geometry/treatment only — no reordering)
- Collapsed rail 88 → 72dp. Expanded stays 272dp (localized labels).
- Active nav item: neutral pill (`secondaryContainer`) with accent-tinted icon/label — replaces the accent-filled block.
- Top bar: all chips pill-shaped; only the section chip keeps accent-container fill; search/continue/weather/clock/playlist share uniform neutral `surfaceContainer` @ 60% alpha.

### ③ Hero carousel
- Existing size mechanics (`HeroBaseWidth`, min/max heights) unchanged; corner → CornerLarge.
- Two-axis scrim (bottom-up + start-side) for text legibility over artwork.
- Metadata block: uppercase `labelMedium` genre eyebrow · `headlineLarge` title · 2-line `bodyMedium` synopsis · quiet dot-separated meta row.
- Hero focus: white ring only, no scale.

### ④ Detail & preview panes
- Titles `headlineMedium/Large`; metadata as one quiet `labelMedium` dot-separated row (tabular numerals); synopsis `bodyMedium` capped ≈46ch.
- Button row: one accent-filled pill (Play) + neutral tonal pills.
- Posters CornerMedium. Live preview pane: same hierarchy for channel name + now/next EPG rows.

## Constraints & invariants

- **i18n boundary holds:** no string changes in this work; all text keeps resolving via `stringResource` in Compose. RTL layouts must be re-checked in the layout phases (start/end-aware paddings, the hero's start-side scrim mirrors).
- **D-pad first:** no focusability changes intended; any layout pass that alters focus order is a defect.
- **Settings compatibility:** ThemeMode, accent presets, custom hex, glass scope/strength, background image, AnimationLevel, UI zoom — all keep working with no migration.
- **Performance:** no new per-frame allocations in glass/focus paths; spring animations must not regress low-end boxes (AnimationLevel OFF is the escape hatch, as today).
- **Lint stays green** (`lintStandardDebug`, PluralsCandidate fatal); unit tests keep passing (`testStandardDebugUnitTest`).

## Risks

- **Contrast regressions** from the neutral re-derivation of secondary/tertiary containers → mitigate by matching current contrast ratios and reviewing screenshots per phase.
- **Focus-order regressions** in Phase 1/2 layout changes → D-pad walkthrough on the Android TV emulator per phase.
- **Font metrics shift** (Figtree runs slightly wider than Roboto at equal size) → watch for unexpected ellipsizing in chips, the EPG grid, and settings rows during phase review.
