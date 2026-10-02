package io.github.aceattacker77.nakedmusicplayer.ui.player

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.playback.toMediaItem
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerConnectionTest {
    private lateinit var player: ExoPlayer
    private lateinit var connection: PlayerConnection

    private fun song(id: Long) = Song(
        id = id, uri = "content://media/external/audio/media/$id", title = "T$id", artist = "A", album = "Al",
        albumId = 1, artistId = 1, durationMs = 200_000, discNumber = 1, trackNumber = 1, dateAddedSec = 0,
        relativePath = "Music/", displayName = "f$id.mp3", year = null,
    )

    /** Player events are delivered asynchronously; settle them before reading UI state. */
    private val state: PlayerUiState
        get() {
            TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)
            return connection.state.value
        }

    private fun queueIds() = state.queue.map { it.mediaId }

    @Before fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        player = TestExoPlayerBuilder(context).setMediaSourceFactory(FakeMediaSourceFactory()).build()
        connection = PlayerConnection(player, TestScope(UnconfinedTestDispatcher()))
    }

    @After fun tearDown() = player.release()

    @Test fun playSongs_setsQueueAndStartsAtIndex() {
        connection.playSongs(listOf(song(1), song(2), song(3)), startIndex = 1)
        assertThat(queueIds()).containsExactly("song:1", "song:2", "song:3").inOrder()
        assertThat(state.currentIndex).isEqualTo(1)
        assertThat(state.current?.mediaId).isEqualTo("song:2")
        assertThat(player.playWhenReady).isTrue()
    }

    @Test fun playNext_insertsAfterCurrent() {
        connection.playSongs(listOf(song(1), song(2), song(3)), startIndex = 0)
        connection.playNext(listOf(song(9)))
        assertThat(queueIds()).containsExactly("song:1", "song:9", "song:2", "song:3").inOrder()
    }

    @Test fun playNext_onEmptyQueue_startsQueue() {
        connection.playNext(listOf(song(5)))
        assertThat(queueIds()).containsExactly("song:5")
        assertThat(state.currentIndex).isEqualTo(0)
        assertThat(player.playWhenReady).isTrue()
    }

    @Test fun addToQueue_appends() {
        connection.playSongs(listOf(song(1), song(2)), startIndex = 0)
        connection.addToQueue(listOf(song(7), song(8)))
        assertThat(queueIds()).containsExactly("song:1", "song:2", "song:7", "song:8").inOrder()
    }

    @Test fun remove_and_move_reflectInState() {
        connection.playSongs(listOf(song(1), song(2), song(3), song(4)), startIndex = 0)
        connection.moveQueueItem(from = 0, to = 2)
        assertThat(queueIds()).containsExactly("song:2", "song:3", "song:1", "song:4").inOrder()
        connection.removeQueueItem(1)
        assertThat(queueIds()).containsExactly("song:2", "song:1", "song:4").inOrder()
    }

    @Test fun cycleRepeat_offAllOneOff() {
        assertThat(state.repeatMode).isEqualTo(Player.REPEAT_MODE_OFF)
        connection.cycleRepeat()
        assertThat(state.repeatMode).isEqualTo(Player.REPEAT_MODE_ALL)
        connection.cycleRepeat()
        assertThat(state.repeatMode).isEqualTo(Player.REPEAT_MODE_ONE)
        connection.cycleRepeat()
        assertThat(state.repeatMode).isEqualTo(Player.REPEAT_MODE_OFF)
    }

    @Test fun toggleShuffle_flipsState() {
        assertThat(state.shuffle).isFalse()
        connection.toggleShuffle()
        assertThat(state.shuffle).isTrue()
    }

    @Test fun state_updatesOnTransition() {
        connection.playSongs(listOf(song(1), song(2)), startIndex = 0)
        connection.next()
        assertThat(state.currentIndex).isEqualTo(1)
        assertThat(state.current?.mediaId).isEqualTo("song:2")
        connection.previous()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)
        assertThat(state.current?.mediaId).isEqualTo("song:1")
    }

    @Test fun togglePlayPause_whenIdleWithQueue_prepares() {
        player.setMediaItems(listOf(song(1).toMediaItem()))
        assertThat(player.playbackState).isEqualTo(Player.STATE_IDLE)
        connection.togglePlayPause()
        assertThat(player.playbackState).isNotEqualTo(Player.STATE_IDLE)
        assertThat(player.playWhenReady).isTrue()
    }

    @Test fun togglePlayPause_whenPlayWhenReady_pauses() {
        connection.playSongs(listOf(song(1)), startIndex = 0)
        TestPlayerRunHelper.runUntilPlaybackState(player, Player.STATE_READY)
        assertThat(state.isPlaying).isTrue()
        connection.togglePlayPause()
        assertThat(player.playWhenReady).isFalse()
        assertThat(state.isPlaying).isFalse()
    }
}
