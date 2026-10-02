# Lightweight Music Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a lightweight, smooth local music-library player for Android 16 (min Android 8.0) with playlists, equalizer, home-screen widget and an importable skin system.

**Architecture:** Single `:app` module. A Media3 `MediaLibraryService` owns ExoPlayer and exposes a browsable library tree; the Compose UI talks to it only through one `PlayerConnection` (MediaController) that publishes `StateFlow<PlayerUiState>`. Library data comes from MediaStore (no own song DB); Room holds playlists/play stats; DataStore holds settings, session and EQ. All visuals read `MaterialTheme` + `LocalSkin`, where skins are data parsed from `skin.json`.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3 (+ adaptive, navigation-suite), Navigation Compose (type-safe routes), AndroidX Media3 (exoplayer, session), Room, DataStore Preferences, Glance, Coil 3, kotlinx.serialization. Tests: JUnit4, Truth, Robolectric, kotlinx-coroutines-test, Turbine, media3-test-utils(-robolectric), Compose UI test, Roborazzi, Macrobenchmark.

**Spec:** `docs/superpowers/specs/2026-10-02-music-player-design.md` — read it alongside this plan.

**Path shorthand:** `…/` = `app/src/main/java/io/github/aceattacker77/nakedmusicplayer/`; `T…/` = `app/src/test/java/io/github/aceattacker77/nakedmusicplayer/`; `AT…/` = `app/src/androidTest/java/io/github/aceattacker77/nakedmusicplayer/`.

## Global Constraints

- `namespace`/`applicationId` = `io.github.aceattacker77.nakedmusicplayer` (rename allowed before Play submission only).
- `compileSdk = 36`, `targetSdk = 36`, `minSdk = 26`. JDK 17. Gradle version catalog `gradle/libs.versions.toml`; use latest stable versions at execution time.
- Single runtime module `:app`. Only additional module allowed: test-only `:baselineprofile` (Task 19).
- Runtime dependencies allowed: Media3 exoplayer + session, Compose BOM (ui, material3, material3-adaptive, material3-adaptive-navigation-suite, material-icons-core; **not** material-icons-extended — use vector XML for extra icons), navigation-compose, Room (runtime, ktx, ksp compiler), datastore-preferences, glance-appwidget + glance-material3, Coil 3 (coil-compose), kotlinx-serialization-json, `com.materialkolor:material-kolor` (color generation), `androidx.palette:palette-ktx` (art colors), `sh.calvin.reorderable:reorderable` (drag reorder), profileinstaller. Anything else needs a plan change. Test deps unrestricted.
- **No `INTERNET` permission** (add `<uses-permission android:name="android.permission.INTERNET" tools:node="remove"/>`). No analytics, ads, accounts, crash reporting.
- Manual DI only: `AppContainer` created in `MusicApp : Application`.
- **No composable hard-codes a color, font, shape or player style.** Read `MaterialTheme.*` and `LocalSkin.current`. (Layout dp spacing is fine.)
- Edge-to-edge (`enableEdgeToEdge()`), `android:enableOnBackInvokedCallback="true"`.
- All user-visible text in `res/values/strings.xml`. Exact copy fixed by spec/plan: `Skipped – can't play %1$s`, `No music found`, `%1$d of %2$d matched`, `Removed "%1$s"`, `Undo`.
- Never auto-play on app start or session restore.
- Performance targets: cold start < 400 ms; 0 dropped frames scrolling 5,000 songs at 120 Hz; release APK < 6 MB; RAM < 120 MB typical.
- Commands: unit tests `./gradlew :app:testDebugUnitTest --tests "<FQN>"`; instrumented `./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=<FQN>` (needs an API 36 emulator/device). On Windows use `gradlew.bat`.
- Every commit message ends with the session's attribution trailer lines.

## Review Focus

1. **A queued/playlisted song is deleted from storage** → playback skips it with `Skipped – can't play …`; playlists silently hide it; no crash. (Tests: Task 6 `skipPolicy_*`, Task 14 `detail_hidesDeletedSongs`.)
2. **Songs with missing tags** (MediaStore returns null or `<unknown>` artist/album, null title, track `0`) → grouped under "Unknown Artist"/"Unknown Album", title falls back to file name without extension. (Task 1 `build_unknownTags_*`.)
3. **Restored session references songs that no longer exist** → missing IDs dropped, index moved to next surviving item at position 0, empty → no session. (Task 6 `restore_*`.)
4. **M3U files from Windows/other players** (backslashes, CRLF, UTF-8 BOM, `file://` URIs, absolute `/storage/emulated/0/` or `/sdcard/` prefixes) still match. (Task 15 `parse_windowsStyle`, `match_absoluteAndUriPaths`.)
5. **Permission revoked while app is backgrounded / storage unavailable** → `query()` throws `SecurityException`; library becomes empty and UI shows the permission state; no crash. (Task 2 `securityException_emitsEmpty`.)

---

### Task 1: Project skeleton, domain models and library building

**Files:**
- Create: `settings.gradle.kts`, `build.gradle.kts`, `gradle/libs.versions.toml`, `app/build.gradle.kts`, `app/src/main/AndroidManifest.xml`, `…/MusicApp.kt`, `…/MainActivity.kt` (blank `setContent {}`), `.gitignore`
- Create: `…/library/model/Models.kt`, `…/library/LibraryBuilder.kt`, `…/library/SongSorting.kt`
- Test: `T…/library/LibraryBuilderTest.kt`, `T…/library/SongSortingTest.kt`

**Interfaces:**
- Produces:
  - `data class AudioRow(id: Long, title: String?, artist: String?, album: String?, albumId: Long, artistId: Long, durationMs: Long, track: Int, dateAddedSec: Long, relativePath: String, displayName: String, year: Int?)` — `track` is raw MediaStore `TRACK` (disc*1000+track).
  - `data class Song(id: Long, uri: String, title: String, artist: String, album: String, albumId: Long, artistId: Long, durationMs: Long, discNumber: Int, trackNumber: Int, dateAddedSec: Long, relativePath: String, displayName: String, year: Int?)` — `uri` = `content://media/external/audio/media/<id>`.
  - `data class Album(id: Long, title: String, artist: String, year: Int?, songs: List<Song>)`, `data class Artist(id: Long, name: String, albums: List<Album>, songs: List<Song>)`, `data class Library(songs: List<Song>, albums: List<Album>, artists: List<Artist>) { companion object { val EMPTY } }`
  - `data class LibraryFilter(minDurationMs: Long = 30_000, excludedFolders: Set<String> = emptySet())` — folders compared as `relativePath` prefixes.
  - `object LibraryBuilder { fun build(rows: List<AudioRow>, filter: LibraryFilter): Library }`
  - `enum class SongSort { TITLE, ARTIST, ALBUM, DATE_ADDED }`, `fun List<Song>.sortedWith(sort: SongSort): List<Song>`
  - String constants `UNKNOWN_ARTIST = "Unknown Artist"`, `UNKNOWN_ALBUM = "Unknown Album"` in `Models.kt` (UI maps to string resources later).

- [ ] **Step 1: Scaffold the Gradle project** with the Global Constraints SDK levels, Compose enabled, kotlin-serialization + ksp plugins, all allowed deps declared in the catalog (only those needed now applied), manifest with `MusicApp`, `MainActivity` (exported launcher), the INTERNET removal, `enableOnBackInvokedCallback`. Run `./gradlew :app:assembleDebug` → BUILD SUCCESSFUL.
- [ ] **Step 2: Write failing tests**

