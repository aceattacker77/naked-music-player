package io.github.aceattacker77.nakedmusicplayer.ui.equalizer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.aceattacker77.nakedmusicplayer.data.InMemoryPreferencesStore
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqRepository
import io.github.aceattacker77.nakedmusicplayer.playback.eq.EqualizerController
import io.github.aceattacker77.nakedmusicplayer.playback.eq.FakeAudioEffectsBackend
import io.github.aceattacker77.nakedmusicplayer.ui.components.ScreenTitle
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongRow
import io.github.aceattacker77.nakedmusicplayer.ui.library.SortBar
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingActions
import io.github.aceattacker77.nakedmusicplayer.ui.player.NowPlayingScreen
import io.github.aceattacker77.nakedmusicplayer.ui.player.PlayerUiState
import io.github.aceattacker77.nakedmusicplayer.ui.settings.SkinPreviewCard
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPlaceholder
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtShape
import io.github.aceattacker77.nakedmusicplayer.ui.skins.BackgroundStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlShape
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlsStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.CornerStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.GlowMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.NavStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekBarStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekColor
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinString
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden images for a skin with every Phase 1 to 3 field on (kana strings included), in dark and light: the Equalizer,
 * the skin picker cards, a Songs header with a playing and an unplayable row, and Now Playing with its status tag.
 * Record with `recordRoborazziDebug`, check with `verifyRoborazziDebug`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class AccentsScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val skin = Skin.FALLBACK.copy(
        cornerStyle = CornerStyle.CHAMFER, chamferDp = 10, headingScaleX = 0.8f, labelCaps = true, labelLetterSpacingEm = 0.14f,
        brackets = true, segmentedMeters = true, navStyle = NavStyle.BLOCK, rowEdge = true,
        statusTags = true, titleCards = true, panelHeader = true, squareSwitch = true, glow = GlowMode.ALWAYS,
        strings = mapOf(
            "now_playing_status" to SkinString("Playing", "再生"),
            "unplayable_tag" to SkinString("Unplayable", "否決"),
            "eq_enabled_tag" to SkinString("Enabled", "稼働"),
            "skin_active_tag" to SkinString("Active", "適用"),
            "library_kicker" to SkinString("Library", "曲目"),
            "eq_kicker" to SkinString("Audio", "音響"),
            "skins_kicker" to SkinString("Skins", "皮膚"),
            "sort_kana" to SkinString("Sort", "順"),
            "save_kana" to SkinString("Save", "保存"),
        ),
        player = Skin.FALLBACK.player.copy(
            background = BackgroundStyle.Solid, artShape = ArtShape.Square, seekBar = SeekBarStyle.SEGMENTED, seekSegments = 40,
            seekColor = SeekColor.TERTIARY, controls = ControlsStyle.MIXED, controlShape = ControlShape.THEME,
            useArtColors = false, artPlaceholder = ArtPlaceholder.HEXAGON, artBorder = true,
        ),
    )

    private val settingsLight = AppSettings(themeMode = ThemeMode.LIGHT, dynamicColor = false)
    private val settingsDark = AppSettings(themeMode = ThemeMode.DARK, dynamicColor = false)

    private var dark by mutableStateOf(false)
    private var composed = false

    private fun capture(name: String, isDark: Boolean, content: @Composable () -> Unit) {
        if (!composed) {
            composed = true
            compose.setContent {
                AppTheme(skin, if (dark) settingsDark else settingsLight) {
                    // The app's root is a Surface, which also sets the content colour for bare text.
                    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        Column(Modifier.fillMaxSize()) { content() }
                    }
                }
            }
        }
        dark = isDark
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun both(scene: String, content: @Composable () -> Unit) {
        listOf(true, false).forEach { isDark -> capture("accents_${scene}_${if (isDark) "dark" else "light"}", isDark, content) }
    }

    private fun song(id: Long, title: String) = Song(
        id = id, uri = "content://x/$id", title = title, artist = "M83", album = "Hurry Up", albumId = id, artistId = 1,
        durationMs = 243_000, discNumber = 1, trackNumber = id.toInt(), dateAddedSec = 0, relativePath = "Music/",
        displayName = "$title.mp3", year = null,
    )

    @Test fun equalizer() {
        val backend = FakeAudioEffectsBackend()
        val controller = EqualizerController(
            backend, EqRepository(InMemoryPreferencesStore()), TestScope(UnconfinedTestDispatcher()).backgroundScope,
        ) {}
        controller.onAudioSessionId(1)
        controller.setEnabled(true)
        controller.setBand(1, 600)
        controller.setBand(3, -450)
        both("equalizer") { EqualizerScreen(controller, onBack = {}) }
    }

    @Test fun skinPicker() {
        both("picker") {
            Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                val settings = if (dark) settingsDark else settingsLight
                SkinPreviewCard(skin.copy(name = "Geofront"), settings, selected = true, onClick = {}, onLongClick = {}, modifier = Modifier.weight(1f))
                SkinPreviewCard(Skin.FALLBACK.copy(name = "Default"), settings, selected = false, onClick = {}, onLongClick = {}, modifier = Modifier.weight(1f))
            }
        }
    }

    @Test fun songsHeaderWithTags() {
        both("songs") {
            ScreenTitle(SkinString("Library", "曲目"), "Songs")
            SortBar(sort = SongSort.TITLE, onSort = {}, trackCount = 3)
            SongRow(song(1, "Intro"), isCurrent = false, unplayable = false, onClick = {}, onLongClick = {})
            SongRow(song(2, "Midnight City"), isCurrent = true, unplayable = false, onClick = {}, onLongClick = {})
            SongRow(song(3, "Reunion"), isCurrent = false, unplayable = true, onClick = {}, onLongClick = {})
        }
    }

    @Test fun nowPlayingTag() {
        val state = PlayerUiState(
            current = MediaItem.Builder().setMediaId("song:2").setMediaMetadata(
                MediaMetadata.Builder().setTitle("Midnight City").setArtist("M83").setAlbumTitle("Hurry Up, We're Dreaming").build(),
            ).build(),
            isPlaying = true, durationMs = 243_000, queue = emptyList(), currentIndex = 1, shuffle = false,
            repeatMode = Player.REPEAT_MODE_OFF,
        )
        both("now_playing") { NowPlayingScreen(state = state, positionMs = 83_000, actions = NowPlayingActions.None) }
    }
}
