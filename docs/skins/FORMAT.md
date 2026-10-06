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
`onErrorContainer`, `inverseSurface`, `inverseOnSurface`, `inversePrimary`, `scrim`.

Not settable (always generated from `primary`): `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`,
`surfaceContainerHigh`, `surfaceContainerHighest`, `surfaceDim`, `surfaceBright`, `surfaceTint`. They colour the mini
player, bottom bar, queue sheet, cards and menus, so a vivid `primary` tints those surfaces.

If you set `primary`, every role you do **not** list is generated from it (Material colour generation); roles you
list always win. If you do not set `primary`, unlisted roles come from the Default skin.

### `typography`

| Field | Type | Notes |
|---|---|---|
| `fontFamily` | string | Path inside the archive to a `.ttf`/`.otf` (e.g. `fonts/Orbitron.ttf`). Applied to all text. |

### `shapes`

| Field | Type | Default | Notes |
|---|---|---|---|
| `cornerRadiusDp` | integer | 28 | Base corner radius; the app's small→large shapes scale from it. |

### `player`

| Field | Type | Values | Default |
|---|---|---|---|
| `background` | object | `{"type": "blurredArt"}` · `{"type": "artGradient"}` · `{"type": "solid"}` · `{"type": "image", "path": "images/bg.webp"}` | `artGradient` |
| `artShape` | object | `{"type": "square"}` · `{"type": "circle"}` · `{"type": "rounded", "radiusDp": 28}` (`radiusDp` default 28) | rounded 28 |
| `artSpin` | boolean | rotate the artwork while playing (suits `circle`) | `false` |
| `seekBar` | string | `wavy` · `flat` · `thin` | `wavy` |
| `controls` | string | `filled` · `outlined` · `iconOnly` | `filled` |
| `controlSize` | string | `small` · `medium` · `large` | `medium` |
| `glow` | boolean | soft glow behind the play button | `false` |
| `shadow` | boolean | drop shadow under the artwork | `false` |
| `useArtColors` | boolean | derive accent colours from the album art | `true` |

### `layout`

| Field | Type | Values | Default |
|---|---|---|---|
| `type` | string | `classic` · `vinyl` · `minimal` · `cassette` · `compact` | `classic` |
| `slots.artPosition` | string | `top` · `left` · `center` (where it makes sense for the layout) | `top` |

On landscape and large screens the artwork is always placed on the left.

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
| Unknown enum value | `invalid value '<value>' for '<field>'` (e.g. `'player.seekBar'`) |
| Image background without a path | `missing field 'player.background.path'` |
| Referenced file has another extension than png, webp, ttf, otf | `unsupported file type '<path>'` |
| Referenced file not in the archive | `missing file '<path>'` |
| Image cannot be decoded | `image '<path>' could not be read` |
| Image wider or taller than 2048 px | `image '<path>' is larger than 2048 px` |
| Font cannot be loaded | `font '<path>' could not be loaded` |

Only `skin.json` and the files it references are installed; other files in the archive are ignored.
