package io.github.aceattacker77.nakedmusicplayer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.aceattacker77.nakedmusicplayer.R
import io.github.aceattacker77.nakedmusicplayer.library.model.UNKNOWN_ALBUM
import io.github.aceattacker77.nakedmusicplayer.library.model.UNKNOWN_ARTIST

/** The library model uses language-neutral constants for missing tags; the UI maps them to strings. */
@Composable
fun artistLabel(artist: String): String =
    if (artist == UNKNOWN_ARTIST) stringResource(R.string.unknown_artist) else artist

@Composable
fun albumLabel(album: String): String =
    if (album == UNKNOWN_ALBUM) stringResource(R.string.unknown_album) else album
