package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.GlowMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.theme.Ornament
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeoPanelTest {
    @get:Rule val compose = createComposeRule()

    private fun show(content: @androidx.compose.runtime.Composable () -> Unit) {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) { content() }
        }
        compose.waitForIdle()
    }

    @Test fun header_showsTitleCodeAndRule() {
        show { GeoPanel(title = "Bands", code = "05 CH") { Text("body") } }
        compose.onNodeWithText("Bands").assertExists()
        compose.onNodeWithText("05 CH").assertExists()
        compose.onNodeWithTag("geo-panel-rule", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("body").assertExists()
    }

    @Test fun withoutATitle_thereIsNoHeader() {
        show { GeoPanel { Text("body") } }
        compose.onNodeWithTag("geo-panel-rule", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithText("body").assertExists()
    }

    private fun livePanelShows(mode: GlowMode) {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) {
                CompositionLocalProvider(LocalOrnament provides Ornament.OFF.copy(glow = mode)) {
                    GeoPanel(live = true) { Text("body") }
                }
            }
        }
        compose.waitForIdle()
        compose.onNodeWithText("body").assertExists()
    }

    @Test fun livePanel_glowOff_keepsItsContent() = livePanelShows(GlowMode.OFF)

    @Test fun livePanel_glowAlways_keepsItsContent() = livePanelShows(GlowMode.ALWAYS)

    @Test fun livePanel_glowDark_keepsItsContent() = livePanelShows(GlowMode.DARK)
}