```kotlin
class LibraryBuilderTest {
  private fun row(id: Long, title: String? = "T$id", artist: String? = "A", album: String? = "Al",
                  albumId: Long = 1, dur: Long = 200_000, track: Int = 1, path: String = "Music/",
                  name: String = "f$id.mp3") =
    AudioRow(id, title, artist, album, albumId, 1, dur, track, 0, path, name, null)

  @Test fun build_filtersShortSongs() {
    val lib = LibraryBuilder.build(listOf(row(1, dur = 29_999), row(2, dur = 30_000)), LibraryFilter())
    assertThat(lib.songs.map { it.id }).containsExactly(2L)
  }
  @Test fun build_filtersExcludedFolders() {
    val lib = LibraryBuilder.build(listOf(row(1, path = "Music/WhatsApp/"), row(2)),
      LibraryFilter(excludedFolders = setOf("Music/WhatsApp/")))
    assertThat(lib.songs.map { it.id }).containsExactly(2L)
  }
  @Test fun build_decodesDiscAndTrack_andOrdersAlbumTracks() {
    val lib = LibraryBuilder.build(listOf(row(1, track = 2003), row(2, track = 1005), row(3, track = 2001)), LibraryFilter())
    val album = lib.albums.single()
    assertThat(album.songs.map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
    assertThat(album.songs[1].discNumber to album.songs[1].trackNumber).isEqualTo(2 to 1)
  }
  @Test fun build_unknownTags_groupedUnderUnknown() {
    val lib = LibraryBuilder.build(listOf(row(1, title = null, artist = "<unknown>", album = null, name = "My Song.mp3"),
                                          row(2, artist = " ", album = "")), LibraryFilter())
    assertThat(lib.songs.first { it.id == 1L }.title).isEqualTo("My Song")
    assertThat(lib.artists.map { it.name }).containsExactly(UNKNOWN_ARTIST)
    assertThat(lib.songs.map { it.album }.distinct()).containsExactly(UNKNOWN_ALBUM)
  }
  @Test fun build_trackZero_meansDisc1Track0_sortedLast() { /* track=0 → disc 1, track 0; sorts after numbered tracks */ }
}
```

`SongSortingTest`: `title_isCaseInsensitive` (["b","A","c"] → A,b,c), `artist_thenAlbum_thenTrack`, `dateAdded_isNewestFirst`.

- [ ] **Step 3: Run** `./gradlew :app:testDebugUnitTest --tests "*LibraryBuilderTest" --tests "*SongSortingTest"` → FAIL (unresolved references).
- [ ] **Step 4: Implement** models, `LibraryBuilder.build` (normalize blank/`<unknown>` → constants; title fallback = `displayName.substringBeforeLast('.')`; disc = `max(1, track/1000)`, track = `track % 1000`; album songs ordered by disc, then track with 0 last, then title; artists grouped by `artistId`), and `sortedWith` using `java.text.Collator` (PRIMARY strength).
- [ ] **Step 5: Run tests** → PASS.
- [ ] **Step 6: Commit** `feat: project skeleton and library model`.

---

### Task 2: MediaStore source and LibraryRepository

**Files:**
- Create: `…/library/AudioRowSource.kt`, `…/library/MediaStoreAudioRowSource.kt`, `…/library/LibraryRepository.kt`
- Test: `T…/library/LibraryRepositoryTest.kt`

**Interfaces:**
- Consumes: Task 1 `AudioRow`, `LibraryBuilder`, `LibraryFilter`, `Library`.
- Produces:
  - `interface AudioRowSource { suspend fun query(): List<AudioRow>; fun changes(): Flow<Unit> }`
  - `class MediaStoreAudioRowSource(context: Context) : AudioRowSource` — queries `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` with `IS_MUSIC != 0`; `changes()` = `callbackFlow` around a `ContentObserver` (notifyForDescendants = true).
  - `class LibraryRepository(source: AudioRowSource, filter: Flow<LibraryFilter>, scope: CoroutineScope) { val library: StateFlow<Library>; val permissionDenied: StateFlow<Boolean>; fun refresh() }`

- [ ] **Step 1: Write failing tests** with a `FakeAudioRowSource` (mutable rows, `MutableSharedFlow<Unit>` changes, optional `throwOnQuery: Throwable?`) and `runTest` + Turbine:
  - `initialLoad_emitsBuiltLibrary`
  - `changes_areDebounced500ms` — emit 3 changes within 100 ms → exactly one extra `query()` after `advanceTimeBy(500)`.
  - `filterChange_rebuildsWithoutRequery` — new filter → library updated, query count unchanged.
  - `securityException_emitsEmpty` — source throws `SecurityException` → `library.value == Library.EMPTY`, `permissionDenied.value == true`; a later successful `refresh()` sets it back to `false`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement** — keep last raw rows; combine `rows` with `filter` via `LibraryBuilder.build` on `Dispatchers.Default`; changes `debounce(500)`.
- [ ] **Step 4: Implement `MediaStoreAudioRowSource`** (projection = all `AudioRow` fields; `RELATIVE_PATH` on API 29+, else derive from `DATA` minus `/storage/emulated/0/` and file name).
- [ ] **Step 5: Run tests** → PASS.
- [ ] **Step 6: Commit** `feat: MediaStore-backed library repository`.

---

### Task 3: Settings and session storage

**Files:**
- Create: `…/data/settings/AppSettings.kt`, `…/data/settings/SettingsRepository.kt`, `…/data/session/SessionStore.kt`
- Test: `T…/data/SettingsRepositoryTest.kt`, `T…/data/SessionStoreTest.kt`

**Interfaces:**
- Consumes: Task 1 `SongSort`, `LibraryFilter`.
- Produces:
  - `enum class ThemeMode { SYSTEM, LIGHT, DARK }`
  - `data class AppSettings(themeMode: ThemeMode = SYSTEM, dynamicColor: Boolean = true, activeSkinId: String = "builtin.default", minDurationMs: Long = 30_000, excludedFolders: Set<String> = emptySet(), scanFolderUris: Set<String> = emptySet(), songSort: SongSort = SongSort.TITLE) { fun libraryFilter(): LibraryFilter }`
  - `class SettingsRepository(store: DataStore<Preferences>) { val settings: Flow<AppSettings>; suspend fun update(transform: (AppSettings) -> AppSettings) }`
  - `data class SavedSession(mediaIds: List<String>, index: Int, positionMs: Long, shuffle: Boolean, repeatMode: Int)`
  - `class SessionStore(store: DataStore<Preferences>) { suspend fun save(session: SavedSession); suspend fun load(): SavedSession?; suspend fun clear() }`
  - Single app DataStore file `settings.preferences_pb` provided by `AppContainer`.

