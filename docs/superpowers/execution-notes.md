# Execution notes — music player plan

Written when the plan (`plans/2026-10-02-music-player.md`) was executed on a machine with **no Android
device or emulator**. It records what was verified, what was not, and the judgement calls made along
the way. Branch: `feat/music-player`. Last full run: 321 JVM/Robolectric tests, 0 failures; Roborazzi
goldens verified; Android lint has no errors; release APK 4.0 MB with no `INTERNET` permission.

## Not verified (needs a device or emulator)

The plan's performance and on-device steps are **unverified**: nothing below has been run.

- Memory with **5,000 songs** (< 120 MB PSS) and the API 26 emulator run. The Gradle macrobenchmark runner does not work on this phone (see below), so the numbers below come from manual `adb` measurements, not `StartupBenchmark`/`ScrollBenchmark`.
- Every item in `docs/manual-test-checklist.md` (Bluetooth, calls, lock screen, widget, foldable, TalkBack, audible equalizer…).
- Real file-backed DataStore, `PlatformAudioEffectsBackend`, `FolderScanner`, the M3U/skin file pickers and widget actions.

## Verified on a device

- `androidTest`: 10/10 pass on a Xiaomi POCO M8 5G (Android 16, API 36) over wireless adb: `FormatPlaybackTest` (all six formats, gapless, skip-on-error through the service, give-up after three corrupt files) and `AndroidProbesTest`. Run by hand with `adb install -t` and `am instrument`, because the Gradle runner's install flag is blocked on Xiaomi.
- Two test bugs were found and fixed doing this: the player was built with the test APK's context (no application context), and the give-up test counted errors on the controller, which the service clears before it can see them.
- The debug app installs and launches, and the library and UI load after granting audio access.

- **Minified release build** (signed with the debug key): launches, library loads, playback via the service, all four skins render from `skin.json`, the equalizer opens with the phone's 5 real bands, and skin / EQ (+8.7 dB band, -4.3 dB preamp) / session survive a force-stop. No R8 problems found. Not checked: widget, lock screen, Bluetooth, calls, M3U/skin import pickers.
- **Baseline profile** generated on the phone (67,646 rules, ~4 min) and installed with the profileinstaller broadcast: `speed-profile`, reason `baseline`. The generated file is *not* committed (7.8 MB, untracked at `app/src/release/generated/baselineProfiles/baseline-prof.txt`); decide whether to commit it or trim it.
- **Cold start (manual `am start -W`, 12 runs, with profile): median ~525 ms (440-590, first run 726). The 400 ms target is NOT met on this POCO M8 5G.**
- **Startup breakdown (temporary instrumentation + A/B switches on the same benchmark build, interleaved, medians of 8; code reverted, not committed):** an empty Compose screen in this app's process already takes **~350 ms** here; theme/skin/settings add ~20-35 ms; the song list adds ~65-125 ms (album art ~6 ms, fast scroller ~14 ms); the rest of the scaffold ~0-20 ms. Removing the WorkManager and EmojiCompat startup initializers made no measurable difference (blank 357 vs 349 ms, full 506 vs 492 ms), so they were left alone. For reference the same measurement gives 537 ms for the stock Calculator and 964 ms for Settings on this phone. The first frame is the only slow one (~135 ms: ~91 ms measure/layout/composition on the main thread, ~30 ms issuing draw commands on the render thread); later frames are ~19 ms. So the 400 ms target is mostly a device floor, not app code; at best the app-owned part (~140 ms) could shave 60-100 ms.
- **Scroll (8 up/down swipes, 574 frames, 60 Hz): 1 janky frame (0.17%) by the current metric, p50 6 ms, p90 19 ms, p99 21 ms; 68 frames (11.9%) over 16.7 ms by the legacy metric. "0 dropped frames" is not strictly met.**
- **Memory: 98 MB total PSS** after scrolling, but with only 25 songs in MediaStore, so the 5,000-song target is unmeasured.
- **Gradle macrobenchmark runner fails here:** MIUI does not deliver the profile-install broadcast to a freshly installed, never-opened app ("install broadcast was not received"), and `:app:generateBaselineProfile` reports failure (runner exit code 1) after the generator test passes, before copying the profile into the app.

## Manual checklist run 1 (POCO M8 5G, Android 16, 2026-10-03) — findings

