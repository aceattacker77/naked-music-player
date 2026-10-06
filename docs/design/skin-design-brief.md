# Naked Music Player: skin design brief

A reference sheet for designing a **custom skin** for Naked Music Player, an Android music player. It describes the
app's interface, exactly what a skin can and cannot change, the numbers a designer needs, and what has to be handed
back. Everything here was read from the app's code and tested on a phone; where a value is a default or a guess it
says so.

**Companion files:** [`reference/`](reference/) (screenshots rendered from the app with sample data),
[`../skins/FORMAT.md`](../skins/FORMAT.md) (the skin file format in full),
[`../skins/example/`](../skins/example/README.md) (a complete example skin).

## 0. The brief in one paragraph

Design one skin for a local-music player: a palette (light and/or dark), optional fonts, a corner-radius style, and
a look for the **Now Playing** screen (background, artwork shape, seek bar, controls, layout). The skin is delivered
as a **`skin.json`** plus optional images and font files, zipped and renamed to **`.mskin`**. A skin contains no
code. The person using the app imports it (Settings → Skins → Import skin) and it applies instantly everywhere.

**What to hand back:**

1. `skin.json` (valid against §9).
2. Any image and font files it references (limits in §8).
3. A short rationale and, if possible, preview images of Songs, Now Playing and the widget in light and dark.
4. The packaging steps filled in (see §11).

A ready-to-use prompt for the designer: *"Using this brief, design a skin called `<name>` with the mood `<mood>` and
the palette direction `<palette>`. Return `skin.json`, any assets, and previews. Respect every limit in §8 and §10."*
Fill the blanks from the decisions in §12.

## 1. The app in brief

- **Purpose:** plays the audio files on the phone. Local only: no streaming, accounts, ads or online content, so
  there are no brand, social or store elements to design.
- **Structure:** four top-level tabs (Songs, Albums, Artists, Playlists) in a bottom bar; a **mini player** above the
  bar that expands into **Now Playing**; a queue sheet; an equalizer screen; settings; the skin picker; and a
  home-screen widget.
- **Character:** a lightweight utility that is meant to be personalised through skins. The built-in Default skin is plain Material 3
  (Android's design system) and follows the phone's wallpaper colours on Android 12+. The library contains **Latin and Japanese
  titles** side by side, so type choices must cope with both (see §5).
- **Dark use is common.** Treat dark mode as a first-class design, not a derivative.

## 2. What a skin controls, and what it cannot

| A skin **can** change | A skin **cannot** change |
|---|---|
| The whole colour scheme of the app (every Material 3 colour role), separately for light and dark | Screen structure and navigation (tabs, bars, list layouts) outside Now Playing |
| Up to three font families (headings, body, labels) or one for all text | Icons (fixed vector set) and the strings |
| A base corner radius, or a chamfered (cut-corner) style, that shapes every component in the app (§6) | Animations and transitions (only the optional artwork spin) |
| Now Playing: background, artwork shape, artwork spin, seek bar style, control style and size, glow, shadow, whether colours follow the album art, and the layout (five choices, §7) | The widget's structure (its colours and corner radius follow the skin; its text does **not** use the skin's font) |
| Whether the skin follows the phone's wallpaper colours (`mode: "system"`) or is fixed light, dark or both | Notification, lock screen and system UI |

One important behaviour: on Now Playing, **`useArtColors: true` (the default) replaces the skin's palette** with one
generated from the current album's artwork colour. To make Now Playing show the skin's own colours exactly, set
`useArtColors` to `false`. Songs with no artwork fall back to the skin's palette either way.

## 3. Screens

Layouts are shown schematically; the numbers are in density-independent pixels (dp). Colour roles are named in
§4.

### 3.1 Songs (also the pattern for other lists)

