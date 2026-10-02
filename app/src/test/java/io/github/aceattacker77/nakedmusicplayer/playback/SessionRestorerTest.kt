package io.github.aceattacker77.nakedmusicplayer.playback

import androidx.media3.common.MediaItem
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.session.SavedSession
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SessionRestorerTest {
    private fun saved(ids: List<String>, index: Int, position: Long = 5_000) =
        SavedSession(ids, index, position, shuffle = false, repeatMode = 0)

    private fun resolver(vararg missing: String): (String) -> MediaItem? = { id ->
        if (id in missing) null else MediaItem.Builder().setMediaId(id).build()
    }

    private fun SessionRestorer.Restored.ids() = items.map { it.mediaId }

    @Test fun restore_allPresent_keepsIndexAndPosition() {
        val r = SessionRestorer.restore(saved(listOf("a", "b", "c"), 1), resolver())!!
        assertThat(r.ids()).containsExactly("a", "b", "c").inOrder()
        assertThat(r.index).isEqualTo(1)
        assertThat(r.positionMs).isEqualTo(5_000)
    }

    @Test fun restore_missingBeforeCurrent_shiftsIndex() {
        val r = SessionRestorer.restore(saved(listOf("a", "b", "c"), 2), resolver("b"))!!
        assertThat(r.ids()).containsExactly("a", "c").inOrder()
        assertThat(r.index).isEqualTo(1)
        assertThat(r.positionMs).isEqualTo(5_000)
    }

    @Test fun restore_currentMissing_movesToNextAtZero() {
        val r = SessionRestorer.restore(saved(listOf("a", "b", "c"), 1), resolver("b"))!!
        assertThat(r.ids()).containsExactly("a", "c").inOrder()
        assertThat(r.index).isEqualTo(1)
        assertThat(r.positionMs).isEqualTo(0)
    }

    @Test fun restore_currentMissingAndLast_wrapsToPrevious() {
        val r = SessionRestorer.restore(saved(listOf("a", "b", "c"), 2), resolver("c"))!!
        assertThat(r.ids()).containsExactly("a", "b").inOrder()
        assertThat(r.index).isEqualTo(1)
        assertThat(r.positionMs).isEqualTo(0)
    }

    @Test fun restore_allMissing_returnsNull() {
        assertThat(SessionRestorer.restore(saved(listOf("a", "b"), 0), resolver("a", "b"))).isNull()
    }

    @Test fun restore_indexOutOfRange_clamped() {
        val r = SessionRestorer.restore(saved(listOf("a", "b", "c"), 99), resolver())!!
        assertThat(r.index).isEqualTo(2)
        val negative = SessionRestorer.restore(saved(listOf("a", "b", "c"), -4), resolver())!!
        assertThat(negative.index).isEqualTo(0)
    }

    @Test fun restore_emptySession_returnsNull() {
        assertThat(SessionRestorer.restore(saved(emptyList(), 0), resolver())).isNull()
    }
}
