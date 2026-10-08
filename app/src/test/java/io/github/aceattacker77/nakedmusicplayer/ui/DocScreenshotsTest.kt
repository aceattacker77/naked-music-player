package io.github.aceattacker77.nakedmusicplayer.ui

import android.Manifest
import android.app.Application
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.test.utils.FakeMediaSourceFactory
import androidx.media3.test.utils.TestExoPlayerBuilder
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.aceattacker77.nakedmusicplayer.LocalAppContainer
import io.github.aceattacker77.nakedmusicplayer.TestContainer
import io.github.aceattacker77.nakedmusicplayer.data.InMemoryPreferencesStore
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.library.FakeAudioRowSource
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqRepository
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqualizerController
import io.github.aceattacker77.nakedmusicplayer.playback.eq.FakeAudioEffectsBackend
import io.github.aceattacker77.nakedmusicplayer.ui.equalizer.EqualizerScreen
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The screenshots in `docs/screenshots/`: each page of the app with a made-up library (no personal data), in the Default skin
 * with fixed colours. Record with `./gradlew :app:recordRoborazziDebug --tests "*DocScreenshotsTest*"`; the normal
 * `verifyRoborazziDebug` run then fails when a page changes until the images are recorded again, so the docs stay current.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xhdpi")
@RunWith(RobolectricTestRunner::class)
class DocScreenshotsTest {
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

    private fun row(id: Long, title: String, artist: String, album: String, albumId: Long, artistId: Long, seconds: Long, track: Int, year: Int) =
        AudioRow(id, title, artist, album, albumId, artistId, seconds * 1000, track, 1_700_000_000 + id, "Music/$artist/", "$title.mp3", year)

    private val library = listOf(
        row(1, "Midnight Harbour", "Ada Vale", "Slow Light", 1, 1, 243, 1, 2021),
        row(2, "Paper Lanterns", "Ada Vale", "Slow Light", 1, 1, 198, 2, 2021),
        row(3, "Static Garden", "Ada Vale", "Slow Light", 1, 1, 276, 3, 2021),
        row(4, "Northbound", "The Brass Orchard", "Field Notes", 2, 2, 221, 1, 2019),
        row(5, "Salt and Rust", "The Brass Orchard", "Field Notes", 2, 2, 187, 2, 2019),
        row(6, "Copper Hour", "The Brass Orchard", "Field Notes", 2, 2, 254, 3, 2019),
        row(7, "Glass Season", "Mio Tanabe", "Quiet Rooms", 3, 3, 312, 1, 2023),
        row(8, "Rain on Tin", "Mio Tanabe", "Quiet Rooms", 3, 3, 167, 2, 2023),
        row(9, "Last Train Home", "Mio Tanabe", "Quiet Rooms", 3, 3, 289, 3, 2023),
        row(10, "Lowlands", "Ada Vale", "Tidewater", 4, 1, 205, 1, 2024),
    )

    private fun launch() {
        source.rows = library
        container = TestContainer(app, source, player)
        runBlocking { container.settingsRepository.update { it.copy(themeMode = ThemeMode.DARK, dynamicColor = false) } }
        compose.setContent { CompositionLocalProvider(LocalAppContainer provides container) { AppRoot() } }
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("../docs/screenshots/$name.png")
    }

    private fun tab(label: String) {
        compose.onNodeWithText(label).performClick()
        compose.waitForIdle()
    }

    @Test fun songs() {
        launch()
        shot("songs")
    }

    @Test fun albums() {
        launch()
        tab("Albums")
        shot("albums")
    }

    @Test fun artists() {
        launch()
        tab("Artists")
        shot("artists")
    }

    @Test fun playlists() {
        launch()
        runBlocking {
            val repo = container.playlistRepository
            val songs = container.libraryRepository.library.value.songs.sortedBy { it.id }
            repo.add(repo.create("Road trip"), songs.take(5))
            repo.add(repo.create("Quiet evening"), songs.drop(6).take(3))
        }
        tab("Playlists")
        shot("playlists")
    }

    @Test fun settings() {
        launch()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Settings").performClick()
        compose.waitForIdle()
        shot("settings")
    }

    @Test fun skins() {
        launch()
        compose.onNodeWithContentDescription("More options").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Settings").performClick()
        compose.waitForIdle()
        compose.onNodeWithText("Skins").performClick()
        compose.waitForIdle()
        shot("skins")
    }

    @Test fun equalizer() {
        val controller = EqualizerController(
            FakeAudioEffectsBackend(),
            EqRepository(InMemoryPreferencesStore()),
            TestScope(UnconfinedTestDispatcher()).backgroundScope,
        ) {}
        controller.onAudioSessionId(1)
        controller.setEnabled(true)
        controller.setBand(0, 300)
        controller.setBand(1, 600)
        controller.setBand(3, -450)
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(themeMode = ThemeMode.DARK, dynamicColor = false)) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { EqualizerScreen(controller, onBack = {}) }
            }
        }
        shot("equalizer")
    }
}
