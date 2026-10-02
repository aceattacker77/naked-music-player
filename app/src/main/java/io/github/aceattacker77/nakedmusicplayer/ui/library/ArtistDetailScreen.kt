package io.github.aceattacker77.nakedmusicplayer.ui.library

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.library.sortedWith
import io.github.aceattacker77.nakedmusicplayer.playback.UnplayableRegistry
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArt
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongRow
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.indexLetter

/** An artist's detail: a row of their albums, then all their songs. Route or Artists-tab detail pane. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistDetailPane(
    artistId: Long,
    viewModel: LibraryViewModel,
    showBack: Boolean,
    onBack: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAddToPlaylist: (List<Song>) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val artist = artists.firstOrNull { it.id == artistId }
    Column(modifier.fillMaxSize()) {
        if (showBack) {
            TopAppBar(
                title = { Text(artist?.let { artistLabel(it.name) }.orEmpty(), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
                    }
                },
                windowInsets = WindowInsets(0),
            )
        }
        if (artist == null) {
            EmptyState(
                title = stringResource(R.string.no_music_found),
                message = stringResource(R.string.no_music_tips),
                action = null,
            )
        } else {
            val currentId by viewModel.currentMediaId.collectAsStateWithLifecycle()
            val unplayable by UnplayableRegistry.ids.collectAsStateWithLifecycle()
            ArtistDetailContent(
                artist = artist,
                currentMediaId = currentId,
                unplayable = unplayable,
                onPlay = viewModel::play,
                onAlbumClick = onAlbumClick,
                onSongLongClick = onSongLongClick,
                onAddToPlaylist = { onAddToPlaylist(artist.songs.sortedWith(SongSort.ARTIST)) },
            )
        }
    }
}

@Composable
private fun ArtistDetailContent(
    artist: Artist,
    currentMediaId: String?,
    unplayable: Set<String>,
    onPlay: (List<Song>, Int) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onSongLongClick: (Song) -> Unit,
    onAddToPlaylist: () -> Unit,
) {
    val songs = remember(artist) { artist.songs.sortedWith(SongSort.ARTIST) }
    LazyColumn(Modifier.fillMaxSize().testTag("artist-detail")) {
        item(key = "header") { ArtistHeader(artist, onPlayAll = { onPlay(songs, 0) }, onAddToPlaylist = onAddToPlaylist) }
        item(key = "albums-title") { SectionTitle(stringResource(R.string.library_albums)) }
        item(key = "albums") {
            LazyRow(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(artist.albums, key = { it.id }) { album ->
                    Column(
                        modifier = Modifier.width(140.dp).testTag("album-${album.id}").clickable { onAlbumClick(album) },
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        AlbumArt(album.id, Modifier.size(140.dp))
                        Text(albumLabel(album.title), style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
        item(key = "songs-title") { SectionTitle(stringResource(R.string.library_songs)) }
        itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "song" }) { index, song ->
            SongRow(
                song = song,
                isCurrent = currentMediaId == "song:${song.id}",
                unplayable = "song:${song.id}" in unplayable,
                onClick = { onPlay(songs, index) },
                onLongClick = { onSongLongClick(song) },
            )
        }
    }
}

@Composable
private fun ArtistHeader(artist: Artist, onPlayAll: () -> Unit, onAddToPlaylist: () -> Unit) {
    val name = artistLabel(artist.name)
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier.size(96.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = indexLetter(name).toString(),
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Text(name, style = MaterialTheme.typography.headlineSmall)
        val albums = pluralStringResource(R.plurals.albums_count, artist.albums.size, artist.albums.size)
        val songs = pluralStringResource(R.plurals.songs_count, artist.songs.size, artist.songs.size)
        Text("$albums · $songs", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onPlayAll) { Text(stringResource(R.string.play)) }
            OutlinedButton(onClick = onAddToPlaylist) { Text(stringResource(R.string.add_to_playlist)) }
        }
    }
}

@Composable
internal fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
    )
}
