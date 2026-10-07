package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.GlowMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinString
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SkinStringsTest {
    @get:Rule val compose = createComposeRule()

    private val strings = mapOf("library_kicker" to SkinString("Library", "曲目"))

    @Test fun resolve_returnsTheSkinEntry() {
        assertThat(resolveSkinString(strings, "library_kicker", "Fallback")).isEqualTo(SkinString("Library", "曲目"))
    }

    @Test fun resolve_fallsBackToPlainEnglish() {
        assertThat(resolveSkinString(strings, "eq_kicker", "Audio")).isEqualTo(SkinString("Audio", null))
        assertThat(resolveSkinString(emptyMap(), "library_kicker", "Library")).isEqualTo(SkinString("Library", null))
    }

    @Test fun glowActive_truthTable() {
        assertThat(glowActive(GlowMode.OFF, isDark = true)).isFalse()
        assertThat(glowActive(GlowMode.OFF, isDark = false)).isFalse()
        assertThat(glowActive(GlowMode.ALWAYS, isDark = true)).isTrue()
        assertThat(glowActive(GlowMode.ALWAYS, isDark = false)).isTrue()
        assertThat(glowActive(GlowMode.DARK, isDark = true)).isTrue()
        assertThat(glowActive(GlowMode.DARK, isDark = false)).isFalse()
    }

    @Test fun skinText_readsThePackInsideTheTheme() {
        compose.setContent {
            AppTheme(Skin.FALLBACK.copy(strings = strings), AppSettings(dynamicColor = false)) {
                Text(skinText("library_kicker", "Plain").english + "/" + skinText("library_kicker", "Plain").kana)
            }
        }
        compose.onNodeWithText("Library/曲目").assertExists()
    }

    @Test fun skinText_fallsBackOutsideTheTheme() {
        compose.setContent { Text(skinText("library_kicker", "Plain").english) }
        compose.onNodeWithText("Plain").assertExists()
    }
}
