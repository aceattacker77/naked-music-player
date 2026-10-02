package io.github.aceattacker77.nakedmusicplayer.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
abstract class PlayStatDao {
    @Query("SELECT * FROM play_stats WHERE songId = :songId")
    abstract suspend fun get(songId: Long): PlayStatEntity?

    @Query("SELECT songId FROM play_stats ORDER BY playCount DESC, lastPlayedAt DESC LIMIT :limit")
    abstract fun mostPlayed(limit: Int): Flow<List<Long>>

    @Query("SELECT songId FROM play_stats ORDER BY lastPlayedAt DESC LIMIT :limit")
    abstract fun recentlyPlayed(limit: Int): Flow<List<Long>>

    @Query("SELECT songId FROM play_stats")
    protected abstract suspend fun allSongIds(): List<Long>

    @Query("DELETE FROM play_stats WHERE songId IN (:songIds)")
    protected abstract suspend fun deleteBySongIds(songIds: List<Long>)

    @Query("UPDATE play_stats SET playCount = playCount + 1, lastPlayedAt = :now WHERE songId = :songId")
    protected abstract suspend fun increment(songId: Long, now: Long): Int

    @Insert
    protected abstract suspend fun insert(stat: PlayStatEntity)

    // No UPSERT: Android 8 ships SQLite 3.18, which predates ON CONFLICT DO UPDATE (3.24).
    @Transaction
    open suspend fun recordPlay(songId: Long, now: Long) {
        if (increment(songId, now) == 0) insert(PlayStatEntity(songId, 1, now))
    }

    @Transaction
    open suspend fun prune(validSongIds: Set<Long>) {
        allSongIds().filter { it !in validSongIds }.chunked(500).forEach { deleteBySongIds(it) }
    }
}
