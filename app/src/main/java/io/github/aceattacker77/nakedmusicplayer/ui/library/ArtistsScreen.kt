package io.github.aceattacker77.nakedmusicplayer.ui.library

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.layout.AnimatedPane
import androidx.compose.material3.adaptive.layout.ListDetailPaneScaffoldRole
import androidx.compose.material3.adaptive.layout.PaneAdaptedValue
import androidx.compose.material3.adaptive.navigation.NavigableListDetailPaneScaffold
import androidx.compose.material3.adaptive.navigation.rememberListDetailPaneScaffoldNavigator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Artist
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.indexLetter
import kotlinx.coroutines.launch

/** The Artists tab; list-detail like [AlbumsScreen]. */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun ArtistsScreen(
    viewModel: LibraryViewModel,
    onAlbumClick: (Album) -> Unit,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artists by viewModel.artists.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = rememberCoroutineScope()

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                LazyColumn(Modifier.fillMaxSize().testTag("artist-list")) {
                    items(artists, key = { it.id }, contentType = { "artist" }) { artist ->
                        ArtistRow(artist, onClick = {
                            scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, artist.id) }
                        })
                    }
                }
            }
        },
        detailPane = {
            AnimatedPane {
                val artistId = navigator.currentDestination?.contentKey
                if (artistId == null) {
                    SelectPrompt(stringResource(R.string.select_artist))
                } else {
                    ArtistDetailPane(
                        artistId = artistId,
                        viewModel = viewModel,
                        showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                        onBack = { scope.launch { navigator.navigateBack() } },
                        onAlbumClick = onAlbumClick,
                        onSongLongClick = onSongLongClick,
                    )
                }
            }
        },
    )
}

@Composable
fun ArtistRow(artist: Artist, onClick: () -> Unit) {
    val name = artistLabel(artist.name)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("artist-${artist.id}")
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = indexLetter(name).toString(),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val albums = pluralStringResource(R.plurals.albums_count, artist.albums.size, artist.albums.size)
            val songs = pluralStringResource(R.plurals.songs_count, artist.songs.size, artist.songs.size)
            Text(
                text = "$albums · $songs",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
