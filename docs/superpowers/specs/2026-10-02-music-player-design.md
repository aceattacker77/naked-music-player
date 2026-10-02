# Lightweight Music Player — Design Spec

- **Date:** 2026-10-02
- **Status:** Draft — awaiting review
- **Working name:** Music Player (final name TBD before Play Store submission; does not affect implementation)

## 1. Intent

### What the user asked for
- A lightweight local music player for Android 16 whose core job is playing audio files with a smooth UI/UX.
- Primary use: a **music library** (albums/artists matter most).
- **May be published on the Play Store later** → must support older Android versions and Play policy.
- v1 extras: **playlists, equalizer, home-screen widget, full skin system (incl. importing skins from files)**.
- **Android Auto:** not in v1, but architecture must make it easy to add.
- Play **all popular audio formats**.

### Assumptions (confirmed by not being corrected)
- Local files only — no streaming, no accounts, no ads, no analytics.
- No `INTERNET` permission.
- Kotlin codebase.

### Success criteria
- Plays MP3, AAC/M4A, FLAC, Ogg Vorbis, Opus, WAV reliably, gapless between tracks.
- Cold start < 400 ms; 0 jank at 120 Hz scrolling a 5,000-song library.
- Release APK < ~6 MB; RAM < ~120 MB in normal use.
- Background playback with notification, lock-screen and Bluetooth controls.
- A third party can create a skin by editing a JSON file and import it without code changes.

### Out of scope for v1
Android Auto UI, FFmpeg decoder extension, sleep timer, ReplayGain, lyrics, crossfade, in-app skin editor, tag editing, streaming/cloud, iOS.

## 2. Platform & stack

| Item | Choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 (Expressive components where available) |
| Playback | AndroidX Media3: ExoPlayer + `MediaLibraryService` + `MediaController` |
| Navigation | Navigation Compose |
| Persistence | Room (playlists, play stats), DataStore (settings, last queue/position, EQ, active skin) |
| Widget | Jetpack Glance |
| Images | Coil 3 |
| DI | Manual (simple `AppContainer`), no Hilt/Koin |
| compileSdk / targetSdk | 36 (Android 16) |
| minSdk | 26 (Android 8.0) |
| Build | Single Gradle module `:app`, R8 full mode, Baseline Profile |

Android 16-specific behaviour: edge-to-edge (enforced), predictive back (enabled), adaptive layouts for large screens (orientation/resizability restrictions are ignored on large screens in Android 16).

## 3. Architecture

Single module, four package areas. Unidirectional data flow: **UI → ViewModel → PlayerConnection (MediaController) → PlaybackService → state flows back up.**

```
app/
  playback/   PlaybackService, ExoPlayer setup, audio focus, EQ binding, queue persistence, library tree
  library/    MediaStoreRepository, ContentObserver, folder scanner, models (Song, Album, Artist)
  data/       Room DB (playlists, play stats), DataStore (settings, session, EQ)
  ui/         Compose screens + ViewModels, PlayerConnection
  ui/theme/   Theme tokens (color, type, shape)
  ui/skins/   Skin model, parser/validator, loader, built-in skins, layout slots
  widget/     Glance widgets
```

### 3.1 playback/
- `PlaybackService : MediaLibraryService` owns one `ExoPlayer`.
  - `handleAudioBecomingNoisy = true`, `setAudioAttributes(music, handleAudioFocus = true)`.
  - Gapless playback (ExoPlayer default for supported formats using encoder delay/padding metadata).
  - Exposes a **browsable library tree** (root → Albums / Artists / Playlists / Songs) via `MediaLibrarySession.Callback`. This is what makes Android Auto a later add-on rather than a rewrite.
  - Media notification via Media3 `DefaultMediaNotificationProvider`.
  - Persists queue (media IDs), current index and position to DataStore on pause, track change, and `onTaskRemoved`; restores on start **without auto-playing**.
  - Supports playback resumption (`onPlaybackResumption`) so system/Bluetooth "play" resumes the last queue.
  - Stops self when paused and notification dismissed.