- [ ] **Step 1: Write failing tests** using `PreferenceDataStoreFactory.create(scope = testScope) { tmpFolder.newFile("t.preferences_pb") }`: `defaults_whenEmpty`, `update_roundTripsAllFields`, `session_roundTrip`, `session_loadReturnsNullWhenAbsent`, `session_unknownEnumValue_fallsBackToDefault` (write garbage string for `songSort` → `TITLE`).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement** (media IDs stored as one string joined by `\n`). **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: settings and session stores`.

---

### Task 4: Room database — playlists and play stats

**Files:**
- Create: `…/data/db/AppDatabase.kt`, `…/data/db/Entities.kt`, `…/data/db/PlaylistDao.kt`, `…/data/db/PlayStatDao.kt`, `…/data/db/PlayCountRule.kt`
- Test: `T…/data/db/PlaylistDaoTest.kt`, `T…/data/db/PlayStatDaoTest.kt`, `T…/data/db/PlayCountRuleTest.kt` (Robolectric, in-memory Room)

**Interfaces:**
- Produces:
  - Entities per spec §3.3: `PlaylistEntity(id: Long = 0 autoGenerate, name: String, createdAt: Long, updatedAt: Long)`, `PlaylistSongEntity(playlistId: Long, songId: Long, position: Int)` (PK `playlistId,position`; FK cascade delete), `PlayStatEntity(songId: Long PK, playCount: Int, lastPlayedAt: Long)`.
  - `PlaylistDao`: `observePlaylists(): Flow<List<PlaylistEntity>>` (by name, case-insensitive); `suspend create(name: String, now: Long): Long`; `suspend rename(id: Long, name: String, now: Long)`; `suspend delete(id: Long)`; `observeSongIds(playlistId: Long): Flow<List<Long>>` (by position); `suspend addSongs(playlistId: Long, songIds: List<Long>, now: Long)` (append); `suspend insertAt(playlistId: Long, position: Int, songId: Long, now: Long)` (for Undo); `suspend removeAt(playlistId: Long, position: Int, now: Long)`; `suspend move(playlistId: Long, from: Int, to: Int, now: Long)`; `suspend pruneSongs(validSongIds: Set<Long>)`.
  - `PlayStatDao`: `suspend recordPlay(songId: Long, now: Long)`; `mostPlayed(limit: Int): Flow<List<Long>>`; `recentlyPlayed(limit: Int): Flow<List<Long>>`; `suspend prune(validSongIds: Set<Long>)`.
  - `object PlayCountRule { fun countsAsPlay(listenedMs: Long, durationMs: Long): Boolean }` — true when `listenedMs >= durationMs / 2` **or** `listenedMs >= 240_000`.

- [ ] **Step 1: Write failing tests:**
  - `PlayCountRuleTest`: `(100_000, 200_000) → true`, `(99_999, 200_000) → false`, `(240_000, 3_600_000) → true`, `(239_999, 3_600_000) → false`.
  - `PlaylistDaoTest`: `addSongs_appendsInOrder`, `allowsDuplicateSongs`, `removeAt_renumbersPositions` (positions stay 0..n-1), `move_forwardAndBackward`, `insertAt_restoresRemovedItem`, `delete_cascadesSongs`, `pruneSongs_removesMissing_andRenumbers`, `pruneSongs_handles2000Ids` (no SQLite variable-limit crash).
  - `PlayStatDaoTest`: `recordPlay_incrementsAndStampsTime`, `mostPlayed_ordersByCountThenRecent`, `prune_removesMissing`.
- [ ] **Step 2: Run** → FAIL.
- [ ] **Step 3: Implement.** `@Transaction` for reorder ops (shift positions via temporary negative offsets to avoid PK clashes). `pruneSongs`: read distinct song IDs, diff in Kotlin, delete in chunks of 500, then renumber affected playlists.
- [ ] **Step 4: Run** → PASS. **Step 5: Commit** `feat: playlist and play-stat database`.

---

### Task 5: PlaybackService core and library tree

**Files:**
- Create: `…/playback/MediaItems.kt`, `…/playback/LibraryTree.kt`, `…/playback/PlaybackService.kt`, `…/AppContainer.kt`
- Modify: `app/src/main/AndroidManifest.xml`, `…/MusicApp.kt`
- Create fixtures: `app/src/androidTest/assets/fixtures/{tone.mp3,tone.flac,tone.ogg,tone.opus,tone.m4a,tone.wav,corrupt.mp3}`, `scripts/make_fixtures.sh`
- Test: `T…/playback/LibraryTreeTest.kt`, `AT…/playback/FormatPlaybackTest.kt`

**Interfaces:**
- Consumes: `Library`, `Song` (T1), `LibraryRepository` (T2), `PlaylistDao` (T4).
- Produces:
  - `fun Song.toMediaItem(): MediaItem` — mediaId `"song:<id>"`, uri, title/artist/album/trackNumber/discNumber, `artworkUri = content://media/external/audio/albumart/<albumId>`, `isPlayable = true`, `isBrowsable = false`.
  - `object LibraryTree` — IDs `ROOT="root"`, `ALBUMS="albums"`, `ARTISTS="artists"`, `PLAYLISTS="playlists"`, `SONGS="songs"`, `album:<id>`, `artist:<id>`, `playlist:<id>`, `song:<id>`; `fun children(parentId: String, library: Library, playlists: List<PlaylistEntity>, playlistSongIds: (Long) -> List<Long>): List<MediaItem>?` (null = unknown parent); `fun item(mediaId: String, library: Library): MediaItem?`; `fun songIdOf(mediaId: String): Long?`.
  - `class AppContainer(app: Application)` exposing: `settingsRepository`, `sessionStore`, `database`, `libraryRepository`, `applicationScope` (later tasks add fields).
  - `PlaybackService : MediaLibraryService` with ExoPlayer: `setAudioAttributes(USAGE_MEDIA/CONTENT_TYPE_MUSIC, handleAudioFocus = true)`, `setHandleAudioBecomingNoisy(true)`, `setWakeMode(C.WAKE_MODE_LOCAL)`; `MediaLibrarySession.Callback` implementing `onGetLibraryRoot`, `onGetChildren`, `onGetItem`, `onAddMediaItems` (resolve IDs → full items via `LibraryTree.item`). `onTaskRemoved`: stop if not playing.
  - Manifest: `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_MEDIA_PLAYBACK`, `POST_NOTIFICATIONS`, `READ_MEDIA_AUDIO`, `READ_EXTERNAL_STORAGE maxSdkVersion=32`; service `foregroundServiceType="mediaPlayback"`, exported, intent filters `androidx.media3.session.MediaLibraryService` and `android.media.browse.MediaBrowserService`.

- [ ] **Step 1: Write failing `LibraryTreeTest`:** `root_hasFourCategoriesInOrder` (Albums, Artists, Playlists, Songs; all browsable), `album_childrenAreTracksInAlbumOrder`, `artist_childrenAreAlbums`, `playlist_childrenFollowPositions_skippingMissingSongs`, `unknownParent_returnsNull`, `songIdOf_parses` (`"song:42"` → 42, `"album:1"` → null).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement** `MediaItems.kt`, `LibraryTree`. **Step 4: Run** → PASS.
- [ ] **Step 5: Create fixtures** with `scripts/make_fixtures.sh` (ffmpeg, 2 s 440 Hz sine per format; `corrupt.mp3` = 4 KB of random bytes with `.mp3` name). Commit the generated files.
- [ ] **Step 6: Write `FormatPlaybackTest`** (instrumented, real ExoPlayer on main looper, asset URIs `asset:///fixtures/…`): `playsEachSupportedFormat` (mp3, flac, ogg, opus, m4a, wav each reach `STATE_READY` with no error within 5 s), `gapless_transitionHasNoError` (mp3→mp3 playlist reaches second item via `MEDIA_ITEM_TRANSITION_REASON_AUTO`), `corruptFile_raisesPlaybackError`.
- [ ] **Step 7: Implement `PlaybackService`, `AppContainer`, manifest.** Run instrumented test → PASS. Run `./gradlew :app:processDebugMainManifest` and confirm merged manifest has no `android.permission.INTERNET`.
- [ ] **Step 8: Commit** `feat: playback service with browsable library tree`.

---

### Task 6: Error skipping, play counting, session persistence and resumption

**Files:**
- Create: `…/playback/SkipPolicy.kt`, `…/playback/SessionRestorer.kt`, `…/playback/UnplayableRegistry.kt`, `…/playback/PlayTracker.kt`
- Modify: `…/playback/PlaybackService.kt`, `app/src/main/res/values/strings.xml`
- Test: `T…/playback/SkipPolicyTest.kt`, `T…/playback/SessionRestorerTest.kt`, `T…/playback/PlayTrackerTest.kt`

**Interfaces:**
- Consumes: `SavedSession`, `SessionStore` (T3); `PlayStatDao`, `PlayCountRule` (T4); `LibraryTree.item` (T5).
- Produces:
  - `class SkipPolicy(maxConsecutiveFailures: Int = 3) { enum class Action { SKIP, STOP }; fun onError(): Action; fun onItemStartedSuccessfully() }`
  - `object SessionRestorer { data class Restored(items: List<MediaItem>, index: Int, positionMs: Long); fun restore(saved: SavedSession, resolve: (String) -> MediaItem?): Restored? }`
  - `object UnplayableRegistry { val ids: StateFlow<Set<String>>; fun mark(mediaId: String) }` (in-process; UI shows "can't play" icon).
  - `class PlayTracker(onCounted: (songId: Long) -> Unit) { fun onPlaying(mediaId: String, durationMs: Long, nowMs: Long); fun onPaused(nowMs: Long); fun onItemChanged(nowMs: Long) }` — accumulates listened time per item instance; fires `onCounted` once when `PlayCountRule` holds.

- [ ] **Step 1: Write failing tests:**
  - `SkipPolicyTest`: `skipPolicy_firstTwoErrorsSkip_thirdStops`, `skipPolicy_successResetsCount`.
  - `SessionRestorerTest`: `restore_allPresent_keepsIndexAndPosition`, `restore_missingBeforeCurrent_shiftsIndex` (ids [a,b,c], index 2, b missing → index 1, position kept), `restore_currentMissing_movesToNextAtZero` (index 1 b missing → c at 0 ms), `restore_currentMissingAndLast_wrapsToPrevious` (→ last surviving item at 0 ms), `restore_allMissing_returnsNull`, `restore_indexOutOfRange_clamped`.
  - `PlayTrackerTest`: `countsOnceAtHalf`, `pausedTimeNotCounted`, `newItemResetsAccumulation`, `fourMinutesCountsForLongTrack`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement** the three units. **Step 4: Run** → PASS.
- [ ] **Step 5: Wire into `PlaybackService`:**
  - `Player.Listener.onPlayerError` → `UnplayableRegistry.mark(currentId)`, `Toast` `Skipped – can't play %1$s` (title), then `SkipPolicy.onError()`: `SKIP` → `seekToNextMediaItem()` + `prepare()`; `STOP` → `stop()`. `onIsPlayingChanged(true)` → `onItemStartedSuccessfully()`.
  - Save `SavedSession` on pause, media item transition, and `onTaskRemoved`.
  - On service create: load session → `SessionRestorer.restore` → `setMediaItems(items, index, positionMs)` + `prepare()` **without** `play()`.
  - `MediaLibrarySession.Callback.onPlaybackResumption` → same restore, returned as `MediaItemsWithStartPosition`.
  - `PlayTracker.onCounted` → `playStatDao.recordPlay` on `applicationScope`.
