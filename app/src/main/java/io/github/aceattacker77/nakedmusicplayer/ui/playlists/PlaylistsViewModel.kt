package io.github.aceattacker77.nakedmusicplayer.ui.playlists

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aceattacker77.nakedmusicplayer.data.playlists.IndexedSong
import io.github.aceattacker77.nakedmusicplayer.data.playlists.PlaylistDetail
import io.github.aceattacker77.nakedmusicplayer.data.playlists.PlaylistRepository
import io.github.aceattacker77.nakedmusicplayer.data.playlists.PlaylistSummary
import io.github.aceattacker77.nakedmusicplayer.data.playlists.SmartPlaylist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PlaylistsViewModel(private val repository: PlaylistRepository) : ViewModel() {
    val playlists: StateFlow<List<PlaylistSummary>> = repository.playlists()
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun detail(id: Long): Flow<PlaylistDetail?> = repository.detail(id)

    fun smart(kind: SmartPlaylist): Flow<List<Song>> = repository.smart(kind)

    fun create(name: String) {
        viewModelScope.launch { repository.create(name) }
    }

    fun createAndAdd(name: String, songs: List<Song>) {
        viewModelScope.launch {
            val id = repository.create(name)
            repository.add(id, songs)
        }
    }

    fun rename(id: Long, name: String) {
        viewModelScope.launch { repository.rename(id, name) }
    }

    fun delete(id: Long) {
        viewModelScope.launch { repository.delete(id) }
    }

    fun add(id: Long, songs: List<Song>) {
        viewModelScope.launch { repository.add(id, songs) }
    }

    fun move(id: Long, from: Int, to: Int) {
        viewModelScope.launch { repository.move(id, from, to) }
    }

    /** Removes an entry; [onRemoved] receives it so the caller can offer Undo. */
    fun remove(id: Long, position: Int, onRemoved: (IndexedSong) -> Unit) {
        viewModelScope.launch { onRemoved(repository.remove(id, position)) }
    }

    fun undoRemove(id: Long, removed: IndexedSong) {
        viewModelScope.launch { repository.undoRemove(id, removed) }
    }

    suspend fun importM3u(fileName: String, text: String, fallbackName: String) =
        repository.importM3u(fileName, text, fallbackName)

    suspend fun exportM3u(id: Long) = repository.exportM3u(id)
}