```
┌──────────────────────────────┐
│ ‹Title›        🔍   ⋮        │  top app bar: app name, search, overflow (Settings)
│                  Title  ≡    │  sort bar: current sort + sort icon, right-aligned
│ ▢  Song title            3:36│  row: 48 dp artwork, title, "Artist · Album", duration
│    Artist · Album          A │  ← A–Z fast scroller strip down the right edge
│ ▢  Current song (accented)   │  the playing song's title uses `primary`
│ ▢  ⚠ Unplayable file         │  warning icon in `error`
│ ┌──────────────────────────┐ │
│ │▢ Title  Artist     ▶  ⏭ │ │  mini player (§3.2)
│ └──────────────────────────┘ │
│  ♪Songs ◉Albums ☺Artists ≡Playlists │  bottom navigation, selected tab in a pill
└──────────────────────────────┘
```

- Row: 16 dp horizontal and 8 dp vertical padding, 12 dp gap; artwork **48 dp**, rounded 8 dp; title `bodyLarge`
  (`onSurface`, or `primary` for the playing song); subtitle `bodyMedium` (`onSurfaceVariant`); duration `bodySmall`.
- Artwork placeholder (no art): a `surfaceVariant` tile with a music-note icon at half-strength `onSurfaceVariant`.
- The fast scroller is a 24 dp wide strip of single letters in `labelSmall`, colour `primary`. It lists every initial
  that exists, including non-Latin headings, so it can be **35 entries tall**: design letters to stay legible at about
  10–11 sp with roughly 40 px of height each.
- **Long-press** on a song opens a menu: Play next, Add to queue, Add to playlist, Go to album, Go to artist.

### 3.2 Mini player

A bar docked above the bottom navigation, with `surfaceContainerHigh` fill and 3 dp tonal elevation: 44 dp artwork,
title `bodyLarge`, artist `bodySmall` (`onSurfaceVariant`), play/pause and next icons, and a thin progress line on top.
Tap or swipe up to expand into Now Playing (the artwork animates between the two).

### 3.3 Albums, Artists and their detail screens

- **Albums:** an adaptive grid of cards, minimum 150 dp wide, artwork on top, album and artist below.
- **Artists:** a list; each row has a **48 dp circular avatar** in `secondaryContainer` with the artist's initial in
  `onSecondaryContainer`.
- **Album detail:** a 200 dp artwork (16 dp corners) header, a filled **Play** button and an outlined **Add to
  playlist** button, then the track list (the playing track in `primary`).
- **Artist detail:** a 96 dp circular avatar, a **Play** button, a row of 140 dp album covers, then all songs.

### 3.4 Playlists

A list that starts with three read-only smart lists (Recently added, Most played, Recently played), then "Your
playlists", with **New playlist** and **Import playlist** rows. A playlist opens to a track list with drag handles
(reorder) and swipe-to-remove, which reveals `errorContainer` with an `onErrorContainer` bin icon, followed by an
**Undo** snackbar (default Material inverse colours).

### 3.5 Equalizer

A **Switch** row to enable it, a **Preset** selector (outlined button with a menu), one **vertical slider per band**
(five on the test phone; the number comes from the device) labelled with its frequency and level, a **Bass boost**
slider, a **Preamp** slider (−6 to 0 dB) and a **Save as preset** outlined button. Standard Material switch and slider
colours: `primary` for the active part, `surfaceVariant`/`onSurfaceVariant` for tracks and labels.

### 3.6 Settings and the skin picker

- **Settings:** a list: Theme (three radio rows: System, Light, Dark), **Dynamic colour** switch, Skins, a Library
  section (minimum song length as four filter chips: Off, 15 s, 30 s, 60 s; excluded folders; folders to scan with
  **Add folder**; **Rescan now**), Equalizer, About. Section headers use `primary`.
- **Skin picker:** a two-column grid of **live preview cards**: each card is a miniature Now Playing rendered in
  that skin, with the skin name beneath and the active one outlined in its `primary`. Cards use `outlineVariant`
  borders. Long-press a card for Export and Delete. See [`reference/skin_picker_cards.png`](reference/skin_picker_cards.png).
  **The first thing a user sees of a new skin is its card, so the Now Playing design must read well at about
  170 dp wide.**

### 3.7 Home-screen widget

