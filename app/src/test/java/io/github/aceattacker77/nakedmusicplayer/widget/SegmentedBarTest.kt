package io.github.aceattacker77.nakedmusicplayer.widget

import android.graphics.Color
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@RunWith(RobolectricTestRunner::class)
class SegmentedBarTest {
    @Test fun rects_spanTheWidthWithEqualGaps() {
        val rects = segmentRects(widthPx = 100, heightPx = 20, cells = 4, gapPx = 4f)
        assertThat(rects).hasSize(4)
        assertThat(rects.first().left).isEqualTo(0f)
        assertThat(rects.last().right).isWithin(0.01f).of(100f)
        val gaps = rects.zipWithNext { a, b -> b.left - a.right }
        gaps.forEach { assertThat(it).isWithin(0.01f).of(4f) }
        val widths = rects.map { it.width() }
        widths.forEach { assertThat(it).isWithin(0.01f).of(widths.first()) }
        rects.forEach { assertThat(it.height()).isEqualTo(20f) }
    }

    @Test fun rects_useFewerCellsWhenTheBarIsNarrow() {
        // 40 requested, but at least 6 px per cell plus a 4 px gap only fits 5 in 50 px.
        assertThat(segmentRects(widthPx = 50, heightPx = 20, cells = 40, gapPx = 4f, minCellPx = 6f)).hasSize(5)
    }

    @Test fun rects_areEmptyWhenNothingFits() {
        assertThat(segmentRects(widthPx = 0, heightPx = 20, cells = 10, gapPx = 4f)).isEmpty()
        assertThat(segmentRects(widthPx = 100, heightPx = 20, cells = 0, gapPx = 4f)).isEmpty()
        assertThat(segmentRects(widthPx = 100, heightPx = 0, cells = 10, gapPx = 4f)).isEmpty()
    }

    @Test fun bitmap_fillsTheFirstCellsAndLeavesTheRestAndTheGapsClear() {
        val fill = Color.rgb(255, 0, 255)
        val border = Color.rgb(0, 255, 0)
        val bitmap = renderSegmentedBar(widthPx = 100, heightPx = 20, filled = 2, cells = 4, fill = fill, border = border, gapPx = 4f, strokePx = 2f)
        assertThat(bitmap.width).isEqualTo(100)
        assertThat(bitmap.height).isEqualTo(20)
        val rects = segmentRects(100, 20, 4, 4f)
        fun centre(i: Int) = bitmap.getPixel(rects[i].centerX().toInt(), rects[i].centerY().toInt())
        assertThat(centre(0)).isEqualTo(fill)
        assertThat(centre(1)).isEqualTo(fill)
        assertThat(centre(2)).isEqualTo(Color.TRANSPARENT)
        assertThat(centre(3)).isEqualTo(Color.TRANSPARENT)
        // The gap between two cells is clear, and each cell has a border along its edge.
        val gapX = ((rects[0].right + rects[1].left) / 2f).toInt()
        assertThat(bitmap.getPixel(gapX, 10)).isEqualTo(Color.TRANSPARENT)
        assertThat(bitmap.getPixel(rects[3].centerX().toInt(), 0)).isEqualTo(border)
    }

    @Test fun bitmap_withNoFilledCellsHasOnlyBorders() {
        val bitmap = renderSegmentedBar(100, 20, filled = 0, cells = 4, fill = Color.RED, border = Color.BLUE, gapPx = 4f, strokePx = 2f)
        val rects = segmentRects(100, 20, 4, 4f)
        assertThat(bitmap.getPixel(rects[0].centerX().toInt(), rects[0].centerY().toInt())).isEqualTo(Color.TRANSPARENT)
    }
}
