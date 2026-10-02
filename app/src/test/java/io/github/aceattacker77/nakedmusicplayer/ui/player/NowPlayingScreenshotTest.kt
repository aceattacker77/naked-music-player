package io.github.aceattacker77.nakedmusicplayer.ui.player

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ArtPosition
import io.github.aceattacker77.nakedmusicplayer.ui.skins.BuiltInSkins
import io.github.aceattacker77.nakedmusicplayer.ui.skins.LayoutSpec
import io.github.aceattacker77.nakedmusicplayer.ui.skins.LayoutType
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinParser
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Golden-image tests for Now Playing: every built-in skin in light and dark, every layout type,
 * and the no-artwork placeholder. Record with `recordRoborazziDebug`, check with `verifyRoborazziDebug`;
 * a plain unit-test run only exercises the composition.
 */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class NowPlayingScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val skins: List<Skin> by lazy {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        BuiltInSkins.load(assets) { json, defaults -> SkinParser.parse(json, defaults) }
    }

    private val state = PlayerUiState(
        current = MediaItem.Builder()
            .setMediaId("song:1")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Midnight City")
                    .setArtist("M83")
                    .setAlbumTitle("Hurry Up, We're Dreaming")
                    .setDurationMs(243_000)
                    .build(),
            )
            .build(),
        // Paused: a playing wavy seek bar / spinning art animates forever and Compose never goes idle.
        isPlaying = false,
        durationMs = 243_000,
        queue = emptyList(),
        currentIndex = 0,
        shuffle = true,
        repeatMode = Player.REPEAT_MODE_ALL,
    )

    private class Scene(val skin: Skin, val dark: Boolean, val state: PlayerUiState)

    // createComposeRule allows one setContent per test, so later captures swap the scene instead.
    private var scene by mutableStateOf<Scene?>(null)
    private var composed = false

    private fun capture(name: String, skin: Skin, dark: Boolean, state: PlayerUiState = this.state) {
        if (!composed) {
            composed = true
            compose.setContent {
                scene?.let { s ->
                    val settings = AppSettings(themeMode = if (s.dark) ThemeMode.DARK else ThemeMode.LIGHT, dynamicColor = false)
                    AppTheme(s.skin, settings) {
                        NowPlayingScreen(state = s.state, positionMs = 83_000, actions = NowPlayingActions.None)
                    }
                }
            }
        }
        scene = Scene(skin, dark, state)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    private fun skin(id: String) = skins.first { it.id == id }

    @Test fun builtInSkins_lightAndDark() {
        // One composition per capture, so each skin/mode is a separate golden file.
        BuiltInSkins.IDS.forEach { id ->
            listOf(false, true).forEach { dark ->
                capture("now_playing_${id.removePrefix("builtin.")}_${if (dark) "dark" else "light"}", skin(id), dark)
            }
        }
    }

    @Test fun layouts_allFiveTypes() {
        val base = skin("builtin.default")
        listOf(
            LayoutSpec(LayoutType.CLASSIC, ArtPosition.LEFT),
            LayoutSpec(LayoutType.VINYL, ArtPosition.CENTER),
            LayoutSpec(LayoutType.MINIMAL, ArtPosition.TOP),
            LayoutSpec(LayoutType.CASSETTE, ArtPosition.CENTER),
            LayoutSpec(LayoutType.COMPACT, ArtPosition.LEFT),
        ).forEach { spec ->
            capture("layout_${spec.type.name.lowercase()}", base.copy(layout = spec), dark = false)
        }
    }

    @Test fun missingArt_showsPlaceholder() {
        // No album id and nothing to load: the themed placeholder must stand in for the artwork.
        capture("missing_art", skin("builtin.default"), dark = false)
    }
}
