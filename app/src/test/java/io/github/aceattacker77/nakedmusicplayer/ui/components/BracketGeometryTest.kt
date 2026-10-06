package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class BracketGeometryTest {
    @Test fun lines_topLeftAndBottomRight_outsideByOutset() {
        assertThat(bracketLines(Size(100f, 60f), length = 14f, outset = 1f)).containsExactly(
            Offset(-1f, -1f) to Offset(13f, -1f),
            Offset(-1f, -1f) to Offset(-1f, 13f),
            Offset(101f, 61f) to Offset(87f, 61f),
            Offset(101f, 61f) to Offset(101f, 47f),
        ).inOrder()
    }

    @Test fun lines_zeroSize_isFourSegmentsWithoutNaN() {
        val lines = bracketLines(Size.Zero, length = 14f, outset = 1f)
        assertThat(lines).hasSize(4)
        lines.forEach { (a, b) ->
            listOf(a.x, a.y, b.x, b.y).forEach { assertThat(it.isNaN()).isFalse() }
        }
    }

    @Test fun length_isClampedToHalfTheShorterSide() {
        val lines = bracketLines(Size(100f, 20f), length = 14f, outset = 0f)
        assertThat(lines[0]).isEqualTo(Offset(0f, 0f) to Offset(10f, 0f))
        assertThat(lines[1]).isEqualTo(Offset(0f, 0f) to Offset(0f, 10f))
    }
}
