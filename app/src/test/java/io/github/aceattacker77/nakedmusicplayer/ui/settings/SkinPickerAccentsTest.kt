package io.github.aceattacker77.nakedmusicplayer.ui.settings

import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinString
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
class SkinPickerAccentsTest {
    @get:Rule val compose = createComposeRule()

    private val settings = AppSettings(dynamicColor = false)

    private fun showCard(ornament: Ornament, selected: Boolean) {
        compose.setContent {
            AppTheme(Skin.FALLBACK, settings) {
                CompositionLocalProvider(LocalOrnament provides ornament) {
                    SkinPreviewCard(Skin.FALLBACK, settings, selected, onClick = {}, onLongClick = {}, modifier = Modifier)
                }
            }
        }
        compose.waitForIdle()
    }

    @Test fun selectedCard_showsTheActiveTag() {
        showCard(Ornament.OFF.copy(statusTags = true, brackets = true), selected = true)
        compose.onNodeWithContentDescription("Active", useUnmergedTree = true).assertExists()
    }

    @Test fun unselectedCard_hasNoActiveTag() {
        showCard(Ornament.OFF.copy(statusTags = true, brackets = true), selected = false)
        compose.onNodeWithContentDescription("Active", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun plainSkin_hasNoActiveTag() {
        showCard(Ornament.OFF, selected = true)
        compose.onNodeWithContentDescription("Active", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun importButton_keepsItsLabelAndClick_andHidesTheKana() {
        var clicks = 0
        val skin = Skin.FALLBACK.copy(strings = mapOf("import_kana" to SkinString("Import", "取込")))
        compose.setContent {
            AppTheme(skin, settings) { ImportSkinButton(onClick = { clicks++ }, accent = true) }
        }
        compose.onNodeWithText("Import skin").performClick()
        assertThat(clicks).isEqualTo(1)
        compose.onNodeWithText("取込", substring = true).assertDoesNotExist()
    }

    @Test fun importButton_plainStyleStillWorks() {
        var clicks = 0
        compose.setContent {
            AppTheme(Skin.FALLBACK, settings) { ImportSkinButton(onClick = { clicks++ }, accent = false) }
        }
        compose.onNodeWithText("Import skin").performClick()
        assertThat(clicks).isEqualTo(1)
    }

    @Test fun selectedCard_withBracketsOnly_keepsTheClassicBorder() {
        showCard(Ornament.OFF.copy(brackets = true), selected = true)
        compose.onNodeWithTag("active-panel", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test fun selectedCard_withBracketsAndAccents_isABracketedPanel() {
        showCard(Ornament.OFF.copy(brackets = true, statusTags = true), selected = true)
        compose.onNodeWithTag("active-panel", useUnmergedTree = true).assertExists()
    }

    @Test fun importButton_keepsItsOriginalAccessibleTextUnderLabelCaps() {
        val skin = Skin.FALLBACK.copy(labelCaps = true)
        compose.setContent { AppTheme(skin, settings) { ImportSkinButton(onClick = {}, accent = true) } }
        compose.onNodeWithContentDescription("Import skin", useUnmergedTree = true).assertExists()
    }
}
