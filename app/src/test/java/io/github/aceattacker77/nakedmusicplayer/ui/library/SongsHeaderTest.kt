package io.github.aceattacker77.nakedmusicplayer.ui.library

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.theme.Ornament
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SongsHeaderTest {
    @get:Rule val compose = createComposeRule()

    private fun show(ornament: Ornament, count: Int, skin: Skin = Skin.FALLBACK) {
        compose.setContent {
            AppTheme(skin, AppSettings(dynamicColor = false)) {
                CompositionLocalProvider(LocalOrnament provides ornament) {
                    SortBar(sort = SongSort.TITLE, onSort = {}, trackCount = count)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun titleCards_showTheTrackCountForZero() {
        show(Ornament.OFF.copy(titleCards = true), 0)
        compose.onNodeWithText("0 songs").assertExists()
    }

    @Test fun titleCards_showTheTrackCountForOne() {
        show(Ornament.OFF.copy(titleCards = true), 1)
        compose.onNodeWithText("1 song").assertExists()
    }

    @Test fun titleCards_showTheTrackCountForMany() {
        show(Ornament.OFF.copy(titleCards = true), 25)
        compose.onNodeWithText("25 songs").assertExists()
    }

    @Test fun withoutTitleCards_thereIsNoCount() {
        show(Ornament.OFF, 25)
        compose.onNodeWithText("25 songs").assertDoesNotExist()
    }

    @Test fun titleCards_countKeepsItsOriginalAccessibleTextUnderLabelCaps() {
        show(Ornament.OFF.copy(titleCards = true), 25, Skin.FALLBACK.copy(labelCaps = true))
        compose.onNodeWithText("25 SONGS").assertExists()
        compose.onNodeWithContentDescription("25 songs", useUnmergedTree = true).assertExists()
    }
}
