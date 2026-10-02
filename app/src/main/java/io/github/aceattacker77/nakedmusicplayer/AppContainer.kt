package io.github.aceattacker77.nakedmusicplayer

import android.app.Application
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import io.github.aceattacker77.nakedmusicplayer.data.db.AppDatabase
import io.github.aceattacker77.nakedmusicplayer.data.session.SessionStore
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import io.github.aceattacker77.nakedmusicplayer.library.LibraryRepository
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.File

/** Manual DI root, created once in [MusicApp]. Later tasks add fields. */
class AppContainer(private val app: Application) {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val dataStore by lazy {
        PreferenceDataStoreFactory.create(scope = applicationScope) { app.preferencesDataStoreFile("settings") }
    }

    val settingsRepository by lazy { SettingsRepository(dataStore) }
    val sessionStore by lazy { SessionStore(dataStore) }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(app, AppDatabase::class.java, "music.db").build()
    }

    val libraryRepository by lazy {
        LibraryRepository(
            source = MediaStoreAudioRowSource(app),
            filter = settingsRepository.settings.map { it.libraryFilter() }.distinctUntilChanged(),
            scope = applicationScope,
        )
    }

    /** Connects to the playback service on first use; the MediaController must live on the main thread. */
    val playerConnection: Deferred<PlayerConnection> by lazy {
        applicationScope.async(Dispatchers.Main) { PlayerConnection.connect(app, applicationScope) }
    }

    /** Built-in skins are parsed once here; imported ones are read from `filesDir/skins`. */
    val skinManager: SkinManager by lazy {
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
