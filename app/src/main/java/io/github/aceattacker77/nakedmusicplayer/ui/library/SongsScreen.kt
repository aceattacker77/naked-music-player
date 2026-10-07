package io.github.aceattacker77.nakedmusicplayer.ui.library

import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinText
import io.github.aceattacker77.nakedmusicplayer.ui.theme.LocalOrnament
import io.github.aceattacker77.nakedmusicplayer.ui.components.themedButtonShape
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.material3.OutlinedButton
import io.github.aceattacker77.nakedmusicplayer.ui.theme.originalLabel
import io.github.aceattacker77.nakedmusicplayer.ui.theme.skinLabel
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.SongSort
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import io.github.aceattacker77.nakedmusicplayer.playback.UnplayableRegistry
import io.github.aceattacker77.nakedmusicplayer.ui.components.EmptyState
import io.github.aceattacker77.nakedmusicplayer.ui.components.FastScroller
import io.github.aceattacker77.nakedmusicplayer.ui.components.SongRow
import io.github.aceattacker77.nakedmusicplayer.ui.components.indexLetter

@Composable
fun SongsScreen(
    viewModel: LibraryViewModel,
    modifier: Modifier = Modifier,
    onSongLongClick: (Song) -> Unit = {},
    onAddFolder: (() -> Unit)? = null,
) {
    val songs by viewModel.songs.collectAsStateWithLifecycle()
    val isLoaded by viewModel.isLoaded.collectAsStateWithLifecycle()
    val sort by viewModel.sort.collectAsStateWithLifecycle()
    val currentId by viewModel.currentMediaId.collectAsStateWithLifecycle()
    val unplayable by UnplayableRegistry.ids.collectAsStateWithLifecycle()

    if (isLoaded && songs.isEmpty()) {
        EmptyState(
            title = stringResource(R.string.no_music_found),
            message = stringResource(R.string.no_music_tips),
            action = onAddFolder?.let { stringResource(R.string.add_folder_to_scan) to it },
            modifier = modifier,
        )
        return
    }

    val listState = rememberLazyListState()
    // Letters only make sense when the list is ordered by a text field.
    val letterIndex = remember(songs, sort) { letterIndexFor(songs, sort) }

    Column(modifier.fillMaxSize()) {
        SortBar(sort = sort, onSort = viewModel::setSort, trackCount = songs.size)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyColumn(state = listState, modifier = Modifier.fillMaxSize().testTag("song-list")) {
                itemsIndexed(songs, key = { _, song -> song.id }, contentType = { _, _ -> "song" }) { index, song ->
                    SongRow(
                        song = song,
                        isCurrent = currentId == "song:${song.id}",
                        unplayable = "song:${song.id}" in unplayable,
                        onClick = { viewModel.play(songs, index) },
                        onLongClick = { onSongLongClick(song) },
                    )
                }
            }
            if (letterIndex.isNotEmpty()) {
                FastScroller(
                    lazyListState = listState,
                    letters = letterIndex.keys.toList(),
                    indexOfLetter = { letterIndex.getValue(it) },
                    modifier = Modifier.align(Alignment.CenterEnd),
                )
            }
        }
    }
}

/** Maps each bucket letter to the index of its first song; empty for orderings without letters. */
private fun letterIndexFor(songs: List<Song>, sort: SongSort): Map<Char, Int> {
    val key: (Song) -> String = when (sort) {
        SongSort.TITLE -> { s -> s.title }
        SongSort.ARTIST -> { s -> s.artist }
        SongSort.ALBUM -> { s -> s.album }
        SongSort.DATE_ADDED -> return emptyMap()
    }
    val result = LinkedHashMap<Char, Int>()
    songs.forEachIndexed { index, song -> result.getOrPut(indexLetter(key(song))) { index } }
    return result
}

@Composable
internal fun SortBar(sort: SongSort, onSort: (SongSort) -> Unit, trackCount: Int) {
    var open by remember { mutableStateOf(false) }
    val titleCards = LocalOrnament.current.titleCards
    val sortLabel = stringResource(sort.labelRes())
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = if (titleCards) Arrangement.SpaceBetween else Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (titleCards) {
            Text(
                text = skinLabel(pluralStringResource(R.plurals.songs_count, trackCount, trackCount)),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            Text(
                text = skinLabel(sortLabel),
                modifier = Modifier.originalLabel(sortLabel),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box {
            if (titleCards) {
                val sortName = stringResource(R.string.sort)
                val kana = skinText("sort_kana", "").kana
                OutlinedButton(
                    onClick = { open = true },
                    shape = themedButtonShape(),
                    modifier = Modifier.semantics { contentDescription = "$sortName: $sortLabel" },
                ) {
                    Text(skinLabel(sortLabel), style = MaterialTheme.typography.labelLarge)
                    if (kana != null) {
                        Text(" $kana", style = MaterialTheme.typography.labelLarge, modifier = Modifier.clearAndSetSemantics {})
                    }
                }
            } else {
                IconButton(onClick = { open = true }) {
                    Icon(painterResource(R.drawable.ic_sort), contentDescription = stringResource(R.string.sort))
                }
            }
            DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
                SongSort.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(stringResource(option.labelRes())) },
                        onClick = {
                            open = false
                            onSort(option)
                        },
                    )
                }
            }
        }
    }
}

private fun SongSort.labelRes(): Int = when (this) {
    SongSort.TITLE -> R.string.sort_title
    SongSort.ARTIST -> R.string.sort_artist
    SongSort.ALBUM -> R.string.sort_album
    SongSort.DATE_ADDED -> R.string.sort_date_added
}
