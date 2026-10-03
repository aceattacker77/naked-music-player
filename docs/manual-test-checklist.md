# Manual test checklist

Everything the automated suite cannot reach: real hardware, the system UI around the app, and
behaviour that depends on a real device's audio stack. Run it on a release build
(`./gradlew :app:installBenchmarkRelease`, or install the signed release APK) and tick items off.

**Status: run 1 done over wireless adb on one phone (2026-10-03).** Ticked boxes were verified on the
device; a note after `—` says how, or what was *not* covered. Open boxes need hardware or a person
(Bluetooth headset, a phone call, TalkBack, a foldable, listening to the audio, display-size changes).

| Run | Device | Android | Date | Result |
|---|---|---|---|---|
| 1 | Xiaomi POCO M8 5G (25118PC98G, HyperOS) | 16 (API 36) | 2026-10-03 | 10/10 androidTest; minified release smoke-tested; items below ticked |

## Setup

- [ ] `scripts/make_fixtures.sh` (needs ffmpeg) then `scripts/seed_library.sh` to put 5,000 songs on the device. — *not done as written:* no ffmpeg here, so a 1,500-song library was generated with Python instead (1,500 two-second 8 kHz WAV files in 50 album folders, 48 MB, titles spread over A-Z, no artwork), pushed to `Music/NMPBulk`, indexed, tested and removed.
- [ ] In the app: Settings → Library → Minimum song length → **Off** (the seed songs are 2 s long).
- [ ] Also keep a handful of real songs in different formats (mp3, flac, ogg, opus, m4a, wav) and one with no tags.

## Playback and system integration

- [x] **Bluetooth headset buttons:** play/pause, next, previous work from the headset, also with the screen off. — reported working by the owner on their own headset (no detail given on screen-off use or with the app killed; the dead-process resumption path was separately verified with a media key).
- [ ] **Incoming call:** playback pauses on ring and does *not* resume by itself after the call ends unless it was playing before and focus returns (system behaviour for transient focus loss).
- [ ] **Lock screen / notification controls:** artwork, title, seek bar, play/pause, next, previous all work. — *partly:* notification checked with dumpsys (MediaStyle, public, title/artist, prev/play/next, session token); the test songs have no artwork, so artwork/seek bar on the lock screen were not seen.
- [ ] **Unplug headphones** (wired or Bluetooth disconnect) while playing: playback pauses.
- [ ] **Notification dismissed while paused:** the service stops; nothing keeps running (check `adb shell dumpsys activity services io.github.aceattacker77.nakedmusicplayer`). — *partly:* dismissing removed the notification, and the service is not foreground, but it stays started+bound while the app process lives (own player connection); swiping the app from recents did not stop it either. See the notes file.
- [x] **Never auto-plays:** force-stop the app mid-song, reopen: the queue and position are restored *paused*. — force-stop mid-song, reopened: queue restored paused (position was last saved at track start, by design).
- [x] **Process death:** play, then `adb shell am kill io.github.aceattacker77.nakedmusicplayer`, reopen from the launcher: queue and position restored, paused. Pressing Play resumes. — `am crash` while paused: reopened paused at the exact position (31 s); Play resumed. (`am kill` does not kill a bound service process.)
- [x] **Resumption from Bluetooth:** with the app killed, press Play on the headset: the last queue starts. — media-key PLAY with the process dead restarts the app and resumes the queue. This needed a fix (Media3 MediaButtonReceiver was missing from the manifest); tested with a media key, not a real headset.
- [x] **Unplayable file:** put a corrupt `.mp3` in a queue between good songs: toast "Skipped – can't play …", next song plays, the row shows the can't-play icon. Three corrupt files in a row stop playback. — skip toast shown, next song played, row shows the can't-play icon, three corrupt files in a row stop playback.
- [x] **Deleted song in queue / playlist:** delete a queued song's file from a file manager: playback skips it; playlists hide it without a crash. — queue played past a deleted file without a crash; playlist hiding of a deleted song was not tested.
- [ ] **Gapless:** two consecutive tracks of a gapless album play with no audible gap.

## Library and permissions