- `EqualizerController`: attaches `android.media.audiofx.Equalizer` + `BassBoost` to ExoPlayer's audio session ID; reapplies on session change; catches `UnsupportedOperationException`/`RuntimeException` and reports "unsupported".
- Error policy: on `PlaybackException` for an item → toast "Skipped – can't play X", mark item unplayable in memory, advance. 3 consecutive failures → stop.

### 3.2 library/
- `MediaStoreRepository` queries `MediaStore.Audio.Media` (`IS_MUSIC != 0`), applies filters (min duration default 30 s, excluded folders), maps to `Song`, groups into `Album`, `Artist`.
- Exposes `Flow<Library>`; re-queries on `ContentObserver` change (debounced 500 ms).
- Album art: `ContentResolver.loadThumbnail` (API 29+) / album-art URI fallback, loaded through a Coil fetcher with memory + disk cache cap.
- `FolderScanner`: for user-added folders (via SAF tree URI), calls `MediaScannerConnection.scanFile` for audio files MediaStore missed (e.g. `.opus`, files in `.nomedia`-free hidden paths).
- Sorting: title, artist, album, date added; album tracks by disc/track number.

### 3.3 data/
- Room entities:
  - `PlaylistEntity(id, name, createdAt, updatedAt)`
  - `PlaylistSongEntity(playlistId, songId, position)`
  - `PlayStatEntity(songId, playCount, lastPlayedAt)` — a play counts after 50% or 4 min.
- Deleted songs: filtered out at read time by joining against current library IDs; orphan rows pruned on library refresh.
- DataStore keys: theme mode, dynamic color, active skin ID, min duration, excluded/added folders, EQ state (enabled, preset, band levels, bass boost, preamp), last session.

### 3.4 ui/
- `PlayerConnection`: wraps one `MediaController`; exposes `StateFlow<PlayerUiState>` (current item, isPlaying, position (ticker only while visible), duration, queue, shuffle, repeat). Single source of truth for all screens and the mini player.
- ViewModels per screen; screens are stateless composables taking state + callbacks.

## 4. Screens & navigation

- **Top-level:** Songs, Albums, Artists, Playlists — `NavigationSuiteScaffold` (bottom bar on phones, rail on tablets/foldables). Search in top app bar; Settings in overflow.
- **Detail:** Album (art header + tracks by disc/track), Artist (albums row + all songs), Playlist (drag reorder, swipe remove with Undo).
- **Mini player** above nav bar; tap or swipe up → **Now Playing** via shared-element transition of artwork; swipe down to collapse. Predictive back collapses it.
- **Now Playing:** composed from skin-arrangeable slots: `Artwork`, `TrackInfo`, `SeekBar`, `Controls` (prev/play/next), `SecondaryControls` (shuffle, repeat, EQ, add-to-playlist), `QueueHandle`. Queue as bottom sheet with drag reorder / swipe remove.
- **Long-press song menu:** Play next, Add to queue, Add to playlist, Go to album, Go to artist.
- **Equalizer screen:** enable switch, preset dropdown, N band sliders (N from device), bass boost slider, preamp slider, "Save as preset", link to system audio effects panel if available.
- **Settings:** Theme (system/light/dark, dynamic color toggle), Skins (picker), Library (min duration, excluded folders, added folders, rescan), About (version, licenses).
- **Onboarding:** single rationale screen → request `READ_MEDIA_AUDIO` (33+) or `READ_EXTERNAL_STORAGE` (≤32); `POST_NOTIFICATIONS` on 33+. Denied → explanatory empty state with "Open settings". Partial/limited access handled gracefully.
- **Large screens:** list–detail two-pane for Albums/Artists/Playlists; Now Playing two-pane (art | controls+queue).

