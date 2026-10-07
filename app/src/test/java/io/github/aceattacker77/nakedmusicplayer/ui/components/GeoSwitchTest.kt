package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.ui.platform.testTag
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class GeoSwitchTest {
    @get:Rule val compose = createComposeRule()

    private val changes = mutableListOf<Boolean>()

    private fun show(checked: Boolean, callback: ((Boolean) -> Unit)? = { changes += it }) {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) { GeoSwitch(checked, callback, Modifier.testTag("geo-switch")) }
        }
        compose.waitForIdle()
    }

    @Test fun hasSwitchRoleAndReportsItsState() {
        show(checked = true)
        compose.onNodeWithTag("geo-switch").assertIsOn()
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Switch))
    }

    @Test fun reportsOffState() {
        show(checked = false)
        compose.onNodeWithTag("geo-switch").assertIsOff()
    }

    @Test fun click_callsBackWithTheOppositeValueOnce() {
        show(checked = false)
        compose.onNodeWithTag("geo-switch").performClick()
        assertThat(changes).containsExactly(true)
    }

    @Test fun nullCallback_isDisabled() {
        show(checked = false, callback = null)
        compose.onNodeWithTag("geo-switch").assertIsNotEnabled()
    }

    @Test fun withACallback_isEnabledAndHasAFullTouchTarget() {
        show(checked = false)
        compose.onNodeWithTag("geo-switch").assertIsEnabled().assertTouchHeightIsEqualTo(48.dp)
    }
}
