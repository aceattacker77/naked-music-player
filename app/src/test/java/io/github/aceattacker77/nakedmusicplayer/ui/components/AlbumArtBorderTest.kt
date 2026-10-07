package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.theme.Ornament
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AlbumArtBorderTest {
    @get:Rule val compose = createComposeRule()

    private fun show(ornament: Ornament, highlighted: Boolean) {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) {
                CompositionLocalProvider(LocalOrnament provides ornament) {
                    AlbumArt(albumId = null, highlighted = highlighted)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun noBorder_byDefault() {
        show(Ornament.OFF, highlighted = false)
        compose.onNodeWithTag("art-border", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun artBorder_bordersEveryTile() {
        show(Ornament.OFF.copy(artBorder = true), highlighted = false)
        compose.onNodeWithTag("art-border", useUnmergedTree = true).assertExists()
    }

    @Test fun highlightedTile_getsABorderEvenWithoutArtBorder() {
        show(Ornament.OFF, highlighted = true)
        compose.onNodeWithTag("art-border", useUnmergedTree = true).assertExists()
    }
}
