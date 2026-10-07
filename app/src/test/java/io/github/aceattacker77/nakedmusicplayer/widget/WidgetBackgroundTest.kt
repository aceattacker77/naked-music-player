package io.github.aceattacker77.nakedmusicplayer.widget

import android.content.Context
import android.content.res.Configuration
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WidgetBackgroundTest {
    private val skin = Skin.FALLBACK.copy(
        light = lightColorScheme(surface = Color(0xFFF6F2E8), background = Color(0xFFECE7DA)),
        dark = darkColorScheme(surface = Color(0xFF0B0B0E), background = Color(0xFF000000)),
    )

    private fun context(night: Boolean): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val config = Configuration(base.resources.configuration).apply {
            uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
                if (night) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
        }
        return base.createConfigurationContext(config)
    }

    @Test fun background_isTheSkinsSurface_inLightMode() {
        val colour = widgetBackground(widgetColorProviders(skin)).getColor(context(night = false))
        assertThat(colour.toArgb()).isEqualTo(Color(0xFFF6F2E8).toArgb())
    }

    @Test fun background_isTheSkinsSurface_inDarkMode() {
        val colour = widgetBackground(widgetColorProviders(skin)).getColor(context(night = true))
        assertThat(colour.toArgb()).isEqualTo(Color(0xFF0B0B0E).toArgb())
    }
}
