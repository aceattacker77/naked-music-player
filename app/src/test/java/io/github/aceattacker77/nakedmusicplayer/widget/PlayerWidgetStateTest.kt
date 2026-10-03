package io.github.aceattacker77.nakedmusicplayer.widget

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.testing.unit.runGlanceAppWidgetUnitTest
import androidx.glance.testing.unit.hasContentDescription
import androidx.glance.testing.unit.hasText
import androidx.media3.common.Player
import androidx.test.core.app.ApplicationProvider
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Glance keeps a widget session alive and recomposes it on every update, so the content must read the
 * widget's saved state *inside* the composition. A copy captured before `provideContent` goes stale.
 */
@RunWith(RobolectricTestRunner::class)
class PlayerWidgetStateTest {
    private val medium = DpSize(250.dp, 110.dp)

    @Test fun content_showsTheSavedWidgetState() = runGlanceAppWidgetUnitTest {
        setContext(ApplicationProvider.getApplicationContext())
        setAppWidgetSize(medium)
        val saved = mutablePreferencesOf()
        WidgetState(
            title = "Midnight City", artist = "M83", albumId = null, isPlaying = true,
            shuffle = true, repeatMode = Player.REPEAT_MODE_ONE, progress = 0.4f,
        ).writeTo(saved)
        setState(saved)

        provideComposable { GlanceTheme { StatefulPlayerWidgetContent(art = null) } }

        onNode(hasText("Midnight City")).assertExists()
        onNode(hasContentDescription("Shuffle: on")).assertExists()
        onNode(hasContentDescription("Repeat: one")).assertExists()
        onNode(hasContentDescription("Pause")).assertExists()
    }
}