- [ ] **First launch:** rationale → Allow → library appears. Denying shows "Open settings"; granting it there and returning refreshes the library. — *partly:* the system notification prompt and library load were seen; the in-app rationale/denied screens could not be reached because HyperOS re-grants audio permission after `adb` revokes it. Try it by hand in system settings.
- [ ] **Revoking permission while the app is in the background:** returning shows the permission state, no crash. — *partly:* revoke killed the process and relaunch did not crash, but HyperOS re-granted the permission, so the permission-state screen was not seen.
- [ ] **Limited access (Android 14+ "selected music" if offered):** the granted subset is shown.
- [x] **Add music while the app is open:** it appears within a couple of seconds (ContentObserver). — files pushed and scanned appeared in search while the app was open (not timed).
- [x] **Folder scanning:** add a folder containing an `.opus` file MediaStore skipped: it appears after the scan. Remove the folder: its persisted permission is released. — added a folder with an .opus file via the tree picker: it appeared in the library; removing the folder released the grant. (MediaStore may have indexed the file too, so the 'skipped by MediaStore' case is not proven.)
- [ ] **Scrolling 5,000 songs:** smooth, fast-scroller works, artwork loads without stutter. — *partly, with ~1,570 songs, no artwork:* Songs list scroll 0.05% janky frames, p99 10 ms (same as the small library); Albums (50) and Artists tabs 0% janky; search and all four sort orders work; the fast scroller was **broken and is fixed** (see the notes), 11 of 11 letters then land correctly and Z and the last heading are reachable. Artwork loading at scale and the full 5,000 were not tested.

## UI

- [ ] **Edge-to-edge:** content is not hidden behind the status bar, navigation bar or cutout on gesture and 3-button navigation. — *partly:* verified on the phone in **portrait with 3-button navigation and a top-centre camera cutout** (status bar strip 128 px): an automated check of every on-screen text/control found nothing in the status-bar strip or under the navigation bar on Songs, Albums, Artists, Playlists, Search, Settings (top and bottom), Skins, About, Now Playing, Equalizer, the queue sheet (last row clears the nav bar), album and artist detail, and the add-to-playlist sheet; Now Playing, the queue and Settings were also checked by eye. **Gesture navigation was not tested** (it needs a system setting change) and neither were other cutout shapes.
- [ ] **Predictive back:** from a detail screen, from search, and from the expanded player (the player shrinks as you drag, then collapses). — *partly:* Back collapses the expanded player; the drag-to-shrink animation was not inspected.
- [ ] **Player expand/collapse:** the artwork animates between the mini player and Now Playing; swipe up on the mini player and swipe down on Now Playing both work. — *partly:* swipe-up on the mini player expands, the collapse button works; swipe-down and the artwork animation were not checked.
- [x] **Skins:** apply each built-in skin; art spin, wavy seek bar, vinyl and cassette layouts look right; switching is instant with no restart. — all four built-in skins and an imported one applied instantly with no restart (screens captured); art spin/wavy bar not judged by eye.
- [x] **Skin import/export:** import `docs/skins/example` (zipped as `.mskin`), delete it; export a built-in skin and re-import the file after renaming its id. — imported docs/skins/example as .mskin, applied, deleted (fell back to Default); exported Vinyl (valid zip with skin.json), re-imported with a new id. Note: Android appended .zip to the export name.
- [ ] **Foldable / tablet:** Albums and Artists show list and detail side by side; Now Playing puts the artwork on the left in landscape. — *partly:* only landscape on a phone: Now Playing puts the artwork on the left; its Queue button starts below the fold and needs a scroll. No foldable/tablet seen.
- [ ] **Large font / display size:** nothing clipped on Settings, Now Playing and the equalizer.
- [ ] **TalkBack:** queue rows offer Move up / Move down / Remove actions; controls have labels.

## Equalizer

- [x] **Audible change:** enable, push a band to +, hear it; presets change the sound; disabling restores flat sound but keeps the sliders. — reported fine by the owner by ear (no detail given on presets or on disabling restoring flat sound).
- [x] **Persists** across app restarts and across track changes. — equalizer on, band +8.7 dB and preamp -4.3 dB survived a force-stop (track-change persistence not checked).
- [x] **Preamp** attenuates while enabled and is ignored while disabled. — measured on the phone from the audio system's per-track gain (`dumpsys media.audio_flinger`), not by ear: equalizer off + preamp -2.9 dB -> 0 dB; switched on -> -2.9 dB on both channels; moving the slider while on -> -4.6, then -2.4 dB (the gain followed each value exactly); switched off -> 0 dB, slider value kept. Reset to 0.0 dB / off afterwards.
- [ ] **No-equalizer device / emulator image:** the Now Playing EQ button and the Settings row are hidden and nothing crashes.

