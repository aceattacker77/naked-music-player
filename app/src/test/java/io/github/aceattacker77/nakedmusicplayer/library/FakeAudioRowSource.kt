package io.github.aceattacker77.nakedmusicplayer.library

import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** Controllable [AudioRowSource] for tests: set [rows], emit on [changeFlow], or set [throwOnQuery]. */
class FakeAudioRowSource : AudioRowSource {
    var rows: List<AudioRow> = emptyList()
    var throwOnQuery: Throwable? = null
    var queryCount = 0
    val changeFlow = MutableSharedFlow<Unit>(extraBufferCapacity = 16)

    override suspend fun query(): List<AudioRow> {
        queryCount++
        throwOnQuery?.let { throw it }
        return rows
    }

    override fun changes(): Flow<Unit> = changeFlow
}
