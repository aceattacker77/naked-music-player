package io.github.aceattacker77.nakedmusicplayer.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import io.github.aceattacker77.nakedmusicplayer.library.LibraryRepository
import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.library.sortedWith
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerConnection
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State for the Songs / Albums / Artists tabs. The player connection is awaited lazily so the
 * library shows immediately while the playback service is still binding.
 */
class LibraryViewModel(
    libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    private val playerConnection: Deferred<PlayerConnection>,
    computeDispatcher: CoroutineDispatcher,
) : ViewModel() {
    val isLoaded: StateFlow<Boolean> = libraryRepository.isLoaded

    val sort: StateFlow<SongSort> = settingsRepository.settings
        .map { it.songSort }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SongSort.TITLE)

    val songs: StateFlow<List<Song>> = combine(libraryRepository.library, sort) { library, sort ->
        library.songs.sortedWith(sort)
    }.flowOn(computeDispatcher).stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val albums: StateFlow<List<Album>> = libraryRepository.library
        .map { it.albums }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val artists: StateFlow<List<Artist>> = libraryRepository.library
        .map { it.artists }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Media id of the item currently loaded in the player, for highlighting the playing row. */
    val currentMediaId: StateFlow<String?> = flow { emitAll(playerConnection.await().state.map { it.current?.mediaId }) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setSort(sort: SongSort) {
        viewModelScope.launch { settingsRepository.update { it.copy(songSort = sort) } }
    }

    fun play(songs: List<Song>, index: Int) {
        viewModelScope.launch { playerConnection.await().playSongs(songs, index) }
    }
}