## Playlists

- [x] Create, rename, delete; add from the song menu, from an album, from Now Playing. — create, add from the song menu, rename and delete (with the confirm dialog) verified; add from an album and from Now Playing were not tried.
- [x] Drag to reorder; swipe to remove with Undo. — verified on the phone, after three bugs were found and fixed (see the notes): drag down and up persists across reopen and a force-stop; swipe-remove after a reorder removes the swiped song; Undo brings the row back on screen; a second quick removal replaces the first Undo prompt.
- [x] **M3U:** export a playlist, then import the exported file: "N of N matched" and an identically ordered copy named with " (2)". — exported (valid #EXTM3U, UTF-8, durations, relative paths) and imported back: copy named "... (2)", same 3 songs, same order.
- [ ] Import an `.m3u8` made on Windows (CRLF, `C:\Music\…` paths) and one from another Android player. — *partly:* a CRLF + BOM + `C:\Music\...` file matched 2 of 3 (the missing entry was skipped); a playlist from another Android player was not tried.

## Widget

- [x] Add the 4×1 widget and the resized 4×2 widget. — both render (the 4×2 adds a progress bar, shuffle and repeat). Both were stuck on the loading spinner until the R8 keep rules were added (see the notes).
- [x] Play, pause, next, previous work from the widget; shuffle and repeat on the 4×2. — all verified on the phone. Shuffle and repeat icons update within a second; before a fix they stayed stale (the widget captured its state outside the composition).
- [x] Tapping the widget body opens the app. — opens MainActivity.
- [x] Play on the widget with nothing playing resumes the last queue. — with the app process crashed, Play on the widget restarted the app, restored the queue and played from the saved position.
- [x] Change skin: the widget recolours on its next update. — it recoloured as soon as the skin changed (Vinyl: reddish-brown background), before any playback update; Default restored afterwards.
- [x] Dynamic colour on Android 12+ when the active skin is the Default one. — on: wallpaper palette (blue, bg 42/48/66); off: the app's own palette (purple-grey, bg 51/46/64). A setting change shows on the widget at its next update. This needed a fix (see the notes).

## Performance and release (see the plan, Task 19)

- [ ] `./gradlew :app:generateBaselineProfile` produces `app/src/release/generated/baselineProfiles/baseline-prof.txt`. — *partly:* the generator test passes on the phone (67,646 rules) but the Gradle task reports failure before copying; the profile was copied by hand and is not committed.
- [ ] `./gradlew :baselineprofile:connectedBenchmarkAndroidTest` on a physical 120 Hz device: cold start median `timeToInitialDisplayMs` < 400; scroll `frameOverrunMs` P99 < 0.
- [ ] `adb shell dumpsys meminfo io.github.aceattacker77.nakedmusicplayer`: TOTAL PSS < 120 MB after browsing every tab and Now Playing with the 5,000-song library. — *partly:* 113 MB with ~1,570 songs, against 119 MB for the same routine with the small library (run-to-run it has ranged 98-119 MB), so library size made no visible difference, but the app sits close to the 120 MB line either way (Graphics ~43 MB, Native ~20 MB). Not measured with 5,000 songs.
- [x] `scripts/check_release.sh` passes (APK under 6 MB, no INTERNET). Last result on the build machine: 4.0 MB, no INTERNET. — last run 4.0 MB, no INTERNET.
- [x] `./gradlew :app:connectedDebugAndroidTest` passes on an API 26 emulator and an API 36 emulator. — the 10 androidTest tests pass on a real API 36 device (run with adb because the Gradle installer is blocked on Xiaomi); no API 26 run.
- [x] Smoke-test the **minified release** build once: skins load (R8 keep rules for kotlinx.serialization), playlists, equalizer settings and navigation all work. — launches, library, playback, skins, equalizer persistence verified; the skin and M3U import pickers were exercised on later builds. The widget was not run.
