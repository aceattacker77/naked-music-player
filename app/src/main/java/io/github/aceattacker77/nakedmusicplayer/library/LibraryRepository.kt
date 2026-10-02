package io.github.aceattacker77.nakedmusicplayer.library

import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.Library
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class LibraryRepository(
    private val source: AudioRowSource,
    filter: Flow<LibraryFilter>,
    private val scope: CoroutineScope,
    computeDispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val rows = MutableStateFlow<List<AudioRow>?>(null)
    private val _permissionDenied = MutableStateFlow(false)
    private val _allSongIds = MutableStateFlow<Set<Long>?>(null)

    val permissionDenied: StateFlow<Boolean> = _permissionDenied

    /**
     * Ids of every song MediaStore reported, ignoring the library filter; null until a query has
     * succeeded and after a permission failure. Stored playlists are pruned against this, so
     * tightening a filter (or losing permission) never deletes playlist entries.
     */
    val allSongIds: StateFlow<Set<Long>?> = _allSongIds

    // null until the first query has been turned into a Library.
    private val built: StateFlow<Library?> = combine(rows.filterNotNull(), filter) { raw, f ->
        LibraryBuilder.build(raw, f)
    }.flowOn(computeDispatcher).stateIn(scope, SharingStarted.Eagerly, null)

    val library: StateFlow<Library> = built.map { it ?: Library.EMPTY }.stateIn(scope, SharingStarted.Eagerly, Library.EMPTY)

    /** False until the first library is built (even an empty or permission-denied one), so the UI can tell "loading" from "empty". */
    val isLoaded: StateFlow<Boolean> = built.map { it != null }.stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch {
            load()
            source.changes().debounce(500).collect { load() }
        }
    }

    fun refresh() {
        scope.launch { load() }
    }

    private suspend fun load() {
        try {
            val result = source.query()
            rows.value = result
            _allSongIds.value = result.mapTo(HashSet()) { it.id }
            _permissionDenied.value = false
        } catch (e: SecurityException) {
            rows.value = emptyList()
            _allSongIds.value = null
            _permissionDenied.value = true
        }
    }
}
