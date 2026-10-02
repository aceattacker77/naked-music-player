package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ColorMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SchemeSelectionTest {
    private val light = lightColorScheme(primary = Color(0xFF111111))
    private val dark = darkColorScheme(primary = Color(0xFF222222))
    private val dynamicLight = lightColorScheme(primary = Color(0xFF333333))
    private val dynamicDark = darkColorScheme(primary = Color(0xFF444444))
    private val dynamic: (Boolean) -> ColorScheme = { isDark -> if (isDark) dynamicDark else dynamicLight }

    private fun skin(mode: ColorMode, l: ColorScheme? = light, d: ColorScheme? = dark) =
        Skin.FALLBACK.copy(colorMode = mode, light = l, dark = d)

    @Test fun lightOnlySkin_staysLightWhenSystemDark() {
        val s = skin(ColorMode.LIGHT, d = null)
        val result = selectColorScheme(s, AppSettings(themeMode = ThemeMode.SYSTEM), systemDark = true, dynamic = null)
        assertThat(result).isEqualTo(light)
    }

    @Test fun darkOnlySkin_staysDarkWhenThemeLight() {
        val s = skin(ColorMode.DARK, l = null)
        val result = selectColorScheme(s, AppSettings(themeMode = ThemeMode.LIGHT), systemDark = false, dynamic = null)
        assertThat(result).isEqualTo(dark)
    }

    @Test fun bothMode_followsThemeSetting() {
        val s = skin(ColorMode.BOTH)
        assertThat(selectColorScheme(s, AppSettings(themeMode = ThemeMode.DARK), false, null)).isEqualTo(dark)
        assertThat(selectColorScheme(s, AppSettings(themeMode = ThemeMode.LIGHT), true, null)).isEqualTo(light)
        assertThat(selectColorScheme(s, AppSettings(themeMode = ThemeMode.SYSTEM), true, null)).isEqualTo(dark)
        assertThat(selectColorScheme(s, AppSettings(themeMode = ThemeMode.SYSTEM), false, null)).isEqualTo(light)
    }

    @Test fun systemMode_usesDynamicWhenAvailableAndEnabled() {
        val s = skin(ColorMode.SYSTEM)
        val on = AppSettings(themeMode = ThemeMode.DARK, dynamicColor = true)
        assertThat(selectColorScheme(s, on, false, dynamic)).isEqualTo(dynamicDark)
        val off = AppSettings(themeMode = ThemeMode.DARK, dynamicColor = false)
        assertThat(selectColorScheme(s, off, false, dynamic)).isEqualTo(dark)
    }

    @Test fun systemMode_noDynamic_fallsBackToDefault() {
        val s = skin(ColorMode.SYSTEM, l = null, d = null)
        val result = selectColorScheme(s, AppSettings(themeMode = ThemeMode.DARK), false, dynamic = null)
        assertThat(result).isEqualTo(Skin.FALLBACK.dark)
        val lightResult = selectColorScheme(s, AppSettings(themeMode = ThemeMode.LIGHT), true, dynamic = null)
        assertThat(lightResult).isEqualTo(Skin.FALLBACK.light)
    }

    @Test fun fixedModes_ignoreDynamicColor() {
        val s = skin(ColorMode.DARK)
        assertThat(selectColorScheme(s, AppSettings(dynamicColor = true), false, dynamic)).isEqualTo(dark)
    }
}
