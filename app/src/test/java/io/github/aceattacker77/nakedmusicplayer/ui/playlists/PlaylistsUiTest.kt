package io.github.aceattacker77.nakedmusicplayer.ui.playlists

import android.Manifest
import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
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
import io.github.aceattacker77.nakedmusicplayer.ui.AppRoot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h2400dp")
@RunWith(RobolectricTestRunner::class)
class PlaylistsUiTest {
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

    private fun row(id: Long, title: String) =
        AudioRow(id, title, "Ann", "Album", 10, 100, 200_000, id.toInt(), 0, "Music/", "f$id.mp3", null)

    private fun settle() {
        compose.waitForIdle()
    }

    private fun SemanticsNodeInteraction.click() {
        performClick()
        settle()
    }

    private fun SemanticsNodeInteraction.type(text: String) {
        performTextInput(text)
        settle()
    }

    private fun launch() {
        source.rows = listOf(row(1, "Alpha"), row(2, "Beta"), row(3, "Gamma"))
        container = TestContainer(app, source, player)
        compose.setContent { CompositionLocalProvider(LocalAppContainer provides container) { AppRoot() } }
        settle()
    }

    private val repo get() = container.playlistRepository

    /** A playlist "Mix" containing Alpha, Beta, Gamma. */
    private fun seedMix(): Long = runBlocking {
        val id = repo.create("Mix")
        repo.add(id, container.libraryRepository.library.value.songs.sortedBy { it.id })
        id
    }

    private fun songIds(id: Long) = runBlocking { repo.detail(id).first()!!.songs.map { it.song.id } }

    private fun openPlaylistsTab() {
        compose.onNodeWithText("Playlists").click()
        settle()
    }

    @Test fun create_rename_delete() {
        launch()
        openPlaylistsTab()

        compose.onNodeWithText("New playlist").click()
        compose.onNodeWithTag("playlist-name-field").type("Road")
        compose.onNodeWithText("Create").click()
        settle()
        compose.onNodeWithText("Road").assertIsDisplayed()

        compose.onNodeWithContentDescription("Playlist options").click()
        compose.onNodeWithText("Rename").click()
        compose.onNodeWithTag("playlist-name-field").type("trip")
        compose.onNodeWithText("Save").click()
        settle()
        compose.onNodeWithText("Roadtrip").assertIsDisplayed()

        compose.onNodeWithContentDescription("Playlist options").click()
        compose.onNodeWithText("Delete").click()
        compose.onNodeWithText("Delete playlist").assertIsDisplayed()
        compose.onNodeWithTag("confirm-delete").click()
        settle()
        compose.onNodeWithText("Roadtrip").assertDoesNotExist()
    }

    @Test fun create_blankName_isNotAccepted() {
        launch()
        openPlaylistsTab()
        compose.onNodeWithText("New playlist").click()
        compose.onNodeWithTag("playlist-name-field").type("   ")
        compose.onNodeWithText("Create").assertIsNotEnabled()
    }

    @Test fun addToPlaylist_fromSongMenu() {
        launch()
        val id = runBlocking { repo.create("Mix") }
        compose.onNodeWithTag("song-2").performTouchInput { longClick() }
        settle()
        compose.onNodeWithText("Add to playlist").click()
        settle()
        compose.onNodeWithText("Mix").click()
        settle()
        assertThat(songIds(id)).containsExactly(2L)
    }

    @Test fun addToPlaylist_newPlaylistFromSheet() {
        launch()
        compose.onNodeWithTag("song-1").performTouchInput { longClick() }
        settle()
        compose.onNodeWithText("Add to playlist").click()
        settle()
        compose.onNodeWithText("New playlist").click()
        compose.onNodeWithTag("playlist-name-field").type("Fresh")
        compose.onNodeWithText("Create").click()
        settle()
        val summary = runBlocking { repo.playlists().first().single() }
        assertThat(summary.name).isEqualTo("Fresh")
        assertThat(songIds(summary.id)).containsExactly(1L)
    }

    @Test fun swipeRemove_showsUndo_restores() {
        launch()
        val id = seedMix()
        openPlaylistsTab()
        compose.onNodeWithText("Mix").click()
        settle()

        compose.onNodeWithTag("playlist-row-1").performTouchInput { swipeLeft() }
        settle()
        assertThat(songIds(id)).containsExactly(1L, 3L).inOrder()
        compose.onNodeWithText("Removed \"Beta\"").assertIsDisplayed()

        compose.onNodeWithText("Undo").click()
        settle()
        assertThat(songIds(id)).containsExactly(1L, 2L, 3L).inOrder()
    }

    @Test fun dragReorder_persists() {
        launch()
        val id = seedMix()
        openPlaylistsTab()
        compose.onNodeWithText("Mix").click()
        settle()

        val handle = compose.onNodeWithTag("playlist-drag-0", useUnmergedTree = true)
        handle.performTouchInput { down(center) }
        settle()
        listOf(30f, 60f, 60f, 60f).forEach { dy ->
            handle.performTouchInput { moveBy(Offset(0f, dy)) }
            settle()
        }
        handle.performTouchInput { up() }
        settle()

        val order = songIds(id)
        assertThat(order.first()).isNotEqualTo(1L)
        assertThat(order).containsExactly(1L, 2L, 3L)
    }

    @Test fun smartPlaylists_listedFirst() {
        launch()
        seedMix()
        openPlaylistsTab()
        val smart = listOf("Recently added", "Most played", "Recently played")
        val mix = compose.onNodeWithText("Mix").getBoundsInRoot().top.value
        smart.forEach { name ->
            assertThat(compose.onNodeWithText(name).getBoundsInRoot().top.value).isLessThan(mix)
        }
    }

    @Test fun smartPlaylist_opensReadOnlyList() {
        launch()
        openPlaylistsTab()
        compose.onNodeWithText("Recently added").click()
        settle()
        compose.onNodeWithTag("smart-playlist-detail").assertIsDisplayed()
    }

    @Test fun playlistDetail_playsFromTappedSong() {
        launch()
        seedMix()
        openPlaylistsTab()
        compose.onNodeWithText("Mix").click()
        settle()
        compose.onNodeWithTag("playlist-row-2").click()
        settle()
        TestPlayerRunHelper.runUntilPendingCommandsAreFullyHandled(player)
        val state = runBlocking { container.playerConnection.await() }.state.value
        assertThat(state.queue.map { it.mediaId }).containsExactly("song:1", "song:2", "song:3").inOrder()
        assertThat(state.currentIndex).isEqualTo(2)
    }
}
