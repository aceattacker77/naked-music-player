package io.github.aceattacker77.nakedmusicplayer.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import io.github.aceattacker77.nakedmusicplayer.data.db.PlaylistEntity
import io.github.aceattacker77.nakedmusicplayer.library.model.Library

/** The browsable media tree exposed by the service (and, later, to Android Auto). */
object LibraryTree {
    const val ROOT = "root"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val PLAYLISTS = "playlists"
    const val SONGS = "songs"
    const val ALBUM_PREFIX = "album:"
    const val ARTIST_PREFIX = "artist:"
    const val PLAYLIST_PREFIX = "playlist:"
    const val SONG_PREFIX = "song:"

    /** Display names of the four root categories; the service supplies localized strings. */
    data class Labels(
        val albums: String = "Albums",
        val artists: String = "Artists",
        val playlists: String = "Playlists",
        val songs: String = "Songs",
    )

    fun children(
        parentId: String,
        library: Library,
        playlists: List<PlaylistEntity>,
        labels: Labels = Labels(),
        playlistSongIds: (Long) -> List<Long>,
    ): List<MediaItem>? = when {
        parentId == ROOT -> listOf(
            folder(ALBUMS, labels.albums),
            folder(ARTISTS, labels.artists),
            folder(PLAYLISTS, labels.playlists),
            folder(SONGS, labels.songs),
        )
        parentId == ALBUMS -> library.albums.map(::albumItem)
        parentId == ARTISTS -> library.artists.map(::artistItem)
        parentId == PLAYLISTS -> playlists.map(::playlistItem)
        parentId == SONGS -> library.songs.map { it.toMediaItem() }
        parentId.startsWith(ALBUM_PREFIX) ->
            library.albums.firstOrNull { it.id == idOf(parentId) }?.songs?.map { it.toMediaItem() }
        parentId.startsWith(ARTIST_PREFIX) ->
            library.artists.firstOrNull { it.id == idOf(parentId) }?.albums?.map(::albumItem)
        parentId.startsWith(PLAYLIST_PREFIX) -> {
            val id = idOf(parentId)
            if (id == null || playlists.none { it.id == id }) {
                null
            } else {
                val byId = library.songs.associateBy { it.id }
                playlistSongIds(id).mapNotNull { byId[it]?.toMediaItem() }
            }
        }
        else -> null
    }

    fun item(mediaId: String, library: Library, labels: Labels = Labels()): MediaItem? = when {
        mediaId == ROOT -> folder(ROOT, "")
        mediaId == ALBUMS -> folder(ALBUMS, labels.albums)
        mediaId == ARTISTS -> folder(ARTISTS, labels.artists)
        mediaId == PLAYLISTS -> folder(PLAYLISTS, labels.playlists)
        mediaId == SONGS -> folder(SONGS, labels.songs)
        mediaId.startsWith(SONG_PREFIX) -> songIdOf(mediaId)?.let { id ->
            library.songs.firstOrNull { it.id == id }?.toMediaItem()
        }
        mediaId.startsWith(ALBUM_PREFIX) ->
            library.albums.firstOrNull { it.id == idOf(mediaId) }?.let(::albumItem)
        mediaId.startsWith(ARTIST_PREFIX) ->
            library.artists.firstOrNull { it.id == idOf(mediaId) }?.let(::artistItem)
        else -> null
    }

    fun songIdOf(mediaId: String): Long? =
        if (mediaId.startsWith(SONG_PREFIX)) idOf(mediaId) else null

    private fun idOf(mediaId: String): Long? = mediaId.substringAfter(':').toLongOrNull()

    private fun folder(id: String, title: String, subtitle: String? = null, artworkAlbumId: Long? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(id)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title)
                    .setSubtitle(subtitle)
                    .setArtworkUri(artworkAlbumId?.let(::albumArtUri))
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

    private fun albumItem(album: io.github.aceattacker77.nakedmusicplayer.library.model.Album) =
        folder("$ALBUM_PREFIX${album.id}", album.title, album.artist, album.id)

    private fun artistItem(artist: io.github.aceattacker77.nakedmusicplayer.library.model.Artist) =
        folder("$ARTIST_PREFIX${artist.id}", artist.name)

    private fun playlistItem(playlist: PlaylistEntity) =
        folder("$PLAYLIST_PREFIX${playlist.id}", playlist.name)
}
