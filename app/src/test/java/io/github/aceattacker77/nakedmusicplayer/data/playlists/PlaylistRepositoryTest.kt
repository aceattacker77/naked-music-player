package io.github.aceattacker77.nakedmusicplayer.data.playlists

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.db.AppDatabase
import io.github.aceattacker77.nakedmusicplayer.library.LibraryBuilder
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.Library
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistRepositoryTest {
    private val now = 100L * 24 * 60 * 60 * 1000 // day 100, in ms
    private lateinit var db: AppDatabase
    private lateinit var library: MutableStateFlow<Library>
    private lateinit var repo: PlaylistRepository

    private fun row(id: Long, addedDaysAgo: Int = 0) =
        AudioRow(id, "T$id", "A", "Al", 1, 1, 200_000, id.toInt(), (now / 1000) - addedDaysAgo * 86_400L, "Music/", "f$id.mp3", null)

    private fun libraryOf(vararg rows: AudioRow) = LibraryBuilder.build(rows.toList(), LibraryFilter())

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java).allowMainThreadQueries().build()
        library = MutableStateFlow(libraryOf(row(1), row(2), row(3)))
        repo = PlaylistRepository(db.playlistDao(), db.playStatDao(), library, clock = { now })
    }

    @After fun tearDown() = db.close()

    @Test fun detail_hidesDeletedSongs() = runTest {
        val id = repo.create("Mix")
        repo.add(id, library.value.songs.filter { it.id in setOf(1L, 2L, 3L) }.sortedBy { it.id })
        library.value = libraryOf(row(1), row(3)) // song 2 deleted from storage

        val detail = repo.detail(id).first()!!
        assertThat(detail.songs.map { it.song.id }).containsExactly(1L, 3L).inOrder()
        // Positions are the real ones, so the remaining rows still address the right entries.
        assertThat(detail.songs.map { it.position }).containsExactly(0, 2).inOrder()
    }

    @Test fun detail_unknownPlaylist_isNull() = runTest {
        assertThat(repo.detail(999).first()).isNull()
    }

    @Test fun remove_thenUndo_restoresPosition() = runTest {
        val id = repo.create("Mix")
        repo.add(id, library.value.songs.sortedBy { it.id })
        val removed = repo.remove(id, 1)
        assertThat(removed.song.id).isEqualTo(2L)
        assertThat(repo.detail(id).first()!!.songs.map { it.song.id }).containsExactly(1L, 3L).inOrder()

        repo.undoRemove(id, removed)
        val restored = repo.detail(id).first()!!
        assertThat(restored.songs.map { it.song.id }).containsExactly(1L, 2L, 3L).inOrder()
        assertThat(restored.songs.map { it.position }).containsExactly(0, 1, 2).inOrder()
    }

    @Test fun move_reordersSongs() = runTest {
        val id = repo.create("Mix")
        repo.add(id, library.value.songs.sortedBy { it.id })
        repo.move(id, 0, 2)
        assertThat(repo.detail(id).first()!!.songs.map { it.song.id }).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test fun smart_recentlyAdded_last30DaysOnly() = runTest {
        library.value = libraryOf(row(1, addedDaysAgo = 29), row(2, addedDaysAgo = 31), row(3, addedDaysAgo = 1))
        val songs = repo.smart(SmartPlaylist.RECENTLY_ADDED).first()
        assertThat(songs.map { it.id }).containsExactly(3L, 1L).inOrder() // newest first
    }

    @Test fun smart_mostPlayed_orderedByCount() = runTest {
        val stats = db.playStatDao()
        stats.recordPlay(1, 10); stats.recordPlay(2, 20); stats.recordPlay(2, 30); stats.recordPlay(3, 40)
        assertThat(repo.smart(SmartPlaylist.MOST_PLAYED).first().map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test fun smart_recentlyPlayed_newestFirst_andSkipsDeletedSongs() = runTest {
        val stats = db.playStatDao()
        stats.recordPlay(1, 10); stats.recordPlay(2, 30); stats.recordPlay(3, 20)
        library.value = libraryOf(row(1), row(3))
        assertThat(repo.smart(SmartPlaylist.RECENTLY_PLAYED).first().map { it.id }).containsExactly(3L, 1L).inOrder()
    }

    @Test fun summary_countsOnlyExistingSongs() = runTest {
        val id = repo.create("Mix")
        repo.create("Empty")
        repo.add(id, library.value.songs.sortedBy { it.id })
        library.value = libraryOf(row(1), row(3))

        val summaries = repo.playlists().first().associate { it.name to it.songCount }
        assertThat(summaries).containsExactly("Mix", 2, "Empty", 0)
    }

    @Test fun create_blankName_rejected() = runTest {
        runCatching { repo.create("   ") }.also { assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java) }
        assertThat(repo.playlists().first()).isEmpty()
    }

    @Test fun create_trimsName() = runTest {
        repo.create("  Road trip  ")
        assertThat(repo.playlists().first().single().name).isEqualTo("Road trip")
    }

    @Test fun rename_trimsAndRejectsBlank() = runTest {
        val id = repo.create("Old")
        repo.rename(id, " New ")
        assertThat(repo.playlists().first().single().name).isEqualTo("New")
        runCatching { repo.rename(id, "") }.also { assertThat(it.exceptionOrNull()).isInstanceOf(IllegalArgumentException::class.java) }
    }

    @Test fun delete_removesPlaylist() = runTest {
        val id = repo.create("Mix")
        repo.delete(id)
        assertThat(repo.playlists().first()).isEmpty()
    }

    @Test fun prune_removesMissingSongsFromPlaylistsAndStats() = runTest {
        val id = repo.create("Mix")
        repo.add(id, library.value.songs.sortedBy { it.id })
        db.playStatDao().recordPlay(2, 10)
        repo.prune(setOf(1L, 3L))
        assertThat(db.playlistDao().observeSongIds(id).first()).containsExactly(1L, 3L).inOrder()
        assertThat(db.playStatDao().get(2)).isNull()
    }
}
