package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.model.Song

/** Long-press menu for a song: Play next, Add to queue, Add to playlist, Go to album, Go to artist. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenu(
    song: Song,
    onPlayNext: () -> Unit,
    onAddToQueue: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onGoToAlbum: () -> Unit,
    onGoToArtist: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(Modifier.navigationBarsPadding()) {
            ListItem(
                headlineContent = { Text(song.title) },
                supportingContent = { Text("${artistLabel(song.artist)} · ${albumLabel(song.album)}") },
                colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
            )
            MenuEntry(R.drawable.ic_skip_next, R.string.menu_play_next) { onPlayNext(); onDismiss() }
            MenuEntry(R.drawable.ic_queue_music, R.string.menu_add_to_queue) { onAddToQueue(); onDismiss() }
            MenuEntry(R.drawable.ic_add, R.string.menu_add_to_playlist) { onAddToPlaylist(); onDismiss() }
            MenuEntry(R.drawable.ic_album, R.string.menu_go_to_album) { onDismiss(); onGoToAlbum() }
            MenuEntry(R.drawable.ic_person, R.string.menu_go_to_artist) { onDismiss(); onGoToArtist() }
        }
    }
}

@Composable
private fun MenuEntry(icon: Int, label: Int, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(stringResource(label)) },
        leadingContent = { Icon(painterResource(icon), contentDescription = null) },
        colors = ListItemDefaults.colors(containerColor = androidx.compose.ui.graphics.Color.Transparent),
        modifier = Modifier.clickable(onClick = onClick),
    )
}
