# Sample skin: Geofront

`geofront.mskin` is a ready-to-import skin: black and orange, hard-edged and instrument-panel in feel. It uses every
field the skin format offers for shape and type, so it doubles as a worked example of a full skin.

**Import it:** copy `geofront.mskin` to the phone, then **Settings → Skins → Import skin**. If a skin with the same
`id` is installed, the app asks before replacing it.

## What it sets

| Area | Value |
|---|---|
| Colours | `both`: separate dark and light palettes, every role listed, including the surface container family, so surfaces stay neutral under the orange accent |
| Shape | `cornerStyle` `chamfer`, `chamferDp` 10 (top-right and bottom-left corners cut) |
| Fonts | headings Shippori Mincho B1 ExtraBold, body Archivo Narrow Regular, labels IBM Plex Mono Regular |
| Type treatment | `headingScaleX` 0.8, `labelCaps` true, `labelLetterSpacingEm` 0.14 |
| Now Playing | solid background, square artwork with a border and a hexagon "No artwork" placeholder, segmented seek bar (40 green cells, with a `POSITION` header), `mixed` controls (play solid, previous and next outlined), `controlShape` `theme` |
| Chrome | `brackets` (bracketed mini player and artwork), `navStyle` `block` (chamfered selected tab), `rowEdge` (playing-row bar), `glow` `dark` (a soft glow on the live panels in dark mode) |
| Widget | the home-screen widget gets the frame and corner brackets, the hexagon tile, the tile border and a segmented progress bar from the same fields; it stays rectangular and in the system font |
| Accents | `statusTags`, `titleCards`, `panelHeader` and `squareSwitch` on, with a strings pack giving the kicker and tag words their kana (for example `Playing\|再生`, `Library\|曲目`, `Audio\|音響`) |
| Layout | `classic`, artwork on top |

The `id` is `com.nakedmusic.geofront`, the version is 1.5 and the author is Ace Attacker. See
[`../FORMAT.md`](../FORMAT.md) for every field.

## Fonts and licences

The archive bundles three font files, all published under the SIL Open Font License 1.1. The licence text for each
family, with its copyright notice, is in [`licenses/`](licenses/), fetched from Google's `google/fonts` repository.

| File in the archive | Family | Copyright notice | Licence text |
|---|---|---|---|
| `fonts/ShipporiMinchoB1-ExtraBold.ttf` | Shippori Mincho B1 | 2021 The Shippori Mincho Project Authors | [`shipporiminchob1-OFL.txt`](licenses/shipporiminchob1-OFL.txt) |
| `fonts/ArchivoNarrow-Regular.ttf` | Archivo Narrow | 2019 The Archivo Narrow Project Authors | [`archivonarrow-OFL.txt`](licenses/archivonarrow-OFL.txt) |
| `fonts/IBMPlexMono-Regular.ttf` | IBM Plex Mono | 2017 IBM Corp., Reserved Font Name "Plex" | [`ibmplexmono-OFL.txt`](licenses/ibmplexmono-OFL.txt) |

Archivo Narrow and IBM Plex Mono are used as published and are not renamed; Shippori Mincho is a subset (below).

The three licence texts are also inside `geofront.mskin` under `licenses/`, so they travel with the archive. The app
ignores files that `skin.json` does not reference, so they are not installed on the phone.

Shippori Mincho is a subset here: Latin, hiragana, katakana and 22 kanji (the 20 the strings pack uses, plus 承 and 認 for spare slots) (298 KB instead of 15 MB).
The app draws the kana accents in the heading font, so they appear in Mincho. Any other Japanese text, such as song
titles, still falls back to the system font; that is expected (see the skin design brief, section 5). The subset keeps
the font's name because the Shippori Mincho licence declares no Reserved Font Name; its licence text ships with it.
