package io.github.aceattacker77.nakedmusicplayer.ui.settings

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.data.settings.ThemeMode
import io.github.aceattacker77.nakedmusicplayer.ui.skins.BuiltInSkins
import io.github.aceattacker77.nakedmusicplayer.ui.skins.SkinParser
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** Golden image of the skin picker grid: the miniature previews must look like the real player. */
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
@RunWith(RobolectricTestRunner::class)
class SkinPreviewScreenshotTest {
    @get:Rule val compose = createComposeRule()

    @Test fun builtInSkinCards() {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        val skins = BuiltInSkins.load(assets) { json, defaults -> SkinParser.parse(json, defaults) }
        val settings = AppSettings(themeMode = ThemeMode.LIGHT, dynamicColor = false)
        compose.setContent {
            AppTheme(skins.first(), settings) {
                Surface {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        skins.chunked(2).forEach { pair ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                pair.forEachIndexed { i, skin ->
                                    SkinPreviewCard(
                                        skin = skin,
                                        settings = settings,
                                        selected = skin == skins.first() && i == 0,
                                        onClick = {},
                                        onLongClick = {},
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/skin_picker_cards.png")
    }
}
