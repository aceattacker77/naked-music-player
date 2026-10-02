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
import io.github.aceattacker77.nakedmusicplayer.ui.player.connect
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

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
}
