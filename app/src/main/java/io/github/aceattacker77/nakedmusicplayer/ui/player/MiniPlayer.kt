package io.github.aceattacker77.nakedmusicplayer.ui.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArt
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import kotlinx.coroutines.flow.Flow

private const val EXPAND_FLING_VELOCITY = -400f

/**
 * Bar above the navigation bar showing the current track. Tap or swipe up to expand. The position
 * flow is collected only while this composable is on screen, so a hidden mini player costs nothing.
 */
@Composable
fun MiniPlayer(
    state: PlayerUiState,
    positionMs: Flow<Long>,
    onExpand: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    artworkModifier: Modifier = Modifier,
) {
    val position by positionMs.collectAsStateWithLifecycle(initialValue = 0L)
    val meta = state.current?.mediaMetadata
    val albumId = remember(state.current) { state.current?.mediaMetadata?.artworkUri?.lastPathSegment?.toLongOrNull() }
    val duration = state.durationMs.coerceAtLeast(1L)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("mini-player")
            .draggable(
                state = rememberDraggableState {},
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> if (velocity < EXPAND_FLING_VELOCITY) onExpand() },
            )
            .clickable(onClick = onExpand),
        tonalElevation = 3.dp,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column {
            LinearProgressIndicator(
                progress = { (position.toFloat() / duration).coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxWidth(),
            )
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AlbumArt(albumId, Modifier.size(44.dp).then(artworkModifier))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = meta?.title?.toString().orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artistLabel(meta?.artist?.toString().orEmpty()),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(onClick = onPlayPause) {
                    Icon(
                        painter = painterResource(if (state.isPlaying) R.drawable.ic_pause else R.drawable.ic_play_arrow),
                        contentDescription = stringResource(if (state.isPlaying) R.string.pause else R.string.play),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(painterResource(R.drawable.ic_skip_next), contentDescription = stringResource(R.string.next))
                }
            }
        }
    }
}
