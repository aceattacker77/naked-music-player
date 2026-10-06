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
| Now Playing | solid background, square artwork, flat seek bar, `mixed` controls (play solid, previous and next outlined), `controlShape` `theme` |
| Layout | `classic`, artwork on top |

The `id` is `com.nakedmusic.geofront`, the version is 1.2 and the author is Ace Attacker. See
[`../FORMAT.md`](../FORMAT.md) for every field.

## Fonts and licences

The archive bundles three font files, all published under the SIL Open Font License 1.1. The licence text for each
family, with its copyright notice, is in [`licenses/`](licenses/), fetched from Google's `google/fonts` repository.

| File in the archive | Family | Copyright notice | Licence text |
|---|---|---|---|
| `fonts/ShipporiMinchoB1-ExtraBold.ttf` | Shippori Mincho B1 | 2021 The Shippori Mincho Project Authors | [`shipporiminchob1-OFL.txt`](licenses/shipporiminchob1-OFL.txt) |
| `fonts/ArchivoNarrow-Regular.ttf` | Archivo Narrow | 2019 The Archivo Narrow Project Authors | [`archivonarrow-OFL.txt`](licenses/archivonarrow-OFL.txt) |
| `fonts/IBMPlexMono-Regular.ttf` | IBM Plex Mono | 2017 IBM Corp., Reserved Font Name "Plex" | [`ibmplexmono-OFL.txt`](licenses/ibmplexmono-OFL.txt) |

The font files are used as published and are not renamed.

The three licence texts are also inside `geofront.mskin` under `licenses/`, so they travel with the archive. The app
ignores files that `skin.json` does not reference, so they are not installed on the phone.

Shippori Mincho is a Latin-only subset here, so Japanese text in song titles falls back to the system font; that is
expected (see the skin design brief, section 5).
