package io.github.aceattacker77.nakedmusicplayer.ui.playlists

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.data.playlists.PlaylistSummary
import io.github.aceattacker77.nakedmusicplayer.data.playlists.SmartPlaylist
import io.github.aceattacker77.nakedmusicplayer.ui.library.SectionTitle

@Composable
fun smartPlaylistTitle(kind: SmartPlaylist): String = stringResource(
    when (kind) {
        SmartPlaylist.RECENTLY_ADDED -> R.string.smart_recently_added
        SmartPlaylist.MOST_PLAYED -> R.string.smart_most_played
        SmartPlaylist.RECENTLY_PLAYED -> R.string.smart_recently_played
    },
)

/** Smart playlists first, then the user's own, with create / rename / delete. */
@Composable
fun PlaylistsScreen(
    viewModel: PlaylistsViewModel,
    onOpenPlaylist: (Long) -> Unit,
    onOpenSmart: (SmartPlaylist) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    var creating by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<PlaylistSummary?>(null) }
    var deleting by remember { mutableStateOf<PlaylistSummary?>(null) }

    LazyColumn(modifier.testTag("playlists-list")) {
        items(SmartPlaylist.entries, key = { "smart-${it.name}" }) { kind ->
            ListItem(
                headlineContent = { Text(smartPlaylistTitle(kind)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_library_music), contentDescription = null) },
                modifier = Modifier.clickable { onOpenSmart(kind) }.testTag("smart-${kind.name}"),
            )
        }
        item(key = "your-title") { SectionTitle(stringResource(R.string.your_playlists)) }
        item(key = "new") {
            ListItem(
                headlineContent = { Text(stringResource(R.string.new_playlist)) },
                leadingContent = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                modifier = Modifier.clickable { creating = true },
            )
        }
        items(playlists, key = { it.id }) { playlist ->
            PlaylistRow(
                playlist = playlist,
                onClick = { onOpenPlaylist(playlist.id) },
                onRename = { renaming = playlist },
                onDelete = { deleting = playlist },
            )
        }
    }

    if (creating) {
        PlaylistNameDialog(
            title = stringResource(R.string.new_playlist),
            confirmLabel = stringResource(R.string.create),
            initialName = "",
            onConfirm = {
                viewModel.create(it)
                creating = false
            },
            onDismiss = { creating = false },
        )
    }
    renaming?.let { playlist ->
        PlaylistNameDialog(
            title = stringResource(R.string.rename),
            confirmLabel = stringResource(R.string.save),
            initialName = playlist.name,
            onConfirm = {
                viewModel.rename(playlist.id, it)
                renaming = null
            },
            onDismiss = { renaming = null },
        )
    }
    deleting?.let { playlist ->
        DeletePlaylistDialog(
            name = playlist.name,
            onConfirm = {
                viewModel.delete(playlist.id)
                deleting = null
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun PlaylistRow(
    playlist: PlaylistSummary,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(playlist.name) },
        supportingContent = { Text(pluralStringResource(R.plurals.songs_count, playlist.songCount, playlist.songCount)) },
        leadingContent = { Icon(painterResource(R.drawable.ic_playlist_play), contentDescription = null) },
        trailingContent = {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(painterResource(R.drawable.ic_more_vert), contentDescription = stringResource(R.string.playlist_options))
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(text = { Text(stringResource(R.string.rename)) }, onClick = { menuOpen = false; onRename() })
                    DropdownMenuItem(text = { Text(stringResource(R.string.delete)) }, onClick = { menuOpen = false; onDelete() })
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick).testTag("playlist-${playlist.id}"),
    )
}
