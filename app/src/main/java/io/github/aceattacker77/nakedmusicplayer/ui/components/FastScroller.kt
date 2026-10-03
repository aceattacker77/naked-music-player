package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/** An A–Z strip along the list's edge: tap or drag to jump to the first item for a letter. */
@Composable
fun FastScroller(
    lazyListState: LazyListState,
    letters: List<Char>,
    indexOfLetter: (Char) -> Int,
    modifier: Modifier = Modifier,
) {
    if (letters.size < 2) return
    var heightPx by remember { mutableIntStateOf(1) }
    val scope = rememberCoroutineScope()

    fun jumpTo(y: Float) {
        val bucket = (y / heightPx * letters.size).toInt().coerceIn(0, letters.lastIndex)
        scope.launch { lazyListState.scrollToItem(indexOfLetter(letters[bucket])) }
    }

    // Every letter gets an equal slot of the strip, so a letter's drawn position and the touch-to-letter
    // formula in jumpTo() always agree, however many letters there are.
    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) { detectTapGestures { jumpTo(it.y) } }
            .pointerInput(letters) { detectVerticalDragGestures { change, _ -> jumpTo(change.position.y) } },
    ) {
        val style = MaterialTheme.typography.labelSmall
        letters.forEach { letter ->
            Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = letter.toString(),
                    // Line height = font size, so a crowded strip's letters stay inside their slots.
                    style = style.copy(lineHeight = style.fontSize),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}
