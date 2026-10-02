package io.github.aceattacker77.nakedmusicplayer.ui.playlists

import androidx.compose.foundation.background
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.playlists.PlaylistDetail
import io.github.aceattacker77.nakedmusicplayer.data.playlists.SmartPlaylist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.playback.UnplayableRegistry
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArt
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongRow
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import io.github.aceattacker77.nakedmusicplayer.ui.library.LibraryViewModel
import kotlinx.coroutines.launch
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

/**
 * One playlist: drag the handle to reorder, swipe a row away to remove it (with Undo), tap to play
 * from that song. Songs missing from storage are hidden by the repository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailScreen(
    playlistId: Long,
    playlistsViewModel: PlaylistsViewModel,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val detail by remember(playlistId) { playlistsViewModel.detail(playlistId) }.collectAsStateWithLifecycle(initialValue = null)
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pendingExport by remember { mutableStateOf<String?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/x-mpegurl")) { uri ->
        val text = pendingExport
        pendingExport = null
        if (uri != null && text != null) {
            scope.launch {
                if (!writeM3uText(context, uri, text)) {
                    Toast.makeText(context, context.getString(R.string.m3u_export_failed), Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    val removedMessage = stringResource(R.string.removed_from_playlist, "%s")
    val undoLabel = stringResource(R.string.undo)

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(detail?.name.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    TextButton(
                        onClick = {
                            scope.launch {
                                val export = playlistsViewModel.exportM3u(playlistId) ?: return@launch
                                pendingExport = export.text
                                exportLauncher.launch(export.fileName)
                            }
                        },
                        enabled = detail != null,
                    ) { Text(stringResource(R.string.export_playlist)) }
                },
                windowInsets = WindowInsets(0),
            )
            val current = detail
            if (current == null || current.songs.isEmpty()) {
                EmptyState(
                    title = current?.name.orEmpty(),
                    message = stringResource(R.string.playlist_empty),
                    action = null,
                )
            } else {
                PlaylistSongs(
                    detail = current,
                    libraryViewModel = libraryViewModel,
                    onMove = { from, to -> playlistsViewModel.move(playlistId, from, to) },
                    onRemove = { position ->
                        playlistsViewModel.remove(playlistId, position) { removed ->
                            scope.launch {
                                val result = snackbar.showSnackbar(
                                    message = removedMessage.replace("%s", removed.song.title),
                                    actionLabel = undoLabel,
                                    withDismissAction = false,
                                )
                                if (result == SnackbarResult.ActionPerformed) playlistsViewModel.undoRemove(playlistId, removed)
                            }
                        }
                    },
                    onSongLongClick = onSongLongClick,
                )
            }
        }
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
private fun PlaylistSongs(
    detail: PlaylistDetail,
    libraryViewModel: LibraryViewModel,
    onMove: (fromPosition: Int, toPosition: Int) -> Unit,
    onRemove: (position: Int) -> Unit,
    onSongLongClick: (Song) -> Unit,
) {
    // Edits show immediately; the stored result arrives with the next emission and replaces this copy.
    var songs by remember(detail.songs) { mutableStateOf(detail.songs.map { it.song }) }
    // Visible slot k always corresponds to the k-th stored position of the latest snapshot.
    val slotPositions = remember(detail.songs) { detail.songs.map { it.position } }
    val currentId by libraryViewModel.currentMediaId.collectAsStateWithLifecycle()
    val unplayable by UnplayableRegistry.ids.collectAsStateWithLifecycle()

    // The same song may appear twice in a playlist, so keys carry an occurrence counter.
    val keys = remember(songs) {
        val seen = HashMap<Long, Int>()
        songs.map { "${it.id}#${seen.merge(it.id, 1, Int::plus)}" }
    }

    val listState = rememberLazyListState()
    val reorderState = rememberReorderableLazyListState(listState) { from, to ->
        songs = songs.toMutableList().apply { add(to.index, removeAt(from.index)) }
        onMove(slotPositions[from.index], slotPositions[to.index])
    }

    Column(Modifier.fillMaxSize()) {
        Button(
            onClick = { libraryViewModel.play(songs, 0) },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        ) { Text(stringResource(R.string.play)) }

        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("playlist-detail")) {
            itemsIndexed(songs, key = { index, _ -> keys[index] }) { index, song ->
                val key = keys[index]
                ReorderableItem(reorderState, key = key) { _ ->
                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { value ->
                            if (value == SwipeToDismissBoxValue.EndToStart) {
                                val position = slotPositions.getOrNull(index)
                                if (position != null) {
                                    songs = songs.toMutableList().apply { removeAt(index) }
                                    onRemove(position)
                                }
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
                                Icon(painterResource(R.drawable.ic_delete), contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            }
                        },
                    ) {
                        PlaylistSongRow(
                            song = song,
                            position = slotPositions.getOrNull(index) ?: index,
                            isCurrent = currentId == "song:${song.id}",
                            unplayable = "song:${song.id}" in unplayable,
                            onClick = { libraryViewModel.play(songs, index) },
                            onLongClick = { onSongLongClick(song) },
                            dragHandle = Modifier.draggableHandle(),
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistSongRow(
    song: Song,
    position: Int,
    isCurrent: Boolean,
    unplayable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    dragHandle: Modifier,
) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("playlist-row-$position")
                .combinedClickable(onClick = onClick, onLongClick = onLongClick)
                .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AlbumArt(song.albumId, Modifier.size(48.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = song.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "${artistLabel(song.artist)} · ${albumLabel(song.album)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (unplayable) {
                Icon(
                    painter = painterResource(R.drawable.ic_error_outline),
                    contentDescription = stringResource(R.string.cant_play),
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
            }
            Box(
                modifier = Modifier.size(48.dp).testTag("playlist-drag-$position").then(dragHandle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(R.drawable.ic_drag_handle), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A read-only list of songs (the three smart playlists). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartPlaylistDetailScreen(
    kind: SmartPlaylist,
    playlistsViewModel: PlaylistsViewModel,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val songs by remember(kind) { playlistsViewModel.smart(kind) }.collectAsStateWithLifecycle(initialValue = emptyList())
    val currentId by libraryViewModel.currentMediaId.collectAsStateWithLifecycle()
    val unplayable by UnplayableRegistry.ids.collectAsStateWithLifecycle()
    Column(modifier.fillMaxSize().testTag("smart-playlist-detail")) {
        TopAppBar(
            title = { Text(smartPlaylistTitle(kind)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                }
            },
            windowInsets = WindowInsets(0),
        )
        if (songs.isEmpty()) {
            EmptyState(title = smartPlaylistTitle(kind), message = stringResource(R.string.smart_empty), action = null)
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = currentId == "song:${song.id}",
                        unplayable = "song:${song.id}" in unplayable,
                        onClick = { libraryViewModel.play(songs, index) },
                        onLongClick = { onSongLongClick(song) },
                    )
                }
            }
        }
    }
}
