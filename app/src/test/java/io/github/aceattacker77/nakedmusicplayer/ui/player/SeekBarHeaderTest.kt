package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SeekBarStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(qualifiers = "w411dp-h891dp")
@RunWith(RobolectricTestRunner::class)
class SeekBarHeaderTest {
    @get:Rule val compose = createComposeRule()

    private fun show(positionMs: Long) {
        compose.setContent {
            AppTheme(Skin.FALLBACK, AppSettings(dynamicColor = false)) {
                SeekBarWithTimes(
                    style = SeekBarStyle.SEGMENTED, positionMs = positionMs, durationMs = 100_000, onSeek = {},
                    isPlaying = false, showHeader = true,
                )
            }
        }
        compose.waitForIdle()
    }

    @Test fun header_showsThePlaybackPosition() {
        show(positionMs = 25_000)
        compose.onNodeWithText("25.0 %").assertExists()
    }

    @Test fun header_followsADragInProgress() {
        show(positionMs = 0)
        compose.onNodeWithText("0.0 %").assertExists()
        // Press at the middle of the bar without releasing: the header must track the finger, not the player.
        compose.onNodeWithContentDescription("Seek").performTouchInput { down(Offset(width * 0.5f, height / 2f)) }
        compose.waitForIdle()
        compose.onNodeWithText("50.0 %").assertExists()
    }
}
