# Skin format (`.mskin`, format version 1)

A skin is a zip archive. It changes colours, fonts, shapes and the look of the Now Playing screen. It contains
**no executable content** of any kind — only JSON, images and fonts.

```
skin.json            required
images/*.png|webp    optional (e.g. a background texture)
fonts/*.ttf|otf      optional
```

To create one, write a `skin.json`, zip it (with any images/fonts) so `skin.json` is at the **top level of the
archive**, and rename the zip to `<something>.mskin`. Import it from **Settings → Skins → Import skin**.
See [`example/`](example/README.md).

## Partial skins

Every field except `format`, `id` and `name` is optional. Anything you leave out is inherited from the
built-in **Default** skin, so a skin can be as small as:

```json
{ "format": 1, "id": "com.me.mine", "name": "Mine", "colors": { "mode": "dark", "dark": { "primary": "#FF8800" } } }
```

Unknown fields are ignored, which keeps skins forward-compatible.

## `skin.json` fields

### Top level

| Field | Type | Required | Notes |
|---|---|---|---|
| `format` | integer | yes | Must be `1`. |
| `id` | string | yes | Unique id, ≤ 64 chars, lowercase letters/digits/`_`/`-`, dot-separated groups (e.g. `com.example.neon`). Importing an existing `id` asks to replace it. |
| `name` | string | yes | Shown in the skin picker. |
| `author` | string | no | Default empty. |
| `version` | string | no | Default `"1.0"`. |
| `colors` | object | no | See below. |
| `typography` | object | no | See below. |
| `shapes` | object | no | See below. |
| `player` | object | no | Now Playing look. |
| `layout` | object | no | Now Playing arrangement. |
| `components` | object | no | Optional ornament switches (brackets, segmented meters, bottom bar style, playing-row edge, status tags, title cards, panel header, square switch, glow). See below. |
| `strings` | object | no | Short `"English\|Kana"` strings for a fixed set of slots. See below. |

### `colors`

| Field | Type | Values | Default |
|---|---|---|---|
| `mode` | string | `light` — always the light scheme · `dark` — always dark · `both` — light or dark following the app theme setting · `system` — Android dynamic colour (Android 12+, if enabled in Settings), otherwise the Default skin's colours | Default skin's mode (`system`) |
| `light` | object | colour roles for the light scheme | Default skin's light colours |
| `dark` | object | colour roles for the dark scheme | Default skin's dark colours |

A colour is `"#RRGGBB"` or `"#AARRGGBB"`. Roles you can set: `primary`, `onPrimary`, `primaryContainer`,
`onPrimaryContainer`, `secondary`, `onSecondary`, `secondaryContainer`, `onSecondaryContainer`, `tertiary`,
`onTertiary`, `tertiaryContainer`, `onTertiaryContainer`, `background`, `onBackground`, `surface`, `onSurface`,
`surfaceVariant`, `onSurfaceVariant`, `outline`, `outlineVariant`, `error`, `onError`, `errorContainer`,
`onErrorContainer`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, `scrim`, and the surface family
`surfaceDim`, `surfaceBright`, `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`,
`surfaceContainerHigh`, `surfaceContainerHighest`, `surfaceTint`. The container roles colour the mini player, bottom
bar, queue sheet, cards and menus. When you leave them out they are generated from `primary`, so a vivid `primary`
tints those surfaces; list them to keep those surfaces neutral.

If you set `primary`, every role you do **not** list is generated from it (Material colour generation); roles you
list always win. If you do not set `primary`, unlisted roles come from the Default skin.

### `typography`