- [ ] **Step 6: Extend `FormatPlaybackTest`** with `serviceSkipsCorrupt_thenPlaysNext` (queue [corrupt, tone.mp3] via `MediaController` → reaches index 1 playing) and `threeCorruptInARow_stops`. Run → PASS.
- [ ] **Step 7: Commit** `feat: skip-on-error, play stats, session restore`.

---

### Task 7: PlayerConnection (UI ↔ service)

**Files:**
- Create: `…/ui/player/PlayerUiState.kt`, `…/ui/player/PlayerConnection.kt`
- Modify: `…/AppContainer.kt` (add `playerConnection: Deferred<PlayerConnection>` or lazy connect)
- Test: `T…/ui/player/PlayerConnectionTest.kt` (Robolectric + `TestExoPlayerBuilder` from media3-test-utils)

**Interfaces:**
- Consumes: `Song.toMediaItem()` (T5).
- Produces:
  - `data class PlayerUiState(current: MediaItem?, isPlaying: Boolean, durationMs: Long, queue: List<MediaItem>, currentIndex: Int, shuffle: Boolean, repeatMode: Int) { companion object { val EMPTY } }`
  - `class PlayerConnection(player: Player, scope: CoroutineScope)` with `val state: StateFlow<PlayerUiState>`; `fun positionMs(): Flow<Long>` (polls every 250 ms only while collected); `fun playSongs(songs: List<Song>, startIndex: Int)`; `fun playNext(songs: List<Song>)`; `fun addToQueue(songs: List<Song>)`; `fun moveQueueItem(from: Int, to: Int)`; `fun removeQueueItem(index: Int)`; `fun togglePlayPause()`; `fun next()`; `fun previous()`; `fun seekTo(positionMs: Long)`; `fun toggleShuffle()`; `fun cycleRepeat()`; `fun release()`.
  - `suspend fun PlayerConnection.Companion.connect(context: Context, scope: CoroutineScope): PlayerConnection` (builds `MediaController` for `PlaybackService`).

- [ ] **Step 1: Write failing tests:** `playSongs_setsQueueAndStartsAtIndex`, `playNext_insertsAfterCurrent`, `playNext_onEmptyQueue_startsQueue`, `addToQueue_appends`, `remove_and_move_reflectInState`, `cycleRepeat_offAllOneOff`, `state_updatesOnTransition`, `togglePlayPause_whenIdleWithQueue_prepares` (state IDLE → `prepare()` then `play()`).
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement** (state rebuilt from `Player.Listener.onEvents`). **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: player connection`.

---

### Task 8: Skin model, parser and theme

**Files:**
- Create: `…/ui/skins/Skin.kt`, `…/ui/skins/SkinJson.kt` (`@Serializable` DTOs, all fields nullable), `…/ui/skins/SkinParser.kt`, `…/ui/theme/AppTheme.kt`, `…/ui/theme/LocalSkin.kt`
- Test: `T…/ui/skins/SkinParserTest.kt`, `T…/ui/theme/SchemeSelectionTest.kt`

**Interfaces:**
- Consumes: `ThemeMode`, `AppSettings` (T3).
- Produces:
  - `enum class ColorMode { LIGHT, DARK, BOTH, SYSTEM }` — LIGHT/DARK: always that scheme; BOTH: light or dark per `ThemeMode` (SYSTEM → `isSystemInDarkTheme()`); SYSTEM: dynamic color on API 31+ when `AppSettings.dynamicColor`, else the Default skin's generated scheme per ThemeMode.
  - `sealed interface BackgroundStyle { BlurredArt; ArtGradient; Solid; data class Image(path: String) }`, `sealed interface ArtShape { Square; data class Rounded(radiusDp: Int); Circle }`, `enum SeekBarStyle { WAVY, FLAT, THIN }`, `enum ControlsStyle { FILLED, OUTLINED, ICON_ONLY }`, `enum ControlSize { SMALL, MEDIUM, LARGE }`, `enum LayoutType { CLASSIC, VINYL, MINIMAL, CASSETTE, COMPACT }`, `enum ArtPosition { TOP, LEFT, CENTER }`.
  - `data class PlayerStyle(background, artShape, artSpin: Boolean, seekBar, controls, controlSize, glow: Boolean, shadow: Boolean, useArtColors: Boolean)`, `data class LayoutSpec(type: LayoutType, artPosition: ArtPosition)`.
  - `data class Skin(id: String, name: String, author: String, version: String, colorMode: ColorMode, light: ColorScheme?, dark: ColorScheme?, fontPath: String?, cornerRadiusDp: Int, player: PlayerStyle, layout: LayoutSpec, baseDir: File?)` (`baseDir` null for not-yet-installed); `companion object { val FALLBACK: Skin }` — hard-coded equivalent of the Default skin (used only to bootstrap parsing of `builtin.default`).
  - `sealed interface SkinParseResult { data class Ok(skin: Skin, referencedFiles: Set<String>); data class Error(message: String) }`
  - `object SkinParser { const val FORMAT = 1; fun parse(json: String, defaults: Skin): SkinParseResult }`
  - `fun selectColorScheme(skin: Skin, settings: AppSettings, systemDark: Boolean, dynamic: ((dark: Boolean) -> ColorScheme)?): ColorScheme` (pure; `dynamic` null below API 31).
  - `val LocalSkin = staticCompositionLocalOf<Skin> { error("no skin") }`; `@Composable fun AppTheme(skin: Skin, settings: AppSettings, content: @Composable () -> Unit)` providing MaterialTheme (colors, typography with skin font, shapes from `cornerRadiusDp`) + `LocalSkin`.

- [ ] **Step 1: Write failing `SkinParserTest`** (use the spec §5.3 example JSON verbatim as fixture `example`):
  - `parsesSpecExample` — id `com.example.neon`, `colorMode == DARK`, `dark!!.primary == Color(0xFF00E5FF)`, `artShape == Rounded(28)`, `seekBar == WAVY`, `controlSize == LARGE`, `glow`, `layout == LayoutSpec(CLASSIC, TOP)`, `referencedFiles == setOf("fonts/Orbitron.ttf")`.
  - `partialSkin_inheritsDefaults` — `{"format":1,"id":"a.b","name":"X"}` → player/layout/corner equal `defaults`.
  - `unlistedColorRoles_derivedFromPrimary` — only `primary` given → `secondary`, `surface` non-null and `primary` exact.
  - Errors (exact messages): missing format → `missing field 'format'`; `"format":2` → `unsupported format 2`; `"id":"Bad Id"` → `invalid id 'Bad Id'` (regex `^[a-z0-9_-]+(\.[a-z0-9_-]+)*$`, ≤ 64 chars); `"primary":"#GGG"` → `invalid colour 'primary': '#GGG'` (accept `#RRGGBB`/`#AARRGGBB`); malformed JSON → `skin.json is not valid JSON`; unknown enum → `invalid value 'zigzag' for 'player.seekBar'`; unknown extra fields ignored.
  - Background `{"type":"image","path":"images/bg.webp"}` → `Image` and path in `referencedFiles`.
- [ ] **Step 2: Write failing `SchemeSelectionTest`:** `lightOnlySkin_staysLightWhenSystemDark`, `bothMode_followsThemeSetting`, `systemMode_usesDynamicWhenAvailableAndEnabled`, `systemMode_noDynamic_fallsBackToDefault`.
- [ ] **Step 3: Run** → FAIL. **Step 4: Implement** (missing roles via `material-kolor` `dynamicColorScheme(seedColor = primary, isDark)`, then overlay explicit roles). **Step 5: Run** → PASS.
- [ ] **Step 6: Commit** `feat: skin model, parser and theme`.

---

### Task 9: Skin archive validation, install, export + format docs

**Files:**
- Create: `…/ui/skins/SkinArchiveReader.kt`, `…/ui/skins/SkinStore.kt`, `…/ui/skins/AndroidProbes.kt`
- Create: `docs/skins/FORMAT.md`, `docs/skins/example/skin.json` (spec example), `docs/skins/example/README.md` (how to zip as `.mskin`)
- Test: `T…/ui/skins/SkinArchiveReaderTest.kt`, `T…/ui/skins/SkinStoreTest.kt`, `AT…/ui/skins/AndroidProbesTest.kt`

