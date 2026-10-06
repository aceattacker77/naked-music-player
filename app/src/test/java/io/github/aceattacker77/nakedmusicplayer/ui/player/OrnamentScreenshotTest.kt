package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.ui.components.GeoNavigationBar
import io.github.aceattacker77.nakedmusicplayer.ui.components.GeoTab
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongRow
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtShape
import io.github.aceattacker77.nakedmusicplayer.ui.skins.BackgroundStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlShape
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlsStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.CornerStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.NavStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekBarStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekColor
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images for a skin with every ornament field switched on (chamfer corners, hexagon tiles, segmented seek bar,
 * brackets, row edge, block bottom bar), in dark and light. Record with `recordRoborazziDebug`, check with
 * `verifyRoborazziDebug`.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class OrnamentScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val skin = Skin.FALLBACK.copy(
        cornerStyle = CornerStyle.CHAMFER,
        chamferDp = 10,
        headingScaleX = 0.8f,
        labelCaps = true,
        labelLetterSpacingEm = 0.14f,
        brackets = true,
        segmentedMeters = true,
        navStyle = NavStyle.BLOCK,
        rowEdge = true,
        player = Skin.FALLBACK.player.copy(
            background = BackgroundStyle.Solid,
            artShape = ArtShape.Square,
            seekBar = SeekBarStyle.SEGMENTED,
            seekSegments = 40,
            seekColor = SeekColor.TERTIARY,
            controls = ControlsStyle.MIXED,
            controlShape = ControlShape.THEME,
            useArtColors = false,
            artPlaceholder = ArtPlaceholder.HEXAGON,
            artBorder = true,
        ),
    )

    private val nowPlaying = MediaItem.Builder()
        .setMediaId("song:2")
        .setMediaMetadata(
            MediaMetadata.Builder().setTitle("Midnight City").setArtist("M83").setAlbumTitle("Hurry Up, We're Dreaming").build(),
        )
        .build()

    // Paused: moving content would keep Compose from ever going idle.
    private val state = PlayerUiState(
        current = nowPlaying, isPlaying = false, durationMs = 243_000, queue = emptyList(), currentIndex = 1,
        shuffle = false, repeatMode = Player.REPEAT_MODE_OFF,
    )

    private fun song(id: Long, title: String) = Song(
        id = id, uri = "content://x/$id", title = title, artist = "M83", album = "Hurry Up", albumId = id, artistId = 1,
        durationMs = 243_000, discNumber = 1, trackNumber = id.toInt(), dateAddedSec = 0, relativePath = "Music/",
        displayName = "$title.mp3", year = null,
    )

    private var dark by mutableStateOf(false)
    private var composed = false

    private fun capture(name: String, isDark: Boolean, content: @androidx.compose.runtime.Composable () -> Unit) {
        if (!composed) {
            composed = true
            compose.setContent {
                val settings = AppSettings(themeMode = if (dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
                AppTheme(skin, settings) { content() }
            }
        }
        dark = isDark
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test fun nowPlaying_noArtwork() {
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            NowPlayingScreen(state = state, positionMs = 83_000, actions = NowPlayingActions.None)
        }
        listOf(true, false).forEach { isDark -> capture("ornament_now_playing_${if (isDark) "dark" else "light"}", isDark, content) }
    }

    @Test fun songsMiniPlayerAndBottomBar() {
        val tabs = listOf(
            GeoTab(R.drawable.ic_music_note, "Songs", "geo-tab-songs"),
            GeoTab(R.drawable.ic_album, "Albums", "geo-tab-albums"),
            GeoTab(R.drawable.ic_person, "Artists", "geo-tab-artists"),
            GeoTab(R.drawable.ic_playlist_play, "Playlists", "geo-tab-playlists"),
        )
        val content: @androidx.compose.runtime.Composable () -> Unit = {
            Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                SongRow(song(1, "Intro"), isCurrent = false, unplayable = false, onClick = {}, onLongClick = {})
                SongRow(song(2, "Midnight City"), isCurrent = true, unplayable = false, onClick = {}, onLongClick = {})
                SongRow(song(3, "Reunion"), isCurrent = false, unplayable = false, onClick = {}, onLongClick = {})
                Spacer(Modifier.weight(1f))
                MiniPlayer(state = state, positionMs = flowOf(83_000L), onExpand = {}, onPlayPause = {}, onNext = {})
                GeoNavigationBar(tabs, selectedIndex = 0, onSelect = {})
            }
        }
        listOf(true, false).forEach { isDark -> capture("ornament_chrome_${if (isDark) "dark" else "light"}", isDark, content) }
    }
}
