# Naked Music Player: features

What is implemented in the app today, how it behaves, and how far each piece has been checked.

**How to read the status lines**

- **Device** means it was exercised on a physical phone (Xiaomi POCO M8 5G, Android 16, wireless adb), either by
  the maintainer or by scripted taps and measurements.
- **Tests** means it is covered by the automated suite (JVM and Robolectric unit tests, Compose UI tests,
  screenshot goldens, and 10 instrumented tests that were run on the phone).
- **Not verified** means implemented but never seen working on hardware. The full list of open items is in
  [Known limitations](#known-limitations-and-open-items) and in [`manual-test-checklist.md`](manual-test-checklist.md).

Screenshots of each page are in the [README](../README.md#screenshots).

The design intent is in [`superpowers/specs/2026-10-02-music-player-design.md`](superpowers/specs/2026-10-02-music-player-design.md);
decisions taken while building, and every bug found on a device, are in
[`superpowers/execution-notes.md`](superpowers/execution-notes.md).

---

## Library and browsing

| Feature | Behaviour | Status |
|---|---|---|
| Songs / Albums / Artists / Playlists | Four top-level tabs in a bottom bar; Albums and Artists open detail screens (album tracks in disc/track order). | Device, Tests |
| Library source | Reads Android's media database (`MediaStore.Audio`, music only), groups songs into albums and artists. | Device, Tests |
| Live updates | New or removed files appear while the app is open (content observer, debounced 500 ms). | Device |
| Filters | Minimum song length (Off, 15, 30 or 60 s; default **30 s**) and excluded folders. | Device (length), Tests (folders) |
| Added folders | Pick a folder with the system folder picker; audio files the media database missed (for example `.opus`) are scanned and appear. Removing the folder releases its permission. **Rescan now** is in Settings. | Device |
| Sorting | Title, Artist, Album, Date added. | Device |
| Alphabet fast scroller | A–Z strip (plus non-Latin headings) on text sorts; tap or drag to jump. Hidden for Date added. | Device, Tests |
| Search | One search box over songs, albums and artists. | Device, Tests |
| Album artwork | Loaded from the media database with a capped memory and disk cache; a placeholder shows when there is none. | Device (placeholder), Not verified (real artwork on screen) |
| Large screens | List and detail side by side for Albums and Artists on wide windows. | Tests, **Not verified** on a foldable or tablet |
| Scale | A generated library of about 1,570 songs scrolled with the same smoothness as a small one. | Device (see [Performance](#performance-and-size)) |

## Playback

| Feature | Behaviour | Status |
|---|---|---|
| Engine | Media3 ExoPlayer inside a `MediaLibraryService`; one `MediaController` connection for the whole UI. | Device |
| Formats | MP3, FLAC, Ogg Vorbis, Opus, AAC/M4A, WAV (the platform's own decoders). | Device (instrumented test plays each format) |
| Gapless | Consecutive tracks transition with no playback error. | Device (no error); audible gap **not verified** |
| Notification and lock screen | Media-style notification with title, artist and previous, play/pause, next; public on the lock screen. | Device (notification contents); artwork and seek bar on the lock screen **not verified** |
| Headset and Bluetooth buttons | Play, pause, next and previous work. | Device (owner-verified) |
| Resume from a dead app | A headset or system "play" press restarts the app and resumes the last queue. | Device (verified with a media key), Tests |
| Restore | The queue and position survive a force-stop or process death, and the app **never starts playing by itself**. Whether shuffle and repeat are restored was not checked. | Device |
| Unplayable files | A file that fails shows "Skipped – can't play …", is marked in the list with a warning icon, and playback moves on. Three failures in a row stop playback. A file deleted while queued is skipped the same way. | Device, Tests |
| Audio focus and headphones | Pauses on becoming noisy (unplug) and respects audio focus. | **Not verified** (incoming call and unplugging not tried) |
| Queue | Bottom sheet showing the whole queue with drag handles; current track marked. Long-press a song for **Play next**, **Add to queue**, **Add to playlist**, **Go to album**, **Go to artist**. | Device (sheet, menu), Tests (reorder) |
| Play statistics | A play counts after 50% of the track or 4 minutes. Feeds the Most played and Recently played lists. | Tests |
| Service lifetime | No work while paused. The notification can be dismissed, but the idle service stays alive while the app's own process does (see limitations). | Device |
| Android Auto groundwork | The browsable library tree (Albums, Artists, Playlists, Songs) is exposed, so an Android Auto UI can be added later. No Auto UI exists. | Tests |

## Now Playing and mini player

| Feature | Behaviour | Status |
|---|---|---|
| Mini player | Sits above the bottom bar; play/pause and next; tap or swipe up to expand. | Device |
| Now Playing | Artwork, title and artist (scrolls if long), seek bar, previous/play/next, shuffle, equalizer shortcut, add to playlist, repeat, queue. | Device |
| Expand and collapse | Shared-element transition of the artwork; the collapse button and the system Back both collapse it. | Device (button, Back), Tests (transition); swipe down and the drag-back animation **not verified** |
| Layouts | Five arrangements chosen by the skin: Classic, Compact, Minimal, Vinyl and Cassette; artwork on top, centre or side where the layout allows. Landscape puts the artwork on the left. | Device (Classic, Vinyl, landscape), Tests |
| Seek bar styles | Wavy, flat or thin, by skin. | Device (wavy), Tests |
| Edge-to-edge | Content stays clear of the status bar, camera cutout and navigation bar; screens draw behind the bars where intended (for example the Now Playing gradient). | Device (portrait, 3-button navigation); gesture navigation **not verified** |

## Skins

| Feature | Behaviour | Status |
|---|---|---|
| Built-in skins | **Default** (follows the wallpaper colours on Android 12+), **Vinyl** (spinning record), **Minimal**, **AMOLED Black**. | Device, Tests (screenshot goldens) |
| Picker | Grid of live preview cards; tapping applies instantly with no restart. | Device |
| Import | **Settings → Skins → Import skin** reads a `.mskin` (a zip with `skin.json`, optional images and fonts). Partial skins are fine: unspecified values come from Default. | Device |
| Validation | Rejects path traversal, oversized archives, unsupported file types and bad values with a specific message; nothing changes on failure. | Tests |
| Export and delete | Long-press a card: Export (any skin, including built-ins) or Delete (imported skins). Deleting the active skin falls back to Default. | Device |
| Specification | [`skins/FORMAT.md`](skins/FORMAT.md) documents every field; [`skins/example/`](skins/example/README.md) is a complete example and [`skins/geofront/`](skins/geofront/README.md) a full sample skin with fonts. | Reviewed |

## Playlists

| Feature | Behaviour | Status |
|---|---|---|
| Manage | Create, rename and delete (delete asks first and says the songs are kept). Add from the song menu, an album or Now Playing. The same song may appear twice. | Device (song menu, rename, delete), Tests |
| Reorder | Drag a handle; the order is saved. | Device, Tests |
| Remove with Undo | Swipe a row away; a snackbar offers **Undo**. Only the newest removal can be undone. | Device, Tests |
| Smart playlists | Recently added (30 days), Most played, Recently played; read-only. | Tests; listed on device, not opened |
| M3U export | `.m3u8`: UTF-8, `#EXTM3U`, `#EXTINF` with duration and "artist - title", paths relative to storage root, saved through the system file dialog. | Device, Tests |
| M3U import | Matches by path (relative, absolute, `file://`, Windows-style) and falls back to title + artist + duration (±2 s). Tolerates a byte-order mark, CRLF and playlists without `#EXTINF`. Names clash as "Name (2)". Reports how many entries matched. | Device (own export, a Windows-style file), Tests; a playlist from another Android player **not tried** |

## Equalizer

| Feature | Behaviour | Status |
|---|---|---|
| Bands | One slider per band the device's audio effect reports (five on the test phone), with the centre frequency shown. | Device |
| Presets | The device's presets plus **Custom**; **Save as preset** stores your own. | Device (screen), Tests |
| Bass boost | Slider, 0–100%. | Device (screen) |
| Preamp | −6 to 0 dB, applied as player volume. It only takes effect while the equalizer is **on** and is ignored while off; the slider value is kept. | Device (gain measured at 0 dB off and exactly the slider value on) |
| Persistence | On/off, preset, band levels, bass boost and preamp survive restarts. | Device |
| Sound | Changing a band audibly changes the sound. | Device (owner-verified) |
| No equalizer available | The equalizer entry points are hidden and nothing crashes. | Tests; **not verified** on such a device |

## Home-screen widget

| Feature | Behaviour | Status |
|---|---|---|
| 4×1 | Artwork, title and artist, previous / play-pause / next. | Device |
| 4×2 | Adds a progress bar, shuffle and repeat. Resizes between the two. | Device |
| Actions | Buttons control playback; shuffle and repeat show their state within about a second; tapping the card opens the app. **Play** with the app process dead restarts the app and resumes the last queue. | Device |
| Colours | Follows the active skin and its corner radius; on Android 12+ with the Default skin and **Dynamic colour** on, it uses the wallpaper palette. A skin change recolours it immediately. | Device |
| Live progress | Optional (**Settings → Widget → Live progress**, off by default): while a song plays the playback service also pushes the position about once per bar step (one step of a 40-step bar, between 2 and 10 seconds), so the bar moves without opening the app. Costs a little battery. | Tests, Device |

Without live progress, updates are pushed when the track, play state, shuffle or repeat changes, and the bar shows the
position at the last of those events. It is never redrawn on every position tick.

## Settings, permissions and onboarding

- **Settings:** Theme (System / Light / Dark), Dynamic colour, Skins, Library (minimum song length, excluded
  folders, folders to scan, rescan), Equalizer, About (version and open-source licences).
- **Permissions:** audio access (`READ_MEDIA_AUDIO` on Android 13+, `READ_EXTERNAL_STORAGE` before) and
  notifications on Android 13+. The app has **no `INTERNET` permission**: nothing leaves the phone, there are no
  accounts, ads or analytics. Libraries add `ACCESS_NETWORK_STATE`, `WAKE_LOCK` and `RECEIVE_BOOT_COMPLETED` to the
  merged manifest; review these before filling in a Play data-safety form.
- **First launch:** a short explanation, then the system permission prompt. If audio access is denied the app
  shows an explanation with an **Open settings** button and refreshes when you return. (Tests; on the test phone the
  system re-grants the permission after `adb` revokes it, so the denied screens were **not verified** on hardware.)

---

## Performance and size

Measured on the test phone with the release-like build. Targets come from the design spec.

| Measure | Target | Result |
|---|---|---|
| Release APK size | under about 6 MB | **4.0 MB**, no `INTERNET` (checked by `scripts/check_release.sh`) |
| Cold start | under 400 ms | **Not met: about 430–525 ms** (medians of several 8–12 run sets; individual runs 265–590 ms). A blank screen in this app's process already takes about 350 ms here, and a stock calculator app takes about 540 ms on the same phone, so most of the gap is the device. About 140 ms is the app's own work (theme and skin setup about 30 ms, the first song list about 65–125 ms). |
| Scrolling | no dropped frames | **Close:** about 0.05% janky frames over about 1,800 frames, 99th-percentile frame 10 ms, on a 60 Hz display. Albums and Artists tabs: 0% janky. |
| Memory | under about 120 MB | **Borderline:** 98–119 MB total PSS across runs, 113 MB with about 1,570 songs. Library size made no visible difference at that size; 5,000 songs were not tried. |
| Larger library | smooth with 5,000 songs | Tried with about 1,570 songs (generated, no artwork): no scaling problem in loading, scrolling, search or sorting. |

The startup and scroll targets were also written as Macrobenchmark tests, and a baseline profile can be generated
on a device. The Gradle benchmark runner did not work on the test phone (Xiaomi blocks the install and the
profile broadcast), so the figures above come from manual measurements. The generated profile is deliberately not
committed (see the README).

## Testing

- **560 JVM and Robolectric tests** pass: library building, sorting and filters, M3U parsing and matching, skin
  parsing and validation, the equalizer model, play statistics, Room DAOs, view models, Compose UI flows (player,
  playlists, queue, skins, widget content), and a manifest check for the media-button receiver.
- **Screenshot goldens** for the built-in skins and key screens (Roborazzi).
- **10 instrumented tests** (all six formats, gapless, skip-on-error and give-up through the service, plus
  platform probes) passed on the physical phone.
- **Manual checklist:** [`manual-test-checklist.md`](manual-test-checklist.md), run on the phone on 2026-10-03,
  with how each item was checked and what is still open.

## Known limitations and open items

Behaviour that is deliberate or minor:

- The playback service stays alive (idle, not in the foreground) after the notification is dismissed or the app
  is swiped away, as long as the app's own process lives, although the design says it should stop. It does no work.
- Skin export is offered as `<id>.mskin`, but Android adds `.zip` to the saved name. Importing it works either way.
- In landscape the Now Playing controls column scrolls, so the **Queue** button starts below the fold.
- Position is saved on pause, track change and task removal, so a force-stop mid-track restores the track at its
  last saved position (often the start).
- Only the most recent playlist removal can be undone.

Not verified on hardware: an incoming call, unplugging headphones, TalkBack, a foldable or tablet, large font or
display size, gesture navigation, lock-screen artwork, an Android 8 device, a phone with no equalizer, and a
5,000-song library.

Release readiness: the app is **not signed for release**; the release APK is built unsigned and needs a signing
setup before any store submission. The project is licensed under Apache-2.0 (see `LICENSE`).