## 5. Theming & skin system

### 5.1 Rule
**No composable hard-codes a color, font, shape or player style.** Everything reads from `MaterialTheme` + `LocalSkin`.

### 5.2 Token levels
1. **Theme tokens:** `ColorScheme` (light + dark, or single fixed), `Typography` (optional custom font family), `Shapes`.
2. **Player style tokens (`SkinStyle`):**
   - `background`: `blurredArt` | `artGradient` | `solid` | `image` (bundled texture)
   - `artShape`: `square` | `rounded(radiusDp)` | `circle`; `artSpin`: bool (vinyl rotation while playing)
   - `seekBar`: `wavy` | `flat` | `thin`
   - `controls`: `filled` | `outlined` | `iconOnly`; `controlSize`: `small` | `medium` | `large`
   - `glow`/`shadow` toggles
   - `useArtColors`: bool (derive accents from artwork palette)
3. **Layout:** `layout.type` ∈ {`classic`, `vinyl`, `minimal`, `cassette`, `compact`} + `layout.slots` ordering/placement options (e.g. art top vs. left, controls position). Unknown/omitted slots use the layout's defaults.

### 5.3 Skin file format (`.mskin`)
A zip archive:
```
skin.json            (required)
images/*.png|webp    (optional: background, vinyl, cassette, button icon set)
fonts/*.ttf|otf      (optional)
```
`skin.json` (format v1) example:
```json
{
  "format": 1,
  "id": "com.example.neon",
  "name": "Neon",
  "author": "Someone",
  "version": "1.0",
  "colors": {
    "mode": "dark",
    "dark": { "primary": "#00E5FF", "background": "#0A0A12", "surface": "#141420", "onPrimary": "#000000" }
  },
  "typography": { "fontFamily": "fonts/Orbitron.ttf" },
  "shapes": { "cornerRadiusDp": 20 },
  "player": {
    "background": { "type": "artGradient" },
    "artShape": { "type": "rounded", "radiusDp": 28 },
    "artSpin": false,
    "seekBar": "wavy",
    "controls": "filled",
    "controlSize": "large",
    "glow": true,
    "useArtColors": false
  },
  "layout": { "type": "classic", "slots": { "artPosition": "top" } }
}
```
- Missing fields inherit from the **Default** skin (partial skins are valid).
- `colors.mode`: `light` | `dark` | `both` | `system`. Unlisted color roles are derived from `primary` via Material color generation.
- No executable content of any kind.

### 5.4 Import & validation
- Entry: Settings → Skins → Import → `ACTION_OPEN_DOCUMENT` (no storage permission).
- Validation (all must pass, otherwise one specific error and no state change):
  - archive ≤ 10 MB; no path traversal (`..`, absolute paths); ≤ 50 entries
  - `skin.json` present, parses, `format` supported, `id` valid, colors valid hex
  - referenced images exist, decode, ≤ 2048 px each side
  - referenced fonts exist and load via `Typeface`
- On success: extracted to `filesDir/skins/<id>/`; same `id` → prompt to replace.
- Export: any skin (built-in or imported) re-zipped and shared via `ACTION_SEND` / `ACTION_CREATE_DOCUMENT`.

### 5.5 Built-in skins
Default, Vinyl, Minimal, AMOLED Black — shipped as `.mskin`-equivalent assets parsed by the **same loader** (dogfoods the format).

### 5.6 Runtime
- Active skin parsed once at startup, cached; images downsampled to screen size; fonts cached.
- Switching skins recomposes immediately (no activity restart).
- Picker: grid of live preview cards; tap to apply; long-press imported skin → delete/export.
- Deliverable: `docs/skins/FORMAT.md` + an example skin in `docs/skins/example/`.

