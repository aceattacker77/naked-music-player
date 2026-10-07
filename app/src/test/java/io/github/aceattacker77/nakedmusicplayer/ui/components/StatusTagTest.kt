package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.style.TextDecoration
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StatusTagTest {
    @get:Rule val compose = createComposeRule()

    @Test fun kinds_mapToTheirColourRoles() {
        val scheme = lightColorScheme(
            tertiary = Color(0xFF111111), error = Color(0xFF222222), primary = Color(0xFF333333), secondary = Color(0xFF444444),
        )
        assertThat(StatusKind.GOOD.color(scheme)).isEqualTo(Color(0xFF111111))
        assertThat(StatusKind.BAD.color(scheme)).isEqualTo(Color(0xFF222222))
        assertThat(StatusKind.PENDING.color(scheme)).isEqualTo(Color(0xFF333333))
        assertThat(StatusKind.INFO.color(scheme)).isEqualTo(Color(0xFF444444))
    }

    @Test fun unplayableDecoration_onlyWhenUnplayableAndTagged() {
        assertThat(unplayableDecoration(unplayable = true, statusTags = true)).isEqualTo(TextDecoration.LineThrough)
        assertThat(unplayableDecoration(unplayable = true, statusTags = false)).isNull()
        assertThat(unplayableDecoration(unplayable = false, statusTags = true)).isNull()
        assertThat(unplayableDecoration(unplayable = false, statusTags = false)).isNull()
    }

    @Test fun tag_hasOneAccessibleName_andHidesTheKana() {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) {
                StatusTag(StatusKind.GOOD, word = "Playing", kana = "再生", accessibleName = "Now playing")
            }
        }
        compose.onNodeWithContentDescription("Now playing").assertExists()
        compose.onNodeWithText("再生").assertDoesNotExist()
        compose.onNodeWithText("Playing").assertDoesNotExist()
    }
}
