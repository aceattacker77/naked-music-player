package io.github.aceattacker77.nakedmusicplayer.playback

import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.aceattacker77.nakedmusicplayer.library.model.Song

internal fun albumArtUri(albumId: Long): Uri =
    Uri.parse("content://media/external/audio/albumart/$albumId")

fun Song.toMediaItem(): MediaItem = MediaItem.Builder()
    .setMediaId("${LibraryTree.SONG_PREFIX}$id")
    .setUri(uri)
    .setMediaMetadata(
        MediaMetadata.Builder()
            .setTitle(title)
            .setArtist(artist)
            .setAlbumTitle(album)
            .setTrackNumber(trackNumber)
            .setDiscNumber(discNumber)
            .setDurationMs(durationMs)
            .setArtworkUri(albumArtUri(albumId))
            .setIsPlayable(true)
            .setIsBrowsable(false)
            .build(),
    )
    .build()
