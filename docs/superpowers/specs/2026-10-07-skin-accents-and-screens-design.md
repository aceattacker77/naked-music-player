# Skin accents and screens (Phase 3)

Status: draft for review. Phase 3 of the Geofront app-change brief. It builds on the shape and type fields (Phase 1,
`2026-10-06-skin-shape-and-type-design.md`) and the ornament fields (Phase 2, `2026-10-06-skin-ornament-design.md`), both
merged. The widget (Phase 4) and the font subset (Phase 5) are later phases.

## Goal

Let a skin opt into bilingual status accents, per-screen title cards, panel headers with a glow, a square switch, and a
restyled Equalizer and skin picker. Every new field is optional and defaults to today's look, so the built-in skins render
exactly as before and older apps ignore the new fields. `format` stays `1`.

## Success criteria

1. A skin that sets none of the new fields is pixel-identical to the current build (existing Roborazzi screenshots pass
   unchanged).
2. A skin with everything on shows: status tags in place of the red "can't play" icon (struck-through title), Playing/Paused
   on Now Playing, "Enabled" in the Equalizer and "Active" on the skin card; title cards on the four tabs, Equalizer and
   Skins; the restyled Equalizer; the bracketed active skin card.
3. State is never carried by colour alone: every tag shows its English word, and the existing accessible names are kept.
4. Seeking, the Equalizer's band, bass and preamp controls, the switch and navigation keep their gestures and semantics.
5. An invalid value is rejected with one message in the existing style and installs nothing.
6. `FORMAT.md`, the skin design brief (and its HTML copy) and the README test count describe the new fields.

## Non-goals

The hazard warning banner (dropped: nothing depends on it), Settings screen title card (it keeps its Material top bar), the
widget, the font subset, Geofront as a built-in skin, a separate localised Japanese resource set.

## Schema

All fields optional.

| Field | Values | Default | Notes |
|---|---|---|---|
| `components.statusTags` | boolean | false | Status tags replace the red "can't play" icon (song rows, album detail, playlist detail, queue); Playing/Paused tag on Now Playing; "Enabled" on the Equalizer; "Active" on the selected skin card. |
| `components.titleCards` | boolean | false | A title card (kicker, large squeezed heading, 1 dp rule) replaces the app-name top bar on Songs, Albums, Artists, Playlists, and heads the Equalizer and Skins screens. |
| `components.panelHeader` | boolean | false | Panels get a header strip: title left, code right, a rule beneath. Used by the Equalizer's Bands panel. |
| `components.squareSwitch` | boolean | false | The Equalizer's switch is square (52×28 dp track, 22 dp square thumb, 1 dp border, `tertiary` when on). |
| `components.glow` | `off` · `always` · `dark` | `off` | A soft tinted glow around live panels (the mini player and the active skin card); `dark` draws it only in a dark scheme. Needs API 28 or later; older devices show no glow. |
| `strings` | map of key → `"English\|Kana"` | empty | See below. |

`components.segmentedMeters` (Phase 2) additionally gives the Equalizer segmented band columns, a segmented bass meter and
the preamp readout.

Errors use the existing form, for example `invalid value 'sometimes' for 'components.glow'`. The strings pack adds:
more than 20 keys → `strings has more than 20 entries`; a value longer than 64 characters →
`invalid value for 'strings.<key>': longer than 64 characters`; an empty English half →
`invalid value '<value>' for 'strings.<key>'`. Keys outside the whitelist are ignored (unknown fields are ignored
everywhere else too). A wrong JSON type keeps `skin.json is not valid JSON`.

## Strings pack

Whitelisted keys: `now_playing_status`, `now_playing_paused`, `unplayable_tag`, `eq_enabled_tag`, `skin_active_tag`,
`library_kicker`, `albums_kicker`, `artists_kicker`, `playlists_kicker`, `eq_kicker`, `skins_kicker`, `queue_kana`,
`sort_kana`, `import_kana`, `save_kana`. A value is `"English"` or `"English|Kana"`; the first `|` splits it. `Skin` carries
`strings: Map<String, SkinString>` with `data class SkinString(val english: String, val kana: String?)`. `AppTheme` provides
it through a `LocalSkinStrings` composition local whose default is empty. A composable `skinText(key, fallback: String)`
returns the skin's entry or the plain English string resource, so a skin without the pack stays readable. The English half
is the visible word and the accessible name; the kana is decoration and hidden from accessibility. Plain-English counterparts
are added as normal string resources.

## Status tags

`StatusTag(status, word, kana)`: an 8 dp circular lamp (the only round element), the word in tracked caps (label style), and
an optional 1 dp divider and kana. `StatusKind` maps to a colour role: `GOOD` (playing, enabled, active) → `tertiary`,
`BAD` (unplayable) → `error`, `PENDING` → `primary`, `INFO` → `secondary`. In the four places a song can be unplayable, with
`statusTags` on, the tag replaces the `ic_error_outline` icon, the title gets a strike-through, and the existing `cant_play`
string remains the accessible name. Now Playing shows `Playing` (GOOD) while playing and `Paused` (INFO) otherwise, top right;
the Equalizer shows `Enabled` when on; the selected skin card shows `Active`.

