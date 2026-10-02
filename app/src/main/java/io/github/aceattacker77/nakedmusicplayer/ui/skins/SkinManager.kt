package io.github.aceattacker77.nakedmusicplayer.ui.skins

import io.github.aceattacker77.nakedmusicplayer.data.settings.SettingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream

sealed interface ImportOutcome {
    data class Installed(val skin: Skin) : ImportOutcome
    data class AlreadyExists(val id: String) : ImportOutcome
    data class Failed(val message: String) : ImportOutcome
}

/** Owns the list of skins and which one is active; switching recomposes the UI with no restart. */
class SkinManager(
    private val builtIns: List<Skin>,
    private val store: SkinStore,
    private val reader: SkinArchiveReader,
    private val settings: SettingsRepository,
    scope: CoroutineScope,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val default = builtIns.first { it.id == BuiltInSkins.DEFAULT_ID }
    private val imported = MutableStateFlow<List<Skin>>(emptyList())

    val available: StateFlow<List<Skin>> = imported
        .map { builtIns + it.sortedBy { skin -> skin.name.lowercase() } }
        .stateIn(scope, SharingStarted.Eagerly, builtIns)

    val active: StateFlow<Skin> = combine(
        settings.settings.map { it.activeSkinId }.distinctUntilChanged(),
        available,
    ) { id, skins -> skins.firstOrNull { it.id == id } ?: default }
        .stateIn(scope, SharingStarted.Eagerly, default)

    init {
        scope.launch { refreshImported() }
    }

    suspend fun apply(id: String) {
        if (available.value.none { it.id == id }) return
        settings.update { it.copy(activeSkinId = id) }
    }

    suspend fun import(input: InputStream, replace: Boolean): ImportOutcome = withContext(ioDispatcher) {
        when (val read = reader.read(input)) {
            is SkinImportResult.Invalid -> ImportOutcome.Failed(read.message)
            is SkinImportResult.Valid -> {
                val id = read.skin.id
                if (builtIns.any { it.id == id }) {
                    ImportOutcome.Failed("id '$id' is reserved for built-in skins")
                } else {
                    try {
                        when (val result = store.install(read, replace)) {
                            is InstallResult.Installed -> {
                                refreshImported()
                                ImportOutcome.Installed(result.skin)
                            }
                            is InstallResult.AlreadyExists -> ImportOutcome.AlreadyExists(result.id)
                        }
                    } catch (e: IOException) {
                        ImportOutcome.Failed("skin could not be installed")
                    } catch (e: IllegalStateException) {
                        ImportOutcome.Failed("skin could not be installed")
                    }
                }
            }
        }
    }

    suspend fun delete(id: String) {
        if (builtIns.any { it.id == id }) return
        withContext(ioDispatcher) { store.delete(id) }
        refreshImported()
        if (settings.settings.first().activeSkinId == id) {
            settings.update { it.copy(activeSkinId = default.id) }
        }
    }

    private suspend fun refreshImported() {
        imported.value = withContext(ioDispatcher) { store.installed() }
    }
}
