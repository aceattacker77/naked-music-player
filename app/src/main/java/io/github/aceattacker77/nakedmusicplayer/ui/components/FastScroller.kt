package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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

    Column(
        modifier = modifier
            .fillMaxHeight()
            .width(24.dp)
            .onSizeChanged { heightPx = it.height.coerceAtLeast(1) }
            .pointerInput(letters) { detectTapGestures { jumpTo(it.y) } }
            .pointerInput(letters) { detectVerticalDragGestures { change, _ -> jumpTo(change.position.y) } },
        verticalArrangement = Arrangement.SpaceEvenly,
    ) {
        letters.forEach { letter ->
            Text(
                text = letter.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterHorizontally),
            )
        }
    }
}
