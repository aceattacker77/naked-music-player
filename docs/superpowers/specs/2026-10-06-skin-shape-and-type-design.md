# Skin shape and type language (Phase 1)

Status: draft for review. Scope is Phase 1 of the Geofront app-change brief; Phases 2 to 5 (ornament, status tags,
Equalizer, widget, font subset) are explicitly out of scope.

## Goal

Let a skin opt into a hard-edged shape and type language: chamfered corners, squeezed headings, tracked uppercase
labels and control buttons that follow the theme shape. Every new field is optional and defaults to today's behaviour,
so the built-in skins render exactly as before and older apps simply ignore the new fields. The skin `format` stays `1`
because unknown fields are already ignored.

## Success criteria

1. A skin with `shapes.cornerStyle: "chamfer"` draws Material buttons, chips, cards, menus and dialogs with cut corners.
2. A skin without any new field is pixel-identical to the current build (the existing Roborazzi screenshots pass
   unchanged).
3. `headingScaleX`, `labelCaps` and `labelLetterSpacingEm` change only heading and label text.
4. Now Playing controls follow the theme shape when asked, and `mixed` makes play solid and previous/next outlined.
5. An invalid value is rejected with one message and installs nothing; the message names the field.
6. `FORMAT.md`, the skin design brief (and its HTML copy) and the README test count describe the new fields.

## Non-goals

Corner brackets, hexagon placeholder, segmented seek bar, status tags and strings pack, title cards, panel component,
Equalizer restyle, widget changes, bottom-bar indicator shape, font subsetting. Material's own dialog and button text is
not uppercased.

## Schema

All fields optional.

| Field | Type / values | Default | Notes |
|---|---|---|---|
| `shapes.cornerStyle` | `round` · `chamfer` | `round` | |
| `shapes.chamferDp` | integer 1–32 | 10 | Base cut; used only when `cornerStyle` is `chamfer`. `cornerRadiusDp` is ignored in that case. |
| `typography.headingScaleX` | number 0.5–1.0 | 1.0 | Horizontal scale of display, headline and title text. |
| `typography.labelCaps` | boolean | false | Uppercases app-drawn labels (below). |
| `typography.labelLetterSpacingEm` | number 0–0.5 | 0 | Letter spacing on the three label styles. |
| `player.controls` | adds `mixed` | `filled` | Play filled with `primary`; previous and next outlined. |
| `player.controlShape` | `circle` · `theme` | `circle` | `theme` uses the theme's large shape. |

Errors use the existing form, for example `invalid value 'smooth' for 'shapes.cornerStyle'`,
`invalid value '0.2' for 'typography.headingScaleX'`, `invalid value '40' for 'shapes.chamferDp'`. A value of the wrong
JSON type (text where a number belongs) keeps the existing `skin.json is not valid JSON` message.

`Skin` gains matching fields with defaults equal to today (`cornerStyle`, `chamferDp`, `headingScaleX`, `labelCaps`,
`labelLetterSpacingEm`, `controlShape`) appended after the existing ones so current named-argument construction compiles.
`ControlsStyle` gains `MIXED`; a new `CornerStyle` and `ControlShape` enum are added. Export and install already carry the
whole skin directory, so no store change is needed.

## Shape

`ChamferShape(cut: Dp)` is a `Shape` returning `Outline.Generic`, so borders, clipping and `Surface(shape = ...)` follow
it. The top-right and bottom-left corners are cut; the other two stay square, matching the Geofront mockups. The cut is
clamped to half of the shorter side so a small component never inverts.

When the skin is chamfer, `AppTheme` supplies the five Material sizes as `ChamferShape` with cuts of 0.3, 0.5, 0.8, 1.0
and 1.4 times `chamferDp` (3, 5, 8, 10, 14 dp at the default 10). Otherwise the existing radius-based shapes are used,
unchanged.

Hard-coded shapes opt in only for chamfer skins, through one helper that returns the theme shape when the skin is
chamfer and the original shape otherwise: `AlbumArt` (default 8 dp), the album-detail artwork (16 dp) and
`SkinPreviewCard` (16 dp). Artist avatars stay circles. The Now Playing artwork stays governed by `player.artShape`.

## Typography

`withFontFamilies` becomes the single place that builds the skin's `Typography`: it applies fonts per role, a
`TextGeometricTransform(scaleX = headingScaleX)` to display, headline and title styles, and `letterSpacing` to label
styles. With default values the produced `Typography` equals today's.

Caps: one helper turns a label string uppercase when the skin has `labelCaps`; it is used at the app-drawn label sites:
the Songs sort label, bottom-bar tab labels, the Queue label, seek-bar times, the fast-scroller letters and the
Equalizer labels. Accessibility descriptions keep the original text.

## Controls

`PlayerControls` passes `MaterialTheme.shapes.large` as the `shape` of each icon button when `controlShape` is `theme`.
`mixed`: play/pause is a `FilledIconButton`; previous and next are `OutlinedIconButton` using the `outline` colour.
Sizes, glow and the 48 dp minimum touch target are unchanged.

## Backward compatibility

New fields are nullable in `SkinJson` and fall back to the Default skin's values. Built-in skins keep their current
values. An older app opening a skin that uses the new fields shows its palette, fonts and corner radius and ignores the
rest, with no error.

## Testing (written before the code)

- Parser: every default; each new field valid; each invalid value gives the exact message; chamfer ignores
  `cornerRadiusDp`.
- `ChamferShape`: for a given size the outline has the expected cut corners and clamps the cut.
- Typography: `headingScaleX` reaches only display, headline and title; letter spacing reaches only label styles;
  defaults leave `Typography` equal to today's.
- Caps helper: uppercases only when `labelCaps` is on.
- Controls: `mixed` and `controlShape` choose the expected button kinds and shape.
- Regression: full unit suite and `verifyRoborazziDebug` pass without re-recording existing screenshots.
- On the phone: import the updated `geofront.mskin`, check Songs, Now Playing, the mini player and the queue in dark
  and light at font scale 1.0 and 1.3.

## Documentation and skin

Update `docs/skins/FORMAT.md`, `docs/design/skin-design-brief.md` (fields, §4–§9) with its HTML copy, and the README and
`docs/FEATURES.md` test counts. Update `geofront.mskin` to turn the new fields on and bump its version.

## Open decisions deferred

Whether Geofront ships as a built-in skin, how kana accents are delivered, and the warning banner belong to later
phases and are not decided here.