Two sizes, one widget (resizable): **4×1** (artwork, title and artist, previous / play-pause / next) and **4×2**
(adds a progress bar and shuffle and repeat toggles). Its background, text and icon colours come from the skin's
palette and its **corner radius equals `cornerRadiusDp`**. With the Default skin and Dynamic colour on, it follows
the phone's wallpaper colours instead. It sits on arbitrary wallpapers, so the background must be opaque enough to
read on its own.

## 4. Colour

A skin supplies a Material 3 colour scheme per mode. Anything not supplied is derived (see the rules below).

### 4.1 Modes (`colors.mode`)

| Value | Behaviour |
|---|---|
| `light` | Always the light scheme |
| `dark` | Always the dark scheme |
| `both` | Light or dark, following the app's Theme setting (and the phone, when set to System) |
| `system` | Android dynamic colour (the wallpaper palette) on Android 12+ **if** the Dynamic colour setting is on; otherwise falls back to the Default skin's colours |

The built-in Default skin uses `system`. A custom skin with a fixed palette should use `light`, `dark` or `both`.

### 4.2 Derivation rules

- If you set **`primary`**, every role you do **not** list is **generated from it** (Material colour generation) for that
  mode. Roles you list always win.
- If you do not set `primary`, unlisted roles come from the Default skin.
- Colours are `"#RRGGBB"` or `"#AARRGGBB"`.
- **Container roles:** the container family (`surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`,
  `surfaceContainerHigh`, `surfaceContainerHighest`), `surfaceDim`, `surfaceBright` and `surfaceTint` are settable. If you
  leave them out they are generated from `primary`. The mini player, the bottom bar, the queue sheet, cards and menus use
  them, so a strongly coloured `primary` (for example orange) tints those surfaces towards that hue unless you list them.
  **List the container roles whenever you want neutral surfaces with a vivid accent.**
- Because generation is automatic, a skin can be as small as one accent per mode. A designer who wants control should
  list at least `primary`, `onPrimary`, `background`, `onBackground`, `surface`, `onSurface`, `surfaceVariant`,
  `onSurfaceVariant` and `outline`, and check the rest visually.

### 4.3 Where each role appears

Roles marked *(default)* are used by standard Material components (bars, dialogs, switches, chips, snackbars) rather
than by this app's own code, but they appear on screen all the same.

| Role | Where it shows |
|---|---|
| `background` / `onBackground` | Screen background; Now Playing text and icons; the end of the Now Playing gradient and the solid background |
| `surface` / `onSurface` | Top bars, dialogs, sheets (default); primary text in lists; playlist and layout surfaces |
| `surfaceVariant` / `onSurfaceVariant` | Secondary text (artist, album, duration), secondary icons, seek bar track, artwork placeholder tile |
| `surfaceContainerLow` / `surfaceContainerHigh` | Queue sheet / mini player; bottom bar and cards use the container family *(default)*. Settable; generated from `primary` when omitted (§4.2) |
| `primary` / `onPrimary` | Playing-song title, fast-scroller letters, play button fill, seek bar progress, "on" states of shuffle and repeat, buttons, switches, filled chips *(default)*, section headers, the glow, the gradient start |
| `primaryContainer` / `onPrimaryContainer` | Tonal buttons and selected controls *(default)* |
| `secondaryContainer` / `onSecondaryContainer` | Artist avatars; the selected-tab pill in the bottom bar *(default)* |
| `tertiary*` | Not used directly by the app; set them anyway so the scheme stays coherent |
| `error` / `errorContainer` / `onErrorContainer` | The "can't play" warning icon; the swipe-to-remove background and its bin icon |
| `outline` / `outlineVariant` | Cassette and vinyl drawings; outlined buttons and dividers *(default)*; skin-card borders |
| `scrim` | Shading inside the cassette and vinyl layouts; behind sheets and dialogs |
| `inverseSurface` / `inverseOnSurface` / `inversePrimary` | The Undo snackbar *(default)* |

### 4.4 Contrast requirements