**Interfaces:**
- Consumes: `SkinParser`, `Skin`, `SkinParseResult` (T8).
- Produces:
  - `fun interface ImageProbe { fun size(bytes: ByteArray): Pair<Int, Int>? }`, `fun interface FontProbe { fun loads(bytes: ByteArray): Boolean }`; Android impls `BitmapImageProbe` (`inJustDecodeBounds`), `TypefaceFontProbe` (write temp file → `Typeface.Builder(file).build()` non-null, temp deleted).
  - `sealed interface SkinImportResult { data class Valid(skin: Skin, entries: Map<String, ByteArray>); data class Invalid(message: String) }`
  - `class SkinArchiveReader(defaults: Skin, imageProbe: ImageProbe, fontProbe: FontProbe) { fun read(input: InputStream): SkinImportResult }`
  - `class SkinStore(rootDir: File, parse: (String) -> SkinParseResult) { fun installed(): List<Skin>; fun install(valid: SkinImportResult.Valid, replace: Boolean): InstallResult; fun delete(id: String); fun export(skin: Skin, out: OutputStream) }`, `sealed interface InstallResult { data class Installed(skin: Skin); data class AlreadyExists(id: String) }`. Installed at `rootDir/<id>/`.

- [ ] **Step 1: Write failing `SkinArchiveReaderTest`** (zips built in-test with `ZipOutputStream`; fake probes). Exact messages:
  - `tooLarge` — uncompressed total > 10 × 1024 × 1024 → `skin is larger than 10 MB` (count while streaming, abort early — also covers zip bombs).
  - `tooManyEntries` — 51 entries → `skin has more than 50 files`.
  - `pathTraversal` — entry `../evil.json`, `/abs.png`, `images\\x.png`, `C:/x` → `unsafe path '<name>'`.
  - `missingSkinJson` → `skin.json not found`.
  - `parserErrorPropagates` — bad colour → message from T8.
  - `missingReferencedFile` → `missing file 'images/bg.webp'`.
  - `imageTooBig` — probe returns 2049×10 → `image 'images/bg.webp' is larger than 2048 px`.
  - `undecodableImage` — probe null → `image 'images/bg.webp' could not be read`.
  - `badFont` → `font 'fonts/x.ttf' could not be loaded`.
  - `disallowedExtension` — referenced `images/a.gif` → `unsupported file type 'images/a.gif'` (allowed: png, webp, ttf, otf).
  - `validSkin_returnsEntries`.
- [ ] **Step 2: Write failing `SkinStoreTest`:** `install_writesFiles_andListsSkin`, `install_sameId_returnsAlreadyExists_unlessReplace`, `delete_removesDir`, `export_thenRead_roundTrips`, `installed_skipsCorruptDirs` (dir with invalid skin.json is ignored, not crash).
- [ ] **Step 3: Run** → FAIL. **Step 4: Implement.** **Step 5: Run** → PASS.
- [ ] **Step 6: `AndroidProbesTest`** (instrumented): real PNG 10×10 → (10,10); random bytes → null; a bundled test TTF loads; random bytes font → false. Run → PASS.
- [ ] **Step 7: Write `docs/skins/FORMAT.md`** — every field from spec §5.2–5.4 with type, allowed values, default, and the limits/messages above.
- [ ] **Step 8: Commit** `feat: skin import, validation and export`.

---

### Task 10: Built-in skins and SkinManager

**Files:**
- Create: `app/src/main/assets/skins/{default,vinyl,minimal,amoled}/skin.json`, `…/ui/skins/BuiltInSkins.kt`, `…/ui/skins/SkinManager.kt`, `…/ui/skins/SkinAssets.kt`
- Modify: `…/AppContainer.kt`, `…/MainActivity.kt` (wrap content in `AppTheme`)
- Test: `T…/ui/skins/BuiltInSkinsTest.kt`, `T…/ui/skins/SkinManagerTest.kt`

**Interfaces:**
- Consumes: T8 parser/theme, T9 `SkinStore`, T3 `SettingsRepository`.
- Produces:
  - Built-in IDs and values:
    - `builtin.default` "Default": mode SYSTEM, corner 28, ArtGradient, Rounded(28), WAVY, FILLED, MEDIUM, useArtColors true, CLASSIC/TOP.
    - `builtin.vinyl` "Vinyl": mode BOTH, primary `#C0392B`, BlurredArt, Circle, artSpin true, FLAT, FILLED, LARGE, VINYL/CENTER.
    - `builtin.minimal` "Minimal": mode BOTH, primary `#455A64`, Solid, Square, THIN, ICON_ONLY, SMALL, MINIMAL/LEFT.
    - `builtin.amoled` "AMOLED Black": mode DARK, background/surface `#000000`, primary `#BB86FC`, Solid, Rounded(16), FLAT, OUTLINED, MEDIUM, CLASSIC/TOP.
  - `object BuiltInSkins { val IDS: List<String>; fun load(assets: AssetManager, parse: (String) -> SkinParseResult): List<Skin> }` — **same parser** as imports; `Default` is the `defaults` argument for all others (bootstrap: Default parsed against `Skin.FALLBACK` from Task 8).
  - `class SkinManager(builtIns: List<Skin>, store: SkinStore, settings: SettingsRepository, scope: CoroutineScope) { val available: StateFlow<List<Skin>>; val active: StateFlow<Skin>; suspend fun apply(id: String); suspend fun import(input: InputStream, replace: Boolean): ImportOutcome; suspend fun delete(id: String) }`; `sealed interface ImportOutcome { Installed(skin); AlreadyExists(id); Failed(message) }`.
  - `object SkinAssets { fun fontFamily(skin: Skin): FontFamily?; fun imageModel(skin: Skin, path: String): File }` (cached per skin id).

- [ ] **Step 1: Write failing tests:** `BuiltInSkinsTest.allFourParseOk` (Robolectric assets), `…ids`, `amoled_backgroundIsPureBlack`; `SkinManagerTest`: `active_followsSetting`, `activeMissing_fallsBackToDefault` (setting `x.gone` → Default), `deleteActive_switchesToDefault`, `import_failed_leavesActiveUnchanged`, `available_isBuiltInsThenImportedByName`.
- [ ] **Step 2: Run** → FAIL. **Step 3: Implement**; parse active skin on `Dispatchers.IO` once and cache. **Step 4: Run** → PASS.
- [ ] **Step 5: Commit** `feat: built-in skins and skin manager`.

---

### Task 11: App shell — onboarding, navigation and library tabs

**Files:**
- Create: `…/ui/AppRoutes.kt`, `…/ui/AppScaffold.kt`, `…/ui/onboarding/Permissions.kt`, `…/ui/onboarding/PermissionScreen.kt`, `…/ui/library/LibraryViewModel.kt`, `…/ui/library/SongsScreen.kt`, `…/ui/library/AlbumsScreen.kt`, `…/ui/library/ArtistsScreen.kt`, `…/ui/components/SongRow.kt`, `…/ui/components/AlbumArt.kt`, `…/ui/components/EmptyState.kt`, `…/ui/components/FastScroller.kt`
- Modify: `…/MainActivity.kt`, `strings.xml`
- Test: `T…/ui/onboarding/PermissionsTest.kt`, `AT…/ui/AppShellTest.kt`

**Interfaces:**
- Consumes: `LibraryRepository` (T2), `SettingsRepository` (T3), `PlayerConnection` (T7), `AppTheme`/`LocalSkin` (T8/T10), `UnplayableRegistry` (T6).
- Produces:
  - `fun requiredAudioPermission(sdkInt: Int): String` (≥33 → `READ_MEDIA_AUDIO`, else `READ_EXTERNAL_STORAGE`); `fun needsNotificationPermission(sdkInt: Int): Boolean` (≥33).
  - `@Serializable` routes: `Songs`, `Albums`, `Artists`, `Playlists`, `AlbumDetail(id: Long)`, `ArtistDetail(id: Long)`, `PlaylistDetail(id: Long)`, `SmartPlaylistDetail(kind: String)`, `Search`, `Settings`, `Equalizer`, `Skins`.
  - `AppScaffold(nav, miniPlayer: @Composable () -> Unit, content)` using `NavigationSuiteScaffold` (tabs Songs/Albums/Artists/Playlists; top bar with Search + overflow Settings); leaves a `miniPlayer` slot above the bar (filled in Task 13).
  - `SongRow(song, isCurrent, unplayable, onClick, onLongClick)`; `AlbumArt(albumId: Long, modifier, shape)` via Coil with crossfade, placeholder from skin colors; `EmptyState(title, message, action: Pair<String, () -> Unit>?)`; `FastScroller(lazyListState, letters: List<Char>, indexOfLetter: (Char) -> Int)`.
  - `LibraryViewModel(libraryRepository, settingsRepository, playerConnection)`: `songs: StateFlow<List<Song>>` (sorted by setting), `albums`, `artists`, `fun setSort(sort: SongSort)`, `fun play(songs: List<Song>, index: Int)`.

