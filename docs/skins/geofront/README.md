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

The archive bundles three font files. All three are published under the SIL Open Font License 1.1:

| File | Family | Upstream |
|---|---|---|
| `fonts/ShipporiMinchoB1-ExtraBold.ttf` | Shippori Mincho B1 | Google Fonts |
| `fonts/ArchivoNarrow-Regular.ttf` | Archivo Narrow | Google Fonts |
| `fonts/IBMPlexMono-Regular.ttf` | IBM Plex Mono | IBM, via Google Fonts |

The OFL asks that its licence text and the fonts' copyright notices travel with any redistribution of the font files.
Take the full texts from each family's upstream page and keep them with this sample before publishing the repository.

Shippori Mincho is a Latin-only subset here, so Japanese text in song titles falls back to the system font; that is
expected (see the skin design brief, section 5).
