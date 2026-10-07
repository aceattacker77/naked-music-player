# Skin ornament (Phase 2)

Status: draft for review. Phase 2 of the Geofront app-change brief. It builds on the shape and type fields from
Phase 1 (`docs/superpowers/specs/2026-10-06-skin-shape-and-type-design.md`). Panel header strips, the glow, status
tags, title cards, the Equalizer restyle, the widget and the font subset are later phases.

## Goal

Let a skin opt into ornament: hexagon "No artwork" tiles with borders, a segmented seek bar, corner brackets, a
bracketed mini player, a chamfered bottom bar and a playing-row edge. Every new field is optional and defaults to
today's look, so the built-in skins render exactly as before and older apps ignore the new fields. `format` stays `1`.

## Success criteria

1. A skin that sets none of the new fields is pixel-identical to the current build (existing Roborazzi screenshots
   pass unchanged).
2. A skin with all of them switched on renders: hexagon placeholders with borders on list, mini-player and Now Playing
   tiles; a segmented seek bar; corner brackets on the Now Playing artwork and the mini player; the playing-row edge;
   a chamfered primary block on the selected bottom tab.
3. Seeking, accessibility semantics (`progressBarRangeInfo`, `setProgress`) and navigation routing behave exactly as
   before.
4. An invalid value is rejected with one message in the existing style and installs nothing.
5. `FORMAT.md`, the skin design brief and its HTML copy, and the README test count describe the new fields.

## Non-goals

