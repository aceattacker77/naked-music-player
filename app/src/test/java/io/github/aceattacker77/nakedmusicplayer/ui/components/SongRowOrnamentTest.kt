package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.theme.Ornament
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h891dp")
@RunWith(RobolectricTestRunner::class)
class SongRowOrnamentTest {
    @get:Rule val compose = createComposeRule()

    private val song = Song(
        id = 7, uri = "content://x/7", title = "Heats", artist = "Kageyama Hironobu", album = "Music", albumId = 1,
        artistId = 1, durationMs = 244_000, discNumber = 1, trackNumber = 1, dateAddedSec = 0,
        relativePath = "Music/", displayName = "heats.mp3", year = null,
    )

    private fun show(ornament: Ornament, current: Boolean) {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) {
                CompositionLocalProvider(LocalOrnament provides ornament) {
                    SongRow(song, isCurrent = current, unplayable = false, onClick = {}, onLongClick = {})
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun rowEdge_showsEdgeOnTheCurrentRow() {
        show(Ornament.OFF.copy(rowEdge = true), current = true)
        compose.onNodeWithTag("song-edge-7", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("4:04").assertIsDisplayed()
    }

    @Test fun rowEdge_noEdgeOnOtherRows() {
        show(Ornament.OFF.copy(rowEdge = true), current = false)
        compose.onNodeWithTag("song-edge-7", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun noRowEdge_noEdgeEvenOnTheCurrentRow() {
        show(Ornament.OFF, current = true)
        compose.onNodeWithTag("song-edge-7", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("4:04").assertIsDisplayed()
    }
}
