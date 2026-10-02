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

    @Test fun importM3u_createsPlaylistWithMatchedSongs() = runTest {
        val text = "#EXTM3U\nMusic/f1.mp3\nNope/missing.mp3\nMusic/f3.mp3\n"
        val result = repo.importM3u("Road trip.m3u8", text)
        assertThat(result.matched).isEqualTo(2)
        assertThat(result.total).isEqualTo(3)
        val detail = repo.detail(result.playlistId).first()!!
        assertThat(detail.name).isEqualTo("Road trip")
        assertThat(detail.songs.map { it.song.id }).containsExactly(1L, 3L).inOrder()
    }

    @Test fun importM3u_deduplicatesNames() = runTest {
        repo.create("Mix")
        val first = repo.importM3u("Mix.m3u", "Music/f1.mp3\n")
        val second = repo.importM3u("mix.m3u8", "Music/f1.mp3\n")
        val names = repo.playlists().first().map { it.name }
        assertThat(names).containsExactly("Mix", "Mix (2)", "mix (3)")
        assertThat(first.playlistId).isNotEqualTo(second.playlistId)
    }

    @Test fun importM3u_blankBaseName_getsFallbackName() = runTest {
        val result = repo.importM3u(".m3u8", "Music/f1.mp3\n")
        assertThat(repo.detail(result.playlistId).first()!!.name).isNotEmpty()
    }

    @Test fun exportM3u_writesVisibleSongsInOrder() = runTest {
        val id = repo.create("Mix")
        repo.add(id, library.value.songs.sortedBy { it.id })
        library.value = libraryOf(row(1), row(3))
        val export = repo.exportM3u(id)!!
        assertThat(export.fileName).isEqualTo("Mix.m3u8")
        assertThat(export.text.lines().filter { it.isNotEmpty() && !it.startsWith("#") })
            .containsExactly("Music/f1.mp3", "Music/f3.mp3").inOrder()
    }

    @Test fun exportThenImport_matchesEverything() = runTest {
        val id = repo.create("Mix")
        repo.add(id, library.value.songs.sortedBy { it.id })
        val export = repo.exportM3u(id)!!
        val result = repo.importM3u(export.fileName, export.text)
        assertThat(result.matched).isEqualTo(3)
        assertThat(result.total).isEqualTo(3)
    }

    @Test fun exportM3u_unknownPlaylist_isNull() = runTest {
        assertThat(repo.exportM3u(999)).isNull()
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
