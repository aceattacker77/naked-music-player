package io.github.aceattacker77.nakedmusicplayer.playback

import androidx.media3.common.MediaItem
import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.data.db.PlaylistEntity
import io.github.aceattacker77.nakedmusicplayer.library.LibraryBuilder
import io.github.aceattacker77.nakedmusicplayer.library.model.AudioRow
import io.github.aceattacker77.nakedmusicplayer.library.model.LibraryFilter
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LibraryTreeTest {
    private fun row(id: Long, album: String, albumId: Long, artist: String, artistId: Long, track: Int) =
        AudioRow(id, "T$id", artist, album, albumId, artistId, 200_000, track, 0, "Music/", "f$id.mp3", null)

    private val library = LibraryBuilder.build(
        listOf(
            row(1, "Alpha", 10, "Ann", 100, 2),
            row(2, "Alpha", 10, "Ann", 100, 1),
            row(3, "Beta", 11, "Ann", 100, 1),
            row(4, "Gamma", 12, "Bob", 101, 1),
        ),
        LibraryFilter(),
    )

    private fun ids(items: List<MediaItem>?) = items!!.map { it.mediaId }

    @Test fun root_hasFourCategoriesInOrder() {
        val items = LibraryTree.children(LibraryTree.ROOT, library, emptyList()) { emptyList() }!!
        assertThat(items.map { it.mediaId })
            .containsExactly(LibraryTree.ALBUMS, LibraryTree.ARTISTS, LibraryTree.PLAYLISTS, LibraryTree.SONGS).inOrder()
        assertThat(items.all { it.mediaMetadata.isBrowsable == true && it.mediaMetadata.isPlayable == false }).isTrue()
    }

    @Test fun album_childrenAreTracksInAlbumOrder() {
        assertThat(ids(LibraryTree.children("album:10", library, emptyList()) { emptyList() }))
            .containsExactly("song:2", "song:1").inOrder()
    }

    @Test fun artist_childrenAreAlbums() {
        assertThat(ids(LibraryTree.children("artist:100", library, emptyList()) { emptyList() }))
            .containsExactly("album:10", "album:11").inOrder()
    }

    @Test fun albums_artists_songsListings() {
        assertThat(ids(LibraryTree.children(LibraryTree.ALBUMS, library, emptyList()) { emptyList() }))
            .containsExactly("album:10", "album:11", "album:12")
        assertThat(ids(LibraryTree.children(LibraryTree.ARTISTS, library, emptyList()) { emptyList() }))
            .containsExactly("artist:100", "artist:101")
        assertThat(ids(LibraryTree.children(LibraryTree.SONGS, library, emptyList()) { emptyList() }))
            .containsExactly("song:1", "song:2", "song:3", "song:4")
    }

    @Test fun playlists_listsPlaylistsAsBrowsable() {
        val items = LibraryTree.children(LibraryTree.PLAYLISTS, library, listOf(PlaylistEntity(7, "Mix", 0, 0))) { emptyList() }!!
        assertThat(items.map { it.mediaId }).containsExactly("playlist:7")
        assertThat(items.single().mediaMetadata.isBrowsable).isTrue()
    }

    @Test fun playlist_childrenFollowPositions_skippingMissingSongs() {
        val playlists = listOf(PlaylistEntity(7, "Mix", 0, 0))
        val items = LibraryTree.children("playlist:7", library, playlists) { listOf(4L, 999L, 1L, 4L) }
        assertThat(ids(items)).containsExactly("song:4", "song:1", "song:4").inOrder()
    }

    @Test fun unknownParent_returnsNull() {
        assertThat(LibraryTree.children("nope", library, emptyList()) { emptyList() }).isNull()
        assertThat(LibraryTree.children("album:999", library, emptyList()) { emptyList() }).isNull()
        assertThat(LibraryTree.children("playlist:5", library, emptyList()) { emptyList() }).isNull()
    }

    @Test fun songIdOf_parses() {
        assertThat(LibraryTree.songIdOf("song:42")).isEqualTo(42L)
        assertThat(LibraryTree.songIdOf("album:1")).isNull()
        assertThat(LibraryTree.songIdOf("song:abc")).isNull()
    }

    @Test fun item_resolvesSongsAndFolders() {
        assertThat(LibraryTree.item("song:3", library)!!.mediaMetadata.title.toString()).isEqualTo("T3")
        assertThat(LibraryTree.item("album:11", library)!!.mediaMetadata.isBrowsable).isTrue()
        assertThat(LibraryTree.item(LibraryTree.ROOT, library)!!.mediaMetadata.isBrowsable).isTrue()
        assertThat(LibraryTree.item("song:999", library)).isNull()
    }

    @Test fun songToMediaItem_carriesMetadataAndArtwork() {
        val item = library.songs.first { it.id == 1L }.toMediaItem()
        assertThat(item.mediaId).isEqualTo("song:1")
        assertThat(item.localConfiguration!!.uri.toString()).isEqualTo("content://media/external/audio/media/1")
        assertThat(item.mediaMetadata.artist.toString()).isEqualTo("Ann")
        assertThat(item.mediaMetadata.albumTitle.toString()).isEqualTo("Alpha")
        assertThat(item.mediaMetadata.trackNumber).isEqualTo(2)
        assertThat(item.mediaMetadata.discNumber).isEqualTo(1)
        assertThat(item.mediaMetadata.artworkUri.toString()).isEqualTo("content://media/external/audio/albumart/10")
        assertThat(item.mediaMetadata.isPlayable).isTrue()
        assertThat(item.mediaMetadata.isBrowsable).isFalse()
    }
}
