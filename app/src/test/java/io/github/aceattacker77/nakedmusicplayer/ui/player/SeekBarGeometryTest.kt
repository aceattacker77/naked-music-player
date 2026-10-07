package io.github.aceattacker77.nakedmusicplayer.ui.player

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class SeekBarGeometryTest {
    @Test fun segmentsFilled_edges() {
        assertThat(segmentsFilled(0f, 40)).isEqualTo(0)
        assertThat(segmentsFilled(1f, 40)).isEqualTo(40)
        assertThat(segmentsFilled(0.5f, 40)).isEqualTo(20)
    }

    @Test fun segmentsFilled_floorsPartialCells() {
        assertThat(segmentsFilled(0.3125f, 40)).isEqualTo(12)
        assertThat(segmentsFilled(0.99f, 12)).isEqualTo(11)
    }

    @Test fun segmentsFilled_clampsInputs() {
        assertThat(segmentsFilled(-1f, 40)).isEqualTo(0)
        assertThat(segmentsFilled(2f, 40)).isEqualTo(40)
        assertThat(segmentsFilled(1f, 60)).isEqualTo(60)
        assertThat(segmentsFilled(Float.NaN, 40)).isEqualTo(0)
    }

    @Test fun positionPercentText_oneDecimalWithTrailingZero() {
        assertThat(positionPercentText(0.3125f)).isEqualTo("31.3 %")
        assertThat(positionPercentText(0f)).isEqualTo("0.0 %")
        assertThat(positionPercentText(1f)).isEqualTo("100.0 %")
        assertThat(positionPercentText(1.5f)).isEqualTo("100.0 %")
        assertThat(positionPercentText(-0.2f)).isEqualTo("0.0 %")
    }

    @Test fun positionPercentText_neverShows100BeforeTheEnd() {
        assertThat(positionPercentText(0.9996f)).isEqualTo("99.9 %")
        assertThat(positionPercentText(0.99999994f)).isEqualTo("99.9 %")
        assertThat(positionPercentText(1f)).isEqualTo("100.0 %")
    }

    @Test fun segmentCountFor_usesFewerCellsWhenTheBarIsNarrow() {
        assertThat(segmentCountFor(segments = 40, widthPx = 1000f, gapPx = 9f, minCellPx = 6f)).isEqualTo(40)
        assertThat(segmentCountFor(segments = 40, widthPx = 100f, gapPx = 9f, minCellPx = 6f)).isEqualTo(7)
        assertThat(segmentCountFor(segments = 60, widthPx = 0f, gapPx = 9f, minCellPx = 6f)).isEqualTo(0)
        assertThat(segmentCountFor(segments = 12, widthPx = 5f, gapPx = 9f, minCellPx = 6f)).isEqualTo(0)
    }
}
