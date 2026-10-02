package io.github.aceattacker77.nakedmusicplayer.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.playback.UnplayableRegistry
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArt
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongRow
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import io.github.aceattacker77.nakedmusicplayer.ui.library.ArtistRow
import io.github.aceattacker77.nakedmusicplayer.ui.library.LibraryViewModel
import io.github.aceattacker77.nakedmusicplayer.ui.library.SectionTitle

@Composable
fun SearchScreen(
    searchViewModel: SearchViewModel,
    libraryViewModel: LibraryViewModel,
    onBack: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val query by searchViewModel.query.collectAsStateWithLifecycle()
    val results by searchViewModel.results.collectAsStateWithLifecycle()
    val currentId by libraryViewModel.currentMediaId.collectAsStateWithLifecycle()
    val unplayable by UnplayableRegistry.ids.collectAsStateWithLifecycle()
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    Column(modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onBack) {
                Icon(painterResource(R.drawable.ic_arrow_back), contentDescription = stringResource(R.string.back))
            }
            OutlinedTextField(
                value = query,
                onValueChange = searchViewModel::setQuery,
                singleLine = true,
                placeholder = { Text(stringResource(R.string.search_hint)) },
                modifier = Modifier.weight(1f).focusRequester(focus).testTag("search-field"),
            )
        }

        val hasResults = results.songs.isNotEmpty() || results.albums.isNotEmpty() || results.artists.isNotEmpty()
        when {
            query.isBlank() -> Hint(stringResource(R.string.search_hint))
            !hasResults -> Hint(stringResource(R.string.no_results, query.trim()))
            else -> LazyColumn(Modifier.fillMaxSize().testTag("search-results")) {
                if (results.artists.isNotEmpty()) {
                    item(key = "artists-title") { SectionTitle(stringResource(R.string.library_artists)) }
                    items(results.artists, key = { "artist-${it.id}" }) { artist ->
                        ArtistRow(artist, onClick = { onArtistClick(artist) })
                    }
                }
                if (results.albums.isNotEmpty()) {
                    item(key = "albums-title") { SectionTitle(stringResource(R.string.library_albums)) }
                    items(results.albums, key = { "album-${it.id}" }) { album ->
                        AlbumResultRow(album, onClick = { onAlbumClick(album) })
                    }
                }
                if (results.songs.isNotEmpty()) {
                    item(key = "songs-title") { SectionTitle(stringResource(R.string.library_songs)) }
                    itemsIndexed(results.songs, key = { _, song -> "song-${song.id}" }) { index, song ->
                        SongRow(
                            song = song,
                            isCurrent = currentId == "song:${song.id}",
                            unplayable = "song:${song.id}" in unplayable,
                            onClick = { libraryViewModel.play(results.songs, index) },
                            onLongClick = { onSongLongClick(song) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String) {
    Box(Modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.TopCenter) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun AlbumResultRow(album: Album, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("album-${album.id}")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AlbumArt(album.id, Modifier.size(48.dp))
        Column(Modifier.weight(1f)) {
            Text(albumLabel(album.title), style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                artistLabel(album.artist),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
