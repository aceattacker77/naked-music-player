package io.github.aceattacker77.nakedmusicplayer.data

import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsKeys
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Test

class SettingsRepositoryTest {
    private fun newStore() = InMemoryPreferencesStore()

    @Test fun defaults_whenEmpty() = runTest(UnconfinedTestDispatcher()) {
        val repo = SettingsRepository(newStore())
        assertThat(repo.settings.first()).isEqualTo(AppSettings())
    }

    @Test fun update_roundTripsAllFields() = runTest(UnconfinedTestDispatcher()) {
        val repo = SettingsRepository(newStore())
        val changed = AppSettings(
            themeMode = ThemeMode.DARK,
            dynamicColor = false,
            activeSkinId = "com.example.neon",
            minDurationMs = 60_000,
            excludedFolders = setOf("Music/WhatsApp/", "Recordings/"),
            scanFolderUris = setOf("content://tree/primary%3AMusic"),
            songSort = SongSort.DATE_ADDED,
            widgetLiveProgress = true,
        )
        repo.update { changed }
        assertThat(repo.settings.first()).isEqualTo(changed)
    }

    @Test fun widgetLiveProgress_isOffByDefault() = runTest(UnconfinedTestDispatcher()) {
        assertThat(SettingsRepository(newStore()).settings.first().widgetLiveProgress).isFalse()
    }

    @Test fun libraryFilter_mapsMinDurationAndFolders() {
        val filter = AppSettings(minDurationMs = 15_000, excludedFolders = setOf("A/")).libraryFilter()
        assertThat(filter.minDurationMs).isEqualTo(15_000)
        assertThat(filter.excludedFolders).containsExactly("A/")
    }

    @Test fun unknownEnumValue_fallsBackToDefault() = runTest(UnconfinedTestDispatcher()) {
        val store = newStore()
        store.edit {
            it[stringPreferencesKey(SettingsKeys.SONG_SORT)] = "NOT_A_SORT"
            it[stringPreferencesKey(SettingsKeys.THEME_MODE)] = "PURPLE"
        }
        val settings = SettingsRepository(store).settings.first()
        assertThat(settings.songSort).isEqualTo(SongSort.TITLE)
        assertThat(settings.themeMode).isEqualTo(ThemeMode.SYSTEM)
    }
}
