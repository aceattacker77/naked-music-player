package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.cos

class HexagonGeometryTest {
    @Test fun pointyTop_firstTwoVertices() {
        val points = hexagonPoints(Size(100f, 100f), 0f)
        assertThat(points).hasSize(6)
        assertThat(points[0].x).isWithin(0.01f).of(50f)
        assertThat(points[0].y).isWithin(0.01f).of(0f)
        assertThat(points[1].x).isWithin(0.01f).of(50f + 50f * cos(Math.toRadians(-30.0)).toFloat())
        assertThat(points[1].y).isWithin(0.01f).of(25f)
    }

    @Test fun sixDistinctVertices() {
        assertThat(hexagonPoints(Size(100f, 80f), 4f).toSet()).hasSize(6)
    }

    @Test fun oversizedInset_collapsesToCentreWithoutNaN() {
        val points = hexagonPoints(Size(10f, 10f), 20f)
        points.forEach {
            assertThat(it.x).isWithin(0.001f).of(5f)
            assertThat(it.y).isWithin(0.001f).of(5f)
        }
    }

    @Test fun zeroSize_isSixOrigins() {
        assertThat(hexagonPoints(Size.Zero, 0f)).containsExactly(Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero, Offset.Zero)
    }

    @Test fun trackCode_isOneBasedAndZeroPadded() {
        assertThat(trackCode(1)).isEqualTo("TRK 0002")
        assertThat(trackCode(0)).isEqualTo("TRK 0001")
        assertThat(trackCode(9998)).isEqualTo("TRK 9999")
        assertThat(trackCode(9999)).isEqualTo("TRK 10000")
    }

    @Test fun trackCode_noCurrentTrackIsNull() {
        assertThat(trackCode(-1)).isNull()
    }
}