- [ ] **Step 1: Failing `PermissionsTest`:** API 26/32 → `READ_EXTERNAL_STORAGE`; 33/36 → `READ_MEDIA_AUDIO`; notification flag only ≥33.
- [ ] **Step 2: Run** → FAIL; **implement**; → PASS.
- [ ] **Step 3: Failing `AppShellTest`** (Compose UI test with fake repo/connection injected through `AppContainer` test override): `permissionDenied_showsOpenSettings` (text "Open settings"), `emptyLibrary_showsNoMusicFound`, `tabs_switchBetweenSongsAlbumsArtists`, `tapSong_callsPlaySongsWithIndex`, `sortMenu_changesOrder`, `lazyLists_useStableKeys` (assert `key` = song id by scrolling and checking `SongRow` identity is not recomposed — or simpler: check `LazyColumn` items have `testTag("song-<id>")`).
- [ ] **Step 4: Implement** onboarding (rationale text → launcher for permissions; denied → `EmptyState` with "Open settings" → `Settings.ACTION_APPLICATION_DETAILS_SETTINGS`; on resume re-check and `libraryRepository.refresh()`), scaffold, three tabs (`LazyColumn`/`LazyVerticalGrid` with `key` + `contentType`), fast scroller on Songs. Run → PASS.
- [ ] **Step 5: Manual check** on device: edge-to-edge insets correct, predictive back from a tab returns home. **Step 6: Commit** `feat: app shell and library tabs`.

---

### Task 12: Detail screens, search, song menu, large-screen panes

**Files:**
- Create: `…/ui/library/AlbumDetailScreen.kt`, `…/ui/library/ArtistDetailScreen.kt`, `…/ui/search/SearchEngine.kt`, `…/ui/search/SearchScreen.kt`, `…/ui/components/SongMenu.kt`
- Modify: `…/ui/AppScaffold.kt` (use `ListDetailPaneScaffold` for Albums/Artists on expanded width)
- Test: `T…/ui/search/SearchEngineTest.kt`, `AT…/ui/DetailAndMenuTest.kt`

**Interfaces:**
- Consumes: T1 models, T7 `PlayerConnection`, T11 components/routes.
- Produces:
  - `data class SearchResults(songs: List<Song>, albums: List<Album>, artists: List<Artist>)`; `object SearchEngine { fun search(library: Library, query: String): SearchResults }` — trim; blank → empty; case- and diacritic-insensitive (`Normalizer` NFD, strip marks) substring on title/artist/album; songs capped at 100.
  - `SongMenu(song, onPlayNext, onAddToQueue, onAddToPlaylist, onGoToAlbum, onGoToArtist)` bottom sheet — order exactly as spec §4. `onAddToPlaylist` is a callback wired in Task 14.

- [ ] **Step 1: Failing `SearchEngineTest`:** `blank_returnsEmpty`, `matchesTitleArtistAlbum`, `ignoresCaseAndDiacritics` ("beyonce" finds "Beyoncé"), `capsSongsAt100`.
- [ ] **Step 2: Run → FAIL; implement; → PASS.**
- [ ] **Step 3: Failing `DetailAndMenuTest`:** `albumDetail_showsTracksInDiscOrder`, `artistDetail_showsAlbumsThenSongs`, `songMenu_playNext_callsConnection`, `songMenu_goToAlbum_navigates`, `search_typing_showsResults`.
- [ ] **Step 4: Implement; run → PASS.** Verify on a tablet/foldable emulator that Albums shows list + detail side by side.
- [ ] **Step 5: Commit** `feat: details, search, song menu`.

---

### Task 13: Mini player, Now Playing with skin layouts, queue sheet

**Files:**
- Create: `…/ui/player/MiniPlayer.kt`, `…/ui/player/NowPlayingScreen.kt`, `…/ui/player/NowPlayingSlots.kt`, `…/ui/player/layouts/{ClassicLayout,VinylLayout,MinimalLayout,CassetteLayout,CompactLayout}.kt`, `…/ui/player/SeekBars.kt`, `…/ui/player/PlayerBackground.kt`, `…/ui/player/ArtColors.kt`, `…/ui/player/QueueSheet.kt`, `…/ui/player/PlayerSheetState.kt`
- Modify: `…/ui/AppScaffold.kt` (mini player slot + expandable player)
- Test: `T…/ui/player/ArtColorsTest.kt`, `T…/ui/player/NowPlayingScreenshotTest.kt` (Roborazzi), `AT…/ui/player/PlayerUiTest.kt`

**Interfaces:**
- Consumes: T7 `PlayerConnection`/`PlayerUiState`, T8/T10 `Skin`, `LocalSkin`, `SkinAssets`, T11 `AlbumArt`, T12 `SongMenu`.
- Produces:
  - `class NowPlayingSlots(artwork: @Composable () -> Unit, trackInfo: …, seekBar: …, controls: …, secondaryControls: …, queueHandle: …)` and `@Composable fun NowPlayingLayout(spec: LayoutSpec, slots: NowPlayingSlots, modifier: Modifier)` dispatching to the five layouts; each layout honours `artPosition` where it makes sense (CLASSIC: TOP/LEFT; VINYL: CENTER art as spinning record; MINIMAL: no art when `LEFT` → small thumb; CASSETTE: art inside cassette frame drawn with Canvas; COMPACT: art LEFT, everything one column). Landscape/expanded width forces art LEFT.
  - `@Composable fun SeekBar(style: SeekBarStyle, positionMs: Long, durationMs: Long, onSeek: (Long) -> Unit)` — WAVY animates only while playing; use Material 3 Expressive wavy component if present in the BOM, else Canvas sine path.
  - `object ArtColors { fun pickAccent(swatches: List<Pair<Int /*rgb*/, Int /*population*/>>): Int? }` — prefer vibrant, else dominant, else null; Palette wiring in `rememberArtAccent(albumId)`.
  - `enum PlayerSheetValue { Collapsed, Expanded }` + `rememberPlayerSheetState()`; expansion via `SharedTransitionLayout`/`sharedElement` on artwork; drag up/down; `PredictiveBackHandler` collapses with progress.
  - `QueueSheet(state, onMove, onRemove, onPlayIndex)` using `sh.calvin.reorderable`.

- [ ] **Step 1: Failing `ArtColorsTest`:** `prefersVibrant`, `fallsBackToDominant`, `emptyReturnsNull`.
- [ ] **Step 2: Run → FAIL; implement; → PASS.**
- [ ] **Step 3: Failing Roborazzi `NowPlayingScreenshotTest`:** for each built-in skin × {light, dark} render `NowPlayingScreen` with a fixed fake state and placeholder art → record golden (`./gradlew :app:recordRoborazziDebug`), then `verifyRoborazziDebug` must pass. Add a `missingArt_showsPlaceholder` capture.
- [ ] **Step 4: Failing `PlayerUiTest`:** `miniPlayer_hiddenWhenQueueEmpty`, `tapMini_expands_backCollapses`, `queue_dragReorder_callsMove`, `queue_swipeRemove_callsRemove`, `playPause_togglesIcon`, `unplayableItem_showsIcon`.
- [ ] **Step 5: Implement** everything; all styles read from `LocalSkin.current.player`; background per `BackgroundStyle` (BlurredArt uses `Modifier.blur` on API 31+, gradient fallback below). Run all three test classes → PASS.
- [ ] **Step 6: Commit** `feat: mini player, now playing layouts and queue`.

---

### Task 14: Playlists feature (incl. smart playlists)

**Files:**
- Create: `…/data/playlists/PlaylistRepository.kt`, `…/ui/playlists/PlaylistsScreen.kt`, `…/ui/playlists/PlaylistDetailScreen.kt`, `…/ui/playlists/AddToPlaylistSheet.kt`, `…/ui/playlists/PlaylistsViewModel.kt`
- Modify: `…/ui/components/SongMenu.kt` wiring, album/artist overflow "Add to playlist", Now Playing secondary control, `LibraryRepository` consumer that calls `pruneSongs`/`prune` after each library refresh
- Test: `T…/data/playlists/PlaylistRepositoryTest.kt`, `AT…/ui/playlists/PlaylistsUiTest.kt`

