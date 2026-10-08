# Changelog

All notable changes to Naked Music Player. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and versions are the `versionName` in `app/build.gradle.kts` (each release also raises `versionCode`).

How to use this file: add a line under **Unreleased** in the same commit as the change, written for someone who uses the
app rather than for someone who reads the code. When releasing, rename **Unreleased** to the new version and date, add a
fresh empty **Unreleased** above it, and use that section as the GitHub release notes (see
[`docs/RELEASING.md`](docs/RELEASING.md)).

Sections: **Added** for new features, **Changed** for changes to existing behaviour, **Fixed** for bug fixes,
**Removed** for removed features, **Security** for anything that affects privacy or safety.

## [Unreleased]

## [0.1.0] - 2026-10-08

The first public release. A signed APK is attached to the
[GitHub release](https://github.com/aceattacker77/naked-music-player/releases/tag/v0.1.0).

### Added

- Local music library: Songs, Albums, Artists and Playlists, album and artist pages, search, four sort orders, an A–Z
  fast scroller, a minimum song length, excluded folders and extra folders to scan.
- Playback of MP3, FLAC, Ogg Vorbis, Opus, AAC/M4A and WAV, gapless, in the background with a media notification,
  lock-screen controls and headset buttons. The queue and position are restored and playback never starts by itself.
- Now Playing in five layouts (Classic, Compact, Minimal, Vinyl, Cassette), a mini player and a queue sheet.
- Skins: Default, Vinyl, Minimal and AMOLED Black, plus import and export of `.mskin` files. A skin can set colours, fonts,
  corner shape (round or chamfered), uppercase tracked labels, ornament (corner brackets, segmented meters, status tags,
  title cards, a square switch, glow) and a strings pack with a second line of text. The format is in
  [`docs/skins/FORMAT.md`](docs/skins/FORMAT.md); a full sample, Geofront, is in
  [`docs/skins/geofront/`](docs/skins/geofront/).
- Playlists with drag to reorder, swipe to remove with Undo, smart playlists and M3U import and export.
- Equalizer using the device's bands and presets, saved presets, bass boost and a preamp.
- Home-screen widget (4×1, resizing to 4×2 with progress, shuffle and repeat) that follows the active skin. An optional
  setting, **Settings → Widget → Live progress**, moves its progress bar while a song plays.
- No `INTERNET` permission, no accounts, no ads and no analytics.

### Known limits

Tested on one phone (Android 16). Gapless audibility, lock-screen artwork, Bluetooth edge cases and other devices are not
yet verified; see [`docs/FEATURES.md`](docs/FEATURES.md) and
[`docs/manual-test-checklist.md`](docs/manual-test-checklist.md).

[Unreleased]: https://github.com/aceattacker77/naked-music-player/compare/v0.1.0...HEAD
[0.1.0]: https://github.com/aceattacker77/naked-music-player/releases/tag/v0.1.0
