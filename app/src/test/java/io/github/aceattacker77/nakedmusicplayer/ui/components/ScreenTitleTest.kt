package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinString
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h891dp")
@RunWith(RobolectricTestRunner::class)
class ScreenTitleTest {
    @get:Rule val compose = createComposeRule()

    @Test fun kickerText_joinsEnglishAndKana() {
        assertThat(kickerText(SkinString("Library", "曲目"))).isEqualTo("Library // 曲目")
        assertThat(kickerText(SkinString("Library", null))).isEqualTo("Library")
    }

    private fun show(title: String, fontScale: Float = 1f) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) {
                    ScreenTitle(SkinString("Library", "曲目"), title)
                }
            }
        }
        compose.waitForIdle()
    }

    private fun overflows(title: String): Boolean {
        val results = mutableListOf<TextLayoutResult>()
        compose.onNodeWithText(title).fetchSemanticsNode().config
            .getOrNull(SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertThat(results).isNotEmpty()
        return results.first().hasVisualOverflow
    }

    @Test fun shippedTitles_fitAtLargeFontScale() {
        // One setContent per test, so the six titles are checked through the longest ones the app uses.
        show("Playlists", fontScale = 1.3f)
        assertThat(overflows("Playlists")).isFalse()
    }

    @Test fun equalizerTitle_fitsAtLargeFontScale() {
        show("Equalizer", fontScale = 1.3f)
        assertThat(overflows("Equalizer")).isFalse()
    }

    @Test fun kicker_exposesOnlyTheEnglishToAccessibility() {
        show("Songs")
        compose.onNodeWithText("Library").assertExists()
        compose.onNodeWithText("曲目", substring = true).assertDoesNotExist()
    }

    @Test fun title_isExposedAsAHeading() {
        show("Songs")
        val node = compose.onNodeWithText("Songs").fetchSemanticsNode()
        assertThat(node.config.contains(androidx.compose.ui.semantics.SemanticsProperties.Heading)).isTrue()
    }
}