**Interfaces:**
- Consumes: T4 DAOs, T2 `LibraryRepository.library`.
- Produces:
  - `enum class SmartPlaylist { RECENTLY_ADDED, MOST_PLAYED, RECENTLY_PLAYED }` — recently added = `dateAddedSec` within last 30 days, newest first, max 100; most/recently played max 100.
  - `data class PlaylistSummary(id: Long, name: String, songCount: Int)`, `data class PlaylistDetail(id: Long, name: String, songs: List<IndexedSong>)`, `data class IndexedSong(position: Int, song: Song)`.
  - `class PlaylistRepository(dao: PlaylistDao, stats: PlayStatDao, library: StateFlow<Library>, clock: () -> Long)`: `playlists(): Flow<List<PlaylistSummary>>`, `detail(id): Flow<PlaylistDetail?>`, `smart(kind): Flow<List<Song>>`, `create(name): Long`, `rename`, `delete`, `add(id, songs)`, `remove(id, position): IndexedSong`, `undoRemove(id, removed: IndexedSong)`, `move(id, from, to)`.

- [ ] **Step 1: Failing `PlaylistRepositoryTest`** (in-memory Room + fake library): `detail_hidesDeletedSongs` (song id absent from library → not listed, positions of remaining unchanged), `remove_thenUndo_restoresPosition`, `smart_recentlyAdded_last30DaysOnly`, `smart_mostPlayed_orderedByCount`, `summary_countsOnlyExistingSongs`, `create_blankName_rejected` (throws `IllegalArgumentException`; names trimmed).
- [ ] **Step 2: Run → FAIL; implement; → PASS.**
- [ ] **Step 3: Failing `PlaylistsUiTest`:** `create_rename_delete`, `addToPlaylist_fromSongMenu`, `swipeRemove_showsUndo_restores` (snackbar `Removed "…"` / `Undo`), `dragReorder_persists`, `smartPlaylists_listedFirst`.
- [ ] **Step 4: Implement; run → PASS. Step 5: Commit** `feat: playlists`.

---

### Task 15: M3U import and export

**Files:**
- Create: `…/data/playlists/M3u.kt` (parser + writer), `…/data/playlists/PlaylistMatcher.kt`
- Modify: `…/ui/playlists/PlaylistsScreen.kt` (Import), `PlaylistDetailScreen.kt` (Export) using `ActivityResultContracts.OpenDocument(arrayOf("audio/x-mpegurl","audio/mpegurl","application/vnd.apple.mpegurl","*/*"))` and `CreateDocument("audio/x-mpegurl")`
- Test: `T…/data/playlists/M3uTest.kt`, `T…/data/playlists/PlaylistMatcherTest.kt`

