package io.github.aceattacker77.nakedmusicplayer.playback

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

/** In-process record of items that failed to play; the UI shows a "can't play" icon for them. */
object UnplayableRegistry {
    private val _ids = MutableStateFlow<Set<String>>(emptySet())
    val ids: StateFlow<Set<String>> = _ids

    fun mark(mediaId: String) {
        _ids.update { it + mediaId }
    }
}
