package io.github.aceattacker77.nakedmusicplayer

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import io.github.aceattacker77.nakedmusicplayer.data.db.AppDatabase
import io.github.aceattacker77.nakedmusicplayer.data.playlists.PlaylistRepository
import io.github.aceattacker77.nakedmusicplayer.data.session.SessionStore
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import io.github.aceattacker77.nakedmusicplayer.library.LibraryRepository
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqRepository
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqualizerController
import io.github.aceattacker77.nakedmusicplayer.playback.eq.PlatformAudioEffectsBackend
import io.github.aceattacker77.nakedmusicplayer.library.MediaStoreAudioRowSource
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerConnection
import io.github.aceattacker77.nakedmusicplayer.ui.skins.BitmapImageProbe
import io.github.aceattacker77.nakedmusicplayer.ui.skins.BuiltInSkins
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinArchiveReader
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinManager
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinParser
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinStore
import io.github.aceattacker77.nakedmusicplayer.ui.skins.TypefaceFontProbe
import io.github.aceattacker77.nakedmusicplayer.ui.player.connect
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.io.File

/** Manual DI root, created once in [MusicApp]. Later tasks add fields. */
open class AppContainer(private val app: Application) {
    open val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Where CPU-bound library work (grouping, sorting) runs; tests swap in an immediate dispatcher. */
    open val computeDispatcher: CoroutineDispatcher = Dispatchers.Default

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = applicationScope) { app.preferencesDataStoreFile("settings") }
    }

    open val settingsRepository: SettingsRepository by lazy { SettingsRepository(dataStore) }
    open val sessionStore: SessionStore by lazy { SessionStore(dataStore) }

    open val database: AppDatabase by lazy {
        Room.databaseBuilder(app, AppDatabase::class.java, "music.db").build()
    }

    open val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            source = MediaStoreAudioRowSource(app),
            filter = settingsRepository.settings.map { it.libraryFilter() }.distinctUntilChanged(),
            scope = applicationScope,
            computeDispatcher = computeDispatcher,
        )
    }

    /**
     * Where the equalizer preamp reaches the player. The playback service sets this while it runs
     * (and clears it on destroy); the player itself must only be touched on its own thread.
     */
    @Volatile
    var equalizerVolumeSink: ((Float) -> Unit)? = null

    open val equalizerController: EqualizerController by lazy {
        EqualizerController(PlatformAudioEffectsBackend(), EqRepository(dataStore), applicationScope) { volume ->
            equalizerVolumeSink?.invoke(volume)
        }
    }

    open val playlistRepository: PlaylistRepository by lazy {
        PlaylistRepository(
            dao = database.playlistDao(),
            stats = database.playStatDao(),
            library = libraryRepository.library,
            clock = System::currentTimeMillis,
        )
    }

    /**
     * Starts long-lived background jobs. Stored playlist entries and play stats are pruned against
     * the unfiltered song ids after every successful library load; failed loads (permission
     * revoked, storage unavailable) publish nothing, so they can never wipe user data.
     */
    open fun startBackgroundWork() {
        applicationScope.launch {
            libraryRepository.allSongIds.filterNotNull().collect { ids ->
                if (ids.isNotEmpty()) playlistRepository.prune(ids)
            }
        }
    }

    /** Connects to the playback service on first use; the MediaController must live on the main thread. */
    open val playerConnection: Deferred<PlayerConnection> by lazy {
        applicationScope.async(Dispatchers.Main) { PlayerConnection.connect(app, applicationScope) }
    }

    /** Built-in skins are parsed once here; imported ones are read from `filesDir/skins`. */
    open val skinManager: SkinManager by lazy {
        val builtIns = BuiltInSkins.load(app.assets) { json, defaults -> SkinParser.parse(json, defaults) }
        val default = builtIns.first { it.id == BuiltInSkins.DEFAULT_ID }
        SkinManager(
            builtIns = builtIns,
            store = SkinStore(File(app.filesDir, "skins")) { SkinParser.parse(it, default) },
            reader = SkinArchiveReader(
                defaults = default,
                imageProbe = BitmapImageProbe(),
                fontProbe = TypefaceFontProbe(File(app.cacheDir, "skin-probe")),
            ),
            settings = settingsRepository,
            scope = applicationScope,
        )
    }
}
