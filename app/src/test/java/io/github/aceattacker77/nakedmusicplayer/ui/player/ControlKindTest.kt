package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.material3.Shapes
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlShape
import io.github.aceattacker77.nakedmusicplayer.ui.skins.ControlsStyle
import org.junit.Test

class ControlKindTest {
    @Test fun filled_playIsFilled_sidesAreTonal() {
        assertThat(controlKind(ControlsStyle.FILLED, primary = true)).isEqualTo(ControlKind.FILLED)
        assertThat(controlKind(ControlsStyle.FILLED, primary = false)).isEqualTo(ControlKind.TONAL)
    }

    @Test fun outlined_allOutlined() {
        assertThat(controlKind(ControlsStyle.OUTLINED, primary = true)).isEqualTo(ControlKind.OUTLINED)
        assertThat(controlKind(ControlsStyle.OUTLINED, primary = false)).isEqualTo(ControlKind.OUTLINED)
    }

    @Test fun iconOnly_allIcon() {
        assertThat(controlKind(ControlsStyle.ICON_ONLY, primary = true)).isEqualTo(ControlKind.ICON)
        assertThat(controlKind(ControlsStyle.ICON_ONLY, primary = false)).isEqualTo(ControlKind.ICON)
    }

    @Test fun mixed_playIsFilled_sidesAreOutlined() {
        assertThat(controlKind(ControlsStyle.MIXED, primary = true)).isEqualTo(ControlKind.FILLED)
        assertThat(controlKind(ControlsStyle.MIXED, primary = false)).isEqualTo(ControlKind.OUTLINED)
    }

    @Test fun controlShape_circleKeepsMaterialDefault() {
        assertThat(controlShapeFor(ControlShape.CIRCLE, Shapes())).isNull()
    }

    @Test fun controlShape_themeUsesLarge() {
        val shapes = Shapes()
        assertThat(controlShapeFor(ControlShape.THEME, shapes)).isEqualTo(shapes.large)
    }
}
