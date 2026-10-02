package io.github.aceattacker77.nakedmusicplayer.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.model.Album
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.ui.components.AlbumArt
import io.github.aceattacker77.nakedmusicplayer.ui.components.albumLabel
import io.github.aceattacker77.nakedmusicplayer.ui.components.artistLabel
import kotlinx.coroutines.launch

/**
 * The Albums tab. On a phone the detail replaces the grid; on wide windows (tablet, foldable,
 * landscape) both panes show side by side.
 */
@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun AlbumsScreen(
    viewModel: LibraryViewModel,
    onSongLongClick: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val albums by viewModel.albums.collectAsStateWithLifecycle()
    val navigator = rememberListDetailPaneScaffoldNavigator<Long>()
    val scope = rememberCoroutineScope()

    NavigableListDetailPaneScaffold(
        navigator = navigator,
        modifier = modifier,
        listPane = {
            AnimatedPane {
                AlbumGrid(albums, onAlbumClick = { album ->
                    scope.launch { navigator.navigateTo(ListDetailPaneScaffoldRole.Detail, album.id) }
                })
            }
        },
        detailPane = {
            AnimatedPane {
                val albumId = navigator.currentDestination?.contentKey
                if (albumId == null) {
                    SelectPrompt(stringResource(R.string.select_album))
                } else {
                    AlbumDetailPane(
                        albumId = albumId,
                        viewModel = viewModel,
                        showBack = navigator.scaffoldValue[ListDetailPaneScaffoldRole.List] == PaneAdaptedValue.Hidden,
                        onBack = { scope.launch { navigator.navigateBack() } },
                        onSongLongClick = onSongLongClick,
                    )
                }
            }
        },
    )
}

@Composable
private fun AlbumGrid(albums: List<Album>, onAlbumClick: (Album) -> Unit) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(150.dp),
        modifier = Modifier.fillMaxSize().testTag("album-grid"),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        items(albums, key = { it.id }, contentType = { "album" }) { album ->
            AlbumCard(album, onClick = { onAlbumClick(album) })
        }
    }
}

@Composable
private fun AlbumCard(album: Album, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().testTag("album-${album.id}").clickable(onClick = onClick),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        AlbumArt(album.id, Modifier.fillMaxWidth().aspectRatio(1f))
        Text(
            text = albumLabel(album.title),
            style = MaterialTheme.typography.titleSmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 4.dp),
        )
        Text(
            text = artistLabel(album.artist),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Shown in the detail pane of a wide layout before anything is selected. */
@Composable
internal fun SelectPrompt(text: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
