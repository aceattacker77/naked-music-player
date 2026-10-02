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

    val permissionDenied: StateFlow<Boolean> = _permissionDenied

    val library: StateFlow<Library> = combine(rows.filterNotNull(), filter) { raw, f ->
        LibraryBuilder.build(raw, f)
    }.flowOn(computeDispatcher).stateIn(scope, SharingStarted.Eagerly, Library.EMPTY)

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
            rows.value = source.query()
            _permissionDenied.value = false
        } catch (e: SecurityException) {
            rows.value = emptyList()
            _permissionDenied.value = true
        }
    }
}
