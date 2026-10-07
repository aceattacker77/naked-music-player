package io.github.aceattacker77.nakedmusicplayer.ui.library

import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.components.unplayableDecoration
import io.github.aceattacker77.nakedmusicplayer.ui.components.UnplayableMarker
import io.github.aceattacker77.nakedmusicplayer.ui.theme.shapeOr
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.playback.UnplayableRegistry
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArt
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.formatDuration

/**
 * An album's detail, used both as a full-screen route and as the detail pane of the Albums tab.
 * [showBack] is true whenever the detail fills the screen on its own.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumDetailPane(
    albumId: Long,
    viewModel: LibraryViewModel,
    showBack: Boolean,
    onBack: () -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val album = albums.firstOrNull { it.id == albumId }
    Column(modifier.fillMaxSize()) {
        if (showBack) {
            TopAppBar(
                title = { Text(album?.let { albumLabel(it.title) }.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                // The enclosing scaffold already pads for the status bar.
                windowInsets = WindowInsets(0),
            )
        }
        if (album == null) {
            // The album vanished (files deleted) while open.
            EmptyState(
                title = stringResource(R.string.no_music_found),
                message = stringResource(R.string.no_music_tips),
                action = null,
            )
        } else {
            val currentId by viewModel.currentMediaId.collectAsStateWithLifecycle()
            val unplayable by UnplayableRegistry.ids.collectAsStateWithLifecycle()
            AlbumDetailContent(
                album = album,
                currentMediaId = currentId,
                unplayable = unplayable,
                onPlay = viewModel::play,
                onSongLongClick = onSongLongClick,
                onAddToPlaylist = { onAddToPlaylist(album.songs) },
            )
        }
    }
}

@Composable
private fun AlbumDetailContent(
    album: Album,
    currentMediaId: String?,
    unplayable: Set<String>,
    onPlay: (List<Song>, Int) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val discs = album.songs.groupBy { it.discNumber }
    LazyColumn(Modifier.fillMaxSize().testTag("album-detail")) {
        item(key = "header") { AlbumHeader(album, onPlayAll = { onPlay(album.songs, 0) }, onAddToPlaylist = onAddToPlaylist) }
        discs.forEach { (disc, songs) ->
            if (discs.size > 1) {
                item(key = "disc-$disc") {
                    Text(
                        text = stringResource(R.string.disc_number, disc),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
            }
            items(songs, key = { it.id }, contentType = { "track" }) { song ->
                TrackRow(
                    song = song,
                    isCurrent = currentMediaId == "song:${song.id}",
                    unplayable = "song:${song.id}" in unplayable,
                    onClick = { onPlay(album.songs, album.songs.indexOf(song)) },
                    onLongClick = { onSongLongClick(song) },
                )
            }
        }
    }
}

@Composable
private fun AlbumHeader(album: Album, onPlayAll: () -> Unit, onAddToPlaylist: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AlbumArt(album.id, Modifier.size(200.dp), shapeOr(RoundedCornerShape(16.dp), MaterialTheme.shapes.large))
        Text(
            text = albumLabel(album.title),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
        Text(
            text = artistLabel(album.artist),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        val count = pluralStringResource(R.plurals.songs_count, album.songs.size, album.songs.size)
        val total = formatDuration(album.songs.sumOf { it.durationMs })
        val details = listOfNotNull(album.year?.toString(), count, total).joinToString(" · ")
        Text(details, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlayAll) { Text(stringResource(R.string.play)) }
            OutlinedButton(onClick = onAddToPlaylist) { Text(stringResource(R.string.add_to_playlist)) }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackRow(
    song: Song,
    isCurrent: Boolean,
    unplayable: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val accent = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("song-${song.id}")
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = if (song.trackNumber > 0) song.trackNumber.toString() else "–",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
            modifier = Modifier.width(28.dp),
        )
        Text(
            text = song.title,
            style = MaterialTheme.typography.bodyLarge,
            color = accent,
            textDecoration = unplayableDecoration(unplayable, LocalOrnament.current.statusTags),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (unplayable) UnplayableMarker()
        Text(
            text = formatDuration(song.durationMs),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
