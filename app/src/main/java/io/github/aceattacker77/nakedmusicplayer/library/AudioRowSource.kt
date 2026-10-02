package io.github.aceattacker77.nakedmusicplayer.library

import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import kotlinx.coroutines.flow.Flow

interface AudioRowSource {
    /** May throw [SecurityException] when audio permission is missing or revoked. */
    suspend fun query(): List<AudioRow>

    /** Emits whenever the underlying audio collection changes. */
    fun changes(): Flow<Unit>
}
