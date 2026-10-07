package io.github.aceattacker77.nakedmusicplayer.ui.equalizer

import java.util.Locale
import kotlin.math.roundToInt

/**
 * How many of [cells] a band at [levelMb] fills, bottom up, across the device-reported millibel [range]. The range is
 * whatever the device says (never assumed to be ±12 dB); an empty or zero-width range fills nothing.
 */
internal fun bandCellsFilled(levelMb: Int, range: IntRange, cells: Int): Int {
    if (range.isEmpty() || range.first == range.last) return 0
    val fraction = (levelMb - range.first).toFloat() / (range.last - range.first)
    return (fraction * cells).roundToInt().coerceIn(0, cells)
}

/** A level in dB with one decimal and its sign kept (`-3.0`, `0.0`); never `-0.0`. */
internal fun readoutText(db: Float): String {
    val text = String.format(Locale.ROOT, "%.1f", db)
    return if (text == "-0.0") "0.0" else text
}

/** The channel-count code of the Bands panel: `05 CH`. */
internal fun bandsCode(count: Int): String = String.format(Locale.ROOT, "%02d CH", count)