**Interfaces:**
- Consumes: T1 `Song`, `Library`; T14 `PlaylistRepository.create/add`.
- Produces:
  - `data class M3uEntry(path: String, durationSec: Int?, artist: String?, title: String?)`
  - `object M3u { fun parse(text: String): List<M3uEntry>; fun write(songs: List<Song>): String }` — write: `#EXTM3U`, per song `#EXTINF:<sec>,<artist> - <title>` then `relativePath + displayName` (e.g. `Music/Artist/Album/01.mp3`), `\n` line endings, UTF-8 without BOM, file name `<playlist name>.m3u8`.
  - `data class MatchResult(songIds: List<Long>, matched: Int, total: Int)`; `object PlaylistMatcher { fun match(entries: List<M3uEntry>, library: Library): MatchResult }` — normalize path (`\` → `/`, strip `file://` + URL-decode, strip leading `/storage/emulated/0/`, `/sdcard/`, `./`, any `/storage/XXXX-XXXX/`), case-insensitive; match when song key `relativePath+displayName` equals the normalized path or either ends with `/`+other; else title+artist (case-insensitive) with duration within ±2 s (if duration known). First match wins; order preserved; unmatched skipped.
  - UI result toast: `%1$d of %2$d matched`; imported playlist named after file (without extension), deduplicated with ` (2)` suffix.

- [ ] **Step 1: Failing tests:** `M3uTest.write_formatsExtinfAndPaths`, `parse_roundTripsWrite`, `parse_windowsStyle` (BOM + CRLF + `C:\Music\A\01.mp3`), `parse_ignoresCommentsAndBlankLines`, `parse_plainListWithoutExtinf`; `PlaylistMatcherTest.match_relativePath`, `match_absoluteAndUriPaths` (`/storage/emulated/0/Music/a.mp3`, `file:///sdcard/Music/a%20b.mp3`), `match_fallbackTitleArtistDuration`, `match_durationOutside2s_noMatch`, `match_reportsCounts`.
- [ ] **Step 2: Run → FAIL; implement; → PASS.**
- [ ] **Step 3: Wire UI;** manual check: export a playlist, re-import it → `N of N matched`. **Step 4: Commit** `feat: M3U import/export`.

---

### Task 16: Equalizer

**Files:**
- Create: `…/playback/eq/AudioEffectsBackend.kt`, `…/playback/eq/PlatformAudioEffectsBackend.kt`, `…/playback/eq/EqState.kt`, `…/playback/eq/EqRepository.kt`, `…/playback/eq/EqualizerController.kt`, `…/ui/equalizer/EqualizerScreen.kt`
- Modify: `…/playback/PlaybackService.kt` (attach on `onAudioSessionIdChanged`, set `player.volume`), Now Playing secondary controls (hide EQ button when unsupported)
- Test: `T…/playback/eq/EqualizerControllerTest.kt`, `T…/playback/eq/EqRepositoryTest.kt`

**Interfaces:**
- Consumes: T3 DataStore instance; T5 service.
- Produces:
  - `data class EqCapabilities(bandCount: Int, centerFreqsHz: List<Int>, levelRangeMb: IntRange, devicePresets: List<String>, bassBoostSupported: Boolean)`
  - `interface AudioEffectsBackend { fun attach(audioSessionId: Int): EqCapabilities?; fun setEnabled(enabled: Boolean); fun setBandLevels(levelsMb: List<Int>); fun setBassBoost(strength: Int /*0..1000*/); fun deviceBandLevels(presetIndex: Int): List<Int>; fun release() }` — platform impl catches `RuntimeException`/`UnsupportedOperationException` in `attach` → null.
  - `sealed interface PresetRef { data class Device(index: Int); data class Custom(name: String) }` (serializable)
  - `data class EqState(enabled: Boolean = false, preset: PresetRef? = null, bandLevelsMb: List<Int> = emptyList(), bassBoost: Int = 0, preampDb: Float = 0f, customPresets: Map<String, List<Int>> = emptyMap())`
  - `class EqRepository(store: DataStore<Preferences>) { val state: Flow<EqState>; suspend fun update(t: (EqState) -> EqState) }` (JSON-encoded under one key).
  - `fun preampToVolume(db: Float): Float` = `10^(db.coerceIn(-6f, 0f)/20)`.
  - `class EqualizerController(backend: AudioEffectsBackend, repo: EqRepository, scope: CoroutineScope, setVolume: (Float) -> Unit) { val capabilities: StateFlow<EqCapabilities?>; fun onAudioSessionId(id: Int); fun setEnabled(b: Boolean); fun selectDevicePreset(i: Int); fun selectCustomPreset(name: String); fun setBand(index: Int, levelMb: Int); fun setBassBoost(v: Int); fun setPreamp(db: Float); fun saveCustomPreset(name: String) }`

- [ ] **Step 1: Failing tests** with a `FakeBackend` recording calls: `unsupported_capabilitiesNull`, `sessionChange_reattachesAndReappliesState`, `persistedState_appliedOnFirstAttach`, `bandLevels_clampedToRange`, `devicePreset_copiesLevelsAndMarksPreset`, `editingBand_switchesPresetToNull`, `saveCustomPreset_storedAndSelected`, `disable_keepsLevels`, `preampToVolume(-6f) ≈ 0.501f` (±0.001), `preampToVolume(3f) == 1f`; `EqRepositoryTest.roundTrip`, `corruptJson_returnsDefault`.
- [ ] **Step 2: Run → FAIL; implement; → PASS.**
- [ ] **Step 3: Build `EqualizerScreen`** (switch, preset dropdown, one vertical slider per band labelled with Hz/kHz, bass boost slider only if supported, preamp slider −6..0 dB, "Save as preset" dialog, "System sound settings" row shown only if `AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL` resolves). Manual check on device: audible change; app survives on an emulator with no EQ (button hidden).
- [ ] **Step 4: Commit** `feat: equalizer`.

---

### Task 17: Settings screen, skin picker, folder scanning

**Files:**
- Create: `…/ui/settings/SettingsScreen.kt`, `…/ui/settings/SkinPickerScreen.kt`, `…/ui/settings/SkinPreviewCard.kt`, `…/library/FolderScanner.kt`, `…/library/DocumentPaths.kt`, `…/ui/settings/AboutScreen.kt`
- Modify: `…/AppContainer.kt`, `…/library/LibraryRepository.kt` consumers (rescan after folder add)
- Test: `T…/library/DocumentPathsTest.kt`, `AT…/ui/settings/SettingsUiTest.kt`

**Interfaces:**
- Consumes: T3 settings, T10 `SkinManager`, T2 repository, T13 `NowPlayingLayout` (preview cards render a miniature Now Playing with fake state under each skin's `AppTheme`).
- Produces:
  - `object DocumentPaths { fun toFilePath(treeDocumentId: String): String? }` — `primary:Music/X` → `/storage/emulated/0/Music/X`; `ABCD-1234:Music` → `/storage/ABCD-1234/Music`; `primary:` → `/storage/emulated/0`; anything else (e.g. `raw:/…` → path after `raw:`; `msf:12`/unknown) → null.
  - `class FolderScanner(context: Context) { suspend fun scan(treeUri: Uri): Int }` — takes persistable read permission, walks with `DocumentFile`, collects audio extensions (`mp3,m4a,aac,flac,ogg,oga,opus,wav,amr,mka,webm,mp4`), maps to paths via `DocumentPaths`, calls `MediaScannerConnection.scanFile`, returns count; unknown path → 0.
  - Settings sections exactly per spec §4: Theme (System/Light/Dark, Dynamic colour switch on API 31+), Skins (→ picker), Library (Minimum length: Off/15 s/30 s/60 s; Excluded folders chosen from distinct `relativePath`s in library; Folders to scan (add/remove); Rescan now), Equalizer, About (version, open-source licences list).
  - Skin picker: grid of `SkinPreviewCard`s; tap → `SkinManager.apply`; "Import skin" (`OpenDocument("*/*")`) → on `Failed(message)` show the message in a dialog; on `AlreadyExists` confirm "Replace"; long-press imported → Delete / Export (`CreateDocument("application/zip")`, name `<id>.mskin`); built-ins Export only.

- [ ] **Step 1: Failing `DocumentPathsTest`** for each mapping above. **Step 2: Run → FAIL; implement; → PASS.**
- [ ] **Step 3: Failing `SettingsUiTest`:** `themeSwitch_persists`, `minLength_updatesFilter`, `skinTap_appliesImmediately` (Now Playing primary color changes without activity restart), `importInvalid_showsMessage` (fake manager returns `Failed("skin.json not found")`), `deleteImported_removesCard`.
- [ ] **Step 4: Implement; run → PASS.** Manual: add a folder containing an `.opus` file MediaStore skipped → appears after scan.
- [ ] **Step 5: Commit** `feat: settings, skin picker, folder scanning`.

---

### Task 18: Home-screen widget (Glance)

**Files:**
- Create: `…/widget/PlayerWidget.kt`, `…/widget/PlayerWidgetReceiver.kt`, `…/widget/WidgetState.kt`, `…/widget/WidgetActions.kt`, `…/widget/WidgetUpdater.kt`, `app/src/main/res/xml/player_widget_info.xml`
- Modify: `AndroidManifest.xml` (receiver), `…/playback/PlaybackService.kt` (notify updater)
- Test: `T…/widget/WidgetUpdaterTest.kt`, `T…/widget/PlayerWidgetContentTest.kt` (glance-appwidget-testing)

**Interfaces:**
- Consumes: T5 service events, T10 `SkinManager.active` (colors/corner), T6 resumption.
- Produces:
  - `data class WidgetState(title: String?, artist: String?, albumId: Long?, isPlaying: Boolean, shuffle: Boolean, repeatMode: Int, progress: Float)`
  - `object WidgetUpdater { fun shouldUpdate(old: WidgetState?, new: WidgetState): Boolean; suspend fun push(context: Context, state: WidgetState) }` — `shouldUpdate` ignores `progress`-only changes.
  - Sizes via `SizeMode.Responsive(setOf(DpSize(250.dp, 50.dp) /*4×1*/, DpSize(250.dp, 110.dp) /*4×2*/))`; widget info `minWidth=250dp minHeight=40dp`, `targetCellWidth=4 targetCellHeight=1`, resizable vertically.
  - `WidgetActions`: `PrevAction`, `PlayPauseAction`, `NextAction`, `ShuffleAction`, `RepeatAction` (`ActionCallback`s that connect a `MediaController`, act, release); `PlayPause` when idle → `prepare()` + `play()` (session restore via T6); root tap opens `MainActivity`.
  - Colors: `ColorProviders` from active skin scheme (day/night); dynamic colors on API 31+ when skin `colorMode == SYSTEM`; corner radius `cornerRadiusDp` (API 31+ system rounding otherwise).

- [ ] **Step 1: Failing tests:** `WidgetUpdaterTest.progressOnly_noUpdate`, `trackChange_updates`, `playPause_updates`, `shuffleRepeat_updates`, `firstState_updates`; `PlayerWidgetContentTest.small_hasThreeControls_noProgress`, `medium_showsProgressAndShuffleRepeat`, `noTrack_showsAppNameAndPlay`.
- [ ] **Step 2: Run → FAIL; implement; → PASS.**
- [ ] **Step 3: Manual:** add both sizes to the home screen, control playback, switch skin → widget recolours on next update. **Step 4: Commit** `feat: home screen widget`.

---

### Task 19: Performance, release build and verification

**Files:**
- Create: `baselineprofile/build.gradle.kts`, `baselineprofile/src/main/java/io/github/aceattacker77/nakedmusicplayer/baselineprofile/{BaselineProfileGenerator,StartupBenchmark,ScrollBenchmark}.kt`, `scripts/seed_library.sh`, `scripts/check_release.sh`, `docs/manual-test-checklist.md`
- Modify: `settings.gradle.kts`, `app/build.gradle.kts` (release: `isMinifyEnabled = true`, `isShrinkResources = true`, R8 full mode; `baselineProfile` plugin; `profileinstaller`), `app/proguard-rules.pro` (keep `@Serializable` skin DTOs)

**Interfaces:**
- Consumes: whole app. Benchmarks find UI by test tags `song-list` and `song-<id>` (T11).

- [ ] **Step 1: `scripts/seed_library.sh`** — via `adb`, push `tone.mp3` copies as `Music/Seed/<n>.mp3` for n=1..5000 with varied ID3 titles/artists/albums (ffmpeg `-metadata`), then trigger a media scan.
- [ ] **Step 2: Write benchmarks:** `StartupBenchmark` (cold, `CompilationMode.Partial(BaselineProfileMode.Require)`, 10 iterations), `ScrollBenchmark` (`FrameTimingMetric`, fling `song-list` 10×), `BaselineProfileGenerator` (launch, scroll songs, open album, expand player).
- [ ] **Step 3: Generate profile** `./gradlew :app:generateBaselineProfile` → `app/src/release/generated/baselineProfiles/baseline-prof.txt` exists.
- [ ] **Step 4: Run benchmarks** on a physical 120 Hz device (`./gradlew :baselineprofile:connectedBenchmarkAndroidTest`). Pass criteria: `timeToInitialDisplayMs` median < 400; scroll `frameOverrunMs` P99 < 0 (no dropped frames). If failing, profile and fix before continuing.
- [ ] **Step 5: `scripts/check_release.sh`** — builds `assembleRelease`, fails if APK > 6 MB (`stat`), fails if `aapt2 dump permissions` lists `android.permission.INTERNET`. Run → PASS.
- [ ] **Step 6: Memory check:** with 5,000 songs, browse all tabs and Now Playing, `adb shell dumpsys meminfo io.github.aceattacker77.nakedmusicplayer` TOTAL PSS < 120 MB.
- [ ] **Step 7: Run all tests on API 26 emulator** (`connectedDebugAndroidTest`) and API 36 → PASS.
- [ ] **Step 8: Write `docs/manual-test-checklist.md`** (spec §12 manual list: Bluetooth buttons, incoming call, lock screen, widget, unplug headphones, predictive back, foldable/tablet, Android 8 device, process death via `adb shell am kill` then resume) and execute it once; tick results.
- [ ] **Step 9: Commit** `perf: baseline profile, release checks, manual checklist`.
