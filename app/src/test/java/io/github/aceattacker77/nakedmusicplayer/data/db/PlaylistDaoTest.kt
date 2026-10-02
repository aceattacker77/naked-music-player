package io.github.aceattacker77.nakedmusicplayer.data.db

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlaylistDaoTest : DbTestBase() {
    private val dao get() = db.playlistDao()
    private suspend fun ids(id: Long) = dao.observeSongIds(id).first()

    @Test fun addSongs_appendsInOrder() = runTest {
        val id = dao.create("P", 1)
        dao.addSongs(id, listOf(3, 1, 2), 2)
        dao.addSongs(id, listOf(9), 3)
        assertThat(ids(id)).containsExactly(3L, 1L, 2L, 9L).inOrder()
    }

    @Test fun allowsDuplicateSongs() = runTest {
        val id = dao.create("P", 1)
        dao.addSongs(id, listOf(5, 5), 2)
        assertThat(ids(id)).containsExactly(5L, 5L).inOrder()
    }

    @Test fun removeAt_renumbersPositions() = runTest {
        val id = dao.create("P", 1)
        dao.addSongs(id, listOf(1, 2, 3, 4), 2)
        dao.removeAt(id, 1, 3)
        assertThat(ids(id)).containsExactly(1L, 3L, 4L).inOrder()
        assertThat(dao.positions(id)).containsExactly(0, 1, 2).inOrder()
    }

    @Test fun move_forwardAndBackward() = runTest {
        val id = dao.create("P", 1)
        dao.addSongs(id, listOf(1, 2, 3, 4), 2)
        dao.move(id, 0, 2, 3)
        assertThat(ids(id)).containsExactly(2L, 3L, 1L, 4L).inOrder()
        dao.move(id, 3, 1, 4)
        assertThat(ids(id)).containsExactly(2L, 4L, 3L, 1L).inOrder()
        assertThat(dao.positions(id)).containsExactly(0, 1, 2, 3).inOrder()
    }

    @Test fun insertAt_restoresRemovedItem() = runTest {
        val id = dao.create("P", 1)
        dao.addSongs(id, listOf(1, 2, 3), 2)
        dao.removeAt(id, 1, 3)
        dao.insertAt(id, 1, 2, 4)
        assertThat(ids(id)).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test fun delete_cascadesSongs() = runTest {
        val id = dao.create("P", 1)
        dao.addSongs(id, listOf(1, 2), 2)
        dao.delete(id)
        assertThat(ids(id)).isEmpty()
        assertThat(dao.observePlaylists().first()).isEmpty()
    }

    @Test fun playlists_sortedByNameIgnoringCase() = runTest {
        dao.create("banana", 1)
        dao.create("Apple", 1)
        dao.create("cherry", 1)
        assertThat(dao.observePlaylists().first().map { it.name }).containsExactly("Apple", "banana", "cherry").inOrder()
    }

    @Test fun rename_updatesNameAndTimestamp() = runTest {
        val id = dao.create("Old", 1)
        dao.rename(id, "New", 9)
        val p = dao.observePlaylists().first().single()
        assertThat(p.name).isEqualTo("New")
        assertThat(p.updatedAt).isEqualTo(9)
    }

    @Test fun pruneSongs_removesMissing_andRenumbers() = runTest {
        val a = dao.create("A", 1)
        val b = dao.create("B", 1)
        dao.addSongs(a, listOf(1, 2, 3, 4), 2)
        dao.addSongs(b, listOf(2, 5), 2)
        dao.pruneSongs(setOf(1L, 3L, 5L))
        assertThat(ids(a)).containsExactly(1L, 3L).inOrder()
        assertThat(dao.positions(a)).containsExactly(0, 1).inOrder()
        assertThat(ids(b)).containsExactly(5L)
        assertThat(dao.positions(b)).containsExactly(0)
    }

    @Test fun pruneSongs_handles2000Ids() = runTest {
        val id = dao.create("Big", 1)
        dao.addSongs(id, (1L..2000L).toList(), 2)
        dao.pruneSongs(setOf(1L))
        assertThat(ids(id)).containsExactly(1L)
    }
}