Panel header strips (`panelHeader`), glow (`components.glow`), status tags and the strings pack, title cards, the
Equalizer restyle, the widget, kanji accents, a rail version of the chamfered bar (tablets keep Material's rail).

## Schema

All fields optional.

| Field | Values | Default | Notes |
|---|---|---|---|
| `player.artPlaceholder` | `note` · `hexagon` | `note` | What tiles show when there is no artwork. |
| `player.artBorder` | boolean | false | 1 dp `outline` border around artwork and tiles (`primary` on the playing tile). |
| `player.seekBar` | adds `segmented` | `wavy` | |
| `player.seekSegments` | integer 12–60 | 40 | Number of cells in the segmented seek bar (the mini player's progress line has no cells). |
| `player.seekColor` | `primary` · `tertiary` | `primary` | Colour of filled cells. |
| `components.brackets` | boolean | false | Corner brackets on the Now Playing artwork; mini player becomes a bracketed panel. |
| `components.segmentedMeters` | boolean | false | Shows the `POSITION` / percentage header above a segmented seek bar. |
| `components.navStyle` | `material` · `block` | `material` | `block` replaces the compact bottom bar with the chamfered-block bar. |
| `components.rowEdge` | boolean | false | Playing-row edge bar, primary tile border and label-font durations. |

Errors use the existing form, for example `invalid value 'cube' for 'player.artPlaceholder'`,
`invalid value '5' for 'player.seekSegments'`. A wrong JSON type keeps `skin.json is not valid JSON`.

`Skin` and `PlayerStyle` gain matching fields with defaults equal to today; a new `Ornament` value class groups the
`components.*` flags and the placeholder/border settings. `SeekBarStyle` gains `SEGMENTED`; new enums `ArtPlaceholder`,
`SeekColor`, `NavStyle`. Export and install already carry the whole skin directory.

## Ornament local

`AppTheme` provides `LocalOrnament` (art placeholder, art border, brackets, segmented meters, nav style, row edge)
derived from the skin. Its default outside `AppTheme` is "all off", so composables rendered without a theme (tests,
previews) behave as today, the same pattern as `LocalLabelCaps`. Composables read it; no screen takes new parameters
for ornament.

## Tiles

`AlbumArt` draws a hexagon placeholder when the ornament says so: three nested pointy-top hexagons on a `Canvas`,
outer in `outlineVariant`, middle in `outline`, both 1 dp, inner in `primary` at 2 dp, and the note glyph in `primary`
centred. It scales to 44, 48, 200 dp and full screen with no bitmap. With `artBorder`, a 1 dp `outline` border follows
the tile's shape (`primary` for the playing tile). On the Now Playing artwork only, two decorative captions appear
when no art exists: "No artwork" top-left and the track code bottom-left, `TRK` plus the zero-padded one-based queue
index to four digits (`TRK 0002`), from `PlayerUiState.currentIndex`. Both captions are hidden from accessibility.

## Segmented seek bar

`SeekBar` gains a `SEGMENTED` draw mode: 16 dp tall, 3 dp gaps, a 1 dp `outline` border on each cell, filled cells in
`seekColor`, touch area 32 dp. Gesture handling, semantics and `onSeek` are unchanged. A pure function
`segmentsFilled(fraction, segments)` returns the filled cell count. No per-frame animation; it redraws when the
position changes. With `segmentedMeters`, a header row shows `POSITION` and the percentage to one decimal with trailing
zeros kept (`31.3 %`). The mini player's progress is a 2 dp continuous line for a skin that asks for brackets.

## Brackets, panel and mini player

`Modifier.cornerBrackets(color, length = 14.dp, stroke = 2.dp)` draws the top-left and bottom-right brackets outside the
content bounds by half the stroke, so layout does not shift and the chamfer does not clip them. `GeoPanel` is a frame
(1 dp `outline` border, `surface` fill, brackets in a tone colour, default `primary`) without a header. When
`components.brackets` is on, `MiniPlayer` becomes a `GeoPanel` inset 12 dp from the screen edges with tonal elevation
0 (so `surfaceTint` cannot colour it), a primary-bordered artwork tile and ghost play/next buttons. The Now Playing
artwork gets brackets as well. Scroll behaviour and the expand gesture are unchanged.

## Bottom bar

With `navStyle` `block` on a compact width, `AppScaffold` shows `GeoNavigationBar` instead of Material's bar: four
tabs, the selected one a chamfered `primary` block holding an `onPrimary` icon and label, unselected tabs in
`onSurfaceVariant` with tracked caps at 12 sp. Navigation uses the same `popUpTo`/`launchSingleTop`/`restoreState`
as today. The scaffold's own bar is hidden while it is shown. On medium and expanded widths the Material rail stays.
Whether `NavigationSuiteScaffold` offers a slot overload is confirmed at implementation; if not, the scaffold's layout
type is set to none and the custom bar is placed in the existing `Scaffold` bottom.

## Rows

With `rowEdge`, the playing `SongRow` shows a 2 dp `primary` bar on its leading edge (drawn over the existing 16 dp leading padding, so alignment is unchanged), a `primary` tile border, and the duration uses the label style so it takes the
label font. Without the flag the row is unchanged.

## Backward compatibility

New fields are nullable in `SkinJson` and inherit from the Default skin. Built-in skins keep their values. An older app
opening a skin that uses the new fields shows its palette, fonts, radius and the Phase 1 look, and ignores the rest.

## Testing (written before the code)

- Parser: every default; each new field valid; each invalid value gives the exact message; boundaries for
  `seekSegments` 12 and 60 accepted, 11 and 61 rejected.
- Pure functions: `segmentsFilled` (0, 1, partial, full, clamping), the position percentage text, the track-code format,
  hexagon vertex geometry, bracket path segments.
- Ornament local: derived from a skin, defaults to all off.
- Roborazzi: new screenshots of a skin with everything on (Songs, Now Playing without artwork, mini player, bottom bar)
  in dark and light; this also closes the Phase 1 gap where no test renders a chamfer. Existing screenshots must pass
  unchanged.
- On the phone: import the updated `geofront.mskin`; check Songs, Now Playing with and without artwork, mini player and
  the bottom bar in dark and light at font scale 1.0 and 1.3, plus a seek by tap and drag.

## Documentation and skin

Update `docs/skins/FORMAT.md`, `docs/design/skin-design-brief.md` (fields, §3, §7, §9) with its HTML copy, the README
and `docs/FEATURES.md` test counts and the `docs/skins/geofront` sample README. Update `geofront.mskin` (repo sample and
the Android apps copy) to switch the new fields on and bump its version.

## Risks

The custom bottom bar duplicates behaviour Material provides (semantics, ripple, insets), so it is tested for
selected-state semantics and a minimum 48 dp touch target. The hexagon canvas must not recompose per frame. A
bracketed mini player must still read as distinct from the bottom bar (the container ramp already separates them).
