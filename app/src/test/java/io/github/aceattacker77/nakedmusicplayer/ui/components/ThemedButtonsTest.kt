package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.test.junit4.createComposeRule
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.settings.AppSettings
import io.github.aceattacker77.nakedmusicplayer.ui.skins.CornerStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import io.github.aceattacker77.nakedmusicplayer.ui.theme.AppTheme
import io.github.aceattacker77.nakedmusicplayer.ui.theme.ChamferShape
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ThemedButtonsTest {
    @get:Rule val compose = createComposeRule()

    private val chamferSkin = Skin.FALLBACK.copy(cornerStyle = CornerStyle.CHAMFER, chamferDp = 10)

    private fun shapes(skin: Skin): Pair<Shape, Shape> {
        lateinit var result: Pair<Shape, Shape>
        compose.setContent {
            AppTheme(skin, AppSettings(dynamicColor = false)) {
                result = ButtonDefaults.shape to themedButtonShape()
            }
        }
        compose.waitForIdle()
        return result
    }

    @Test fun materialButtonDefault_ignoresTheChamferTheme() {
        // The premise of themedButtonShape: Material's buttons do not follow the theme's shape set on their own.
        assertThat(shapes(chamferSkin).first).isNotInstanceOf(ChamferShape::class.java)
    }

    @Test fun themedButtonShape_isChamferInAChamferSkin() {
        assertThat(shapes(chamferSkin).second).isInstanceOf(ChamferShape::class.java)
    }

    @Test fun themedButtonShape_isTheMaterialDefaultOtherwise() {
        val (material, themed) = shapes(Skin.FALLBACK)
        assertThat(themed).isEqualTo(material)
    }
}
