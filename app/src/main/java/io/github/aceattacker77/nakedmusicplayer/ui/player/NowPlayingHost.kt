package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.lifecycle.compose.collectAsStateWithLifecycle

private const val COLLAPSE_FLING_VELOCITY = 600f
private const val BACK_SHRINK = 0.08f
private const val BACK_FADE = 0.25f

/**
 * Connects [NowPlayingScreen] to a [PlayerConnection]: live state and position, the queue sheet,
 * swipe-down to collapse, and the shrink effect while a predictive back gesture is in progress.
 */
@Composable
fun NowPlayingHost(
    connection: PlayerConnection,
    sheet: PlayerSheetState,
    onAddToPlaylist: () -> Unit,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
) {
    val state by connection.state.collectAsStateWithLifecycle()
    val position by remember(connection) { connection.positionMs() }.collectAsStateWithLifecycle(initialValue = 0L)
    var showQueue by rememberSaveable { mutableStateOf(false) }

    val actions = remember(connection, sheet, onAddToPlaylist) {
        NowPlayingActions(
            onPlayPause = connection::togglePlayPause,
            onPrevious = connection::previous,
            onNext = connection::next,
            onSeek = connection::seekTo,
            onToggleShuffle = connection::toggleShuffle,
            onCycleRepeat = connection::cycleRepeat,
            onOpenQueue = { showQueue = true },
            onCollapse = sheet::collapse,
            onAddToPlaylist = onAddToPlaylist,
        )
    }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                val p = sheet.backProgress
                scaleX = 1f - BACK_SHRINK * p
                scaleY = 1f - BACK_SHRINK * p
                alpha = 1f - BACK_FADE * p
            }
            // Swallow taps so nothing underneath the full-screen player reacts to them.
            .pointerInput(Unit) { detectTapGestures { } }
            .draggable(
                state = rememberDraggableState {},
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> if (velocity > COLLAPSE_FLING_VELOCITY) sheet.collapse() },
            ),
        color = MaterialTheme.colorScheme.background,
    ) {
        NowPlayingScreen(state, position, actions, artworkModifier = artworkModifier)
    }

    if (showQueue) {
        QueueSheet(
            state = state,
            onMove = connection::moveQueueItem,
            onRemove = connection::removeQueueItem,
            onPlayIndex = connection::playQueueItem,
            onDismiss = { showQueue = false },
        )
    }
}
