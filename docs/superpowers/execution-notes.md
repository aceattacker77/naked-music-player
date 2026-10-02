# Execution notes — music player plan

Written when the plan (`plans/2026-10-02-music-player.md`) was executed on a machine with **no Android
device or emulator**. It records what was verified, what was not, and the judgement calls made along
the way. Branch: `feat/music-player`. Last full run: 321 JVM/Robolectric tests, 0 failures; Roborazzi
goldens verified; Android lint has no errors; release APK 4.0 MB with no `INTERNET` permission.

## Not verified (needs a device or emulator)

The plan's performance and on-device steps are **unverified**: nothing below has been run.

- All `androidTest` classes (compile only): `FormatPlaybackTest` (formats, gapless, skip-on-error through the service), `AndroidProbesTest`.
- Baseline profile generation and the startup/scroll benchmarks, so *cold start < 400 ms* and *0 dropped frames* are unmeasured.
- Memory (< 120 MB PSS with 5,000 songs) and the API 26 / API 36 emulator runs.
- Every item in `docs/manual-test-checklist.md` (Bluetooth, calls, lock screen, widget, foldable, TalkBack, audible equalizer…).
- The minified **release** build has not been run: R8 keep rules for kotlinx.serialization are untested at runtime.
- Real file-backed DataStore, `PlatformAudioEffectsBackend`, `FolderScanner`, the M3U/skin file pickers and widget actions.

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
