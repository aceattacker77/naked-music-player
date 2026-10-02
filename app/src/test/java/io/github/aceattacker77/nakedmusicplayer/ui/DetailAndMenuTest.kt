package io.github.aceattacker77.nakedmusicplayer.ui

import android.Manifest
import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

// A tall (but still phone-width) window so lazy lists compose every item the tests look for.
@Config(qualifiers = "w411dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class DetailAndMenuTest {
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

    private fun row(
        id: Long, title: String, artist: String = "Ann", artistId: Long = 100, album: String = "Album",
        albumId: Long = 10, track: Int = 1,
    ) = AudioRow(id, title, artist, album, albumId, artistId, 200_000, track, 0, "Music/", "f$id.mp3", null)

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

    private fun longPress(tag: String) {
        compose.onNodeWithTag(tag).performTouchInput { longClick() }
        compose.waitForIdle()
    }

    @Test fun albumDetail_showsTracksInDiscOrder() {
        launch(
            row(1, "D2T3", track = 2003), row(2, "D1T5", track = 1005),
            row(3, "D2T1", track = 2001), row(4, "D1T2", track = 1002),
        )
        compose.onNodeWithText("Albums").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("album-10").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("album-detail").assertIsDisplayed()
        assertThat(songOrder()).containsExactly("song-4", "song-2", "song-3", "song-1").inOrder()
        compose.onNodeWithText("Disc 2").assertExists()
    }

    @Test fun artistDetail_showsAlbumsThenSongs() {
        launch(
            row(1, "S1", album = "First", albumId = 10), row(2, "S2", album = "Second", albumId = 11),
        )
        compose.onNodeWithText("Artists").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("artist-100").performClick()
        compose.waitForIdle()

        compose.onNodeWithTag("artist-detail").assertIsDisplayed()
        val albumTop = compose.onNodeWithTag("album-10").getBoundsInRoot().top
        val songTop = compose.onNodeWithTag("song-1").getBoundsInRoot().top
        assertThat(albumTop.value).isLessThan(songTop.value)
        compose.onNodeWithTag("album-11").assertExists()
        compose.onNodeWithTag("song-2").assertExists()
    }

    @Test fun songMenu_hasFiveEntriesInSpecOrder() {
        launch(row(1, "A"))
        longPress("song-1")
        val labels = listOf("Play next", "Add to queue", "Add to playlist", "Go to album", "Go to artist")
        val tops = labels.map { compose.onNodeWithText(it).getBoundsInRoot().top.value }
        assertThat(tops).isInOrder()
    }

    @Test fun songMenu_playNext_callsConnection() {
        launch(row(1, "A"), row(2, "B"), row(3, "C"))
        compose.onNodeWithTag("song-1").performClick()
        compose.waitForIdle()
        longPress("song-3")
        compose.onNodeWithText("Play next").performClick()
        compose.waitForIdle()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)

        val state = runBlocking { container.playerConnection.await() }.state.value
        assertThat(state.queue.map { it.mediaId }).containsExactly("song:1", "song:3", "song:2", "song:3").inOrder()
    }

    @Test fun songMenu_addToQueue_appends() {
        launch(row(1, "A"), row(2, "B"))
        compose.onNodeWithTag("song-1").performClick()
        compose.waitForIdle()
        longPress("song-2")
        compose.onNodeWithText("Add to queue").performClick()
        compose.waitForIdle()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)

        val state = runBlocking { container.playerConnection.await() }.state.value
        assertThat(state.queue.map { it.mediaId }).containsExactly("song:1", "song:2", "song:2").inOrder()
    }

    @Test fun songMenu_goToAlbum_navigates() {
        launch(row(1, "A", album = "Great Album"))
        longPress("song-1")
        compose.onNodeWithText("Go to album").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("album-detail").assertIsDisplayed()
    }

    @Test fun songMenu_goToArtist_navigates() {
        launch(row(1, "A"))
        longPress("song-1")
        compose.onNodeWithText("Go to artist").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("artist-detail").assertIsDisplayed()
    }

    @Test fun search_typing_showsResults() {
        launch(row(1, "Alpha Song"), row(2, "Beta Song", artist = "Bob", artistId = 101, album = "Other", albumId = 11))
        compose.onNodeWithContentDescription("Search").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("search-field").performTextInput("alph")
        compose.waitForIdle()

        compose.onNodeWithTag("song-1").assertExists()
        assertThat(songOrder()).containsExactly("song-1")
    }

    @Test fun search_noMatches_showsMessage() {
        launch(row(1, "Alpha Song"))
        compose.onNodeWithContentDescription("Search").performClick()
        compose.waitForIdle()
        compose.onNodeWithTag("search-field").performTextInput("zzzz")
        compose.waitForIdle()
        compose.onNode(hasText("No results", substring = true)).assertIsDisplayed()
    }
}
