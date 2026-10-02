package io.github.aceattacker77.nakedmusicplayer.library

import com.google.common.truth.Truth.assertThat
import io.github.aceattacker77.nakedmusicplayer.library.model.Song
import org.junit.Test

class SongSortingTest {
    private fun song(
        id: Long,
        title: String = "t$id",
        artist: String = "a",
        album: String = "al",
        track: Int = 1,
        added: Long = 0,
    ) = Song(
        id = id, uri = "content://media/external/audio/media/$id", title = title, artist = artist,
        album = album, albumId = 1, artistId = 1, durationMs = 100_000, discNumber = 1,
        trackNumber = track, dateAddedSec = added, relativePath = "Music/", displayName = "f$id.mp3", year = null,
    )

    @Test fun title_isCaseInsensitive() {
        val sorted = listOf(song(1, "b"), song(2, "A"), song(3, "c")).sortedWith(SongSort.TITLE)
        assertThat(sorted.map { it.title }).containsExactly("A", "b", "c").inOrder()
    }

    @Test fun artist_thenAlbum_thenTrack() {
        val sorted = listOf(
            song(1, artist = "B", album = "x", track = 1),
            song(2, artist = "A", album = "z", track = 1),
            song(3, artist = "A", album = "y", track = 2),
            song(4, artist = "A", album = "y", track = 1),
        ).sortedWith(SongSort.ARTIST)
        assertThat(sorted.map { it.id }).containsExactly(4L, 3L, 2L, 1L).inOrder()
    }

    @Test fun dateAdded_isNewestFirst() {
        val sorted = listOf(song(1, added = 10), song(2, added = 30), song(3, added = 20)).sortedWith(SongSort.DATE_ADDED)
        assertThat(sorted.map { it.id }).containsExactly(2L, 3L, 1L).inOrder()
    }
}
