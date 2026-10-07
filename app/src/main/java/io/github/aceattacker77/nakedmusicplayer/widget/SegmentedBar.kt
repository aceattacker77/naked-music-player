package io.github.aceattacker77.nakedmusicplayer.widget

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import io.github.aceattacker77.nakedmusicplayer.ui.player.segmentCountFor
import kotlin.math.floor

/** The progress as a whole percentage, rounded down like the filled cells; out-of-range and NaN inputs are clamped. */
internal fun progressPercent(progress: Float): Int =
    if (progress.isNaN()) 0 else floor(progress.coerceIn(0f, 1f) * 100f).toInt()

/**
 * The cells of a segmented bar: equal widths separated by [gapPx], spanning [widthPx]. Fewer than [cells] are used when
 * each would be narrower than [minCellPx]; none when the bar has no room.
 */
internal fun segmentRects(widthPx: Int, heightPx: Int, cells: Int, gapPx: Float, minCellPx: Float = 1f): List<RectF> {
    if (heightPx <= 0) return emptyList()
    val count = segmentCountFor(cells, widthPx.toFloat(), gapPx, minCellPx)
    if (count == 0) return emptyList()
    val cellWidth = (widthPx - gapPx * (count - 1)) / count
    return List(count) { i ->
        val left = i * (cellWidth + gapPx)
        RectF(left, 0f, left + cellWidth, heightPx.toFloat())
    }
}

/**
 * Draws a segmented progress bar to a bitmap for the widget (RemoteViews cannot draw custom shapes): the first [filled]
 * cells in [fill], every cell outlined in [border]. Drawn only when the track or progress changes, not on a timer.
 */
internal fun renderSegmentedBar(
    widthPx: Int,
    heightPx: Int,
    filled: Int,
    cells: Int,
    fill: Int,
    border: Int,
    gapPx: Float,
    strokePx: Float,
): Bitmap {
    val bitmap = Bitmap.createBitmap(widthPx.coerceAtLeast(1), heightPx.coerceAtLeast(1), Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val fillPaint = Paint().apply { color = fill; style = Paint.Style.FILL }
    val borderPaint = Paint().apply { color = border; style = Paint.Style.STROKE; strokeWidth = strokePx }
    val half = strokePx / 2f
    segmentRects(widthPx, heightPx, cells, gapPx).forEachIndexed { index, rect ->
        if (index < filled) canvas.drawRect(rect, fillPaint)
        canvas.drawRect(rect.left + half, rect.top + half, rect.right - half, rect.bottom - half, borderPaint)
    }
    return bitmap
}
