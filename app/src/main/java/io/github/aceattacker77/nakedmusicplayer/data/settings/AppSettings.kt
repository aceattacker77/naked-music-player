package io.github.aceattacker77.nakedmusicplayer.data.settings

import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter

enum class ThemeMode { SYSTEM, LIGHT, DARK }

data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val activeSkinId: String = "builtin.default",
    val minDurationMs: Long = 30_000,
    val excludedFolders: Set<String> = emptySet(),
    val scanFolderUris: Set<String> = emptySet(),
    val songSort: SongSort = SongSort.TITLE,
) {
    fun libraryFilter(): LibraryFilter = LibraryFilter(minDurationMs, excludedFolders)
}
