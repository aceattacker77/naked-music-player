package io.github.aceattacker77.nakedmusicplayer.widget

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ColorMode
import org.junit.Test

class WidgetColorsTest {
    @Test fun systemSkin_withDynamicEnabled_onAndroid12_usesDynamicColours() {
        assertThat(widgetUsesDynamicColors(ColorMode.SYSTEM, dynamicColorEnabled = true, sdkInt = 31)).isTrue()
        assertThat(widgetUsesDynamicColors(ColorMode.SYSTEM, dynamicColorEnabled = true, sdkInt = 36)).isTrue()
    }

    @Test fun belowAndroid12_neverDynamic() {
        assertThat(widgetUsesDynamicColors(ColorMode.SYSTEM, dynamicColorEnabled = true, sdkInt = 30)).isFalse()
    }

    @Test fun dynamicDisabledInSettings_usesSkinColours() {
        assertThat(widgetUsesDynamicColors(ColorMode.SYSTEM, dynamicColorEnabled = false, sdkInt = 34)).isFalse()
    }

    @Test fun fixedColourSkins_neverDynamic() {
        listOf(ColorMode.LIGHT, ColorMode.DARK, ColorMode.BOTH).forEach {
            assertThat(widgetUsesDynamicColors(it, dynamicColorEnabled = true, sdkInt = 34)).isFalse()
        }
    }
}
