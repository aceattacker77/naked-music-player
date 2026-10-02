package io.github.aceattacker77.nakedmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

private const val SQLITE_CHUNK = 500

@Dao
abstract class PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY name COLLATE NOCASE")
    abstract fun observePlaylists(): Flow<List<PlaylistEntity>>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    abstract fun observeSongIds(playlistId: Long): Flow<List<Long>>

    @Query("SELECT * FROM playlist_songs ORDER BY playlistId, position")
    abstract fun observeAllEntries(): Flow<List<PlaylistSongEntity>>

    @Query("SELECT position FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    abstract suspend fun positions(playlistId: Long): List<Int>

    @Query("SELECT songId FROM playlist_songs WHERE playlistId = :playlistId ORDER BY position")
    protected abstract suspend fun songIds(playlistId: Long): List<Long>

    @Query("SELECT COALESCE(MAX(position) + 1, 0) FROM playlist_songs WHERE playlistId = :playlistId")
    protected abstract suspend fun nextPosition(playlistId: Long): Int

    @Query("SELECT DISTINCT songId FROM playlist_songs")
    protected abstract suspend fun distinctSongIds(): List<Long>

    @Query("SELECT DISTINCT playlistId FROM playlist_songs WHERE songId IN (:songIds)")
    protected abstract suspend fun playlistsContaining(songIds: List<Long>): List<Long>

    @Query("DELETE FROM playlist_songs WHERE songId IN (:songIds)")
    protected abstract suspend fun deleteBySongIds(songIds: List<Long>)

    @Query("DELETE FROM playlist_songs WHERE playlistId = :playlistId")
    protected abstract suspend fun clearSongs(playlistId: Long)

    @Query("UPDATE playlists SET updatedAt = :now WHERE id = :playlistId")
    protected abstract suspend fun touch(playlistId: Long, now: Long)

    @Insert
    protected abstract suspend fun insertPlaylist(playlist: PlaylistEntity): Long

    @Insert
    protected abstract suspend fun insertSongs(rows: List<PlaylistSongEntity>)

    @Query("UPDATE playlists SET name = :name, updatedAt = :now WHERE id = :id")
    abstract suspend fun rename(id: Long, name: String, now: Long)

    @Query("DELETE FROM playlists WHERE id = :id")
    abstract suspend fun delete(id: Long)

    suspend fun create(name: String, now: Long): Long =
        insertPlaylist(PlaylistEntity(name = name, createdAt = now, updatedAt = now))

    @Transaction
    open suspend fun addSongs(playlistId: Long, songIds: List<Long>, now: Long) {
        val start = nextPosition(playlistId)
        insertSongs(songIds.mapIndexed { i, songId -> PlaylistSongEntity(playlistId, songId, start + i) })
        touch(playlistId, now)
    }

    @Transaction
    open suspend fun insertAt(playlistId: Long, position: Int, songId: Long, now: Long) {
        val ids = songIds(playlistId).toMutableList()
        ids.add(position.coerceIn(0, ids.size), songId)
        rewrite(playlistId, ids)
        touch(playlistId, now)
    }

    @Transaction
    open suspend fun removeAt(playlistId: Long, position: Int, now: Long) {
        val ids = songIds(playlistId).toMutableList()
        if (position !in ids.indices) return
        ids.removeAt(position)
        rewrite(playlistId, ids)
        touch(playlistId, now)
    }

    @Transaction
    open suspend fun move(playlistId: Long, from: Int, to: Int, now: Long) {
        val ids = songIds(playlistId).toMutableList()
        if (from !in ids.indices) return
        val item = ids.removeAt(from)
        ids.add(to.coerceIn(0, ids.size), item)
        rewrite(playlistId, ids)
        touch(playlistId, now)
    }

    /** Drops every row whose song is not in [validSongIds] and renumbers the affected playlists. */
    @Transaction
    open suspend fun pruneSongs(validSongIds: Set<Long>) {
        val missing = distinctSongIds().filter { it !in validSongIds }
        if (missing.isEmpty()) return
        val affected = missing.chunked(SQLITE_CHUNK).flatMap { playlistsContaining(it) }.distinct()
        missing.chunked(SQLITE_CHUNK).forEach { deleteBySongIds(it) }
        affected.forEach { playlistId -> rewrite(playlistId, songIds(playlistId)) }
    }

    private suspend fun rewrite(playlistId: Long, ids: List<Long>) {
        clearSongs(playlistId)
        insertSongs(ids.mapIndexed { i, songId -> PlaylistSongEntity(playlistId, songId, i) })
    }
}
