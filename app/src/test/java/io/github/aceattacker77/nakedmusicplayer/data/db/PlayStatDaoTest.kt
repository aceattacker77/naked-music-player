package io.github.aceattacker77.nakedmusicplayer.data.db

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayStatDaoTest : DbTestBase() {
    private val dao get() = db.playStatDao()

    @Test fun recordPlay_incrementsAndStampsTime() = runTest {
        dao.recordPlay(1, 100)
        dao.recordPlay(1, 200)
        val stat = dao.get(1)!!
        assertThat(stat.playCount).isEqualTo(2)
        assertThat(stat.lastPlayedAt).isEqualTo(200)
    }

    @Test fun mostPlayed_ordersByCountThenRecent() = runTest {
        dao.recordPlay(1, 10); dao.recordPlay(1, 20)
        dao.recordPlay(2, 30); dao.recordPlay(2, 40)
        dao.recordPlay(3, 50)
        assertThat(dao.mostPlayed(10).first()).containsExactly(2L, 1L, 3L).inOrder()
        assertThat(dao.mostPlayed(2).first()).containsExactly(2L, 1L).inOrder()
    }

    @Test fun recentlyPlayed_newestFirst() = runTest {
        dao.recordPlay(1, 10); dao.recordPlay(2, 30); dao.recordPlay(3, 20)
        assertThat(dao.recentlyPlayed(10).first()).containsExactly(2L, 3L, 1L).inOrder()
    }

    @Test fun prune_removesMissing() = runTest {
        dao.recordPlay(1, 10); dao.recordPlay(2, 20); dao.recordPlay(3, 30)
        dao.prune(setOf(2L))
        assertThat(dao.mostPlayed(10).first()).containsExactly(2L)
    }
}
