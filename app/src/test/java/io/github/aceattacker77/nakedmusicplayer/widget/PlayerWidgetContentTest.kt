package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.test.core.app.ApplicationProvider
import androidx.glance.testing.unit.hasTestTag
import androidx.glance.testing.unit.hasText
import androidx.media3.common.Player
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PlayerWidgetContentTest {
    private val small = DpSize(250.dp, 50.dp)
    private val medium = DpSize(250.dp, 110.dp)

    private val playing = WidgetState(
        title = "Midnight City", artist = "M83", albumId = null, isPlaying = true, shuffle = true, repeatMode = Player.REPEAT_MODE_ONE, progress = 0.4f,
    )

    @Test fun small_hasThreeControls_noProgress() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(small)
        provideComposable { GlanceTheme { PlayerWidgetContent(playing, art = null) } }
        onNode(hasContentDescription("Previous")).assertExists()
        onNode(hasContentDescription("Pause")).assertExists()
        onNode(hasContentDescription("Next")).assertExists()
        onNode(hasText("Midnight City")).assertExists()
        onNode(hasText("M83")).assertExists()
        onNode(hasTestTag("widget-progress")).assertDoesNotExist()
        onNode(hasContentDescription("Shuffle: on")).assertDoesNotExist()
    }

    @Test fun medium_showsProgressAndShuffleRepeat() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(medium)
        provideComposable { GlanceTheme { PlayerWidgetContent(playing, art = null) } }
        onNode(hasTestTag("widget-progress")).assertExists()
        onNode(hasContentDescription("Shuffle: on")).assertExists()
        onNode(hasContentDescription("Repeat: one")).assertExists()
        onNode(hasContentDescription("Previous")).assertExists()
        onNode(hasContentDescription("Next")).assertExists()
    }

    @Test fun paused_showsPlay() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(small)
        provideComposable { GlanceTheme { PlayerWidgetContent(playing.copy(isPlaying = false), art = null) } }
        onNode(hasContentDescription("Play")).assertExists()
        onNode(hasContentDescription("Pause")).assertDoesNotExist()
    }

    @Test fun noTrack_showsAppNameAndPlay() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(medium)
        provideComposable { GlanceTheme { PlayerWidgetContent(WidgetState.IDLE, art = null) } }
        onNode(hasText("Music Player")).assertExists()
        onNode(hasContentDescription("Play")).assertExists()
        onNode(hasContentDescription("Next")).assertDoesNotExist()
        onNode(hasTestTag("widget-progress")).assertDoesNotExist()
    }

    @Test fun repeatAndShuffleLabels_followState() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(medium)
        provideComposable {
            GlanceTheme { PlayerWidgetContent(playing.copy(shuffle = false, repeatMode = 0), art = null) }
        }
        onNode(hasContentDescription("Shuffle: off")).assertExists()
        onNode(hasContentDescription("Repeat: off")).assertExists()
    }
}
