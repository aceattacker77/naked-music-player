package io.github.aceattacker77.nakedmusicplayer.ui

import android.Manifest
import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppShellTest {
    @get:Rule val compose = createComposeRule()

    private val app = ApplicationProvider.getApplicationContext<Application>()
    private val source = FakeAudioRowSource()
    private lateinit var player: ExoPlayer
    private lateinit var container: TestContainer

    @Before fun setUp() {
        shadowOf(app).grantPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        player = TestExoPlayerBuilder(app).setMediaSourceFactory(FakeMediaSourceFactory()).build()
    }

    @After fun tearDown() = player.release()

    private fun row(id: Long, title: String, artist: String = "Ann", album: String = "Album $id", albumId: Long = id, added: Long = 0) =
        AudioRow(id, title, artist, album, albumId, albumId, 200_000, 1, added, "Music/", "f$id.mp3", null)

    private fun launch(vararg rows: AudioRow) {
        source.rows = rows.toList()
        container = TestContainer(app, source, player)
        compose.setContent { CompositionLocalProvider(LocalAppContainer provides container) { AppRoot() } }
        compose.waitForIdle()
    }

    private fun songOrder(): List<String> = compose.onAllNodes(
        SemanticsMatcher("song row") { node ->
            node.config.getOrNull(SemanticsProperties.TestTag)?.let { Regex("song-\\d+").matches(it) } == true
        },
    ).fetchSemanticsNodes().map { it.config[SemanticsProperties.TestTag] }

    @Test fun permissionNotGranted_showsRationaleWithAllowButton() {
        shadowOf(app).denyPermissions(Manifest.permission.READ_MEDIA_AUDIO)
        launch()
        compose.onNodeWithText("Allow access").assertIsDisplayed()
    }

    @Test fun permissionDenied_showsOpenSettings() {
        source.throwOnQuery = SecurityException("revoked")
        launch()
        compose.onNodeWithText("Open settings").assertIsDisplayed()
    }

    @Test fun emptyLibrary_showsNoMusicFound() {
        launch()
        compose.onNodeWithText("No music found").assertIsDisplayed()
    }

    @Test fun tabs_switchBetweenSongsAlbumsArtists() {
        launch(row(1, "Alpha Song", artist = "Ann", album = "First Album"), row(2, "Beta Song", artist = "Bob", album = "Second Album"))
        compose.onNodeWithText("Alpha Song").assertIsDisplayed()

        compose.onNodeWithText("Albums").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("First Album").assertIsDisplayed()
        // A phone-sized window fits one grid column, so the second album is below the fold.
        compose.onNodeWithTag("album-grid").performScrollToNode(hasText("Second Album"))
        compose.onNodeWithText("Second Album").assertIsDisplayed()

        compose.onNodeWithText("Artists").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Bob").assertIsDisplayed()
    }

    @Test fun tapSong_callsPlaySongsWithIndex() {
        launch(row(1, "A"), row(2, "B"), row(3, "C"))
        compose.onNodeWithTag("song-2").performClick()
        compose.waitForIdle()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)

        val state = runBlocking { container.playerConnection.await() }.state.value
        assertThat(state.queue.map { it.mediaId }).containsExactly("song:1", "song:2", "song:3").inOrder()
        assertThat(state.currentIndex).isEqualTo(1)
        assertThat(player.playWhenReady).isTrue()
    }

    @Test fun sortMenu_changesOrder() {
        launch(row(1, "B", added = 100), row(2, "A", added = 50))
        assertThat(songOrder()).containsExactly("song-2", "song-1").inOrder()

        compose.onNodeWithContentDescription("Sort").performClick()
        compose.onNodeWithText("Date added").performClick()
        compose.waitForIdle()
        assertThat(songOrder()).containsExactly("song-1", "song-2").inOrder()
    }

    @Test fun lazyLists_useStableKeysAndTags() {
        launch(row(1, "A"), row(2, "B"), row(3, "C"))
        compose.onNodeWithTag("song-list").assertExists()
        listOf(1, 2, 3).forEach { compose.onNodeWithTag("song-$it").assertExists() }
    }

    @Test fun unplayableSong_showsCantPlayIcon() {
        launch(row(777, "Broken"), row(778, "Fine"))
        UnplayableRegistry.mark("song:777")
        compose.waitForIdle()
        compose.onNodeWithContentDescription("Can't play").assertIsDisplayed()
    }
}
