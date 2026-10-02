# Manual test checklist

Everything the automated suite cannot reach: real hardware, the system UI around the app, and
behaviour that depends on a real device's audio stack. Run it on a release build
(`./gradlew :app:installBenchmarkRelease`, or install the signed release APK) and tick items off.

**Status: not yet executed.** The project was built and unit-tested on a machine without an Android
device or emulator, so every box below is open. Record the device, Android version and date next to
each run.

| Run | Device | Android | Date | Result |
|---|---|---|---|---|
| 1 | | | | |

## Setup

- [ ] `scripts/make_fixtures.sh` (needs ffmpeg) then `scripts/seed_library.sh` to put 5,000 songs on the device.
- [ ] In the app: Settings → Library → Minimum song length → **Off** (the seed songs are 2 s long).
- [ ] Also keep a handful of real songs in different formats (mp3, flac, ogg, opus, m4a, wav) and one with no tags.

## Playback and system integration

- [ ] **Bluetooth headset buttons:** play/pause, next, previous work from the headset, also with the screen off.
- [ ] **Incoming call:** playback pauses on ring and does *not* resume by itself after the call ends unless it was playing before and focus returns (system behaviour for transient focus loss).
- [ ] **Lock screen / notification controls:** artwork, title, seek bar, play/pause, next, previous all work.
- [ ] **Unplug headphones** (wired or Bluetooth disconnect) while playing: playback pauses.
- [ ] **Notification dismissed while paused:** the service stops; nothing keeps running (check `adb shell dumpsys activity services io.github.aceattacker77.nakedmusicplayer`).
- [ ] **Never auto-plays:** force-stop the app mid-song, reopen: the queue and position are restored *paused*.
- [ ] **Process death:** play, then `adb shell am kill io.github.aceattacker77.nakedmusicplayer`, reopen from the launcher: queue and position restored, paused. Pressing Play resumes.
- [ ] **Resumption from Bluetooth:** with the app killed, press Play on the headset: the last queue starts.
- [ ] **Unplayable file:** put a corrupt `.mp3` in a queue between good songs: toast "Skipped – can't play …", next song plays, the row shows the can't-play icon. Three corrupt files in a row stop playback.
- [ ] **Deleted song in queue / playlist:** delete a queued song's file from a file manager: playback skips it; playlists hide it without a crash.
- [ ] **Gapless:** two consecutive tracks of a gapless album play with no audible gap.

## Library and permissions

- [ ] **First launch:** rationale → Allow → library appears. Denying shows "Open settings"; granting it there and returning refreshes the library.
- [ ] **Revoking permission while the app is in the background:** returning shows the permission state, no crash.
- [ ] **Limited access (Android 14+ "selected music" if offered):** the granted subset is shown.
- [ ] **Add music while the app is open:** it appears within a couple of seconds (ContentObserver).
- [ ] **Folder scanning:** add a folder containing an `.opus` file MediaStore skipped: it appears after the scan. Remove the folder: its persisted permission is released.
- [ ] **Scrolling 5,000 songs:** smooth, fast-scroller works, artwork loads without stutter.

## UI

- [ ] **Edge-to-edge:** content is not hidden behind the status bar, navigation bar or cutout on gesture and 3-button navigation.
- [ ] **Predictive back:** from a detail screen, from search, and from the expanded player (the player shrinks as you drag, then collapses).
- [ ] **Player expand/collapse:** the artwork animates between the mini player and Now Playing; swipe up on the mini player and swipe down on Now Playing both work.
- [ ] **Skins:** apply each built-in skin; art spin, wavy seek bar, vinyl and cassette layouts look right; switching is instant with no restart.
- [ ] **Skin import/export:** import `docs/skins/example` (zipped as `.mskin`), delete it; export a built-in skin and re-import the file after renaming its id.
- [ ] **Foldable / tablet:** Albums and Artists show list and detail side by side; Now Playing puts the artwork on the left in landscape.
- [ ] **Large font / display size:** nothing clipped on Settings, Now Playing and the equalizer.
- [ ] **TalkBack:** queue rows offer Move up / Move down / Remove actions; controls have labels.

## Equalizer

- [ ] **Audible change:** enable, push a band to +, hear it; presets change the sound; disabling restores flat sound but keeps the sliders.
- [ ] **Persists** across app restarts and across track changes.
- [ ] **Preamp** attenuates while enabled and is ignored while disabled.
- [ ] **No-equalizer device / emulator image:** the Now Playing EQ button and the Settings row are hidden and nothing crashes.

## Playlists

- [ ] Create, rename, delete; add from the song menu, from an album, from Now Playing.
- [ ] Drag to reorder; swipe to remove with Undo.
- [ ] **M3U:** export a playlist, then import the exported file: "N of N matched" and an identically ordered copy named with " (2)".
- [ ] Import an `.m3u8` made on Windows (CRLF, `C:\Music\…` paths) and one from another Android player.

## Widget

- [ ] Add the 4×1 widget and the resized 4×2 widget.
- [ ] Play, pause, next, previous work from the widget; shuffle and repeat on the 4×2.
- [ ] Tapping the widget body opens the app.
- [ ] Play on the widget with nothing playing resumes the last queue.
- [ ] Change skin: the widget recolours on its next update.
- [ ] Dynamic colour on Android 12+ when the active skin is the Default one.

## Performance and release (see the plan, Task 19)

- [ ] `./gradlew :app:generateBaselineProfile` produces `app/src/release/generated/baselineProfiles/baseline-prof.txt`.
- [ ] `./gradlew :baselineprofile:connectedBenchmarkAndroidTest` on a physical 120 Hz device: cold start median `timeToInitialDisplayMs` < 400; scroll `frameOverrunMs` P99 < 0.
- [ ] `adb shell dumpsys meminfo io.github.aceattacker77.nakedmusicplayer`: TOTAL PSS < 120 MB after browsing every tab and Now Playing with the 5,000-song library.
- [ ] `scripts/check_release.sh` passes (APK under 6 MB, no INTERNET). Last result on the build machine: 4.0 MB, no INTERNET.
- [ ] `./gradlew :app:connectedDebugAndroidTest` passes on an API 26 emulator and an API 36 emulator.
- [ ] Smoke-test the **minified release** build once: skins load (R8 keep rules for kotlinx.serialization), playlists, equalizer settings and navigation all work.
