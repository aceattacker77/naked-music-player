package io.github.aceattacker77.nakedmusicplayer.ui.settings

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.library.FolderScanner
import io.github.aceattacker77.nakedmusicplayer.library.LibraryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val libraryRepository: LibraryRepository,
    private val folderScanner: FolderScanner,
) : ViewModel() {
    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    /**
     * Folders offered for exclusion: those of the songs in the library plus the ones already
     * excluded (whose songs are hidden, so they would otherwise vanish from the list and could
     * never be re-included).
     */
    val folderOptions: StateFlow<List<String>> = combine(libraryRepository.library, settings) { library, settings ->
        (library.songs.map { it.relativePath } + settings.excludedFolders).filter { it.isNotEmpty() }.distinct().sorted()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun setThemeMode(mode: ThemeMode) = update { it.copy(themeMode = mode) }

    fun setDynamicColor(enabled: Boolean) = update { it.copy(dynamicColor = enabled) }

    fun setMinDuration(ms: Long) = update { it.copy(minDurationMs = ms) }

    fun setExcludedFolders(folders: Set<String>) = update { it.copy(excludedFolders = folders) }

    fun addScanFolder(uri: Uri) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(scanFolderUris = it.scanFolderUris + uri.toString()) }
            folderScanner.scan(uri)
            libraryRepository.refresh()
        }
    }

    fun removeScanFolder(uri: String) {
        viewModelScope.launch {
            settingsRepository.update { it.copy(scanFolderUris = it.scanFolderUris - uri) }
            folderScanner.release(Uri.parse(uri))
        }
    }

    fun rescan() {
        viewModelScope.launch {
            settingsRepository.settings.first().scanFolderUris.forEach { folderScanner.scan(Uri.parse(it)) }
            libraryRepository.refresh()
        }
    }

    private fun update(transform: (AppSettings) -> AppSettings) {
        viewModelScope.launch { settingsRepository.update(transform) }
    }
}
