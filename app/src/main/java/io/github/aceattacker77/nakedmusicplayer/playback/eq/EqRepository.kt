package io.github.aceattacker77.nakedmusicplayer.playback.eq

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlin.math.pow

/** Equalizer settings as one JSON document in the app DataStore; unreadable data means defaults. */
class EqRepository(private val store: DataStore<Preferences>) {
    private val key = stringPreferencesKey(KEY)
    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    val state: Flow<EqState> = store.data.map { decode(it[key]) }

    suspend fun update(transform: (EqState) -> EqState) {
        store.edit { prefs -> prefs[key] = json.encodeToString(EqState.serializer(), transform(decode(prefs[key]))) }
    }

    private fun decode(text: String?): EqState =
        text?.let { runCatching { json.decodeFromString(EqState.serializer(), it) }.getOrNull() } ?: EqState()

    companion object {
        const val KEY = "eq_state"
    }
}

/** Preamp as player volume: −6..0 dB becomes 0.501..1.0, so boosted bands do not clip. */
fun preampToVolume(db: Float): Float = 10f.pow(db.coerceIn(-6f, 0f) / 20f)