## 6. Playlists
- Create from Playlists tab, from song/album/artist "Add to playlist", or Now Playing.
- Rename, delete, drag reorder, swipe remove with Undo snackbar.
- Smart playlists (read-only): Recently added (30 days), Most played, Recently played.
- **M3U export:** `.m3u8` (UTF-8, `#EXTM3U`, `#EXTINF`), relative paths when possible, via `ACTION_CREATE_DOCUMENT`.
- **M3U import:** `ACTION_OPEN_DOCUMENT`; match by path (relative/absolute) → fallback title+artist+duration (±2 s). Report "N of M matched".

## 7. Equalizer
- Platform `Equalizer` + `BassBoost` on ExoPlayer audio session.
- Bands and preset names read from device at runtime.
- Custom presets saved in DataStore; state persists across enable/disable and app restarts.
- Preamp (−6 to 0 dB) applied as player volume scaling to avoid clipping when boosting.
- Unsupported device → EQ entry points hidden.

## 8. Widget (Glance)
- **4×1:** art, title/artist, prev/play-pause/next.
- **4×2:** + progress bar (static, updated on events), shuffle/repeat, tap opens app.
- Colors from active skin (dynamic color on 12+ when skin allows), skin corner radius.
- Updated by `PlaybackService` on media item / isPlaying / shuffle / repeat changes only.
- Actions sent via `MediaController`/service intents; play when idle → resume last session.

## 9. Error handling summary
| Situation | Behaviour |
|---|---|
| Unplayable file | Toast, mark, skip; stop after 3 consecutive |
| Permission denied / limited | Explanatory empty state + Open settings; work with granted subset |
| Empty library | Empty state with tips + "Add folder to scan" |
| Process death / reboot | Restore queue/position, never auto-play |
| Invalid skin / M3U | Specific message, no state change |
| EQ unsupported | Hide EQ UI |
| Logging | Local only (Logcat in debug); no network reporting |

## 10. Formats
- **Supported in v1 (no extensions):** MP3, AAC/M4A, FLAC, Ogg Vorbis, Opus, WAV, AMR, audio in MP4/MKV/WebM. ALAC where the device decoder supports it.
- **Future option:** Media3 FFmpeg extension (ALAC everywhere, AC-3/E-AC-3/DTS) — +2–5 MB per ABI.
- **Not supported:** WMA, APE, DSD.

## 11. Performance targets
- Cold start < 400 ms (release, Baseline Profile + startup profile).
- 0 dropped frames at 120 Hz scrolling 5,000 songs (stable `key`s, `contentType`, immutable models, thumbnail-size art).
- APK < ~6 MB (R8 full mode, resource shrinking, no unused deps).
- RAM < ~120 MB typical; Coil memory cache capped.
- No background work while paused; service stops when notification dismissed.

## 12. Testing
- **Unit (JVM):** MediaStore row → model mapping & grouping, filters, sort; M3U parse/export/matching; skin JSON parsing, defaults merge, validation (incl. malicious zips: traversal, oversized, bad fonts); queue/shuffle logic; EQ state model; play-stat counting rule.
- **Integration (instrumented):** Room DAOs; PlaybackService with fixture files (mp3, flac, ogg, opus, m4a, wav, corrupt.mp3) — plays, gapless transition, skip-on-error, session restore.
- **UI (Compose):** expand/collapse player, queue reorder, apply skin, playlist CRUD; screenshot tests for each built-in skin × light/dark.
- **Macrobenchmark:** cold start, library scroll jank; generates Baseline Profile.
- **Manual checklist:** Bluetooth headset buttons, incoming call, lock screen controls, widget, unplug headphones, predictive back, foldable/tablet layout, Android 8 device/emulator.

## 13. Play Store readiness (later, designed-in now)
- No `INTERNET`, minimal permissions → simple privacy policy / Data safety form ("no data collected").
- Foreground service type `mediaPlayback` declared.
- App signing via Play App Signing; target latest SDK.
- Android Auto later: add `automotive_app_desc.xml` + manifest metadata; library tree already exposed by `MediaLibraryService`.
