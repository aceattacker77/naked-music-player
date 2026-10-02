package io.github.aceattacker77.nakedmusicplayer

import android.app.Application
import androidx.media3.common.Player
import androidx.room.Room
import io.github.aceattacker77.nakedmusicplayer.data.db.AppDatabase
import io.github.aceattacker77.nakedmusicplayer.data.InMemoryPreferencesStore
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import io.github.aceattacker77.nakedmusicplayer.library.FakeAudioRowSource
import io.github.aceattacker77.nakedmusicplayer.library.LibraryRepository
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqRepository
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqualizerController
import io.github.aceattacker77.nakedmusicplayer.playback.eq.FakeAudioEffectsBackend
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerConnection
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.UnconfinedTestDispatcher

/** [AppContainer] wired to in-memory fakes and an immediate dispatcher, for Robolectric UI tests. */
@OptIn(ExperimentalCoroutinesApi::class)
class TestContainer(
    app: Application,
    val source: FakeAudioRowSource,
    player: Player,
) : AppContainer(app) {
    private val dispatcher = UnconfinedTestDispatcher()
    private val prefs = InMemoryPreferencesStore()

    override val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)

    override val computeDispatcher: CoroutineDispatcher = dispatcher

    override val settingsRepository: SettingsRepository by lazy { SettingsRepository(prefs) }

    override val libraryRepository: LibraryRepository by lazy {
        LibraryRepository(
            source = source,
            filter = settingsRepository.settings.map { it.libraryFilter() }.distinctUntilChanged(),
            scope = applicationScope,
            computeDispatcher = computeDispatcher,
        )
    }

    // Room's executors are replaced by direct ones so queries finish on the calling thread and
    // Compose tests need no waiting on background work.
    override val database: AppDatabase by lazy {
        Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    // A device with no equalizer: the Now Playing EQ button stays hidden in UI tests.
    override val equalizerController: EqualizerController by lazy {
        EqualizerController(FakeAudioEffectsBackend(capabilities = null), EqRepository(prefs), applicationScope) {}
    }

    override val playerConnection: Deferred<PlayerConnection> =
        CompletableDeferred(PlayerConnection(player, applicationScope))
}