| Field | Type | Notes |
|---|---|---|
| `fontFamily` | string | Path inside the archive to a `.ttf`/`.otf` (e.g. `fonts/Orbitron.ttf`). Applied to all text that has no font of its own below. |
| `headingFontFamily` | string | Font for headings: the `display`, `headline` and `title` styles (Now Playing track title, album-card titles). Falls back to `fontFamily`. |
| `bodyFontFamily` | string | Font for the `body` styles (list titles and subtitles, durations, artist). Falls back to `fontFamily`. |
| `labelFontFamily` | string | Font for the `label` styles (seek-bar times, queue button, fast-scroller letters, buttons). Falls back to `fontFamily`. |
| `headingScaleX` | number 0.5–1.0 | Horizontal scale of the `display`, `headline` and `title` styles; `0.8` squeezes headings to 80% width. Default `1.0`. |
| `labelCaps` | boolean | Uppercases the labels the app draws itself: the Songs sort label, bottom-bar tabs, Queue, seek-bar times, fast-scroller letters and the "No artwork" caption. Dialog and button text and unit labels (such as the Equalizer's `Hz` and `dB`) stay as written; the original text remains the accessible name. Default `false`. |
| `labelLetterSpacingEm` | number 0–0.5 | Letter spacing of the `label` styles in em; `0` keeps Material's own spacing. Default `0`. |

Each role font is a separate file in the archive, validated like `fontFamily`. A role with neither its own font nor
`fontFamily` keeps the system font.

### `shapes`

| Field | Type | Default | Notes |
|---|---|---|---|
| `cornerRadiusDp` | integer | 28 | Base corner radius; the app's small→large shapes scale from it. Ignored by the app's shapes when `cornerStyle` is `chamfer`, but the home-screen widget still uses it (it cannot chamfer): set it to 0 for a square widget. |
| `cornerStyle` | string | `round` | `round` or `chamfer`. A chamfer cuts the top-right and bottom-left corners at 45° instead of rounding them; every Material button, chip, card, menu and dialog follows it. |
| `chamferDp` | integer 1–32 | 10 | Base cut for `chamfer`. The five Material sizes use 0.3×, 0.5×, 0.8×, 1× and 1.4× of it (3, 5, 8, 10, 14 dp at 10). Artwork shown by the app (lists, album detail, skin cards) follows the chamfer too; the Now Playing artwork stays governed by `player.artShape`. |

### `player`

| Field | Type | Values | Default |
|---|---|---|---|
| `background` | object | `{"type": "blurredArt"}` · `{"type": "artGradient"}` · `{"type": "solid"}` · `{"type": "image", "path": "images/bg.webp"}` | `artGradient` |
| `artShape` | object | `{"type": "square"}` · `{"type": "circle"}` · `{"type": "rounded", "radiusDp": 28}` (`radiusDp` default 28) | rounded 28 |
| `artSpin` | boolean | rotate the artwork while playing (suits `circle`) | `false` |
| `seekBar` | string | `wavy` · `flat` · `thin` · `segmented` (a row of bordered cells) | `wavy` |
| `seekSegments` | integer 12–60 | number of cells in the `segmented` seek bar | `40` |
| `seekColor` | string | `primary` · `tertiary`: colour of the filled cells | `primary` |
| `artPlaceholder` | string | `note` · `hexagon`: what tiles show when there is no artwork (three nested hexagons and the note) | `note` |
| `artBorder` | boolean | a 1 dp `outline` border around every artwork tile; the highlighted tile (the playing row, the bracketed mini player) is always bordered in `primary`, even when this is off | `false` |
| `controls` | string | `filled` · `outlined` · `iconOnly` · `mixed` (play filled, previous and next outlined) | `filled` |
| `controlShape` | string | `circle` · `theme` (control buttons use the theme's large shape, so a chamfer skin gets chamfered buttons) | `circle` |
| `controlSize` | string | `small` · `medium` · `large` | `medium` |
| `glow` | boolean | soft glow behind the play button | `false` |
| `shadow` | boolean | drop shadow under the artwork | `false` |
| `useArtColors` | boolean | derive accent colours from the album art | `true` |

### The home-screen widget

The widget takes the skin's colours (it follows the system light/dark setting) and `cornerRadiusDp`, and reuses these fields with no extra ones: `components.brackets` (a 1 dp `outline` frame and corner brackets in `primary`), `player.artPlaceholder` `hexagon` (a hexagon-and-note in an empty artwork tile), `player.artBorder` (a 1 dp outline on the tile) and `player.seekBar` `segmented` (a segmented progress bar on the 4×2 widget, using `seekColor` and up to 33 of the `seekSegments` cells). It is never chamfered, always uses the system font, and shows no kana, because a widget cannot clip to a path or load a font file.

### `layout`

| Field | Type | Values | Default |
|---|---|---|---|
| `type` | string | `classic` · `vinyl` · `minimal` · `cassette` · `compact` | `classic` |
| `slots.artPosition` | string | `top` · `left` · `center` (where it makes sense for the layout) | `top` |

On landscape and large screens the artwork is always placed on the left.

### `components`

| Field | Type | Values | Default |
|---|---|---|---|
| `brackets` | boolean | corner brackets (top-left and bottom-right) on the Now Playing artwork; the mini player becomes a bracketed panel inset 12 dp from the screen edges | `false` |
| `segmentedMeters` | boolean | a `POSITION` label and the percentage (`31.3 %`) above a `segmented` seek bar | `false` |
| `navStyle` | string | `material` · `block`: `block` replaces the compact bottom bar with one whose selected tab is a block in the theme's medium shape (a chamfer in a chamfer skin). Windows wider than a phone held upright, including landscape phones and tablets, keep Material's rail | `material` |
| `rowEdge` | boolean | the playing row in lists gets a 2 dp `primary` bar on its leading edge and a `primary` tile border (even when `artBorder` is off), and durations use the label font | `false` |
| `statusTags` | boolean | status tags (a lamp, the status word in tracked caps, optional kana) replace the red "can't play" icon in song, album, playlist and queue rows (with the title struck through), and appear as Playing/Paused on Now Playing, Enabled in the Equalizer and Active on the selected skin card. The word is always shown and stays the accessible name | `false` |
| `titleCards` | boolean | a title card (a small kicker, a large squeezed heading and a 1 dp rule) replaces the app-name top bar on Songs, Albums, Artists and Playlists, and heads the Equalizer and Skins screens. Songs also shows the track count and a themed sort button. The heading shrinks toward 24 sp rather than clipping at large font sizes | `false` |
| `panelHeader` | boolean | panels (the Equalizer's Bands panel) get a header strip: the title on the left, a code such as `05 CH` (taken from the device's band count) on the right, and a rule beneath | `false` |
| `squareSwitch` | boolean | the Equalizer switch is a square 52 by 28 dp control with a 1 dp border, `tertiary` when on; it keeps the switch semantics | `false` |
| `glow` | string | `off` · `always` · `dark`: a soft tinted glow around live panels (the bracketed mini player and the active skin card); `dark` draws it only in a dark scheme. It needs Android 9 (API 28) or later and does nothing on older devices | `off` |

With `segmentedMeters` the Equalizer also draws segmented band columns (12 cells), a segmented bass boost meter with a percentage, and a large preamp readout that keeps trailing zeros (`-3.0`). Cell counts come from the device's reported range, never a fixed ±12 dB.

### `strings`

A map from a slot name to `"English"` or `"English|Kana"` (the first `|` splits them). The English half is the visible word and the accessible name; the kana is decoration and hidden from accessibility. A slot the skin leaves out shows plain English. At most 20 entries, each value at most 64 characters, the English half not empty; names outside this list are ignored.

| Slot | Where it shows |
|---|---|
| `now_playing_status`, `now_playing_paused` | the Playing / Paused tag on Now Playing |
| `unplayable_tag` | the Unplayable tag that replaces the red icon |
| `eq_enabled_tag` | the Equalizer's Enabled tag |
| `skin_active_tag` | the Active tag on the selected skin card |
| `library_kicker`, `albums_kicker`, `artists_kicker`, `playlists_kicker`, `eq_kicker`, `skins_kicker` | the kicker above each screen title |
| `sort_kana`, `import_kana`, `save_kana` | the accent on the sort, import and save-preset buttons: the kana half of `"English\|Kana"`, or the whole value when it has no `\|`, so `"順"` and `"Sort\|順"` both show `順` (`queue_kana` is reserved) |

The app draws kana and kanji from the `strings` pack in the skin's heading font. Glyphs that font lacks fall back to the system font, so a skin that wants them in its own typeface should bundle a heading font subset that includes them.

## Import limits and error messages

An import either succeeds completely or changes nothing and shows exactly one of these messages:

| Check | Message |
|---|---|
| Uncompressed size over 10 MB (counted while streaming, so zip bombs are rejected early) | `skin is larger than 10 MB` |
| More than 50 files | `skin has more than 50 files` |
| Entry path contains `..`, starts with `/`, contains `\`, or starts with a drive letter | `unsafe path '<name>'` |
| No `skin.json` at the top level | `skin.json not found` |
| `skin.json` is not valid JSON | `skin.json is not valid JSON` |
| `format` missing / not 1 | `missing field 'format'` / `unsupported format <n>` |
| `id` missing or invalid | `missing field 'id'` / `invalid id '<id>'` |
| `name` missing | `missing field 'name'` |
| Bad colour | `invalid colour '<role>': '<value>'` |
| Unknown enum value | `invalid value '<value>' for '<field>'` (e.g. `'player.seekBar'`, `'shapes.cornerStyle'`) |
| Strings pack too large / too long / empty | `strings has more than 20 entries` / `invalid value for 'strings.<key>': longer than 64 characters` / `invalid value '<value>' for 'strings.<key>'` |
| Number out of range | `invalid value '<value>' for '<field>'` (e.g. `'player.seekSegments'`, `'shapes.chamferDp'`, `'typography.headingScaleX'`, `'typography.labelLetterSpacingEm'`) |
| Image background without a path | `missing field 'player.background.path'` |
| Referenced file has another extension than png, webp, ttf, otf | `unsupported file type '<path>'` |
| Referenced file not in the archive | `missing file '<path>'` |
| Image cannot be decoded | `image '<path>' could not be read` |
| Image wider or taller than 2048 px | `image '<path>' is larger than 2048 px` |
| Font cannot be loaded | `font '<path>' could not be loaded` |

Only `skin.json` and the files it references are installed; other files in the archive are ignored.
