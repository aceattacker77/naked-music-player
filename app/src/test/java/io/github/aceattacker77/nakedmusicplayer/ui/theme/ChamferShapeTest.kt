package io.github.aceattacker77.nakedmusicplayer.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.ui.skins.CornerStyle
import io.github.aceattacker77.nakedmusicplayer.ui.skins.Skin
import org.junit.Test

class ChamferShapeTest {
    @Test fun polygon_cutsTopRightAndBottomLeft() {
        assertThat(chamferPoints(Size(100f, 60f), 10f)).containsExactly(
            Offset(0f, 0f), Offset(90f, 0f), Offset(100f, 10f),
            Offset(100f, 60f), Offset(10f, 60f), Offset(0f, 50f),
        ).inOrder()
    }

    @Test fun cut_isClampedToHalfShorterSide() {
        val square = chamferPoints(Size(20f, 20f), 50f)
        assertThat(square[1]).isEqualTo(Offset(10f, 0f))
        val strip = chamferPoints(Size(100f, 8f), 10f)
        assertThat(strip[1]).isEqualTo(Offset(96f, 0f))
        listOf(square, strip).forEachIndexed { i, points ->
            val size = if (i == 0) Size(20f, 20f) else Size(100f, 8f)
            points.forEach {
                assertThat(it.x).isAtLeast(0f)
                assertThat(it.x).isAtMost(size.width)
                assertThat(it.y).isAtLeast(0f)
                assertThat(it.y).isAtMost(size.height)
            }
        }
    }

    @Test fun cuts_scaleFromChamferDp() {
        assertThat(chamferCutsDp(10)).containsExactly(3.dp, 5.dp, 8.dp, 10.dp, 14.dp).inOrder()
        assertThat(chamferCutsDp(20)).containsExactly(6.dp, 10.dp, 16.dp, 20.dp, 28.dp).inOrder()
    }

    @Test fun chamferSkin_usesChamferShapes_andIgnoresRadius() {
        val shapes = shapesFor(Skin.FALLBACK.copy(cornerStyle = CornerStyle.CHAMFER, chamferDp = 10, cornerRadiusDp = 28))
        assertThat(shapes.extraSmall).isEqualTo(ChamferShape(3.dp))
        assertThat(shapes.small).isEqualTo(ChamferShape(5.dp))
        assertThat(shapes.medium).isEqualTo(ChamferShape(8.dp))
        assertThat(shapes.large).isEqualTo(ChamferShape(10.dp))
        assertThat(shapes.extraLarge).isEqualTo(ChamferShape(14.dp))
    }

    @Test fun roundSkin_isUnchanged() {
        fun r(factor: Float) = RoundedCornerShape((28 * factor).dp)
        val expected = Shapes(extraSmall = r(0.25f), small = r(0.5f), medium = r(0.75f), large = r(1f), extraLarge = r(1.5f))
        assertThat(shapesFor(Skin.FALLBACK)).isEqualTo(expected)
    }

    @Test fun shapeOr_returnsThemedOnlyForChamfer() {
        val fallback = RoundedCornerShape(8.dp)
        assertThat(shapeOr(fallback, ChamferShape(5.dp))).isEqualTo(ChamferShape(5.dp))
        assertThat(shapeOr(fallback, RoundedCornerShape(14.dp))).isEqualTo(fallback)
    }
}
