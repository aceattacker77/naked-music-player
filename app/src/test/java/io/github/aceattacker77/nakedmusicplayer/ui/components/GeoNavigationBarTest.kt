package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h891dp")
@RunWith(RobolectricTestRunner::class)
class GeoNavigationBarTest {
    @get:Rule val compose = createComposeRule()

    private val tabs = listOf("Songs", "Albums", "Artists", "Playlists").mapIndexed { i, label ->
        GeoTab(R.drawable.ic_music_note, label, "geo-tab-$i")
    }
    private val selections = mutableListOf<Int>()

    private fun show(skin: Skin = Skin.FALLBACK, selected: Int = 1) {
        compose.setContent {
            AppTheme(skin, AppSettings(dynamicColor = false)) {
                GeoNavigationBar(tabs, selected, onSelect = { selections += it })
            }
        }
        compose.waitForIdle()
    }

    @Test fun selectedTab_reportsSelectedSemantics() {
        show(selected = 1)
        compose.onNodeWithTag("geo-tab-1").assertIsSelected()
        listOf(0, 2, 3).forEach { compose.onNodeWithTag("geo-tab-$it").assertIsNotSelected() }
    }

    @Test fun everyTab_hasAtLeast48dpTouchTarget() {
        show()
        (0..3).forEach { compose.onNodeWithTag("geo-tab-$it").assertHeightIsAtLeast(48.dp) }
    }

    @Test fun clickingATab_selectsItOnce() {
        show()
        compose.onNodeWithTag("geo-tab-2").performClick()
        assertThat(selections).containsExactly(2)
    }

    @Test fun labels_followTheSkinsLabelCaps() {
        show(Skin.FALLBACK.copy(labelCaps = true))
        compose.onNodeWithText("SONGS").assertExists()
        compose.onNodeWithText("PLAYLISTS").assertExists()
    }

    @Test fun largeFontScale_doesNotClipTheLongestLabel() {
        // Tracked caps at 1.3x text size used to cut "PLAYLISTS" short in its fixed-width cell.
        val skin = Skin.FALLBACK.copy(labelCaps = true, labelLetterSpacingEm = 0.14f)
        compose.setContent {
            val density = androidx.compose.ui.platform.LocalDensity.current
            androidx.compose.runtime.CompositionLocalProvider(
                androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(density.density, fontScale = 1.3f),
            ) {
                AppTheme(skin, AppSettings(dynamicColor = false)) {
                    GeoNavigationBar(tabs, 0, onSelect = {})
                }
            }
        }
        compose.waitForIdle()
        val results = mutableListOf<androidx.compose.ui.text.TextLayoutResult>()
        compose.onNodeWithText("PLAYLISTS").fetchSemanticsNode().config
            .getOrNull(androidx.compose.ui.semantics.SemanticsActions.GetTextLayoutResult)?.action?.invoke(results)
        assertThat(results).isNotEmpty()
        assertThat(results.first().hasVisualOverflow).isFalse()
    }

    @Test fun uppercasedLabels_keepTheirOriginalAccessibleText() {
        show(Skin.FALLBACK.copy(labelCaps = true))
        compose.onNodeWithText("SONGS").assertExists()
        compose.onNodeWithContentDescription("Songs", useUnmergedTree = true).assertExists()
    }

    @Test fun bar_isASelectableGroup() {
        show()
        compose.onNodeWithTag("geo-nav").assert(SemanticsMatcher.keyIsDefined(SemanticsProperties.SelectableGroup))
    }
}
