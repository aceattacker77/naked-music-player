package io.github.aceattacker77.nakedmusicplayer.ui.skins

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BuiltInSkinsTest {
    private val skins by lazy {
        val assets = ApplicationProvider.getApplicationContext<Context>().assets
        BuiltInSkins.load(assets) { json, defaults -> SkinParser.parse(json, defaults) }
    }

    private fun skin(id: String) = skins.first { it.id == id }

    @Test fun allFourParseOk() {
        assertThat(skins).hasSize(4)
    }

    @Test fun ids() {
        assertThat(BuiltInSkins.IDS)
            .containsExactly("builtin.default", "builtin.vinyl", "builtin.minimal", "builtin.amoled").inOrder()
        assertThat(skins.map { it.id }).containsExactlyElementsIn(BuiltInSkins.IDS).inOrder()
    }

    @Test fun default_matchesFallback() {
        val default = skin("builtin.default")
        assertThat(default.name).isEqualTo("Default")
        assertThat(default.colorMode).isEqualTo(ColorMode.SYSTEM)
        assertThat(default.cornerRadiusDp).isEqualTo(28)
        assertThat(default.player).isEqualTo(Skin.FALLBACK.player)
        assertThat(default.layout).isEqualTo(Skin.FALLBACK.layout)
    }

    @Test fun vinyl_values() {
        val vinyl = skin("builtin.vinyl")
        assertThat(vinyl.colorMode).isEqualTo(ColorMode.BOTH)
        assertThat(vinyl.light!!.primary).isEqualTo(Color(0xFFC0392B))
        assertThat(vinyl.player.background).isEqualTo(BackgroundStyle.BlurredArt)
        assertThat(vinyl.player.artShape).isEqualTo(ArtShape.Circle)
        assertThat(vinyl.player.artSpin).isTrue()
        assertThat(vinyl.player.seekBar).isEqualTo(SeekBarStyle.FLAT)
        assertThat(vinyl.player.controls).isEqualTo(ControlsStyle.FILLED)
        assertThat(vinyl.player.controlSize).isEqualTo(ControlSize.LARGE)
        assertThat(vinyl.layout).isEqualTo(LayoutSpec(LayoutType.VINYL, ArtPosition.CENTER))
    }

    @Test fun minimal_values() {
        val minimal = skin("builtin.minimal")
        assertThat(minimal.colorMode).isEqualTo(ColorMode.BOTH)
        assertThat(minimal.light!!.primary).isEqualTo(Color(0xFF455A64))
        assertThat(minimal.player.background).isEqualTo(BackgroundStyle.Solid)
        assertThat(minimal.player.artShape).isEqualTo(ArtShape.Square)
        assertThat(minimal.player.seekBar).isEqualTo(SeekBarStyle.THIN)
        assertThat(minimal.player.controls).isEqualTo(ControlsStyle.ICON_ONLY)
        assertThat(minimal.player.controlSize).isEqualTo(ControlSize.SMALL)
        assertThat(minimal.layout).isEqualTo(LayoutSpec(LayoutType.MINIMAL, ArtPosition.LEFT))
    }

    @Test fun amoled_backgroundIsPureBlack() {
        val amoled = skin("builtin.amoled")
        assertThat(amoled.colorMode).isEqualTo(ColorMode.DARK)
        assertThat(amoled.dark!!.background).isEqualTo(Color(0xFF000000))
        assertThat(amoled.dark!!.surface).isEqualTo(Color(0xFF000000))
        assertThat(amoled.dark!!.primary).isEqualTo(Color(0xFFBB86FC))
        assertThat(amoled.player.artShape).isEqualTo(ArtShape.Rounded(16))
        assertThat(amoled.player.controls).isEqualTo(ControlsStyle.OUTLINED)
        assertThat(amoled.player.controlSize).isEqualTo(ControlSize.MEDIUM)
    }
}