## Title cards

`ScreenTitle(kicker, title)`: the kicker in the label font, tracked caps, `primary`; the title in the heading style at about
44 sp (squeezed by `headingScaleX`); a 1 dp rule beneath. It marquees or wraps and never clips at font scale 1.3. With
`titleCards`, the four top-level screens and the Equalizer and Skins screens use it in place of the Material app bar; the
search and overflow buttons stay as 48 dp touch targets. Songs also shows the track count in label caps (a plural string)
and the sort control as a chamfered outlined button whose kana comes from `sort_kana`.

## Panel header and glow

`GeoPanel` gains optional `title` and `code` parameters; when given (and `panelHeader` is on) a header strip draws: title in
the label font and tone colour on the left, code in `onSurfaceVariant` on the right, a 1 dp rule in the tone colour beneath.
A `live` parameter adds the glow: a shadow tinted with the tone colour, drawn only when the glow setting allows it for the
current scheme (pure function `glowActive(mode, isDark)`) and the API level is 28 or later.

## Equalizer

With the matching fields on:

- **Switch:** `GeoSwitch` replaces `Switch`; it keeps `toggleable`/`Role.Switch` semantics and the `eq-switch` test tag.
- **Bands panel:** a `GeoPanel` titled "Bands" with the code `%02d CH` taken from the device's band count (never fixed),
  tone `secondary`.
- **Columns:** `VerticalSlider` keeps Material's `Slider` for gestures and semantics and draws 12 cells (3 dp gaps, 1 dp
  `outline` border, `secondary` fill) through the slider's track slot. The cell count is mapped from the device-reported
  millibel range through a pure function; no ±12 dB assumption. The value shows above in the label font, the frequency below.
- **Bass boost:** a segmented meter with the percentage to one decimal on the right.
- **Preamp:** a `Readout(label, value, unit)` shows the value in a large label font with a 1 dp baseline rule; the real
  `Slider` (−6 to 0 dB) stays underneath. Trailing zeros are kept (`-3.0`).
- **Buttons:** the preset, reset and save buttons take the themed shape in a chamfer skin, and Save is the one solid action.
  Material's buttons use a fully rounded default shape that the theme's shape set may not reach, so app-drawn buttons take the
  themed shape explicitly through the existing `shapeOr` helper; this is confirmed with a test before relying on it.

## Skin picker

The preview already renders the real Now Playing screen under the previewed skin, so it shows hexagons, brackets and the
segmented bar without new code; at small size the preview uses 14 cells. With the fields on, the selected card is a bracketed
`GeoPanel` (live, so it can glow) with the "Active" tag beneath; other cards keep a 1 dp `outlineVariant` border. The import
button is a chamfered outlined button with its kana from `import_kana`.

## Backward compatibility

New fields are nullable in `SkinJson` and inherit from the Default skin. Built-in skins keep their values. An older app opening
a skin that uses the new fields shows the earlier phases' look and ignores the rest.

## Testing (written before the code)

- Parser: every default; each field valid; each invalid value gives the exact message; strings pack limits (20 keys, 64
  characters, empty English), the first-`|` split, whitelist filtering, inheritance from defaults.
- Pure functions: status → colour role mapping, band cell mapping (range ends, a narrow range, a device range other than
  ±12 dB), the readout text with trailing zeros, `glowActive`, the track-count text, `skinText` resolution.
- Compose: tag semantics (English accessible name, kana hidden, the original `cant_play` name kept); `GeoSwitch` toggles and
  reports its state; a title card at font scale 1.3 does not clip; the band sliders still expose their description and
  progress; the Equalizer's `eq-switch` tag and controls exist in both styles; the picker's active card shows the tag.
- Roborazzi: new goldens for the Equalizer, a title card and the skin picker with an everything-on skin, dark and light.
  Existing goldens must pass unchanged.
- On the phone: import the updated `geofront.mskin`; check each tab's title card, a song that cannot play (if one exists),
  Now Playing's tag, the Equalizer (drag a band, switch, bass, preamp), the skin picker, at font scale 1.0 and 1.3.

## Documentation and skin

Update `docs/skins/FORMAT.md`, `docs/design/skin-design-brief.md` (fields, §3, §5, §9, §13) and its HTML copy, the README and
`docs/FEATURES.md` test counts, and the Geofront sample README. Update `geofront.mskin` (repo sample and the Android apps
copy) to switch the new fields on, with the brief's kana strings, and bump its version.

## Risks

The Equalizer's custom slider track must not break the slider's gestures or accessibility, so only visuals change and the
semantics are asserted by test. The title card replaces a Material app bar, so scroll behaviour, insets and the 48 dp touch
targets of the action buttons are checked. The glow draws a tinted shadow only on API 28 and later and only when allowed, so
it costs nothing otherwise. The kana glyphs fall back to the system font until Phase 5 re-subsets the heading font.
