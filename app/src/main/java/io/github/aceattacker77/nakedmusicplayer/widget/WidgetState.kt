package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

/** What the home-screen widget shows; kept small so it can live in the widget's own saved state. */
data class WidgetState(
    val title: String?,
    val artist: String?,
    val albumId: Long?,
    val isPlaying: Boolean,
    val shuffle: Boolean,
    val repeatMode: Int,
    val progress: Float,
) {
    val hasTrack: Boolean get() = title != null

    fun writeTo(prefs: MutablePreferences) {
        prefs.setOrRemove(TITLE, title)
        prefs.setOrRemove(ARTIST, artist)
        prefs.setOrRemove(ALBUM_ID, albumId)
        prefs[PLAYING] = isPlaying
        prefs[SHUFFLE] = shuffle
        prefs[REPEAT] = repeatMode
        prefs[PROGRESS] = progress.coerceIn(0f, 1f)
    }

    companion object {
        val IDLE = WidgetState(null, null, null, isPlaying = false, shuffle = false, repeatMode = 0, progress = 0f)

        private val TITLE = stringPreferencesKey("title")
        private val ARTIST = stringPreferencesKey("artist")
        private val ALBUM_ID = longPreferencesKey("album_id")
        private val PLAYING = booleanPreferencesKey("playing")
        private val SHUFFLE = booleanPreferencesKey("shuffle")
        private val REPEAT = intPreferencesKey("repeat")
        private val PROGRESS = floatPreferencesKey("progress")

        fun readFrom(prefs: Preferences) = WidgetState(
            title = prefs[TITLE],
            artist = prefs[ARTIST],
            albumId = prefs[ALBUM_ID],
            isPlaying = prefs[PLAYING] ?: false,
            shuffle = prefs[SHUFFLE] ?: false,
            repeatMode = prefs[REPEAT] ?: 0,
            progress = (prefs[PROGRESS] ?: 0f).coerceIn(0f, 1f),
        )

        private fun <T> MutablePreferences.setOrRemove(key: Preferences.Key<T>, value: T?) {
            if (value == null) remove(key) else this[key] = value
        }
    }
}