- Body text against its background: **4.5:1** or better; large text and icons: **3:1**.
- Check **both** schemes if `mode` is `both`.
- Check text over the Now Playing background (§7.1): image and blurred backgrounds get a 60% scrim of the
  `background` colour, which keeps text legible only if `onBackground` contrasts with `background`.
- The widget background must contrast with `onSurface` and `onSurfaceVariant` on its own.

## 5. Typography

- The app uses the standard **Material 3 type scale**; a skin cannot change sizes. It can change the **font family**
  and three treatments: `headingScaleX` (0.5–1.0, default 1.0) squeezes the display, headline and title styles
  horizontally, `labelCaps` uppercases the labels the app draws itself (sort label, bottom-bar tabs, Queue, seek-bar
  times, fast-scroller letters, Equalizer labels; not dialog or button text), and `labelLetterSpacingEm` (0–0.5,
  default 0 = Material's spacing) tracks the label styles. Check that a squeezed heading still reads at font scale 1.3.
- Where each style is used: list titles `bodyLarge`, list subtitles `bodyMedium`, durations and captions `bodySmall`, album-card titles `titleSmall`,
  Now Playing track title `headlineSmall`, artist `bodyLarge`, seek-bar times `labelMedium`, the queue button
  `labelLarge`, fast-scroller letters `labelSmall`. Top bars, dialogs, chips and buttons use the Material defaults.
- A skin can give **one font file** (`typography.fontFamily`) to every text style, or **separate files per role**:
  `headingFontFamily` (display, headline and title styles), `bodyFontFamily` (body styles) and `labelFontFamily` (label
  styles). A role without its own file uses `fontFamily`; with neither it keeps the system font. Each file is one weight:
  any bold or medium weight Material asks for is **synthesised** from it, so choose a family whose single weight reads well both as
  regular and as artificially emboldened, or accept regular weight throughout.
- Fonts bundled with a skin should be Latin. **Japanese (and other non-Latin) text falls back to the system font**,
  which will not match, so a decorative Latin font will sit beside system CJK in the same list. Prefer a font with
  neutral metrics, or tell whoever the skin is for that this is expected.
- The widget does not use the skin font.
- Test at the system font scale up to **1.3×**: long titles are single-line with an ellipsis, and the Now Playing title
  scrolls as a marquee when it does not fit.

## 6. Shape and spacing

**`shapes.cornerRadiusDp`** (default 28) is the base radius, and the app scales it into Material's five shape sizes:

| Shape size | Multiple of the base | With the default 28 | With 8 | With 16 |
|---|---|---|---|---|
| extraSmall | 0.25× | 7 dp | 2 dp | 4 dp |
| small | 0.5× | 14 dp | 4 dp | 8 dp |
| medium | 0.75× | 21 dp | 6 dp | 12 dp |
| large | 1× | 28 dp | 8 dp | 16 dp |
| extraLarge | 1.5× | 42 dp | 12 dp | 24 dp |

These drive buttons, chips, cards, sheets, dialogs and menus. The **widget's corner radius is the base value
itself**. The artwork's own corners are set separately by `player.artShape` (§7.2). Rows and the artwork placeholder
use a fixed 8 dp, or the theme's small shape when the skin is `chamfer`.

**Chamfer.** Set `shapes.cornerStyle` to `chamfer` for cut corners instead of rounded ones: the top-right and bottom-left
corners are cut at 45°, the other two stay square. `shapes.chamferDp` (1–32, default 10) is the base cut and scales like
the radius does: extraSmall 0.3×, small 0.5×, medium 0.8×, large 1×, extraLarge 1.4× (3, 5, 8, 10 and 14 dp at 10).
`cornerRadiusDp` is ignored for a chamfer skin. The widget keeps square corners (it cannot clip to a path).

Key sizes: list artwork 48 dp, mini-player artwork 44 dp, album-detail artwork 200 dp, artist avatar 48 dp (96 dp on
the artist page), page and row padding 16 dp, the standard minimum touch target 48 dp.

## 7. Now Playing in detail

Now Playing is assembled from slots: **Artwork**, **Track info** (title, artist · album), **Seek bar** (with elapsed
and total time), **Controls** (previous, play/pause, next), **Secondary controls** (shuffle, equalizer, add to
playlist, repeat) and a **Queue** button. The skin's `layout` only decides where they go. A collapse chevron sits at
the top, below the status bar.

### 7.1 `player.background`

| Type | What is drawn |
|---|---|
| `artGradient` (default) | A vertical gradient from `primary` at **35% opacity** at the top to `background` at the bottom |
| `blurredArt` | The album art, blurred (**48 dp** radius), tinted with `primary` at about 17%, then a **60%** `background` scrim. Falls back to `background` with no art |
| `solid` | Flat `background` |
| `image` + `path` | Your image (png or webp, §8), covering the screen, with the same **60% `background` scrim** over it. Design the image knowing 60% of the background colour will sit on top |

### 7.2 Artwork, seek bar and controls

| Field | Options | Notes |
|---|---|---|
| `artShape` | `square` · `rounded` (+ `radiusDp`, default 28) · `circle` | `circle` is meant for `artSpin` |
| `artSpin` | `true` / `false` | Rotates the artwork while playing; suits a circular shape |
| `shadow` | `true` / `false` | Drop shadow under the artwork |
| `seekBar` | `wavy` · `flat` · `thin` | See the table below |
| `controls` | `filled` · `outlined` · `iconOnly` · `mixed` | Style of previous, play/pause and next; `mixed` makes play solid and previous/next outlined |
| `controlShape` | `circle` · `theme` | `theme` gives the control buttons the theme's large shape (chamfered in a chamfer skin) |
| `controlSize` | `small` · `medium` · `large` | See the table below |
| `glow` | `true` / `false` | A soft radial glow in `primary` behind the play button |
| `useArtColors` | `true` / `false` | See §2: `true` regenerates the palette from the album art |

| Seek bar | Track height | Thumb | Touch area height |
|---|---|---|---|
| `wavy` | 4 dp stroke, 4 dp amplitude, 28 dp wavelength | 7 dp radius | 32 dp |
| `flat` | 4 dp | 7 dp radius | 24 dp |
| `thin` | 2 dp | 4 dp | 16 dp |

| Control size | Play button | Previous / next | Play icon | Side icons |
|---|---|---|---|---|
| `small` | 44 dp | 40 dp | 24 dp | 22 dp |
| `medium` | 60 dp | 48 dp | 32 dp | 28 dp |
| `large` | 76 dp | 56 dp | 40 dp | 34 dp |

Controls are spaced 16 dp apart; the secondary controls row is spaced 24 dp.

### 7.3 `layout.type`

| Type | Arrangement | Honours `artPosition`? |
|---|---|---|
| `classic` | Large artwork with everything else underneath (`top`, `center`) or beside it (`left`) | yes |
| `compact` | One dense column: a **112 dp** artwork beside the title, then seek bar, controls, extras and queue | no (fixed) |
| `minimal` | Typography first. `left`: a **64 dp** thumbnail beside the title; otherwise a **220 dp** square above it | yes |
| `vinyl` | The artwork sits on a record drawn with grooves and a spindle | yes |
| `cassette` | The artwork sits in the label window of a cassette drawn on a canvas | yes |

`artPosition` is `top`, `left` or `center`. **In landscape and on wide windows the artwork is always on the left**,
whatever the skin says, and the remaining controls scroll in a column on the right. Reference renders of all five
layouts are in [`reference/`](reference/): `layout_classic.png`, `layout_compact.png`, `layout_minimal.png`,
`layout_vinyl.png` and `layout_cassette.png`.

## 8. Assets and limits

- A skin is a zip (renamed `.mskin`) with `skin.json` at the **top level**, plus optional `images/` and `fonts/`.
- **Images:** `.png` or `.webp`, **at most 2048 px** wide or tall. They are only used as the Now Playing `image`
  background.
- **Fonts:** `.ttf` or `.otf`; one shared file and/or one each for headings, body and labels (§5). They count toward the 50-file limit.
- **Limits:** **10 MB** uncompressed in total, **50 files** at most. Only `skin.json` and the files it references are
  installed; anything else in the archive is ignored.
- **Safe paths only:** no `..`, no leading `/`, no backslashes, no drive letters.
- **`id`:** unique, at most 64 characters, lowercase letters, digits, `_` and `-`, in dot-separated groups, for
  example `com.example.neon`. Importing an existing `id` offers to replace it. Use your own name or handle as the author.
- Keep images small: they are decoded at screen size and sit behind a scrolling interface. Prefer a webp of a few
  hundred kilobytes.

## 9. The skin file: every field

A complete `skin.json` with **every field set to its default** (anything you omit takes this value, except the identity
fields, which are required). Colour roles shown are a subset; the roles accepted are the 36 listed in `FORMAT.md`.

```json
{
  "format": 1,
  "id": "com.example.myskin",
  "name": "My Skin",
  "author": "Your Name",
  "version": "1.0",
  "colors": {
    "mode": "system",
    "light": { "primary": "#6750A4", "onPrimary": "#FFFFFF", "background": "#FEF7FF", "surface": "#FEF7FF" },
    "dark":  { "primary": "#D0BCFF", "onPrimary": "#381E72", "background": "#141218", "surface": "#141218" }
  },
  "typography": { "fontFamily": "fonts/MyFont.ttf", "headingScaleX": 1.0, "labelCaps": false, "labelLetterSpacingEm": 0 },
  "shapes": { "cornerRadiusDp": 28, "cornerStyle": "round", "chamferDp": 10 },
  "player": {
    "background": { "type": "artGradient" },
    "artShape": { "type": "rounded", "radiusDp": 28 },
    "artSpin": false,
    "seekBar": "wavy",
    "controls": "filled",
    "controlShape": "circle",
    "controlSize": "medium",
    "glow": false,
    "shadow": false,
    "useArtColors": true
  },
  "layout": { "type": "classic", "slots": { "artPosition": "top" } }
}
```

Notes: the colour values above are illustrative (they are the Material baseline palette, not the Default skin's exact
values); omit `typography` to keep the system font (add `headingFontFamily`, `bodyFontFamily` and `labelFontFamily` for separate fonts); omit `colors.light` or `colors.dark` to inherit. The authoritative
table of fields, defaults and error messages is [`../skins/FORMAT.md`](../skins/FORMAT.md).

## 10. Built-in skins, for reference

| Skin | Mode | Accent (light / dark) | Radius | Background | Art | Seek bar | Controls | Layout |
|---|---|---|---|---|---|---|---|---|
| **Default** | system (wallpaper colours) | n/a | 28 | `artGradient` | rounded 28 | wavy | filled, medium | classic, top |
| **Vinyl** | both | `#C0392B` / `#E57368` | 24 | `blurredArt` | circle, spinning, shadow | flat | filled, large | vinyl, center |
| **Minimal** | both | `#455A64` / `#B0BEC5` | 8 | `solid` | square | thin | iconOnly, small | minimal, left |
| **AMOLED Black** | dark | `#BB86FC`, `background`/`surface` `#000000` | 16 | `solid` | rounded 16 | flat | outlined, medium | classic, top |

Vinyl, Minimal and AMOLED set `useArtColors` to `false`, so their palettes stay fixed; Default leaves it on, so its
Now Playing follows the album art.

Reference renders (sample data, no artwork, so the artwork tile shows the placeholder note), 411 × 891 dp:

| | |
|---|---|
| [`now_playing_default_dark.png`](reference/now_playing_default_dark.png) | [`now_playing_default_light.png`](reference/now_playing_default_light.png) |
| [`now_playing_vinyl_dark.png`](reference/now_playing_vinyl_dark.png) | [`now_playing_amoled_dark.png`](reference/now_playing_amoled_dark.png) |
| [`skin_picker_cards.png`](reference/skin_picker_cards.png) | the five `layout_*.png` files from §7.3 |

## 11. Reference device, testing and delivery

- **Reference phone:** 1080 × 2392 px at 450 dpi (about **384 × 850 dp**), a 60 Hz display, a camera hole at the top
  centre, 3-button navigation. Design for portrait widths of **360–412 dp**, landscape, and tablets or foldables;
  the reference renders above use 411 × 891 dp.
- **Edge-to-edge:** content draws behind the status and navigation bars. Backgrounds should extend to the screen
  edges and keep controls clear of the bars (the app handles the insets).
- **Performance:** keep images small and avoid designs that need many layers; scrolling must stay smooth on a
  mid-range phone.

**To hand over and test a skin:**

1. Put `skin.json` (and `images/`, `fonts/` if used) in a folder with `skin.json` at the top.
2. Zip the **contents** of the folder (not the folder itself) and rename the zip to `<name>.mskin`.
3. On the phone: Settings → Skins → **Import skin** and pick the file. Success applies at once; failure shows exactly
   one message from the table in `FORMAT.md` and changes nothing.
4. Check Songs, Now Playing (with and without artwork), the queue, the equalizer and the widget, in light and dark,
   at font scale 1.0 and 1.3.

## 12. Decisions to settle before designing

These are the choices a skin needs, written so they work whatever your process is. If you already have a brand guide,
a palette, a mood board, a design tool or a font you always use, take the answers from those and skip the question.
If you are designing for someone else, ask them; if you are designing for yourself, decide. The first four shape most
of the result, and each one maps to the skin fields named beside it.

1. **Mood and references** (guides every other choice): three words for the feel, for example "warm, analogue,
   quiet", and any apps, album covers or objects it should evoke.
2. **Palette** (`colors`): one or two anchor colours, or the mood to derive them from; light, dark or both
   (`colors.mode`); whether it should follow the wallpaper (`system`). Decide whether surfaces should be neutral or
   tinted by the accent (see the container roles in §4.2).
3. **Now Playing layout** (`layout`): classic, compact, minimal, vinyl or cassette, and where the artwork sits.
4. **Artwork treatment** (`player.artShape`, `artSpin`, `shadow`, `useArtColors`): square, rounded or circle;
   spinning or still; shadow; whether colours follow the album art or stay fixed.
5. **Fonts** (`typography`): one family for all text, or separate families for headings, body and labels, with the
   source and licence of each (it must allow bundling). Remember the Japanese fallback (§5).
6. **Shape language** (`shapes.cornerRadiusDp`, `cornerStyle`): crisp (radius 4–8), soft (16–24), pill-like (28 and up) or chamfered (cut corners, `chamferDp`).
7. **Background** (`player.background`): gradient, blurred art, flat colour, or a custom image and where it comes from.
8. **Extras** (`player.glow`, `controls`, `controlSize`): glow behind the play button; control style and size.
9. **Identity** (`id`, `name`, `author`): the display name, a unique `id` (see §8) and the author line.

## 13. Worked example: revising an existing skin (Geofront)

Use this pattern when the job is to **revise a skin that already exists** rather than start from §12. The decisions in
§12 are already made and recorded in the skin file, so the file is the source of truth: keep its identity, mood and
palette, and change only what the brief lists under "What to change". The example below revises Geofront
(`id` `com.nakedmusic.geofront`), a hard-edged black-and-orange skin, to use the container roles and separate fonts
that format 1 now supports. Supply the existing `.mskin` alongside this brief.

### 13.1 What Geofront is today

| Aspect | Current value |
|---|---|
| Mood | Black, hard-edged, instrument-panel look; orange primary, cyan secondary, green tertiary |
| Colour mode | `both` (separate dark and light palettes, both fully listed: 28 roles each) |
| Dark | `background` `#000000`, `surface` `#0B0B0E`, `surfaceVariant` `#17161C`, `primary` `#FF6A13` |
| Light | `background` `#ECE7DA`, `surface` `#F6F2E8`, `surfaceVariant` `#FFFFFF`, `primary` `#B3400A` |
| Shape | `cornerRadiusDp` 0 (every corner square) |
| Now Playing | solid background, square artwork, no spin, `flat` seek bar, `outlined` controls, `medium`, no glow or shadow, `useArtColors` false, layout `classic` with artwork at the top |
| Font | one file for everything: `fonts/ArchivoNarrow-Regular.ttf` (single weight; bold is synthesised) |

### 13.2 Known problems

1. **Orange-tinted surfaces.** Geofront sets `primary` but lists none of the container roles, so the app generates
   `surfaceContainerLowest`…`surfaceContainerHighest`, `surfaceDim`, `surfaceBright` and `surfaceTint` from the orange
   primary. The mini player, bottom bar, queue sheet, cards and menus therefore look brown instead of near-black
   (dark) or warm paper (light). Those roles are now settable (§4.2, §4.3).
2. **One font for everything.** Headings, body and labels all use Archivo Narrow Regular, so the hierarchy comes from
   size alone. Separate fonts per role are now possible (§5).

### 13.3 What to change

1. **Add the eight surface roles to both `dark` and `light`**: `surfaceDim`, `surfaceBright`,
   `surfaceContainerLowest`, `surfaceContainerLow`, `surfaceContainer`, `surfaceContainerHigh`,
   `surfaceContainerHighest`, `surfaceTint`.
   - Make them a **neutral ramp** from the skin's own `background`/`surface`/`surfaceVariant`, with no orange cast.
     Dark ramp runs from `#000000` up to about `#1F1E25`; light ramp from white down through warm paper tones.
   - Suggested starting point (adjust by eye): dark `Lowest #000000`, `Low #0B0B0E`, `Container #101015`,
     `High #17161C`, `Highest #1F1E25`, `Dim #000000`, `Bright #2A2931`; light `Lowest #FFFFFF`, `Low #F6F2E8`,
     `Container #F0EBDD`, `High #E9E3D3`, `Highest #E2DCCB`, `Dim #DDD7C8`, `Bright #F6F2E8`.
   - `surfaceTint` is the colour Material blends over raised surfaces; keep it equal to `primary` or make it neutral.
   - Keep the mini player (`surfaceContainerHigh`) visibly separate from the bottom bar and the screen behind it.
2. **Split the font by role.** Propose a pairing and bundle the files: a **heading** font (display, headline, title:
   Now Playing track title, album-card titles), a **body** font (lists, artist, durations) and a **label** font
   (seek-bar times, queue button, fast-scroller letters, buttons). Archivo Narrow may stay for one or more roles.
   Constraints: `.ttf`/`.otf`, Latin, a licence that allows bundling (state the licence and source for each), one
   weight per file (bold is synthesised), and keep file sizes small because the archive limit is 10 MB / 50 files.
   Check that a single-line title still fits at font scale 1.3 and that Japanese text falls back to the system font
   (§5) without looking broken beside the chosen faces.
3. **Re-check contrast** for every pairing that now changes (§4.4), especially `onSurface` on the new container
   fills and `primary` on `surfaceContainerHigh`.

### 13.4 What to keep

Everything not listed above stays as it is: the 28 existing colour roles in both modes, `shapes`, `player`, `layout`,
`id`, `name`, `author` and `version` (bump `version` to `1.1`).

### 13.5 Deliverable

A complete replacement `skin.json` (format 1; unknown fields are ignored, so do not add any) plus the font files in
`fonts/`, referenced by relative path, ready to zip into `geofront.mskin` (§11). With it:
a table of the final values for the eight new roles in both modes, the chosen fonts with their sources and licences,
and renders or notes for Songs, the mini player, the queue sheet and Now Playing in dark and light, at font scale 1.0
and 1.3.

Example of the new fields (values illustrative):

```json
"typography": {
  "fontFamily": "fonts/ArchivoNarrow-Regular.ttf",
  "headingFontFamily": "fonts/Heading.ttf",
  "bodyFontFamily": "fonts/Body.ttf",
  "labelFontFamily": "fonts/Label.ttf"
}
```

