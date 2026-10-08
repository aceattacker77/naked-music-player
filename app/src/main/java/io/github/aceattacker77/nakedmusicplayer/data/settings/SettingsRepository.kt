package io.github.aceattacker77.nakedmusicplayer.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

object SettingsKeys {
    const val THEME_MODE = "theme_mode"
    const val DYNAMIC_COLOR = "dynamic_color"
    const val ACTIVE_SKIN_ID = "active_skin_id"
    const val MIN_DURATION_MS = "min_duration_ms"
    const val EXCLUDED_FOLDERS = "excluded_folders"
    const val SCAN_FOLDER_URIS = "scan_folder_uris"
    const val SONG_SORT = "song_sort"
    const val WIDGET_LIVE_PROGRESS = "widget_live_progress"
}

class SettingsRepository(private val store: DataStore<Preferences>) {
    private val themeMode = stringPreferencesKey(SettingsKeys.THEME_MODE)
    private val dynamicColor = booleanPreferencesKey(SettingsKeys.DYNAMIC_COLOR)
    private val activeSkinId = stringPreferencesKey(SettingsKeys.ACTIVE_SKIN_ID)
    private val minDurationMs = longPreferencesKey(SettingsKeys.MIN_DURATION_MS)
    private val excludedFolders = stringSetPreferencesKey(SettingsKeys.EXCLUDED_FOLDERS)
    private val scanFolderUris = stringSetPreferencesKey(SettingsKeys.SCAN_FOLDER_URIS)
    private val songSort = stringPreferencesKey(SettingsKeys.SONG_SORT)
    private val widgetLiveProgress = booleanPreferencesKey(SettingsKeys.WIDGET_LIVE_PROGRESS)

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            themeMode = p[themeMode].toEnum(d.themeMode),
            dynamicColor = p[dynamicColor] ?: d.dynamicColor,
            activeSkinId = p[activeSkinId] ?: d.activeSkinId,
            minDurationMs = p[minDurationMs] ?: d.minDurationMs,
            excludedFolders = p[excludedFolders] ?: d.excludedFolders,
            scanFolderUris = p[scanFolderUris] ?: d.scanFolderUris,
            songSort = p[songSort].toEnum(d.songSort),
            widgetLiveProgress = p[widgetLiveProgress] ?: d.widgetLiveProgress,
        )
    }

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val current = settingsOf(p)
            val next = transform(current)
            p[themeMode] = next.themeMode.name
            p[dynamicColor] = next.dynamicColor
            p[activeSkinId] = next.activeSkinId
            p[minDurationMs] = next.minDurationMs
            p[excludedFolders] = next.excludedFolders
            p[scanFolderUris] = next.scanFolderUris
            p[songSort] = next.songSort.name
            p[widgetLiveProgress] = next.widgetLiveProgress
        }
    }

    private fun settingsOf(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            themeMode = p[themeMode].toEnum(d.themeMode),
            dynamicColor = p[dynamicColor] ?: d.dynamicColor,
            activeSkinId = p[activeSkinId] ?: d.activeSkinId,
            minDurationMs = p[minDurationMs] ?: d.minDurationMs,
            excludedFolders = p[excludedFolders] ?: d.excludedFolders,
            scanFolderUris = p[scanFolderUris] ?: d.scanFolderUris,
            songSort = p[songSort].toEnum(d.songSort),
            widgetLiveProgress = p[widgetLiveProgress] ?: d.widgetLiveProgress,
        )
    }
}

private inline fun <reified E : Enum<E>> String?.toEnum(default: E): E =
    this?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default
