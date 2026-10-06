# Naked Music Player

A lightweight, local-only music player for Android, written in Kotlin with Jetpack Compose. It plays the audio
files on your phone with a smooth interface, background playback, a skin system you can extend with a JSON file,
an equalizer, playlists and a home-screen widget.

- **Private by design:** no `INTERNET` permission, no accounts, no ads, no analytics.
- **Small:** the release APK is about 4 MB.
- **Skinnable:** four built-in skins, and anyone can write a new one as a `skin.json` and import it.
- **Android 8.0 and up** (`minSdk` 26), built for Android 16 (`targetSdk` 36).

> **Status: version 0.1.0, working and tested on one phone.** Everything below has been run on a Xiaomi POCO M8 5G
> (Android 16). It has not been signed for release, and some checks still need a person or other hardware.
> [`docs/FEATURES.md`](docs/FEATURES.md) says, feature by feature, what has and has not been verified.

## What it does

- **Library:** Songs, Albums, Artists and Playlists tabs; album and artist pages; search; four sort orders; an
  A–Z fast scroller; a minimum song length and excluded folders; extra folders to scan (for example `.opus` files
  the system missed). The library updates live.
- **Playback:** MP3, FLAC, Ogg Vorbis, Opus, AAC/M4A and WAV, gapless between tracks. Background playback with a
  media notification, lock-screen controls and headset or Bluetooth buttons. The queue and position are restored
  after the app is closed, and it never starts playing by itself. Unplayable files are skipped with a message.
- **Now Playing:** a mini player that expands into a full screen with a queue sheet, in one of five
  layouts (Classic, Compact, Minimal, Vinyl or Cassette) depending on the skin.
- **Skins:** Default (wallpaper colours on Android 12+), Vinyl, Minimal and AMOLED Black. Switching is instant.
  Import, export and delete `.mskin` files. The format is documented in [`docs/skins/FORMAT.md`](docs/skins/FORMAT.md).
- **Playlists:** create, rename, delete, drag to reorder, swipe to remove with Undo, smart playlists (Recently
  added, Most played, Recently played), and M3U import and export.
- **Equalizer:** the device's bands and presets, saved custom presets, bass boost and a preamp.
- **Widget:** a 4×1 strip that resizes to a 4×2 card with progress, shuffle and repeat, coloured by the active skin.

## Build and run

You need:

- **JDK 17**
- **Android SDK** with platform **37** and build-tools **36** (set `ANDROID_HOME`, or add `sdk.dir=...` to
  `local.properties`)
- Nothing else to install: the Gradle wrapper downloads Gradle 9.8.0.

The stack is Android Gradle Plugin 9.4.1, Kotlin 2.4.20, Compose BOM 2026.09.00 and Media3 1.11.1; every version is in
[`gradle/libs.versions.toml`](gradle/libs.versions.toml).

```bash
./gradlew :app:assembleDebug        # build a debug APK
./gradlew :app:installDebug         # install it on a connected phone or emulator
```

For anything performance-related, test a release-like build instead of the debug one:

```bash
./gradlew :app:installBenchmarkRelease   # minified, signed with the debug key, profileable
```

## Test

```bash
./gradlew :app:testDebugUnitTest         # 341 JVM and Robolectric tests, including Compose UI tests
./gradlew :app:verifyRoborazziDebug      # compare screenshots with the stored goldens
./gradlew :app:recordRoborazziDebug      # re-record the goldens after an intended visual change
./gradlew :app:lintDebug                 # Android lint
./gradlew :app:connectedDebugAndroidTest # 10 instrumented tests; needs a device (see the note below)
scripts/check_release.sh                 # builds the release APK; fails above 6 MB or if it asks for INTERNET
```

Notes:

- The unit tests run on a normal desktop JVM and need no phone. Goldens were recorded on Windows; re-record them
  if your machine renders differently.
- The instrumented tests use fixture audio files. They are checked in; `scripts/make_fixtures.sh` (needs `ffmpeg`)
  regenerates them. `scripts/seed_library.sh` puts a large test library on a device.
- `<applicationId>` below stands for the app's package id, set as `applicationId` in `app/build.gradle.kts`.
- On Xiaomi phones the system blocks the way Gradle installs test APKs. Install with `adb install -t` and start the
  tests with `adb shell am instrument -w <applicationId>.test/androidx.test.runner.AndroidJUnitRunner`.
- Anything done by hand (Bluetooth, calls, TalkBack, a foldable) is on the
  [manual checklist](docs/manual-test-checklist.md), which records the results of the runs so far.

## Project layout

```
app/src/main/java/<package path>/   (the package named in app/build.gradle.kts)
  playback/    PlaybackService (Media3), library tree, queue restore, skip policy, equalizer (playback/eq)
  library/     MediaStore reading, library model and sorting, folder scanner
  data/        Room (playlists, play stats), DataStore (settings, session, equalizer), M3U
  ui/          Compose screens and view models: library, search, player, playlists, equalizer, settings
  ui/skins/    Skin model, JSON parser, archive validation, store and manager
  ui/theme/    Colour, type and shape from the active skin
  widget/      Glance widget and its colours, state and actions
app/src/main/assets/skins/   the four built-in skins (the same format users import)
app/src/test/                unit, Robolectric and Compose UI tests, plus screenshot goldens
app/src/androidTest/         instrumented tests and audio fixtures
baselineprofile/             startup and scroll benchmarks, and the baseline profile generator
scripts/                     release check, fixture and test-library scripts
docs/                        see below
```

Dependencies are wired by hand through `AppContainer` (no DI framework); tests swap in fakes by subclassing it.

## Documentation

| Document | What it is |
|---|---|
| [`docs/FEATURES.md`](docs/FEATURES.md) | Every feature, how it behaves, and how far it has been verified; performance results; known limitations |
| [`docs/skins/FORMAT.md`](docs/skins/FORMAT.md) | The `.mskin` skin format, with an [example skin](docs/skins/example/README.md) |
| [`docs/design/skin-design-brief.md`](docs/design/skin-design-brief.md) | A UI reference for designing a custom skin: screens, colour roles, sizes, limits, reference screenshots |
| [`docs/manual-test-checklist.md`](docs/manual-test-checklist.md) | What automated tests cannot reach, with the results of each run on a phone |
| [`docs/superpowers/specs/`](docs/superpowers/specs/2026-10-02-music-player-design.md) | The design spec: intent, architecture, targets |
| [`docs/superpowers/plans/`](docs/superpowers/plans/2026-10-02-music-player.md) | The implementation plan the app was built from |
| [`docs/superpowers/execution-notes.md`](docs/superpowers/execution-notes.md) | Decisions taken while building, and each bug found on a device with its fix |

## Things to know before changing it

- **Release builds use R8.** Anything created by reflection needs a keep rule in `app/proguard-rules.pro` and a
  check on a device, because unit tests do not run R8. Skins, equalizer JSON, WorkManager and the widget's button
  actions have already needed this.
- **Widgets live in a long-running Glance session.** Read everything the widget shows inside the composition, not
  before `provideContent`, or it goes stale.
- **The baseline profile is not committed.** `:app:generateBaselineProfile` makes one on a connected device, but
  it is large, goes stale, and was not shown to help; `app/src/release/generated/` is git-ignored on purpose.

## Licence

Copyright 2026 Ace Attacker. Licensed under the [Apache License, Version 2.0](LICENSE); see [`NOTICE`](NOTICE).