- **Bug found and fixed:** a headset/system "play" with the app process dead did nothing, because the manifest lacked Media3's `MediaButtonReceiver`, so `onPlaybackResumption` was unreachable. Fixed with `MediaButtonManifestTest` (red, then green) and re-verified on the phone (crash the process, press media PLAY: app restarts and resumes the queue). Tested with a media key, not a real Bluetooth headset.
- **Service does not stop on notification dismiss / swipe from recents** while the app process is alive: the spec says "stops self when paused and notification dismissed", but the app's own `MediaController` stays bound, so `stopSelf()` has no effect. The service is idle and not foreground; the system will reclaim the cached process. Left as is (minor).
- **Skin export is named `*.mskin.zip`:** Android appends `.zip` because the app asks for a zip MIME type. Re-import works either way. Minor.
- **Landscape Now Playing:** artwork left, controls right, but the Queue button starts below the fold (the control column scrolls). Minor.
- **Could not test from adb:** HyperOS re-grants the audio permission after `adb` revokes it, so the in-app rationale/denied screens were not seen on this phone.
- **Not done (need a person or hardware):** Bluetooth headset buttons, incoming call, unplugging headphones, audible equalizer/gapless, TalkBack, foldable/tablet, large font / display size (system settings), the 5,000-song library (no ffmpeg here), and the widget (adding it needs the launcher).

## Rulings (deviations from the plan, and why)

- **compileSdk 37, targetSdk 36.** Compose 1.12 / core-ktx 1.19 (latest stable) need compileSdk ≥ 37. Pinning older libraries would also have worked.
- **Compose UI tests run under Robolectric in `src/test`**, not as `androidTest`, so they execute without a device. `AppContainer` is `open` with open `by lazy` members so tests inject fakes.
- **DataStore tests use an in-memory test double**: the file-backed store fails on its second write on Windows JVM.
- **`LibraryViewModel` takes `Deferred<PlayerConnection>`** so the library renders while the service binds; `AppContainer` exposes `computeDispatcher` / `ioDispatcher` for deterministic tests.
- **Playlist pruning uses `LibraryRepository.allSongIds`** (unfiltered MediaStore ids, null after any failed load) so a stricter filter or revoked permission can never delete playlist entries.
- **`LibraryRepository.load` swallows non-permission failures** and keeps the last library (an escaped exception would have crashed the app process).
- **Albums match search by title only**; artists have their own result section.
- **`SkinArchiveReader.Valid.entries` holds only `skin.json` + referenced files.** The example skin omits the font from the spec example so it imports without one.
- **`BuiltInSkins.load` takes `(json, defaults)`** (the plan's `(String)` signature cannot say what to inherit from); `SkinManager` takes a reader, dispatcher and bundled-JSON lookup for exporting built-ins. Vinyl/Minimal dark schemes use lighter primaries for legibility.
- **`SkipPolicy` resets after returning STOP**, so a later play attempt is not stopped instantly.
- **Equalizer:** band edits are ignored until the device's band count is known; the preamp attenuates only while the EQ is enabled.
- **Dialogs use `usePlatformDefaultWidth = false`**: the default makes Robolectric's Compose host never idle when a dialog holds a text field; `AlertDialog` caps its own width, so the UI is unchanged.
- **`FolderScanner` uses `DocumentsContract` queries**, since `androidx.documentfile` is not an allowed dependency.
- **Excluded-folder choices = library folders ∪ already-excluded folders** (an excluded folder's songs vanish from the library, so it could otherwise never be re-included).
- **Skin picker previews are a scaled live `NowPlayingScreen`** under each skin's theme, with a golden image.
- **The baseline-profile module targets minSdk 28** (generation needs API 28+); the app stays at 26.
- Screenshot goldens render with `isPlaying = false`: a playing wavy seek bar / spinning art animates forever and Compose never goes idle.

## Things to look at before publishing

- The merged release manifest also holds `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED`, contributed by libraries (WorkManager via Glance, Media3). Review them for the Play Data-safety form; they were left alone rather than risk WorkManager.
- `targetSdk` is 36 as specified; Android lint notes that 37 exists.
- Lint warnings remain about `"%1$d of %2$d matched"` being a plural candidate (that string is fixed by the plan) and a few style nits.
- Goldens in `app/src/test/screenshots` were rendered on Windows; re-record them (`./gradlew :app:recordRoborazziDebug`) if CI renders differently.
