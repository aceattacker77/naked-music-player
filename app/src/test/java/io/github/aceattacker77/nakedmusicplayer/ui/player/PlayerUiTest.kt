package io.github.aceattacker77.nakedmusicplayer.ui.player

import android.Manifest
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.media3.test.utils.robolectric.TestPlayerRunHelper
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.LocalAppContainer
import io.github.aceattacker77.nakedmusicplayer.TestContainer
import io.github.aceattacker77.nakedmusicplayer.library.FakeAudioRowSource
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.playback.UnplayableRegistry
import io.github.aceattacker77.nakedmusicplayer.ui.AppRoot
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h891dp")
@RunWith(RobolectricTestRunner::class)
class PlayerUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val source = FakeAudioRowSource()
    private lateinit var player: ExoPlayer
    private lateinit var container: TestContainer

    @Before fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        player = TestExoPlayerBuilder(app).setMediaSourceFactory(FakeMediaSourceFactory()).build()
    }

    @After fun tearDown() = player.release()

    private companion object {
        const val SETTLE_MS = 1_000L
    }

    private fun row(id: Long, title: String) =
        AudioRow(id, title, "Ann", "Album", 10, 100, 200_000, id.toInt(), 0, "Music/", "f$id.mp3", null)

    // While playing, the expanded player animates forever (wavy seek bar, spinning art), so Compose
    // never reports idle; the clock is stepped by hand instead of waiting for it.
    private fun settle() {
        compose.mainClock.advanceTimeBy(SETTLE_MS)
        compose.waitForIdle()
    }

    private fun launch() {
        compose.mainClock.autoAdvance = false
        source.rows = listOf(row(1, "Alpha"), row(2, "Beta"), row(3, "Gamma"))
        container = TestContainer(app, source, player)
        compose.setContent { CompositionLocalProvider(LocalAppContainer provides container) { AppRoot() } }
        settle()
    }

    private fun startPlayback() {
        val songs = container.libraryRepository.library.value.songs
        compose.runOnUiThread { runBlocking { container.playerConnection.await() }.playSongs(songs, 0) }
        settle()
        TestPlayerRunHelper.runUntilPlaybackState(player, Player.STATE_READY)
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)
        settle()
    }

    private fun queueIds() = runBlocking { container.playerConnection.await() }.state.value.queue.map { it.mediaId }

    private fun expandPlayer() {
        compose.onNodeWithTag("mini-player").performClick()
        settle()
    }

    private fun openQueue() {
        expandPlayer()
        compose.onNodeWithTag("queue-handle").performClick()
        settle()
    }

    @Test fun miniPlayer_hiddenWhenQueueEmpty() {
        launch()
        compose.onNodeWithTag("mini-player").assertDoesNotExist()
        startPlayback()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }

    @Test fun tapMini_expands_backCollapses() {
        launch()
        startPlayback()
        expandPlayer()
        compose.onNodeWithTag("now-playing").assertIsDisplayed()

        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        settle()
        compose.onNodeWithTag("now-playing").assertDoesNotExist()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
    }

    @Test fun playPause_togglesIcon() {
        launch()
        startPlayback()
        compose.onNodeWithTag("mini-player").assertIsDisplayed()
        compose.onNodeWithContentDescription("Pause").assertIsDisplayed()

        compose.onNodeWithContentDescription("Pause").performClick()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)
        settle()
        compose.onNodeWithContentDescription("Play").assertIsDisplayed()
    }

    @Test fun queue_listsCurrentQueue() {
        launch()
        startPlayback()
        openQueue()
        listOf(0, 1, 2).forEach { compose.onNodeWithTag("queue-row-$it").assertExists() }
    }

    @Test fun queue_dragReorder_callsMove() {
        launch()
        startPlayback()
        openQueue()
        val handle = compose.onNodeWithTag("queue-drag-0", useUnmergedTree = true)
        // The reorder library reacts to each frame, so the drag is delivered in steps with the clock
        // advanced in between rather than as one burst.
        handle.performTouchInput { down(center) }
        settle()
        listOf(30f, 60f, 60f, 60f).forEach { dy ->
            handle.performTouchInput { moveBy(Offset(0f, dy)) }
            settle()
        }
        handle.performTouchInput { up() }
        settle()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)
        assertThat(queueIds().first()).isNotEqualTo("song:1")
        assertThat(queueIds()).containsExactly("song:1", "song:2", "song:3")
    }

    @Test fun queue_swipeRemove_callsRemove() {
        launch()
        startPlayback()
        openQueue()
        compose.onNodeWithTag("queue-row-1").performTouchInput { swipeLeft() }
        settle()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)
        assertThat(queueIds()).containsExactly("song:1", "song:3").inOrder()
    }

    @Test fun unplayableItem_showsIcon() {
        launch()
        startPlayback()
        UnplayableRegistry.mark("song:2")
        openQueue()
        compose.onNodeWithTag("queue-row-1").assert(hasContentDescription("Can't play"))
    }
}
