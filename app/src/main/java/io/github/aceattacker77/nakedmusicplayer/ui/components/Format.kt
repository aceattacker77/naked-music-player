package io.github.aceattacker77.nakedmusicplayer.ui.components

import java.text.Normalizer
import java.util.Locale

/** `m:ss`, or `h:mm:ss` from one hour up. Negative (unknown) durations show as `0:00`. */
fun formatDuration(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** The fast-scroller bucket for [text]: its first letter uppercased and without accents, or `#`. */
fun indexLetter(text: String): Char {
    val first = text.trim().firstOrNull() ?: return '#'
    val base = Normalizer.normalize(first.toString(), Normalizer.Form.NFD).first()
    return if (base.isLetter()) base.uppercaseChar() else '#'
}
