package io.github.aceattacker77.nakedmusicplayer.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aceattacker77.nakedmusicplayer.library.LibraryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn

class SearchViewModel(
    libraryRepository: LibraryRepository,
    computeDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query

    val results: StateFlow<SearchResults> = combine(_query, libraryRepository.library) { query, library ->
        SearchEngine.search(library, query)
    }.flowOn(computeDispatcher).stateIn(viewModelScope, SharingStarted.Eagerly, SearchResults.EMPTY)

    fun setQuery(query: String) {
        _query.value = query
    }
}
