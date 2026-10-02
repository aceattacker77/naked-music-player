package io.github.aceattacker77.nakedmusicplayer.data.session

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first

data class SavedSession(
    val mediaIds: List<String>,
    val index: Int,
    val positionMs: Long,
    val shuffle: Boolean,
    val repeatMode: Int,
)

class SessionStore(private val store: DataStore<Preferences>) {
    private val ids = stringPreferencesKey("session_media_ids")
    private val index = intPreferencesKey("session_index")
    private val position = longPreferencesKey("session_position_ms")
    private val shuffle = booleanPreferencesKey("session_shuffle")
    private val repeat = intPreferencesKey("session_repeat_mode")

    suspend fun save(session: SavedSession) {
        store.edit { p ->
            p[ids] = session.mediaIds.joinToString("\n")
            p[index] = session.index
            p[position] = session.positionMs
            p[shuffle] = session.shuffle
            p[repeat] = session.repeatMode
        }
    }

    suspend fun load(): SavedSession? {
        val p = store.data.first()
        val mediaIds = p[ids]?.split('\n')?.filter { it.isNotEmpty() }.orEmpty()
        if (mediaIds.isEmpty()) return null
        return SavedSession(
            mediaIds = mediaIds,
            index = p[index] ?: 0,
            positionMs = p[position] ?: 0L,
            shuffle = p[shuffle] ?: false,
            repeatMode = p[repeat] ?: 0,
        )
    }

    suspend fun clear() {
        store.edit { p ->
            p.remove(ids); p.remove(index); p.remove(position); p.remove(shuffle); p.remove(repeat)
        }
    }
}
