package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.material3.Text
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AppThemeLabelCapsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun skinWithLabelCaps_uppercasesSkinLabels() {
        compose.setContent {
            AppTheme(Skin.FALLBACK.copy(labelCaps = true), AppSettings(dynamicColor = false)) { Text(skinLabel("Songs")) }
        }
        compose.onNodeWithText("SONGS").assertExists()
    }

    @Test fun skinWithoutLabelCaps_leavesSkinLabelsAlone() {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) { Text(skinLabel("Songs")) }
        }
        compose.onNodeWithText("Songs").assertExists()
    }
}
