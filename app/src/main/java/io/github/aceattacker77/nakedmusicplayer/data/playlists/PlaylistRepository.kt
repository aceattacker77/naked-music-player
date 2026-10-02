package io.github.aceattacker77.nakedmusicplayer.data.playlists

import io.github.aceattacker77.nakedmusicplayer.data.db.PlayStatDao
import io.github.aceattacker77.nakedmusicplayer.data.db.PlaylistDao
import io.github.aceattacker77.nakedmusicplayer.library.model.Library
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

enum class SmartPlaylist { RECENTLY_ADDED, MOST_PLAYED, RECENTLY_PLAYED }

data class PlaylistSummary(val id: Long, val name: String, val songCount: Int)

/** A song with its real position in the stored playlist (positions have gaps while songs are missing). */
data class IndexedSong(val position: Int, val song: Song)

data class PlaylistDetail(val id: Long, val name: String, val songs: List<IndexedSong>)

/**
 * Playlists and smart playlists over the current [library]. Songs missing from storage are hidden
 * at read time; the stored rows are only pruned by [prune].
 */
class PlaylistRepository(
    private val dao: PlaylistDao,
    private val stats: PlayStatDao,
    private val library: StateFlow<Library>,
    private val clock: () -> Long,
) {
    fun playlists(): Flow<List<PlaylistSummary>> =
        combine(dao.observePlaylists(), dao.observeAllEntries(), library) { playlists, entries, lib ->
            val existing = lib.songs.mapTo(HashSet()) { it.id }
            val counts = entries.filter { it.songId in existing }.groupingBy { it.playlistId }.eachCount()
            playlists.map { PlaylistSummary(it.id, it.name, counts[it.id] ?: 0) }
        }

    fun detail(id: Long): Flow<PlaylistDetail?> =
        combine(dao.observePlaylists(), dao.observeSongIds(id), library) { playlists, songIds, lib ->
            val playlist = playlists.firstOrNull { it.id == id } ?: return@combine null
            val byId = lib.songs.associateBy { it.id }
            val songs = songIds.mapIndexedNotNull { position, songId -> byId[songId]?.let { IndexedSong(position, it) } }
            PlaylistDetail(playlist.id, playlist.name, songs)
        }

    fun smart(kind: SmartPlaylist): Flow<List<Song>> = when (kind) {
        SmartPlaylist.RECENTLY_ADDED -> library.map { lib ->
            val cutoffSec = clock() / 1000 - RECENT_DAYS * SECONDS_PER_DAY
            lib.songs.filter { it.dateAddedSec >= cutoffSec }.sortedByDescending { it.dateAddedSec }.take(SMART_LIMIT)
        }
        SmartPlaylist.MOST_PLAYED -> combine(stats.mostPlayed(SMART_LIMIT), library) { ids, lib -> resolve(ids, lib) }
        SmartPlaylist.RECENTLY_PLAYED -> combine(stats.recentlyPlayed(SMART_LIMIT), library) { ids, lib -> resolve(ids, lib) }
    }

    suspend fun create(name: String): Long = dao.create(validName(name), clock())

    suspend fun rename(id: Long, name: String) = dao.rename(id, validName(name), clock())

    suspend fun delete(id: Long) = dao.delete(id)

    suspend fun add(id: Long, songs: List<Song>) {
        if (songs.isNotEmpty()) dao.addSongs(id, songs.map { it.id }, clock())
    }

    /** Removes the entry at [position] and returns it so the caller can offer Undo. */
    suspend fun remove(id: Long, position: Int): IndexedSong {
        val songId = dao.observeSongIds(id).first().getOrNull(position)
            ?: throw IllegalArgumentException("no entry at position $position")
        val song = library.value.songs.first { it.id == songId }
        dao.removeAt(id, position, clock())
        return IndexedSong(position, song)
    }

    suspend fun undoRemove(id: Long, removed: IndexedSong) = dao.insertAt(id, removed.position, removed.song.id, clock())

    suspend fun move(id: Long, from: Int, to: Int) = dao.move(id, from, to, clock())

    /** Drops stored entries and play stats for songs that no longer exist. */
    suspend fun prune(validSongIds: Set<Long>) {
        dao.pruneSongs(validSongIds)
        stats.prune(validSongIds)
    }

    private fun resolve(ids: List<Long>, lib: Library): List<Song> {
        val byId = lib.songs.associateBy { it.id }
        return ids.mapNotNull { byId[it] }
    }

    private fun validName(name: String): String =
        name.trim().also { require(it.isNotEmpty()) { "playlist name must not be blank" } }

    private companion object {
        const val RECENT_DAYS = 30L
        const val SECONDS_PER_DAY = 86_400L
        const val SMART_LIMIT = 100
    }
}
