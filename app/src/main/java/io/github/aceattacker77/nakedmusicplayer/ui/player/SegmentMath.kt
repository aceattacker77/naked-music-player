package io.github.aceattacker77.nakedmusicplayer.ui.player

import kotlin.math.floor

// Shared by the seek bar, the Equalizer meters and the home-screen widget.

/** How many of [segments] cells are filled at [fraction] (floored; out-of-range and NaN inputs are clamped). */
internal fun segmentsFilled(fraction: Float, segments: Int): Int =
    if (fraction.isNaN()) 0 else floor(fraction.coerceIn(0f, 1f) * segments).toInt().coerceIn(0, segments)

/** How many cells fit when each needs at least [minCellPx] plus a gap; never more than [segments], 0 when none fit. */
internal fun segmentCountFor(segments: Int, widthPx: Float, gapPx: Float, minCellPx: Float): Int {
    if (widthPx <= 0f) return 0
    return floor((widthPx + gapPx) / (minCellPx + gapPx)).toInt().coerceIn(0, segments)
}
