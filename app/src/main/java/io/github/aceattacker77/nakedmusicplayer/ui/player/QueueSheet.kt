package io.github.aceattacker77.nakedmusicplayer.ui.player

import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.components.unplayableDecoration
import io.github.aceattacker77.nakedmusicplayer.ui.components.UnplayableMarker
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.playback.UnplayableRegistry
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/** A queue entry plus a key that stays the same while it is dragged around, even for duplicate songs. */
private data class QueueEntry(val key: String, val item: MediaItem)

private fun keyed(queue: List<MediaItem>): List<QueueEntry> {
    val seen = HashMap<String, Int>()
    return queue.map { item ->
        val n = seen.merge(item.mediaId, 1, Int::plus)!!
        QueueEntry("${item.mediaId}#$n", item)
    }
}

/**
 * The play queue as a bottom sheet: drag the handle to reorder, swipe a row away to remove it, tap
 * to jump to it. Edits are applied to the local list immediately so dragging stays smooth while the
 * player's confirmation is still on its way.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    state: PlayerUiState,
    onMove: (from: Int, to: Int) -> Unit,
    onRemove: (index: Int) -> Unit,
    onPlayIndex: (index: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        var entries by remember(state.queue) { mutableStateOf(keyed(state.queue)) }
        val unplayable by UnplayableRegistry.ids.collectAsStateWithLifecycle()
        val listState = rememberLazyListState()
        val reorderState = rememberReorderableLazyListState(listState) { from, to ->
            entries = entries.toMutableList().apply { add(to.index, removeAt(from.index)) }
            onMove(from.index, to.index)
        }

        Text(
            text = stringResource(R.string.queue),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().testTag("queue-list"),
        ) {
            itemsIndexed(entries, key = { _, entry -> entry.key }) { index, entry ->
                ReorderableItem(reorderState, key = entry.key) { _ ->
                    val removeEntry = {
                        entries = entries.toMutableList().apply { removeAt(index) }
                        onRemove(index)
                    }
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                removeEntry()
                                true
                            } else {
                                false
                            }
                        },
                    )
                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        backgroundContent = {
                            Box(
                                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.errorContainer).padding(horizontal = 24.dp),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    painterResource(R.drawable.ic_delete),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                )
                            }
                        },
                    ) {
                        QueueRow(
                            entry = entry,
                            index = index,
                            isCurrent = index == state.currentIndex,
                            unplayable = entry.item.mediaId in unplayable,
                            onClick = { onPlayIndex(index) },
                            dragHandle = Modifier.draggableHandle(),
                            accessibilityMoveUp = if (index > 0) ({ onMove(index, index - 1) }) else null,
                            accessibilityMoveDown = if (index < entries.lastIndex) ({ onMove(index, index + 1) }) else null,
                            accessibilityRemove = removeEntry,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueRow(
    entry: QueueEntry,
    index: Int,
    isCurrent: Boolean,
    unplayable: Boolean,
    onClick: () -> Unit,
    dragHandle: Modifier,
    accessibilityMoveUp: (() -> Unit)?,
    accessibilityMoveDown: (() -> Unit)?,
    accessibilityRemove: () -> Unit,
) {
    val meta = entry.item.mediaMetadata
    val moveUp = stringResource(R.string.queue_move_up)
    val moveDown = stringResource(R.string.queue_move_down)
    val remove = stringResource(R.string.queue_remove)
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("queue-row-$index")
                .semantics {
                    // Drag and swipe have keyboard / screen-reader equivalents.
                    customActions = buildList {
                        accessibilityMoveUp?.let { add(CustomAccessibilityAction(moveUp) { it(); true }) }
                        accessibilityMoveDown?.let { add(CustomAccessibilityAction(moveDown) { it(); true }) }
                        add(CustomAccessibilityAction(remove) { accessibilityRemove(); true })
                    }
                }
                .clickable(onClick = onClick)
                .padding(start = 24.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) {
                if (isCurrent) {
                    Icon(
                        painterResource(R.drawable.ic_play_arrow),
                        contentDescription = stringResource(R.string.now_playing),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Box(Modifier.weight(1f)) {
                androidx.compose.foundation.layout.Column {
                    Text(
                        text = meta.title?.toString().orEmpty(),
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = unplayableDecoration(unplayable, LocalOrnament.current.statusTags),
                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = artistLabel(meta.artist?.toString().orEmpty()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (unplayable) UnplayableMarker()
            Box(
                modifier = Modifier.size(48.dp).testTag("queue-drag-$index").then(dragHandle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painterResource(R.drawable.ic_drag_handle),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
